package bg.newlook.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC") fun tasks(): Flow<List<Task>>

    @Query("SELECT * FROM projects") fun projects(): Flow<List<Project>>

    @Query("SELECT * FROM events ORDER BY start") fun events(): Flow<List<Event>>

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC") fun notes(): Flow<List<Note>>

    @Query("SELECT * FROM favorites") fun favorites(): Flow<List<Favorite>>

    @Query("SELECT * FROM templates") fun templates(): Flow<List<UserTemplate>>

    @Query("SELECT * FROM tasks") suspend fun allTasks(): List<Task>

    @Query("SELECT * FROM projects") suspend fun allProjects(): List<Project>

    @Query("SELECT * FROM events") suspend fun allEvents(): List<Event>

    @Query("SELECT * FROM notes") suspend fun allNotes(): List<Note>

    @Query("SELECT * FROM series") suspend fun allSeries(): List<Series>

    @Query("SELECT * FROM templates") suspend fun allTemplates(): List<UserTemplate>

    @Query("SELECT * FROM favorites") suspend fun allFavorites(): List<Favorite>

    @Upsert suspend fun task(value: Task)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun occurrence(value: Task)

    @Upsert suspend fun project(value: Project)

    @Upsert suspend fun event(value: Event)

    @Upsert suspend fun note(value: Note)

    @Upsert suspend fun series(value: Series)

    @Upsert suspend fun template(value: UserTemplate)

    @Upsert suspend fun favorite(value: Favorite)

    @Delete suspend fun deleteTask(value: Task)

    @Delete suspend fun deleteProject(value: Project)

    @Delete suspend fun deleteEvent(value: Event)

    @Delete suspend fun deleteNote(value: Note)

    @Delete suspend fun deleteFavorite(value: Favorite)

    @Delete suspend fun deleteTemplate(value: UserTemplate)

    @Query("DELETE FROM tasks") suspend fun clearTasks()

    @Query("DELETE FROM projects") suspend fun clearProjects()

    @Query("DELETE FROM events") suspend fun clearEvents()

    @Query("DELETE FROM notes") suspend fun clearNotes()

    @Query("DELETE FROM series") suspend fun clearSeries()

    @Query("DELETE FROM favorites") suspend fun clearFavorites()

    @Query("DELETE FROM templates") suspend fun clearTemplates()
}

@Database(
    entities =
        [
            Task::class,
            Project::class,
            Event::class,
            Note::class,
            Series::class,
            Favorite::class,
            UserTemplate::class,
        ],
    version = 2,
    exportSchema = true,
)
abstract class WorkspaceDb : RoomDatabase() {
    abstract fun dao(): WorkspaceDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    for (table in listOf("tasks", "projects", "events", "notes")) db.execSQL(
                        "ALTER TABLE $table ADD COLUMN demo INTEGER NOT NULL DEFAULT 0"
                    )
                    db.execSQL(
                        "ALTER TABLE series ADD COLUMN excludedKeys TEXT NOT NULL DEFAULT ''"
                    )
                }
            }

        fun open(context: Context) =
            Room.databaseBuilder(context, WorkspaceDb::class.java, "workspace.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
