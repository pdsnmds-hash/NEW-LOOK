@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package bg.newlook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*
import kotlinx.coroutines.*

@Composable
fun TaskEditor(
    original: Task,
    w: Workspace,
    settings: Settings,
    repo: Repository,
    vm: WorkspaceModel,
    onDismiss: () -> Unit,
    onDuplicate: (Task) -> Unit,
    onDelete: (Task) -> Unit,
) {
    var raw by rememberSaveable(original.id) { mutableStateOf(json.toJson(original)) }
    val t = json.fromJson(raw, Task::class.java)
    fun update(next: Task) {
        raw = json.toJson(next)
    }
    var more by rememberSaveable { mutableStateOf(false) }
    var frequency by rememberSaveable { mutableStateOf("none") }
    var interval by rememberSaveable { mutableStateOf("1") }
    var days by rememberSaveable { mutableStateOf("1") }
    var until by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf("") }
    var scopeAction by remember { mutableStateOf<String?>(null) }
    var deleteConfirm by remember { mutableStateOf(false) }
    val existing = w.tasks.any { it.id == original.id }
    val dirty = raw != json.toJson(original) || frequency != "none"
    val valid =
        t.title.isNotBlank() &&
            substeps(t).all { it.title.isNotBlank() } &&
            runCatching {
                    t.due?.let(LocalDate::parse)
                    t.time?.let(LocalTime::parse)
                    require(t.time == null || t.due != null)
                    if (frequency != "none") {
                        require(t.due != null)
                        require(interval.toInt() in 1..10000)
                        require(limit.isBlank() || limit.toInt() in 1..20000)
                        require(until.isBlank() || LocalDate.parse(until) >= LocalDate.parse(t.due))
                    }
                }
                .isSuccess
    val save: () -> Unit = {
        if (t.seriesId != null) scopeAction = "save"
        else
            vm.run(
                {
                    if (frequency == "none") repo.save(t)
                    else
                        repo.seriesTask(
                            t,
                            frequency,
                            interval.toInt(),
                            days,
                            until.ifBlank { null },
                            limit.toIntOrNull(),
                        )
                },
                onDismiss,
            )
    }
    EditorShell(
        if (existing) l("Редактиране на задача", "Edit task") else l("Нова задача", "New task"),
        dirty,
        onDismiss,
        save,
        valid,
    ) {
        Field(t.title, { update(t.copy(title = it)) }, l("Заглавие *", "Title *"))
        DateField(
            t.due ?: "",
            {
                update(t.copy(due = it.ifBlank { null }, time = if (it.isBlank()) null else t.time))
            },
            l("Краен срок", "Due date"),
        )
        if (t.due != null)
            TimeField(
                t.time ?: "",
                { update(t.copy(time = it.ifBlank { null })) },
                l("Час (по избор)", "Time (optional)"),
            )
        Choice(l("Приоритет", "Priority"), t.priority, (0..3).map { priorityLabel(it) }) {
            update(t.copy(priority = it))
        }
        ProjectChoice(t.projectId, w) { update(t.copy(projectId = it)) }
        TextButton(onClick = { more = !more }) {
            Text(
                if (more) l("По-малко полета", "Fewer fields")
                else l("Подзадачи, напомняния и още", "Subtasks, reminders & more")
            )
        }
        if (more) {
            Field(
                t.description,
                { update(t.copy(description = it)) },
                l("Описание", "Description"),
                true,
            )
            Choice(
                l("Статус", "Status"),
                listOf("todo", "in_progress", "done").indexOf(t.status),
                listOf(statusLabel("todo"), statusLabel("in_progress"), statusLabel("done")),
            ) { i ->
                val s = listOf("todo", "in_progress", "done")[i]
                if (s == "done" && substeps(t).any { !it.done }) scopeAction = "complete"
                else
                    update(
                        if (s == "done") completed(t, true)
                        else t.copy(status = s, completedAt = null)
                    )
            }
            Field(
                t.tags,
                { update(t.copy(tags = it)) },
                l("Тагове (разделени със запетая)", "Tags (comma separated)"),
            )
            Section(l("Подзадачи", "Subtasks"))
            substeps(t).forEach { s ->
                Row {
                    Checkbox(
                        s.done,
                        { done ->
                            update(
                                t.copy(
                                    subtasks =
                                        json.toJson(
                                            substeps(t).map {
                                                if (it.id == s.id) it.copy(done = done) else it
                                            }
                                        )
                                )
                            )
                        },
                        modifier = Modifier.semantics { contentDescription = s.title },
                    )
                    Column(Modifier.weight(1f)) {
                        Field(
                            s.title,
                            { title ->
                                update(
                                    t.copy(
                                        subtasks =
                                            json.toJson(
                                                substeps(t).map {
                                                    if (it.id == s.id) it.copy(title = title)
                                                    else it
                                                }
                                            )
                                    )
                                )
                            },
                            l("Подзадача", "Subtask"),
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    val list = substeps(t).toMutableList()
                                    val i = list.indexOfFirst { it.id == s.id }
                                    if (i > 0) {
                                        java.util.Collections.swap(list, i, i - 1)
                                        update(t.copy(subtasks = json.toJson(list)))
                                    }
                                }
                            ) {
                                Text("↑")
                            }
                            TextButton(
                                onClick = {
                                    val list = substeps(t).toMutableList()
                                    val i = list.indexOfFirst { it.id == s.id }
                                    if (i < list.lastIndex) {
                                        java.util.Collections.swap(list, i, i + 1)
                                        update(t.copy(subtasks = json.toJson(list)))
                                    }
                                }
                            ) {
                                Text("↓")
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            update(
                                t.copy(subtasks = json.toJson(substeps(t).filter { it.id != s.id }))
                            )
                        }
                    ) {
                        Icon(AppIcons.Close, l("Премахни подзадача", "Remove subtask"))
                    }
                }
            }
            Row {
                OutlinedTextField(
                    step,
                    { step = it },
                    Modifier.weight(1f),
                    label = { Text(l("Нова подзадача", "New subtask")) },
                )
                IconButton(
                    onClick = {
                        if (step.isNotBlank()) {
                            update(
                                t.copy(subtasks = json.toJson(substeps(t) + Subtask(step.trim())))
                            )
                            step = ""
                        }
                    }
                ) {
                    Icon(AppIcons.Add, l("Добави", "Add"))
                }
            }
            ReminderField(t.reminder, settings.reminder) { update(t.copy(reminder = it)) }
            if (t.seriesId == null) {
                Choice(
                    l("Повторение", "Repeat"),
                    listOf("none", "daily", "weekly", "monthly").indexOf(frequency),
                    listOf(
                        l("Без повторение", "Does not repeat"),
                        l("Всеки ден", "Daily"),
                        l("Седмично", "Weekly"),
                        l("Месечно", "Monthly"),
                    ),
                ) {
                    frequency = listOf("none", "daily", "weekly", "monthly")[it]
                }
                if (frequency != "none") {
                    Field(interval, { interval = it }, l("Интервал N", "Interval N"))
                    if (frequency == "weekly")
                        MultiChoice(
                            (1..7).map { it.toString() },
                            days,
                            { if (it.isNotBlank()) days = it },
                        ) {
                            DayOfWeek.of(it.toInt())
                                .getDisplayName(
                                    java.time.format.TextStyle.SHORT,
                                    if (english()) java.util.Locale.ENGLISH
                                    else java.util.Locale.forLanguageTag("bg"),
                                )
                        }
                    DateField(
                        until,
                        { until = it },
                        l("До дата (по избор)", "Until date (optional)"),
                    )
                    Field(
                        limit,
                        { limit = it },
                        l("Брой повторения (по избор)", "Occurrence count (optional)"),
                    )
                }
            }
        }
        if (!valid && t.title.isNotBlank())
            Muted(
                l(
                    "Проверете заглавието, датата, часа и повторението.",
                    "Check the title, date, time and recurrence.",
                )
            )
        if (existing && t.reminder != null) {
            SnoozeControls(t)
        }
        if (existing) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    onClick = {
                        onDuplicate(
                            t.copy(
                                id = uuid(),
                                seriesId = null,
                                occurrenceKey = null,
                                status = "todo",
                                completedAt = null,
                                createdAt = System.currentTimeMillis(),
                            )
                        )
                    },
                ) {
                    Text(l("Дублирай", "Duplicate"))
                }
                Button(
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    onClick = {
                        if (t.seriesId != null) scopeAction = "delete" else deleteConfirm = true
                    },
                ) {
                    Text(l("Изтрий задача", "Delete task"))
                }
            }
        }
    }
    if (deleteConfirm)
        Confirm(l("Изтриване на задача?", "Delete task?"), t.title, { deleteConfirm = false }) {
            onDelete(t)
        }
    scopeAction?.let { action ->
        if (action == "complete")
            Confirm(
                l("Завършване на всички подзадачи?", "Complete every subtask?"),
                t.title,
                { scopeAction = null },
            ) {
                update(completed(t, true))
                scopeAction = null
            }
        else
            AlertDialog(
                onDismissRequest = { scopeAction = null },
                title = { Text(l("Повтаряща се задача", "Repeating task")) },
                text = {
                    Text(
                        l(
                            "Кои екземпляри да променим? Миналите се запазват.",
                            "Which occurrences? Past occurrences will be kept.",
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.run({ repo.future(t, action == "delete") }, onDismiss)
                            scopeAction = null
                        }
                    ) {
                        Text(l("Този и бъдещите", "This and future"))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            scopeAction = null
                            if (action == "delete") onDelete(t)
                            else vm.run({ repo.save(t) }, onDismiss)
                        }
                    ) {
                        Text(l("Само този", "Only this one"))
                    }
                },
            )
    }
}

