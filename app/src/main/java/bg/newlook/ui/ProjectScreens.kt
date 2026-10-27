@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package bg.newlook.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProjectScreen(
    p: Project,
    w: Workspace,
    repo: Repository,
    vm: WorkspaceModel,
    open: (Task) -> Unit,
    toggle: (Task) -> Unit,
    edit: () -> Unit,
    note: () -> Unit,
    editNote: (Note) -> Unit,
    event: (Event) -> Unit,
    history: () -> Unit,
    files: () -> Unit,
    back: () -> Unit,
) {
    var tab by rememberSaveable(p.id) { mutableIntStateOf(0) }
    val ts = w.tasks.filter { it.projectId == p.id }
    val active = ts.filter { it.status != "done" }
    val finished = ts.count { it.status == "done" }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Navigation stays reachable regardless of project length or scroll position.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionIcon(AppIcons.ArrowBack, l("Назад", "Back"), back)
            Text(
                p.title,
                Modifier.weight(1f).padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { open(Task(projectId = p.id)) }) {
                Icon(AppIcons.Add, l("Нова задача", "New task"), Modifier.size(21.dp))
            }
            IconButton(onClick = history) {
                Icon(
                    AppIcons.MenuBook,
                    l("История на проекта", "Project history"),
                    Modifier.size(21.dp),
                )
            }
        }
        Box(Modifier.weight(1f)) {
            Page {
                Heading(p.title)
                if (p.description.isNotBlank()) Muted(p.description)
                Hero(
                    l("НАПРЕДЪК", "PROGRESS"),
                    "$finished / ${ts.size} " + l("задачи", "tasks"),
                    p.target?.let { displayDate(it) } ?: l("Без крайна дата", "No target date"),
                    if (ts.isEmpty()) 0f else finished.toFloat() / ts.size,
                )
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = edit) { Text(l("Редактирай", "Edit")) }
                    TextButton(
                        onClick = { vm.run({ repo.dao.project(p.copy(archived = !p.archived)) }) }
                    ) {
                        Text(
                            if (p.archived) l("Възстанови", "Unarchive")
                            else l("Архивирай", "Archive")
                        )
                    }
                    TextButton(onClick = history) { Text(l("История", "History") + " ($finished)") }
                }
                val attachments by repo.attachments.items.collectAsState()
                OutlinedButton(
                    onClick = files,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                ) {
                    Icon(AppIcons.Description, null, Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        l("Файлове в проекта", "Project files") +
                            " · ${attachments.count { it.projectId == p.id }}"
                    )
                }
                Segments(
                    listOf(
                        l("Задачи", "Tasks"),
                        l("Табло", "Board"),
                        l("Бележки", "Notes"),
                        l("Събития", "Events"),
                    ),
                    tab,
                ) {
                    tab = it
                }
                when (tab) {
                    0 -> {
                        QuickAdd(
                            placeholder = l("Нова задача в проекта…", "Add a task to this project…")
                        ) { title ->
                            vm.run({ repo.save(Task(title = title, projectId = p.id)) })
                        }
                        Section(l("Задачи в проекта", "Project tasks"))
                        Column(
                            Modifier.fillMaxWidth().animateContentSize(tween(350)),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            if (active.isEmpty())
                                Empty(
                                    if (ts.isEmpty())
                                        l(
                                            "Първата стъпка започва тук.",
                                            "Your first step starts here.",
                                        )
                                    else l("Всичко е завършено.", "All caught up."),
                                    if (ts.isEmpty())
                                        l(
                                            "Добавете задача по-горе или използвайте бутона +.",
                                            "Add a task above or use the + button.",
                                        )
                                    else
                                        l(
                                            "Завършените задачи са в историята на този проект.",
                                            "Completed tasks are in this project’s history.",
                                        ),
                                )
                            active.forEach { TaskRow(it, w, toggle, open) }
                        }
                    }
                    1 ->
                        ProjectBoard(
                            ts,
                            open,
                            { task, status ->
                                if (status == "done") toggle(task)
                                else
                                    vm.run({
                                        repo.save(task.copy(status = status, completedAt = null))
                                    })
                            },
                            history,
                        )
                    2 -> {
                        val notes = w.notes.filter { it.projectId == p.id }
                        if (notes.isEmpty())
                            Empty(
                                l("Идеите за този проект.", "Ideas for this project."),
                                l("Запазете първата бележка.", "Save your first note."),
                            )
                        notes.forEach { n ->
                            CardBlock({ editNote(n) }) {
                                Text(n.title, style = MaterialTheme.typography.titleMedium)
                                Muted(n.body)
                            }
                        }
                        Button(onClick = note) { Text(l("Нова бележка", "New note")) }
                    }
                    3 -> {
                        val events = w.events.filter { it.projectId == p.id }
                        if (events.isEmpty())
                            Empty(
                                l("Няма събития в проекта.", "No project events yet."),
                                l(
                                    "Добавете събитие и изберете този проект.",
                                    "Create an event and choose this project.",
                                ),
                            )
                        events.forEach { EventRow(it, event) }
                    }
                }
                if (ts.isNotEmpty())
                    TextButton(
                        onClick = {
                            val first =
                                ts.mapNotNull { it.due?.let(LocalDate::parse) }.minOrNull()
                                    ?: LocalDate.now()
                            val steps =
                                ts.map {
                                    TemplateStep(
                                        it.title,
                                        it.due?.let { date ->
                                            java.time.temporal.ChronoUnit.DAYS.between(
                                                    first,
                                                    LocalDate.parse(date),
                                                )
                                                .toInt()
                                        } ?: 0,
                                        it.priority,
                                    )
                                }
                            vm.run({
                                repo.dao.template(
                                    UserTemplate(
                                        title = p.title,
                                        description = p.description,
                                        steps = json.toJson(steps),
                                    )
                                )
                            })
                        }
                    ) {
                        Icon(AppIcons.AutoAwesome, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(l("Запази като шаблон", "Save as template"))
                    }
            }
        }
    }
}

