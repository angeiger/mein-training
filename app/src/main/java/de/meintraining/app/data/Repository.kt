package de.meintraining.app.data

import de.meintraining.app.catalog.Catalog
import de.meintraining.app.catalog.Category
import de.meintraining.app.catalog.Exercise
import de.meintraining.app.catalog.Kind
import de.meintraining.app.catalog.Muscle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanItem(
    val id: Long,
    val exercise: Exercise,
    val sets: Int,
    val amount: Int,
    val restSeconds: Int,
) {
    /** "2 × 12 Wdh." or "10 Min." */
    val summary: String
        get() = if (exercise.kind == Kind.CARDIO) exercise.kind.amountShort(amount)
        else "$sets × ${exercise.kind.amountShort(amount)}"
}

data class Plan(val id: Long, val name: String, val items: List<PlanItem>) {
    /** Rough duration in minutes, rounded to 5. */
    val minutes: Int
        get() {
            val seconds = items.sumOf { item ->
                val work = when (item.exercise.kind) {
                    Kind.REPS -> item.sets * (item.amount * 4 + item.restSeconds)
                    Kind.HOLD -> item.sets * (item.amount + item.restSeconds)
                    Kind.CARDIO -> item.amount * 60
                }
                work + 60
            }
            return ((seconds / 60 + 4) / 5 * 5).coerceAtLeast(5)
        }
}

data class SettingValue(val label: String, val value: String)

data class MachineSettings(
    val photoPath: String? = null,
    val note: String = "",
    val values: List<SettingValue> = emptyList(),
) {
    /** Values she actually filled in. */
    val filled: List<SettingValue> get() = values.filter { it.value.isNotBlank() }
    val isEmpty: Boolean get() = photoPath == null && note.isBlank() && filled.isEmpty()
}

/**
 * Single source of truth for the screens. Writes run in the application scope, so
 * they finish even if the screen that started them is closed straight away.
 */
