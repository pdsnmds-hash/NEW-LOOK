package bg.newlook.data

import androidx.room.*
import java.util.UUID

fun uuid(): String = UUID.randomUUID().toString()

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String = uuid(),
    val title: String = "",
    val description: String = "",
    val color: Int = 0,
    val target: String? = null,
    val archived: Boolean = false,
    val demo: Boolean = false,
)

@Entity(
    tableName = "tasks",
    foreignKeys =
        [
            ForeignKey(
                entity = Project::class,
                parentColumns = ["id"],
                childColumns = ["projectId"],
                onDelete = ForeignKey.SET_NULL,
            )
        ],
    indices = [Index("projectId"), Index(value = ["seriesId", "occurrenceKey"], unique = true)],
)
data class Task(
    @PrimaryKey val id: String = uuid(),
    val title: String = "",
    val description: String = "",
    val projectId: String? = null,
    val status: String = "todo",
    val priority: Int = 1,
    val due: String? = null,
    val time: String? = null,
    val tags: String = "",
    val subtasks: String = "[]",
    val reminder: Int? = null,
    val seriesId: String? = null,
    val occurrenceKey: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val demo: Boolean = false,
)

data class Subtask(val title: String, val done: Boolean = false, val id: String = uuid())

@Entity(
    tableName = "events",
    foreignKeys =
        [
            ForeignKey(
                entity = Project::class,
                parentColumns = ["id"],
                childColumns = ["projectId"],
                onDelete = ForeignKey.SET_NULL,
            )
        ],
    indices = [Index("projectId")],
)
data class Event(
    @PrimaryKey val id: String = uuid(),
    val title: String = "",
    val start: String = "",
    val end: String = "",
    val allDay: Boolean = false,
    val zone: String = java.time.ZoneId.systemDefault().id,
    val location: String = "",
    val notes: String = "",
    val projectId: String? = null,
    val reminder: Int? = null,
    val demo: Boolean = false,
)

@Entity(
    tableName = "notes",
    foreignKeys =
        [
            ForeignKey(
                entity = Project::class,
                parentColumns = ["id"],
                childColumns = ["projectId"],
                onDelete = ForeignKey.SET_NULL,
            )
        ],
    indices = [Index("projectId")],
)
data class Note(
    @PrimaryKey val id: String = uuid(),
    val title: String = "",
    val body: String = "",
    val tags: String = "",
    val pinned: Boolean = false,
    val projectId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val demo: Boolean = false,
)

@Entity(tableName = "series")
data class Series(
    @PrimaryKey val id: String = uuid(),
    val prototype: String = "",
    val frequency: String = "daily",
    val interval: Int = 1,
    val weekdays: String = "1",
    val start: String = "",
    val until: String? = null,
    val limit: Int? = null,
    val zone: String = java.time.ZoneId.systemDefault().id,
    val generatedThrough: String? = null,
    val active: Boolean = true,
    @ColumnInfo(defaultValue = "''") val excludedKeys: String = "",
)

@Entity(tableName = "favorites", primaryKeys = ["type", "targetId"])
data class Favorite(val type: String, val targetId: String)

@Entity(tableName = "templates")
data class UserTemplate(
    @PrimaryKey val id: String = uuid(),
    val title: String = "",
    val description: String = "",
    val steps: String = "[]",
)

