package bg.newlook.domain

import bg.newlook.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.*
import java.time.format.DateTimeFormatter

val json = Gson()

fun substeps(task: Task): List<Subtask> =
    json.fromJson(task.subtasks, object : TypeToken<List<Subtask>>() {}.type)

fun completed(task: Task, done: Boolean, clock: Clock = Clock.systemUTC()) =
    task.copy(
        status = if (done) "done" else "todo",
        completedAt = if (done) clock.millis() else null,
        subtasks =
            if (done) json.toJson(substeps(task).map { it.copy(done = true) }) else task.subtasks,
    )

fun overdue(task: Task, clock: Clock = Clock.systemDefaultZone()): Boolean {
    if (task.status == "done" || task.due == null) return false
    val date = LocalDate.parse(task.due)
    val today = LocalDate.now(clock)
    return date.isBefore(today) ||
        (date == today &&
            task.time != null &&
            LocalTime.parse(task.time).isBefore(LocalTime.now(clock)))
}

fun dates(series: Series, through: LocalDate): List<LocalDate> {
    require(series.interval in 1..10000)
    val start = LocalDate.parse(series.start)
    val end = minOf(through, series.until?.let(LocalDate::parse) ?: through)
    if (!series.active || end < start) return emptyList()
    val result = mutableListOf<LocalDate>()
    var index = 0L
    var occurrences = 0
    val weekdays = series.weekdays.split(',').mapNotNull { it.toIntOrNull() }.toSet()
    var day = start
    while (day <= end) {
        val accepted =
            when (series.frequency) {
                "monthly" -> true
                "weekly" ->
                    day.dayOfWeek.value in weekdays &&
                        (java.time.temporal.ChronoUnit.WEEKS.between(
                            start.with(
                                java.time.temporal.TemporalAdjusters.previousOrSame(
                                    DayOfWeek.MONDAY
                                )
                            ),
                            day.with(
                                java.time.temporal.TemporalAdjusters.previousOrSame(
                                    DayOfWeek.MONDAY
                                )
                            ),
                        ) % series.interval == 0L)
                else -> true
            }
        if (accepted) {
            occurrences++
            if (series.limit != null && occurrences > series.limit) break
            result.add(day)
        }
        index++
        day =
            when (series.frequency) {
                "monthly" -> start.plusMonths(index * series.interval)
                "weekly" -> day.plusDays(1)
                else -> start.plusDays(index * series.interval)
            }
        require(index <= 200000) { "Series too large" }
    }
    return result
}

data class Filters(
    val statuses: Set<String> = emptySet(),
    val priorities: Set<Int> = emptySet(),
    val projects: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val hasDue: Boolean? = null,
)

fun matches(t: Task, q: String, f: Filters) =
    listOf(t.title, t.description, t.tags).any { it.contains(q, true) } &&
        (f.statuses.isEmpty() || t.status in f.statuses) &&
        (f.priorities.isEmpty() || t.priority in f.priorities) &&
        (f.projects.isEmpty() || (t.projectId ?: "") in f.projects) &&
        (f.tags.isEmpty() || t.tags.split(',').map { it.trim() }.any { it in f.tags }) &&
        (f.hasDue == null || (t.due != null) == f.hasDue)

fun csv(tasks: List<Task>): String {
    fun cell(v: String): String {
        val safe =
            if (v.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$v" else v
        return "\"${safe.replace("\"","\"\"")}\""
    }
    return "\uFEFF" +
        listOf(listOf("ID", "Title", "Description", "Status", "Priority", "Due", "Time", "Tags"))
            .plus(
                tasks.map {
                    listOf(
                        it.id,
                        it.title,
                        it.description,
                        it.status,
                        it.priority.toString(),
                        it.due ?: "",
                        it.time ?: "",
                        it.tags,
                    )
                }
            )
            .joinToString("\r\n") { row -> row.joinToString(",", transform = ::cell) }
}

fun ics(event: Event, clock: Clock = Clock.systemUTC()): String {
    fun escape(s: String) =
        s.replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\r", "")
    val format = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    val lines =
        mutableListOf(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//NEW LOOK//Organizer//EN",
            "CALSCALE:GREGORIAN",
            "BEGIN:VEVENT",
            "UID:${event.id}@newlook.local",
            "DTSTAMP:${format.format(clock.instant())}",
            "SUMMARY:${escape(event.title)}",
        )
    if (event.allDay) {
        lines += "DTSTART;VALUE=DATE:${event.start.replace("-","")}"
        lines += "DTEND;VALUE=DATE:${event.end.replace("-","")}"
    } else {
        lines += "DTSTART:${format.format(Instant.parse(event.start))}"
        lines += "DTEND:${format.format(Instant.parse(event.end))}"
    }
    lines += "LOCATION:${escape(event.location)}"
    lines += "DESCRIPTION:${escape(event.notes)}"
    lines += "END:VEVENT"
    lines += "END:VCALENDAR"
    // Fold by UTF-8 octet count, preserving Unicode code points.
    return lines.joinToString("\r\n") { line ->
        buildString {
            var size = 0
            line.codePoints().forEach { cp ->
                val s = String(Character.toChars(cp))
                val bytes = s.toByteArray(Charsets.UTF_8).size
                if (size + bytes > 73) {
                    append("\r\n ")
                    size = 1
                }
                append(s)
                size += bytes
            }
        }
    } + "\r\n"
}

