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
import androidx.room.migration.Migration
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

/** Her note for one machine. Shared by every plan that uses the exercise. */
@Entity(tableName = "machine_settings")
data class MachineSettingsEntity(
    @PrimaryKey val exerciseId: String,
    val note: String,
)

/** A photo or video of a machine, e.g. of the seat setting or a demonstration. */
@Entity(tableName = "machine_media", indices = [Index("exerciseId")])
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val path: String,
    /** [MediaEntity.PHOTO] or [MediaEntity.VIDEO]. */
    val type: String,
    /** What it shows, e.g. "Sitzposition". May be empty. */
    val caption: String,
    val position: Int,
    /** Still picture of a video, for lists. */
    val thumbPath: String? = null,
) {
    companion object {
        const val PHOTO = "PHOTO"
        const val VIDEO = "VIDEO"
    }
}

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

    @Query("SELECT * FROM machine_media ORDER BY exerciseId, position, id")
    fun allMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM machine_media WHERE exerciseId = :id ORDER BY position, id")
    suspend fun media(id: String): List<MediaEntity>

    @Query("SELECT * FROM machine_media WHERE id = :mediaId")
    suspend fun mediaItem(mediaId: Long): MediaEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM machine_media WHERE exerciseId = :id")
    suspend fun maxMediaPosition(id: String): Int

    @Insert
    suspend fun insertMedia(media: MediaEntity): Long

    @Query("UPDATE machine_media SET caption = :caption WHERE id = :mediaId")
    suspend fun setCaption(mediaId: Long, caption: String)

    @Update
    suspend fun updateMedia(media: List<MediaEntity>)

    @Query("DELETE FROM machine_media WHERE id = :mediaId")
    suspend fun deleteMedia(mediaId: Long)
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
        MediaEntity::class,
    ],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plans(): PlanDao
    abstract fun settings(): SettingsDao
    abstract fun customExercises(): CustomExerciseDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "training.db")
                .addCallback(SeedExamplePlan)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/**
 * Version 2 allows several photos and videos per machine. The single photo of
 * version 1 moves into the new table, so nothing she already took is lost.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `machine_media` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`exerciseId` TEXT NOT NULL, `path` TEXT NOT NULL, `type` TEXT NOT NULL, `caption` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, `thumbPath` TEXT)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_machine_media_exerciseId` ON `machine_media` (`exerciseId`)")
        db.execSQL(
            "INSERT INTO machine_media (exerciseId, path, type, caption, position) " +
                "SELECT exerciseId, photoPath, 'PHOTO', '', 0 FROM machine_settings WHERE photoPath IS NOT NULL",
        )
        // SQLite cannot drop a column on older Android versions: rebuild the table.
        db.execSQL(
            "CREATE TABLE `machine_settings_new` (`exerciseId` TEXT NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`exerciseId`))",
        )
        db.execSQL("INSERT INTO machine_settings_new (exerciseId, note) SELECT exerciseId, note FROM machine_settings")
        db.execSQL("DROP TABLE machine_settings")
        db.execSQL("ALTER TABLE machine_settings_new RENAME TO machine_settings")
    }
}

/** A first plan, so the app is not empty when she opens it for the first time. */
private object SeedExamplePlan : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO plans (id, name, position) VALUES (1, 'Ganzkörper (Beispiel)', 0)")
        val items = listOf(
            Triple("fahrrad", 1, 10),
            Triple("beinpresse", 3, 15),
            Triple("brustpresse", 3, 15),
            Triple("latzug", 3, 15),
            Triple("rudern", 3, 15),
            Triple("beinstrecker", 3, 15),
            Triple("beinbeuger_sitzend", 3, 15),
            Triple("rueckenstrecker", 3, 15),
            Triple("bauchmaschine", 3, 15),
        )
        items.forEachIndexed { i, (id, sets, amount) ->
            db.execSQL(
                "INSERT INTO plan_items (planId, exerciseId, position, sets, amount, restSeconds) VALUES (1, ?, ?, ?, ?, 120)",
                arrayOf<Any>(id, i, sets, amount),
            )
        }
    }
}