@Composable
fun ProjectHistoryScreen(
    p: Project,
    w: Workspace,
    open: (Task) -> Unit,
    toggle: (Task) -> Unit,
    back: () -> Unit,
) {
    val completed =
        w.tasks
            .filter { it.projectId == p.id && it.status == "done" }
            .sortedByDescending { it.completedAt }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionIcon(AppIcons.ArrowBack, l("Назад", "Back"), back)
            Text(
                l("История на проекта", "Project history"),
                Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Box(Modifier.weight(1f)) {
            Page {
                Heading(l("Свършеното остава тук.", "A record of what’s done."))
                Muted(p.title)
                Muted(
                    l(
                        "Махнете отметката, за да върнете задача в активните.",
                        "Uncheck a task to move it back to the active list.",
                    )
                )
                Section("${completed.size} · " + l("завършени задачи", "completed tasks"))
                if (completed.isEmpty())
                    Empty(
                        l(
                            "Историята започва с първата отметка.",
                            "History starts with the first checkmark.",
                        ),
                        l(
                            "Тук се показват само завършените задачи на този проект.",
                            "Only this project’s completed tasks appear here.",
                        ),
                    )
                completed.forEach { t ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskRow(t, w, toggle, open)
                        t.completedAt?.let { millis ->
                            Text(
                                l("Завършена на ", "Completed on ") +
                                    Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .format(
                                            DateTimeFormatter.ofPattern(
                                                "d MMM · HH:mm",
                                                if (english()) Locale.ENGLISH
                                                else Locale.forLanguageTag("bg"),
                                            )
                                        ),
                                Modifier.padding(start = 16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectBoard(
    tasks: List<Task>,
    open: (Task) -> Unit,
    move: (Task, String) -> Unit,
    history: () -> Unit,
) {
    val statuses = listOf("todo", "in_progress", "done")
    val labels =
        listOf(l("За изпълнение", "To do"), l("В процес", "In progress"), l("Готово", "Done"))
    Text(
        l(
            "Всички етапи на един екран. Използвайте ⋯ за преместване.",
            "All stages in one view. Use ⋯ to move a task.",
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val large = LocalDensity.current.fontScale > 1.3f
    val compactLanes = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp <= 340 && !large
    @Composable
    fun lane(index: Int, modifier: Modifier) {
        val status = statuses[index]
        val laneTasks = tasks.filter { it.status == status }
        val tint =
            when (index) {
                0 -> MaterialTheme.colorScheme.primary
                1 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        Column(
            modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(if (compactLanes) 4.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 62.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(labels[index], style = MaterialTheme.typography.labelMedium, color = tint)
                Text(
                    laneTasks.size.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = tint,
                )
            }
            if (status == "done") {
                Surface(
                    Modifier.fillMaxWidth().clickable(onClick = history),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        Modifier.padding(if (compactLanes) 4.dp else 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(AppIcons.MenuBook, null, Modifier.size(20.dp), tint = tint)
                        Text(
                            l("В историята", "In history"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Icon(
                            AppIcons.ArrowForward,
                            l("История на проекта", "Project history"),
                            Modifier.size(17.dp),
                        )
                    }
                }
            } else if (laneTasks.isEmpty()) {
                Text(
                    l("Няма задачи", "No tasks"),
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else
                laneTasks.forEach { task ->
                    var menu by remember(task.id) { mutableStateOf(false) }
                    Surface(
                        Modifier.fillMaxWidth().clickable { open(task) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            Modifier.padding(if (compactLanes) 4.dp else 9.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                task.title,
                                style = if (compactLanes) MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    lineHeight = 14.sp,
                                    hyphens = androidx.compose.ui.text.style.Hyphens.None,
                                    lineBreak = androidx.compose.ui.text.style.LineBreak.Simple,
                                ) else MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (task.priority >= 2)
                                Text(
                                    priorityLabel(task.priority),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            task.due?.let {
                                Text(
                                    displayDate(it),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box(Modifier.align(Alignment.End)) {
                                val moveLabel = l("Премести", "Move") + ": " + task.title
                                IconButton(
                                    onClick = { menu = true },
                                    modifier =
                                        Modifier.size(40.dp).semantics {
                                            contentDescription = moveLabel
                                        },
                                ) {
                                    Icon(AppIcons.MoreHoriz, null, Modifier.size(20.dp))
                                }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    statuses.forEachIndexed { target, s ->
                                        if (s != status)
                                            DropdownMenuItem(
                                                text = { Text(labels[target]) },
                                                onClick = {
                                                    menu = false
                                                    move(task, s)
                                                },
                                            )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
    if (large) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            statuses.indices.forEach { lane(it, Modifier.fillMaxWidth()) }
        }
    } else
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Top,
        ) {
            statuses.indices.forEach { lane(it, Modifier.weight(1f)) }
        }
}