@Composable
fun SnoozeControls(task: Task) {
    val c = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var day by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var time by rememberSaveable {
        mutableStateOf(LocalTime.now().plusHours(1).withSecond(0).withNano(0).toString())
    }
    var saved by remember { mutableStateOf(false) }
    Section(l("Отложи напомнянето", "Snooze reminder"))
    Row {
        TextButton(
            onClick = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        ReminderScheduler.snooze(c, task.id, task.title, "task", 10)
                    }
                    saved = true
                }
            }
        ) {
            Text("+10 min")
        }
        TextButton(
            onClick = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        ReminderScheduler.snooze(c, task.id, task.title, "task", 60)
                    }
                    saved = true
                }
            }
        ) {
            Text("+1 h")
        }
    }
    DateField(day, { day = it }, l("Дата на напомнянето", "Reminder date"))
    TimeField(time, { time = it }, l("Час", "Time"))
    val at =
        runCatching {
                LocalDate.parse(day)
                    .atTime(LocalTime.parse(time))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            }
            .getOrNull()
    OutlinedButton(
        onClick = {
            at?.let {
                val instant = it
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        ReminderScheduler.snoozeAt(c, task.id, task.title, "task", instant)
                    }
                    saved = true
                }
            }
        },
        enabled = at != null && at > Instant.now(),
    ) {
        Text(l("Отложи до избрания час", "Snooze until selected time"))
    }
    if (saved) Muted(l("Напомнянето е отложено.", "Reminder snoozed."))
}