fun eventOn(e: Event, date: LocalDate): Boolean =
    if (e.allDay) date >= LocalDate.parse(e.start) && date < LocalDate.parse(e.end)
    else {
        val zone = ZoneId.systemDefault()
        val begin = Instant.parse(e.start)
        val finish = Instant.parse(e.end)
        begin < date.plusDays(1).atStartOfDay(zone).toInstant() &&
            (finish > date.atStartOfDay(zone).toInstant() ||
                (begin == finish && begin >= date.atStartOfDay(zone).toInstant()))
    }

fun recommendations(types: Set<Int>, frequency: Int, goal: Int): List<Pair<Int, Int>> =
    (0..4)
        .map { i ->
            i to
                ((if (i in types) 6 else 0) +
                    (if (goal == i) 3 else 0) +
                    (if (frequency == 2 && i == 0) 1 else 0))
        }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }

data class Backup(
    val schemaVersion: Int = 2,
    val exportDate: String = Instant.now().toString(),
    val appVersion: String = "1.0.0",
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val events: List<Event> = emptyList(),
    val notes: List<Note> = emptyList(),
    val series: List<Series> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    val templates: List<UserTemplate> = emptyList(),
)

fun validateBackup(text: String): Backup {
    require(text.toByteArray().size <= 10 * 1024 * 1024) { "Maximum backup size: 10 MB" }
    val b = json.fromJson(text, Backup::class.java)
    require(b.schemaVersion == 2) { "Unsupported backup version" }
    Instant.parse(b.exportDate)
    require(
        listOf(
                b.tasks.size,
                b.projects.size,
                b.events.size,
                b.notes.size,
                b.series.size,
                b.favorites.size,
                b.templates.size,
            )
            .sum() <= 20000
    ) {
        "Too many records"
    }
    fun ids(list: List<String>) {
        require(list.size == list.toSet().size) { "Duplicate ID" }
        list.forEach { require(UUID_VALID.matches(it)) { "Invalid ID" } }
    }
    ids(b.tasks.map { it.id })
    ids(b.projects.map { it.id })
    ids(b.notes.map { it.id })
    ids(b.events.map { it.id })
    ids(b.series.map { it.id })
    ids(b.templates.map { it.id })
    val projects = b.projects.map { it.id }.toSet()
    val series = b.series.map { it.id }.toSet()
    fun project(id: String?) {
        require(id == null || id in projects) { "Missing project" }
    }
    b.projects.forEach {
        require(it.title.isNotBlank())
        it.target?.let(LocalDate::parse)
    }
    b.tasks.forEach { t ->
        require(
            t.title.isNotBlank() &&
                t.priority in 0..3 &&
                t.status in listOf("todo", "in_progress", "done")
        )
        require((t.status == "done") == (t.completedAt != null))
        project(t.projectId)
        require(t.time == null || t.due != null)
        t.due?.let(LocalDate::parse)
        t.time?.let(LocalTime::parse)
        require(t.seriesId == null || t.seriesId in series)
        require((t.seriesId == null) == (t.occurrenceKey == null))
        t.occurrenceKey?.let(LocalDate::parse)
        val steps = substeps(t)
        ids(steps.map { it.id })
        require(steps.all { it.title.isNotBlank() })
        require(t.reminder == null || t.reminder in 0..10080)
    }
    require(
        b.tasks
            .filter { it.seriesId != null }
            .map { it.seriesId to it.occurrenceKey }
            .let { it.size == it.toSet().size }
    )
    b.events.forEach { e ->
        project(e.projectId)
        require(e.title.isNotBlank())
        ZoneId.of(e.zone)
        if (e.allDay) require(LocalDate.parse(e.end) > LocalDate.parse(e.start))
        else require(Instant.parse(e.end) >= Instant.parse(e.start))
        require(e.reminder == null || e.reminder in 0..10080)
    }
    b.notes.forEach { project(it.projectId) }
    b.series.forEach { s ->
        s.excludedKeys.split(',').filter { it.isNotBlank() }.forEach { LocalDate.parse(it) }
        ZoneId.of(s.zone)
        LocalDate.parse(s.start)
        s.until?.let { require(LocalDate.parse(it) >= LocalDate.parse(s.start)) }
        require(s.interval in 1..10000 && s.frequency in listOf("daily", "weekly", "monthly"))
        require(s.limit == null || s.limit in 1..20000)
        require(s.weekdays.split(',').all { it.toIntOrNull() in 1..7 })
        val t = json.fromJson(s.prototype, Task::class.java)
        project(t.projectId)
        require(
            t.title.isNotBlank() &&
                t.priority in 0..3 &&
                t.status in listOf("todo", "in_progress", "done")
        )
        t.due?.let(LocalDate::parse)
        t.time?.let(LocalTime::parse)
        require(t.time == null || t.due != null)
        require(t.reminder == null || t.reminder in 0..10080)
        require(substeps(t).all { it.title.isNotBlank() })
    }
    b.templates.forEach {
        require(it.title.isNotBlank())
        val steps: List<TemplateStep> =
            json.fromJson(it.steps, object : TypeToken<List<TemplateStep>>() {}.type)
        require(
            steps.isNotEmpty() &&
                steps.size <= 200 &&
                steps.all { it.title.isNotBlank() && it.offset in -3650..3650 }
        )
    }
    require(b.favorites.size == b.favorites.toSet().size)
    b.favorites.forEach { f ->
        require(
            when (f.type) {
                "service" -> f.targetId in services.map { it.id }
                "article" -> f.targetId in guides.map { it.id }
                "template" -> f.targetId in templates.map { it.id } + b.templates.map { it.id }
                else -> false
            }
        )
    }
    return b
}

private val UUID_VALID =
    Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
