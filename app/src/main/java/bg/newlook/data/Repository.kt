package bg.newlook.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import bg.newlook.BuildConfig
import bg.newlook.domain.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

val Context.settings by preferencesDataStore("settings")

data class Settings(
    val theme: String = "system",
    val onboarded: Boolean = false,
    val sort: String = "due",
    val weekStart: Int = 1,
    val reminder: Int = 15,
    val agenda: Boolean = false,
    val filters: String = "",
)

data class Workspace(
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val events: List<Event> = emptyList(),
    val notes: List<Note> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    val templates: List<UserTemplate> = emptyList(),
)

class Repository(
    val context: Context,
    val db: WorkspaceDb,
    val scope: CoroutineScope,
    val clock: Clock = Clock.systemDefaultZone(),
) {
    val dao = db.dao()
    val attachments = ProjectFiles(context)
    val workspace =
        combine(
                combine(dao.tasks(), dao.projects(), dao.events()) { t, p, e ->
                    Workspace(tasks = t, projects = p, events = e)
                },
                dao.notes(),
                dao.favorites(),
                dao.templates(),
            ) { w, n, f, t ->
                w.copy(notes = n, favorites = f, templates = t)
            }
            .stateIn(scope, SharingStarted.Eagerly, Workspace())
    val settings =
        context.settings.data
            .map { p ->
                Settings(
                    p[stringPreferencesKey("theme")] ?: "system",
                    p[booleanPreferencesKey("onboarded")] ?: false,
                    p[stringPreferencesKey("sort")] ?: "due",
                    p[intPreferencesKey("weekStart")] ?: 1,
                    p[intPreferencesKey("reminder")] ?: 15,
                    p[booleanPreferencesKey("agenda")] ?: false,
                    p[stringPreferencesKey("filters")] ?: "",
                )
            }
            .stateIn(scope, SharingStarted.Eagerly, Settings())
    val loaded = CompletableDeferred<Unit>()
    val startupError = MutableStateFlow<String?>(null)

    init {
        scope.launch { initialize() }
    }

    suspend fun initialize() {
        try {
            android.util.Log.d("NewLookStartup", "Reading preferences")
            context.settings.data.first()
            android.util.Log.d("NewLookStartup", "Preparing occurrences")
            generate(LocalDate.now(clock).plusMonths(3))
            android.util.Log.d("NewLookStartup", "Reconciling reminders")
            ReminderScheduler.reconcile(context, dao)
            attachments.retainProjects(dao.allProjects().map { it.id }.toSet())
            startupError.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            startupError.value = e.message ?: "Workspace unavailable"
        } finally {
            android.util.Log.d("NewLookStartup", "Workspace ready")
            loaded.complete(Unit)
        }
    }

    suspend fun setting(key: String, value: String) {
        context.settings.edit { it[stringPreferencesKey(key)] = value }
    }

    suspend fun setting(key: String, value: Boolean) {
        context.settings.edit { it[booleanPreferencesKey(key)] = value }
    }

    suspend fun setting(key: String, value: Int) {
        context.settings.edit { it[intPreferencesKey(key)] = value }
    }

    suspend fun save(task: Task) {
        require(task.title.isNotBlank())
        require(task.priority in 0..3 && task.status in listOf("todo", "in_progress", "done"))
        require((task.status == "done") == (task.completedAt != null))
        require(substeps(task).all { it.title.isNotBlank() })
        require(task.time == null || task.due != null)
        task.due?.let(LocalDate::parse)
        task.time?.let(LocalTime::parse)
        db.withTransaction {
            task.seriesId?.let { id ->
                dao.allSeries()
                    .find { it.id == id }
                    ?.let { s ->
                        dao.series(
                            s.copy(
                                excludedKeys =
                                    s.excludedKeys
                                        .split(',')
                                        .filter { it.isNotBlank() && it != task.occurrenceKey }
                                        .joinToString(",")
                            )
                        )
                    }
            }
            dao.task(task)
        }
        ReminderScheduler.clearSnooze(context, task.id)
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun save(event: Event) {
        require(event.title.isNotBlank())
        if (event.allDay) require(LocalDate.parse(event.end) > LocalDate.parse(event.start))
        else require(Instant.parse(event.end) >= Instant.parse(event.start))
        dao.event(event)
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun delete(task: Task) {
        db.withTransaction {
            task.seriesId?.let { id ->
                dao.allSeries()
                    .find { it.id == id }
                    ?.let { s ->
                        dao.series(
                            s.copy(
                                excludedKeys =
                                    (s.excludedKeys.split(',').filter { it.isNotBlank() } +
                                            listOfNotNull(task.occurrenceKey))
                                        .toSet()
                                        .joinToString(",")
                            )
                        )
                    }
            }
            dao.deleteTask(task)
        }
        ReminderScheduler.clearSnooze(context, task.id)
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun seriesTask(
        task: Task,
        frequency: String,
        interval: Int,
        weekdays: String,
        until: String?,
        limit: Int?,
    ) {
        require(task.due != null)
        val s =
            Series(
                prototype = json.toJson(task.copy(seriesId = null, occurrenceKey = null)),
                frequency = frequency,
                interval = interval,
                weekdays = weekdays,
                start = task.due,
                until = until,
                limit = limit,
            )
        db.withTransaction {
            dao.series(s)
            dao.task(task.copy(seriesId = s.id, occurrenceKey = task.due))
        }
        generate(LocalDate.parse(task.due).plusMonths(3))
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun generate(through: LocalDate) {
        db.withTransaction {
            dao.allSeries()
                .filter { it.active }
                .forEach { s ->
                    val prototype = json.fromJson(s.prototype, Task::class.java)
                    dates(s, through)
                        .filter { it.toString() !in s.excludedKeys.split(',') }
                        .forEach { date ->
                            dao.occurrence(
                                prototype.copy(
                                    id =
                                        java.util.UUID.nameUUIDFromBytes(
                                                "${s.id}:$date".toByteArray()
                                            )
                                            .toString(),
                                    seriesId = s.id,
                                    occurrenceKey = date.toString(),
                                    due = date.toString(),
                                    status = "todo",
                                    completedAt = null,
                                    subtasks =
                                        json.toJson(
                                            substeps(prototype).map {
                                                it.copy(done = false, id = uuid())
                                            }
                                        ),
                                )
                            )
                        }
                    dao.series(s.copy(generatedThrough = through.toString()))
                }
        }
    }

    suspend fun future(task: Task, remove: Boolean) {
        val s = dao.allSeries().find { it.id == task.seriesId } ?: return
        val date = LocalDate.parse(task.occurrenceKey)
        db.withTransaction {
            dao.series(
                s.copy(
                    until =
                        if (date > LocalDate.parse(s.start)) date.minusDays(1).toString()
                        else s.until,
                    active = date > LocalDate.parse(s.start),
                )
            )
            dao.allTasks()
                .filter { it.seriesId == s.id && LocalDate.parse(it.occurrenceKey) >= date }
                .forEach { dao.deleteTask(it) }
            if (!remove) {
                val newS =
                    s.copy(
                        id = uuid(),
                        excludedKeys = "",
                        prototype = json.toJson(task.copy(seriesId = null, occurrenceKey = null)),
                        start = task.due ?: date.toString(),
                        until = s.until?.let { maxOf(it, task.due ?: date.toString()) },
                        generatedThrough = null,
                        limit = s.limit?.let { maxOf(1, it - dates(s, date.minusDays(1)).size) },
                    )
                dao.series(newS)
            }
        }
        generate(date.plusMonths(3))
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun deleteProject(p: Project, contents: Boolean) {
        db.withTransaction {
            if (contents) {
                dao.allTasks().filter { it.projectId == p.id }.forEach { dao.deleteTask(it) }
                dao.allNotes().filter { it.projectId == p.id }.forEach { dao.deleteNote(it) }
                dao.allEvents().filter { it.projectId == p.id }.forEach { dao.deleteEvent(it) }
            }
            dao.allSeries().forEach { s ->
                val proto = json.fromJson(s.prototype, Task::class.java)
                if (proto.projectId == p.id)
                    dao.series(
                        s.copy(
                            active = if (contents) false else s.active,
                            prototype = json.toJson(proto.copy(projectId = null)),
                        )
                    )
            }
            dao.deleteProject(p)
        }
        attachments.retainProjects(dao.allProjects().map { it.id }.toSet())
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun rescheduleUnfinished(ids: Set<String>, date: LocalDate) {
        db.withTransaction {
            dao.allTasks()
                .filter { it.id in ids && it.status != "done" }
                .forEach { dao.task(it.copy(due = date.toString())) }
        }
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun attach(projectId: String, uri: android.net.Uri) {
        require(dao.allProjects().any { it.id == projectId }) { "Project no longer exists" }
        val item = attachments.importUri(projectId, uri)
        if (dao.allProjects().none { it.id == projectId }) {
            attachments.delete(item)
            error("Project no longer exists")
        }
    }

    suspend fun apply(
        title: String,
        description: String,
        steps: List<TemplateStep>,
        start: LocalDate,
        projectId: String? = null,
    ): String =
        db.withTransaction {
            require(title.isNotBlank())
            require(steps.isNotEmpty() && steps.all { it.title.isNotBlank() })
            val p =
                if (projectId == null) {
                    Project(title = title, description = description).also { dao.project(it) }
                } else {
                    requireNotNull(dao.allProjects().find { it.id == projectId && !it.archived }) {
                        "Project is no longer available"
                    }
                }
            steps.forEach {
                dao.task(
                    Task(
                        title = it.title,
                        priority = it.priority,
                        projectId = p.id,
                        due = start.plusDays(it.offset.toLong()).toString(),
                    )
                )
            }
            p.id
        }

    suspend fun favorite(type: String, id: String) {
        val f = Favorite(type, id)
        if (f in dao.allFavorites()) dao.deleteFavorite(f) else dao.favorite(f)
    }

    suspend fun backup(): Backup =
        db.withTransaction {
            Backup(
                appVersion = BuildConfig.VERSION_NAME,
                tasks = dao.allTasks(),
                projects = dao.allProjects(),
                events = dao.allEvents(),
                notes = dao.allNotes(),
                series = dao.allSeries(),
                favorites = dao.allFavorites(),
                templates = dao.allTemplates(),
            )
        }

    suspend fun replace(b: Backup) {
        db.withTransaction {
            clear()
            b.projects.forEach { dao.project(it) }
            b.series.forEach { dao.series(it) }
            b.tasks.forEach { dao.task(it) }
            b.events.forEach { dao.event(it) }
            b.notes.forEach { dao.note(it) }
            b.templates.forEach { dao.template(it) }
            b.favorites.forEach { dao.favorite(it) }
        }
        attachments.retainProjects(dao.allProjects().map { it.id }.toSet())
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun clear() {
        dao.clearTasks()
        dao.clearEvents()
        dao.clearNotes()
        dao.clearSeries()
        dao.clearFavorites()
        dao.clearTemplates()
        dao.clearProjects()
    }

    suspend fun reset() {
        db.withTransaction { clear() }
        attachments.retainProjects(emptySet())
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun removeDemo() {
        dao.allSeries()
            .filter { json.fromJson(it.prototype, Task::class.java).demo }
            .forEach { dao.series(it.copy(active = false)) }
        dao.allProjects().filter { it.demo }.forEach { deleteProject(it, false) }
        db.withTransaction {
            dao.allTasks().filter { it.demo }.forEach { dao.deleteTask(it) }
            dao.allEvents().filter { it.demo }.forEach { dao.deleteEvent(it) }
            dao.allNotes().filter { it.demo }.forEach { dao.deleteNote(it) }
            dao.allProjects().filter { it.demo }.forEach { dao.deleteProject(it) }
        }
        ReminderScheduler.reconcile(context, dao)
    }

    suspend fun demo() {
        val today = LocalDate.now(clock)
        db.withTransaction {
            val p =
                Project(
                    title = "Демо · Адаптация на служител",
                    description = "Демонстрационен проект",
                    target = today.plusDays(7).toString(),
                    demo = true,
                )
            dao.project(p)
            listOf(
                    "Подготовка на среща",
                    "Проверка на CRM",
                    "Преглед на документи",
                    "Планиране на въвеждането",
                )
                .forEachIndexed { i, t ->
                    dao.task(
                        Task(
                            title = t,
                            projectId = p.id,
                            due = today.plusDays(if (i == 3) 1 else 0).toString(),
                            status = if (i == 2) "done" else "todo",
                            completedAt = if (i == 2) clock.millis() else null,
                            priority = if (i == 1) 2 else 1,
                            demo = true,
                        )
                    )
                }
            dao.event(
                Event(
                    title = "Демо · Работна среща",
                    start =
                        today.atTime(14, 0).atZone(ZoneId.systemDefault()).toInstant().toString(),
                    end =
                        today.atTime(14, 45).atZone(ZoneId.systemDefault()).toInstant().toString(),
                    projectId = p.id,
                    demo = true,
                )
            )
        }
    }
}