class Repository(
    private val db: AppDatabase,
    private val photos: PhotoStore,
    private val scope: CoroutineScope,
) {
    private val planDao = db.plans()
    private val settingsDao = db.settings()
    private val customDao = db.customExercises()

    val exercises: StateFlow<List<Exercise>> = customDao.all()
        .map { list -> Catalog.exercises + list.map { it.toExercise() } }
        .stateIn(scope, SharingStarted.Eagerly, Catalog.exercises)

    val plans: Flow<List<Plan>> = combine(planDao.plans(), planDao.allItems(), exercises) { plans, items, all ->
        val byId = all.associateBy { it.id }
        val grouped = items.groupBy { it.planId }
        plans.map { p ->
            Plan(
                id = p.id,
                name = p.name,
                items = grouped[p.id].orEmpty().mapNotNull { it.toItem(byId) },
            )
        }
    }

    fun plan(id: Long): Flow<Plan?> = plans.map { list -> list.firstOrNull { it.id == id } }

    val settings: Flow<Map<String, MachineSettings>> =
        combine(settingsDao.all(), settingsDao.allValues()) { settings, values ->
            val grouped = values.groupBy { it.exerciseId }
            val ids = settings.map { it.exerciseId } + grouped.keys
            ids.toSet().associateWith { id ->
                val s = settings.firstOrNull { it.exerciseId == id }
                MachineSettings(
                    photoPath = s?.photoPath,
                    note = s?.note.orEmpty(),
                    values = grouped[id].orEmpty().map { SettingValue(it.label, it.value) },
                )
            }
        }

    fun settings(exerciseId: String): Flow<MachineSettings> =
        settings.map { it[exerciseId] ?: MachineSettings() }

    // ---- Plans ------------------------------------------------------------------------

    suspend fun createPlan(name: String): Long {
        val position = planDao.maxPlanPosition() + 1
        return planDao.insertPlan(PlanEntity(name = name.trim().ifEmpty { "Neuer Plan" }, position = position))
    }

    fun renamePlan(id: Long, name: String) = scope.launch { planDao.rename(id, name) }

    fun deletePlan(id: Long) = scope.launch { planDao.deletePlan(id) }

    fun addExercises(planId: Long, exercises: List<Exercise>) = scope.launch {
        val start = planDao.maxItemPosition(planId) + 1
        planDao.insertItems(
            exercises.mapIndexed { i, e ->
                PlanItemEntity(
                    planId = planId,
                    exerciseId = e.id,
                    position = start + i,
                    sets = e.sets,
                    amount = e.amount,
                    restSeconds = 60,
                )
            },
        )
    }

    fun updateItem(id: Long, sets: Int, amount: Int, restSeconds: Int) =
        scope.launch { planDao.updateItem(id, sets, amount, restSeconds) }

    fun removeItem(id: Long) = scope.launch { planDao.deleteItem(id) }

    /** Moves an item one step up (-1) or down (+1). */
    fun moveItem(planId: Long, itemId: Long, direction: Int) = scope.launch {
        val items = planDao.items(planId).toMutableList()
        val from = items.indexOfFirst { it.id == itemId }
        val to = from + direction
        if (from < 0 || to !in items.indices) return@launch
        val moved = items.removeAt(from)
        items.add(to, moved)
        planDao.updateItems(items.mapIndexed { i, it -> it.copy(position = i) })
    }

    // ---- Machine settings -------------------------------------------------------------------

    fun saveSettings(exerciseId: String, note: String, values: List<SettingValue>) = scope.launch {
        val current = settingsDao.get(exerciseId)
        settingsDao.upsert(MachineSettingsEntity(exerciseId, current?.photoPath, note.trim()))
        settingsDao.replaceValues(
            exerciseId,
            values.filter { it.label.isNotBlank() }.mapIndexed { i, v ->
                SettingValueEntity(exerciseId = exerciseId, label = v.label.trim(), value = v.value.trim(), position = i)
            },
        )
    }

    /** Stores a new photo for the machine and deletes the one it replaces. */
    fun setPhoto(exerciseId: String, path: String?) = scope.launch {
        val current = settingsDao.get(exerciseId)
        if (current?.photoPath != null && current.photoPath != path) photos.delete(current.photoPath)
        settingsDao.upsert(MachineSettingsEntity(exerciseId, path, current?.note.orEmpty()))
    }

    // ---- Her own exercises ----------------------------------------------------------------------

    /** Creates her own exercise and, if [planId] is given, adds it to that plan. */
    suspend fun createCustomExercise(name: String, kind: Kind, muscles: Set<Muscle>, planId: Long?): String {
        val entity = CustomExerciseEntity(name = name.trim(), kind = kind.name, muscles = muscles.joinToString(",") { it.name })
        val id = customDao.insert(entity)
        if (planId != null) {
            val exercise = entity.copy(id = id).toExercise()
            planDao.insertItems(
                listOf(
                    PlanItemEntity(
                        planId = planId,
                        exerciseId = exercise.id,
                        position = planDao.maxItemPosition(planId) + 1,
                        sets = exercise.sets,
                        amount = exercise.amount,
                        restSeconds = 60,
                    ),
                ),
            )
        }
        return customId(id)
    }

    fun deleteCustomExercise(exerciseId: String) = scope.launch {
        val id = exerciseId.removePrefix(CUSTOM_PREFIX).toLongOrNull() ?: return@launch
        planDao.deleteItemsFor(exerciseId)
        settingsDao.get(exerciseId)?.photoPath?.let { photos.delete(it) }
        settingsDao.delete(exerciseId)
        settingsDao.clearValues(exerciseId)
        customDao.delete(id)
    }

    private fun PlanItemEntity.toItem(byId: Map<String, Exercise>): PlanItem? {
        val e = byId[exerciseId] ?: return null
        return PlanItem(id, e, sets, amount, restSeconds)
    }

    companion object {
        const val CUSTOM_PREFIX = "eigene-"
        fun customId(id: Long) = "$CUSTOM_PREFIX$id"
    }
}

private fun CustomExerciseEntity.toExercise(): Exercise {
    val kind = runCatching { Kind.valueOf(kind) }.getOrDefault(Kind.REPS)
    return Exercise(
        id = Repository.customId(id),
        name = name,
        category = Category.EIGENE,
        kind = kind,
        primary = muscles.split(',').mapNotNull { m -> runCatching { Muscle.valueOf(m) }.getOrNull() },
        steps = emptyList(),
        settings = if (kind == Kind.CARDIO) listOf("Stufe / Widerstand") else listOf("Sitzhöhe", "Gewicht"),
        custom = true,
    )
}
