package de.meintraining.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "plan_items",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId")],
)
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    /** Id from the built-in catalogue, or "eigene-<n>" for her own exercises. */
    val exerciseId: String,
    val position: Int,
    val sets: Int,
    /** Repetitions, seconds or minutes, depending on the exercise. */
    val amount: Int,
    val restSeconds: Int,
)

/** Her notes for one machine. Shared by every plan that uses the exercise. */
@Entity(tableName = "machine_settings")
data class MachineSettingsEntity(
    @PrimaryKey val exerciseId: String,
    val photoPath: String?,
    val note: String,
)

@Entity(tableName = "setting_values", indices = [Index("exerciseId")])
data class SettingValueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val label: String,
    val value: String,
    val position: Int,
)

@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Name of a [de.meintraining.app.catalog.Kind]. */
    val kind: String,
    /** Comma-separated [de.meintraining.app.catalog.Muscle] names. */
    val muscles: String,
)

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY position, id")
    fun plans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plan_items ORDER BY planId, position, id")
    fun allItems(): Flow<List<PlanItemEntity>>

    @Query("SELECT * FROM plan_items WHERE planId = :planId ORDER BY position, id")
    suspend fun items(planId: Long): List<PlanItemEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM plans")
    suspend fun maxPlanPosition(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM plan_items WHERE planId = :planId")
    suspend fun maxItemPosition(planId: Long): Int

    @Insert
    suspend fun insertPlan(plan: PlanEntity): Long

    @Query("UPDATE plans SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    @Insert
    suspend fun insertItems(items: List<PlanItemEntity>)

    @Update
    suspend fun updateItems(items: List<PlanItemEntity>)

    @Query("UPDATE plan_items SET sets = :sets, amount = :amount, restSeconds = :rest WHERE id = :id")
    suspend fun updateItem(id: Long, sets: Int, amount: Int, rest: Int)

    @Query("DELETE FROM plan_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM plan_items WHERE exerciseId = :exerciseId")
    suspend fun deleteItemsFor(exerciseId: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM machine_settings")
    fun all(): Flow<List<MachineSettingsEntity>>

    @Query("SELECT * FROM setting_values ORDER BY exerciseId, position, id")
    fun allValues(): Flow<List<SettingValueEntity>>

    @Query("SELECT * FROM machine_settings WHERE exerciseId = :id")
    suspend fun get(id: String): MachineSettingsEntity?

    @Upsert
    suspend fun upsert(settings: MachineSettingsEntity)

    @Query("DELETE FROM machine_settings WHERE exerciseId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM setting_values WHERE exerciseId = :id")
    suspend fun clearValues(id: String)

    @Insert
    suspend fun insertValues(values: List<SettingValueEntity>)

    @Transaction
    suspend fun replaceValues(id: String, values: List<SettingValueEntity>) {
        clearValues(id)
        insertValues(values)
    }
}

@Dao
interface CustomExerciseDao {
    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<CustomExerciseEntity>>

    @Insert
    suspend fun insert(exercise: CustomExerciseEntity): Long

    @Query("DELETE FROM custom_exercises WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(
    entities = [
        PlanEntity::class,
        PlanItemEntity::class,
        MachineSettingsEntity::class,
        SettingValueEntity::class,
        CustomExerciseEntity::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plans(): PlanDao
    abstract fun settings(): SettingsDao
    abstract fun customExercises(): CustomExerciseDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "training.db")
                .addCallback(SeedExamplePlan)
                .build()
    }
}

/** A first plan, so the app is not empty when she opens it for the first time. */
private object SeedExamplePlan : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO plans (id, name, position) VALUES (1, 'Ganzkörper (Beispiel)', 0)")
        val items = listOf(
            Triple("fahrrad", 1, 10),
            Triple("beinpresse", 2, 12),
            Triple("brustpresse", 2, 12),
            Triple("latzug", 2, 12),
            Triple("rudern", 2, 12),
            Triple("beinstrecker", 2, 12),
            Triple("beinbeuger_sitzend", 2, 12),
            Triple("rueckenstrecker", 2, 12),
            Triple("bauchmaschine", 2, 12),
        )
        items.forEachIndexed { i, (id, sets, amount) ->
            db.execSQL(
                "INSERT INTO plan_items (planId, exerciseId, position, sets, amount, restSeconds) VALUES (1, ?, ?, ?, ?, 60)",
                arrayOf<Any>(id, i, sets, amount),
            )
        }
    }
}
