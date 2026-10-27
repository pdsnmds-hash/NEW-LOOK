package bg.newlook.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*
import java.time.format.*
import java.util.Locale

fun workspaceTab(route: String): String =
    when {
        route.startsWith("calendar") -> "calendar"
        route in listOf("today", "tasks", "company") -> route
        route.startsWith("service") || route in listOf("finder", "contacts") -> "company"
        else -> "more"
    }

@Composable
fun WorkspaceDock(route: String, navigate: (String) -> Unit) {
    val tabs = listOf("today", "tasks", "calendar", "company", "more")
    val labels =
        listOf(
            l("Днес", "Today"),
            l("Задачи", "Tasks"),
            l("Календар", "Calendar"),
            "NEW LOOK",
            l("Още", "More"),
        )
    val icons =
        listOf(
            AppIcons.Dashboard,
            AppIcons.Checklist,
            AppIcons.CalendarMonth,
            AppIcons.Business,
            AppIcons.MoreHoriz,
        )
    val active = workspaceTab(route)
    Box(
        Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp, top = 8.dp)
    ) {
        Surface(
            Modifier.fillMaxWidth()
                .shadow(
                    16.dp,
                    RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = .06f),
                    spotColor = Color.Black.copy(alpha = .06f),
                ),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
        ) {
            if (LocalDensity.current.fontScale > 1.3f) {
                Column(
                    Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = active == tab
                            Box(
                                Modifier.weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { navigate(tab) }
                                    .semantics {
                                        contentDescription = labels[index]
                                        this.selected = selected
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    icons[index],
                                    null,
                                    Modifier.size(24.dp),
                                    tint =
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Text(
                        labels[tabs.indexOf(active).coerceAtLeast(0)],
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else
                Row(Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = active == tab
                        val tint by
                            animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "dock",
                            )
                        Column(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { navigate(tab) }
                                .semantics { this.selected = selected }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                Modifier.width(42.dp)
                                    .height(29.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                        RoundedCornerShape(11.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (tab == "company")
                                    androidx.compose.foundation.Image(
                                        androidx.compose.ui.res.painterResource(
                                            bg.newlook.R.drawable.newlook_logo
                                        ),
                                        null,
                                        Modifier.size(29.dp),
                                    )
                                else Icon(icons[index], null, Modifier.size(20.dp), tint = tint)
                            }
                            Text(
                                labels[index],
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                            )
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSheet(dismiss: () -> Unit, create: (Int) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = dismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Heading(l("Нещо ново.", "Something new."))
            Muted(l("Освободете място за следващата стъпка.", "Make room for your next step."))
            listOf(
                    l("Задача", "Task") to AppIcons.Checklist,
                    l("Проект", "Project") to AppIcons.FolderOpen,
                    l("Бележка", "Note") to AppIcons.Description,
                    l("Събитие", "Event") to AppIcons.CalendarMonth,
                )
                .forEachIndexed { index, (name, icon) ->
                    MenuCard(name, icon) {
                        create(index)
                        dismiss()
                    }
                }
        }
    }
}

@Composable
fun TodayScreen(
    w: Workspace,
    repo: Repository,
    vm: WorkspaceModel,
    open: (Task) -> Unit,
    toggle: (Task) -> Unit,
    event: (Event) -> Unit,
    navigate: (String) -> Unit,
    create: () -> Unit,
    newProject: () -> Unit,
) {
    val locale = if (english()) Locale.ENGLISH else Locale.forLanguageTag("bg")
    val preferences by repo.settings.collectAsState()
    val now = LocalDate.now()
    val today = w.tasks.filter { it.due == now.toString() }
    val overdue = w.tasks.filter { overdue(it) && it.due != now.toString() }
    val done = today.count { it.status == "done" }
    val events = w.events.filter { eventOn(it, now) }
    val remaining = today.count { it.status != "done" }
    Page {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BrandWordmark()
            Spacer(Modifier.weight(1f))
            ActionIcon(AppIcons.Search, l("Търсене навсякъде", "Search everything")) {
                navigate("search")
            }
            Spacer(Modifier.width(8.dp))
            ActionIcon(AppIcons.Add, l("Създаване", "Create"), create)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)).replaceFirstChar {
                    it.titlecase(locale)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                l("Вашият ден,\nс повече яснота.", "Your day,\na little clearer."),
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        WeekStrip(now, w) { date -> navigate("calendar?day=$date") }
        DailyFocus(today.size, done)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                remaining.toString(),
                l("за днес", "for today"),
                AppIcons.Checklist,
                Modifier.weight(1f),
            ) {
                navigate("tasks")
            }
            StatTile(
                events.size.toString(),
                l("събития", "events"),
                AppIcons.CalendarMonth,
                Modifier.weight(1f),
            ) {
                navigate("calendar")
            }
            StatTile(
                w.projects.count { !it.archived }.toString(),
                l("проекта", "projects"),
                AppIcons.FolderOpen,
                Modifier.weight(1f),
            ) {
                navigate("projects")
            }
        }
        CardBlock {
            Text(
                l("Вашите собствени шаблони", "Your own templates"),
                style = MaterialTheme.typography.titleMedium,
            )
            Muted(
                l(
                    "Създайте свой чеклист веднъж и го използвайте във всеки проект. Можете да запазите и готов проект като шаблон.",
                    "Build a checklist once and reuse it in any project. You can also save an existing project as a template.",
                )
            )
            Button(
                onClick = { navigate("templates/new") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(AppIcons.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(l("Нов собствен шаблон", "Create your own template"))
            }
            OutlinedButton(
                onClick = { navigate("templates") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(l("Моите шаблони", "My templates") + " · ${w.templates.size}")
            }
        }
        CardBlock({ navigate("weekly-review") }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconTile(AppIcons.CheckCircle)
                Column(
                    Modifier.weight(1f).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        l("Седмичен обзор", "Weekly review"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val count =
                        weeklyReview(
                                w.tasks,
                                weekStart(now, preferences.weekStart),
                                ZoneId.systemDefault(),
                            )
                            .completed
                            .size
                    Muted(
                        "$count " +
                            l(
                                "завършени задачи · вижте напредъка",
                                "tasks completed · see your progress",
                            )
                    )
                }
                Icon(AppIcons.ChevronRight, null, Modifier.size(20.dp))
            }
        }
        val projects = w.projects.filter { !it.archived }
        Section(l("Моите проекти", "My projects"), l("Всички проекти", "All projects")) {
            navigate("projects")
        }
        if (projects.isEmpty())
            Empty(
                l("Започнете с проект.", "Start with a project."),
                l(
                    "Съберете задачите, бележките и събитията около една цел.",
                    "Bring tasks, notes and events together around one goal.",
                ),
            )
        projects.forEach { p -> ProjectCard(p, w) { navigate("project/${p.id}") } }
        OutlinedButton(
            onClick = newProject,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(17.dp),
        ) {
            Icon(AppIcons.Add, null, Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(l("Нов проект", "New project"))
        }
        if (events.isNotEmpty()) {
            Section(l("В календара", "In your calendar"))
            events.forEach { EventRow(it, event) }
        }
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .5f))
                .clickable { navigate("templates") }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.AutoAwesome,
                null,
                Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    l("Добър план. По-лесен старт.", "A good plan. A simpler start."),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    l("8 готови работни процеса", "8 ready-to-use workflows"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                AppIcons.ArrowForward,
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
fun DailyFocus(total: Int, done: Int) {
    val progress by
        androidx.compose.animation.core.animateFloatAsState(
            if (total == 0) 0f else done.toFloat() / total,
            label = "day progress",
        )
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(27.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF6251E7), Color(0xFF40348D))))
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                l("ДНЕС НА ФОКУС", "TODAY IN FOCUS"),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.3.sp,
                color = Color(0xFFDCD7FF),
            )
            Text(
                if (total == 0) l("Чисто начало.", "A fresh start.")
                else if (done == total) l("Добре свършена работа.", "A day well done.")
                else "${total-done} " + l("следващи стъпки.", "next steps."),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                if (total == 0)
                    l("Добавете първата задача за днес.", "Add your first task for today.")
                else "$done / $total · " + l("завършени задачи", "tasks completed"),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE8E4FF),
            )
        }
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val stroke =
                    androidx.compose.ui.graphics.drawscope.Stroke(
                        5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                val inset = 4.dp.toPx()
                val side =
                    androidx.compose.ui.geometry.Size(
                        size.width - inset * 2,
                        size.height - inset * 2,
                    )
                drawArc(
                    Color.White.copy(alpha = .14f),
                    -90f,
                    360f,
                    false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = side,
                    style = stroke,
                )
                drawArc(
                    Color(0xFFBDE9D5),
                    -90f,
                    360f * progress,
                    false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = side,
                    style = stroke,
                )
            }
            Text(
                if (total == 0) "—" else "${done*100/total}%",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
fun StatTile(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    click: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = click)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            Icon(
                icon,
                null,
                Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun WeekStrip(selected: LocalDate, w: Workspace, change: (LocalDate) -> Unit) {
    val monday = selected.minusDays(selected.dayOfWeek.value.toLong() - 1)
    val locale = if (english()) Locale.ENGLISH else Locale.forLanguageTag("bg")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(7) { index ->
            val day = monday.plusDays(index.toLong())
            val active = day == selected
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        1.dp,
                        if (active) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(17.dp),
                    )
                    .clickable { change(day) }
                    .semantics {
                        contentDescription =
                            day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
                        this.selected = active
                    }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                val color =
                    if (active) MaterialTheme.colorScheme.background
                    else MaterialTheme.colorScheme.onSurface
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = .7f),
                )
                Text(
                    day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                )
                Box(
                    Modifier.size(3.dp)
                        .background(
                            if (
                                w.tasks.any { it.due == day.toString() } ||
                                    w.events.any { eventOn(it, day) }
                            )
                                (if (active) Color(0xFFBCE4CF)
                                else MaterialTheme.colorScheme.primary)
                            else Color.Transparent,
                            CircleShape,
                        )
                )
            }
        }
    }
}

