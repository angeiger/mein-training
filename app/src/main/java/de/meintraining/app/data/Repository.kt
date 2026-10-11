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

/** A photo or video of a machine. */
data class MachineMedia(
    val id: Long,
    val path: String,
    val isVideo: Boolean,
    val caption: String,
    /** Picture to show in lists: the photo itself, or a still from the video. */
    val preview: String?,
)

data class MachineSettings(
    val media: List<MachineMedia> = emptyList(),
    val note: String = "",
    val values: List<SettingValue> = emptyList(),
) {
    /** Values she actually filled in. */
    val filled: List<SettingValue> get() = values.filter { it.value.isNotBlank() }
    val isEmpty: Boolean get() = media.isEmpty() && note.isBlank() && filled.isEmpty()
}

/**
 * Single source of truth for the screens. Writes run in the application scope, so
 * they finish even if the screen that started them is closed straight away.
 */
class Repository(
    private val db: AppDatabase,
    private val files: MediaFiles,
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
        combine(settingsDao.all(), settingsDao.allValues(), settingsDao.allMedia()) { settings, values, media ->
            val notes = settings.associate { it.exerciseId to it.note }
            val valuesBy = values.groupBy { it.exerciseId }
            val mediaBy = media.groupBy { it.exerciseId }
            (notes.keys + valuesBy.keys + mediaBy.keys).associateWith { id ->
                MachineSettings(
                    media = mediaBy[id].orEmpty().map { it.toMedia() },
                    note = notes[id].orEmpty(),
                    values = valuesBy[id].orEmpty().map { SettingValue(it.label, it.value) },
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
        settingsDao.upsert(MachineSettingsEntity(exerciseId, note.trim()))
        settingsDao.replaceValues(
            exerciseId,
            values.filter { it.label.isNotBlank() }.mapIndexed { i, v ->
                SettingValueEntity(exerciseId = exerciseId, label = v.label.trim(), value = v.value.trim(), position = i)
            },
        )
    }

    /** Adds an already stored photo or video at the end of the machine's list. Returns its id. */
    suspend fun addMedia(exerciseId: String, stored: MediaFiles.Stored, caption: String = ""): Long =
        settingsDao.insertMedia(
            MediaEntity(
                exerciseId = exerciseId,
                path = stored.path,
                type = if (stored.isVideo) MediaEntity.VIDEO else MediaEntity.PHOTO,
                caption = caption.trim(),
                position = settingsDao.maxMediaPosition(exerciseId) + 1,
                thumbPath = stored.thumbPath,
            ),
        )

    fun setCaption(mediaId: Long, caption: String) = scope.launch { settingsDao.setCaption(mediaId, caption.trim()) }

    /** Moves a photo or video one place earlier (-1) or later (+1). */
    fun moveMedia(exerciseId: String, mediaId: Long, direction: Int) = scope.launch {
        val list = settingsDao.media(exerciseId).toMutableList()
        val from = list.indexOfFirst { it.id == mediaId }
        val to = from + direction
        if (from < 0 || to !in list.indices) return@launch
        list.add(to, list.removeAt(from))
        settingsDao.updateMedia(list.mapIndexed { i, m -> m.copy(position = i) })
    }

    fun deleteMedia(mediaId: Long) = scope.launch {
        val m = settingsDao.mediaItem(mediaId) ?: return@launch
        settingsDao.deleteMedia(mediaId)
        files.delete(m.path)
        m.thumbPath?.let { files.delete(it) }
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
        for (m in settingsDao.media(exerciseId)) {
            settingsDao.deleteMedia(m.id)
            files.delete(m.path)
            m.thumbPath?.let { files.delete(it) }
        }
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

private fun MediaEntity.toMedia() = MachineMedia(
    id = id,
    path = path,
    isVideo = type == MediaEntity.VIDEO,
    caption = caption,
    preview = if (type == MediaEntity.VIDEO) thumbPath else path,
)

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