@Composable
fun MoreScreen(
    w: Workspace,
    search: String,
    change: (String) -> Unit,
    task: (Task) -> Unit,
    project: (Project) -> Unit,
    note: (Note) -> Unit,
    navigate: (String) -> Unit,
) {
    Page {
        PageHeader(
            l("Вашето пространство.", "Make space."),
            l("РАБОТА & ИДЕИ", "WORK & IDEAS"),
            l("Инструментите за по-подреден ден.", "A few tools for a more organized day."),
        )
        SearchField(search, change)
        if (search.isNotBlank()) SearchResults(w, search, task, project, note, navigate)
        else {
            val tiles =
                listOf(
                    Triple("projects", l("Проекти", "Projects"), AppIcons.FolderOpen),
                    Triple("notes", l("Бележки", "Notes"), AppIcons.Description),
                    Triple("templates", l("Шаблони", "Templates"), AppIcons.AutoAwesome),
                    Triple("guides", l("Справочник", "Guides"), AppIcons.MenuBook),
                )
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { index, (route, title, icon) ->
                        val detail =
                            when (route) {
                                "projects" ->
                                    "${w.projects.count { !it.archived }} " + l("активни", "active")
                                "notes" -> "${w.notes.size} " + l("записани", "saved")
                                "templates" ->
                                    "${8 + w.templates.size} " + l("работни процеса", "workflows")
                                else -> l("Практически насоки", "Practical reading")
                            }
                        Column(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (route == "templates")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    1.dp,
                                    if (route == "templates") Color.Transparent
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(24.dp),
                                )
                                .clickable { navigate(route) }
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Icon(
                                icon,
                                null,
                                Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Section(l("Съхранено & лично", "Saved & personal"))
            CardBlock {
                MenuCard(l("Избрани", "Favorites"), AppIcons.BookmarkBorder) {
                    navigate("favorites")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MenuCard(l("Резервни копия", "Backups"), AppIcons.SaveAlt) { navigate("backup") }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MenuCard(l("Седмичен обзор", "Weekly review"), AppIcons.CheckCircle) {
                    navigate("weekly-review")
                }
                MenuCard(l("Настройки", "Settings"), AppIcons.Settings) { navigate("settings") }
            }
            Row(
                Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    AppIcons.Lock,
                    null,
                    Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        l("Пространство само за вас.", "A space that’s yours."),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Muted(
                        l(
                            "Без профил, облак и автоматично споделяне на работните записи.",
                            "No account, cloud or automatic sharing of your work.",
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NotesScreen(
    w: Workspace,
    query: String,
    change: (String) -> Unit,
    open: (Note) -> Unit,
    create: () -> Unit,
    back: () -> Unit,
) {
    Page {
        BackTitle(l("Бележки", "Notes"), back)
        Muted(l("Мисли, които заслужават място.", "Thoughts worth keeping."))
        SearchField(query, change)
        val notes = w.notes.filter { (it.title + it.body + it.tags).contains(query, true) }
        if (notes.isEmpty())
            Empty(
                l("Една идея е достатъчна.", "One idea is a start."),
                l(
                    "Създайте бележка за среща, проект или следващата добра идея.",
                    "Save a meeting note, project thought or your next good idea.",
                ),
            )
        notes.forEach { n ->
            CardBlock({ open(n) }) {
                Row {
                    Text(
                        n.title.ifBlank { l("Без заглавие", "Untitled") },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (n.pinned)
                        Icon(
                            AppIcons.Bookmark,
                            l("Закрепена", "Pinned"),
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                }
                if (n.body.isNotBlank())
                    Text(
                        n.body.take(160),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                Row {
                    Text(
                        Instant.ofEpochMilli(n.updatedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("d MMM")),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    w.projects
                        .find { it.id == n.projectId }
                        ?.let {
                            Text(
                                it.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                }
            }
        }
        TextButton(onClick = create) {
            Icon(AppIcons.Add, null)
            Text(l("Нова бележка", "New note"))
        }
    }
}
