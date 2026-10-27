@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package bg.newlook.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*

data class ListPreferences(
    val group: Int = 0,
    val statuses: String = "",
    val priorities: String = "",
    val projects: String = "",
    val tags: String = "",
    val deadline: Int = 0,
)

@Composable
fun TasksScreen(
    w: Workspace,
    settings: Settings,
    repo: Repository,
    vm: WorkspaceModel,
    search: String,
    onSearch: (String) -> Unit,
    open: (Task) -> Unit,
    toggle: (Task) -> Unit,
    delete: (List<Task>) -> Unit,
) {
    val prefs = remember {
        runCatching { json.fromJson(settings.filters, ListPreferences::class.java) }.getOrNull()
            ?: ListPreferences()
    }
    val exporter = rememberFileExport("text/csv", vm)
    var group by rememberSaveable { mutableIntStateOf(prefs.group) }
    var filter by rememberSaveable { mutableStateOf(false) }
    var statuses by rememberSaveable { mutableStateOf(prefs.statuses) }
    var priorities by rememberSaveable { mutableStateOf(prefs.priorities) }
    var projects by rememberSaveable { mutableStateOf(prefs.projects) }
    var tags by rememberSaveable { mutableStateOf(prefs.tags) }
    var deadline by rememberSaveable { mutableIntStateOf(prefs.deadline) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var bulkComplete by remember { mutableStateOf<List<Task>?>(null) }
    var bulkDate by rememberSaveable { mutableStateOf("") }
    var bulkProject by rememberSaveable { mutableStateOf<String?>(null) }
    var bulkTools by remember { mutableStateOf(false) }
    LaunchedEffect(group, statuses, priorities, projects, tags, deadline) {
        repo.setting(
            "filters",
            json.toJson(ListPreferences(group, statuses, priorities, projects, tags, deadline)),
        )
    }
    val f =
        Filters(
            statuses.split(',').filter { it.isNotBlank() }.toSet(),
            priorities.split(',').mapNotNull { it.toIntOrNull() }.toSet(),
            projects.split(',').filter { it.isNotBlank() }.toSet(),
            tags.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet(),
            when (deadline) {
                1 -> true
                2 -> false
                else -> null
            },
        )
    val today = LocalDate.now()
    val list =
        w.tasks
            .filter { t ->
                matches(t, search, f) &&
                    (group == 5 || t.status != "done") &&
                    when (group) {
                        1 -> t.status != "done" && t.due == today.toString()
                        2 -> overdue(t)
                        3 ->
                            t.status != "done" &&
                                t.due?.let { LocalDate.parse(it) in today..today.plusDays(6) } ==
                                    true
                        4 -> t.due == null
                        5 -> t.status == "done"
                        6 -> t.projectId == null
                        else -> true
                    }
            }
            .let { ts ->
                when (settings.sort) {
                    "priority" -> ts.sortedByDescending { it.priority }
                    "created" -> ts.sortedByDescending { it.createdAt }
                    else ->
                        ts.sortedWith(
                            compareBy<Task> { it.due == null }
                                .thenBy { it.due }
                                .thenBy { it.time ?: "23:59" }
                        )
                }
            }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = contentGutter()),
        contentPadding = PaddingValues(top = 20.dp, bottom = 94.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageHeader(
                l("Една по една.", "One thing at a time."),
                l("МОИТЕ ЗАДАЧИ", "MY TASKS"),
                l("Малки стъпки. Видим напредък.", "Small steps. Visible progress."),
            )
        }
        item { SearchField(search, onSearch) }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                        l("Всички", "All"),
                        l("Днес", "Today"),
                        l("Просрочени", "Overdue"),
                        l("7 дни", "7 days"),
                        l("Без срок", "No date"),
                        l("Завършени", "Done"),
                        l("Входящи", "Inbox"),
                    )
                    .forEachIndexed { i, label ->
                        FilterChip(
                            selected = group == i,
                            onClick = { group = i },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(13.dp),
                            border = null,
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                                    selectedLabelColor = MaterialTheme.colorScheme.background,
                                ),
                        )
                    }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${list.size} " + l("задачи", "tasks"),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = { filter = true }) {
                    Icon(AppIcons.Tune, l("Филтри", "Filters"), Modifier.size(20.dp))
                }
                TextButton(
                    onClick = {
                        selecting = !selecting
                        selected = emptyList()
                    }
                ) {
                    Text(if (selecting) l("Готово", "Done") else l("Избор", "Select"))
                }
            }
        }
        if (selecting)
            item {
                CardBlock {
                    Text(
                        "${selected.size} · " + l("избрани", "selected"),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { selected = list.map { it.id } }) {
                            Text(l("Всички", "All"))
                        }
                        TextButton(
                            onClick = {
                                val ts = w.tasks.filter { it.id in selected }
                                if (ts.any { substeps(it).any { step -> !step.done } })
                                    bulkComplete = ts
                                else {
                                    vm.run({ ts.forEach { repo.save(completed(it, true)) } })
                                    selected = emptyList()
                                }
                            },
                            enabled = selected.isNotEmpty(),
                        ) {
                            Text(l("Завърши", "Complete"))
                        }
                        TextButton(
                            onClick = { bulkTools = true },
                            enabled = selected.isNotEmpty(),
                        ) {
                            Text(l("Действия", "Actions"))
                            Spacer(Modifier.width(6.dp))
                            Icon(AppIcons.ExpandMore, null, Modifier.size(16.dp))
                        }
                    }
                }
            }
        if (list.isEmpty())
            item {
                Empty(
                    l("Няма задачи тук.", "Nothing here yet."),
                    l(
                        "Добавете задача или променете филтрите.",
                        "Add a task or adjust your filters.",
                    ),
                )
            }
        items(list, key = { it.id }) { t ->
            Box(Modifier.animateItem()) {
                TaskRow(
                    t,
                    w,
                    toggle,
                    open,
                    t.id in selected,
                    if (selecting) {
                        { selected = if (t.id in selected) selected - t.id else selected + t.id }
                    } else null,
                )
            }
        }
    }
    if (bulkTools)
        ModalBottomSheet(
            onDismissRequest = { bulkTools = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = contentGutter())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Heading(l("Избраните задачи.", "Your selected tasks."))
                Muted("${selected.size} · " + l("избрани", "selected"))
                DateField(bulkDate, { bulkDate = it }, l("Нов срок", "New due date"))
                Button(
                    onClick = {
                        val date = LocalDate.parse(bulkDate).toString()
                        val ts = w.tasks.filter { it.id in selected }
                        vm.run(
                            { ts.forEach { repo.save(it.copy(due = date)) } },
                            { bulkTools = false },
                        )
                    },
                    enabled =
                        selected.isNotEmpty() &&
                            runCatching { LocalDate.parse(bulkDate) }.isSuccess,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Text(l("Промени само срока", "Change due date only"))
                }
                ProjectChoice(bulkProject, w) { bulkProject = it }
                OutlinedButton(
                    onClick = {
                        val ts = w.tasks.filter { it.id in selected }
                        vm.run(
                            { ts.forEach { repo.save(it.copy(projectId = bulkProject)) } },
                            { bulkTools = false },
                        )
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(l("Назначи само проекта", "Assign project only"))
                }
                TextButton(
                    onClick = {
                        exporter("newlook-selected.csv", csv(w.tasks.filter { it.id in selected }))
                    },
                    enabled = selected.isNotEmpty(),
                ) {
                    Icon(AppIcons.SaveAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(l("Експорт на избраните CSV", "Export selected as CSV"))
                }
                TextButton(
                    onClick = {
                        bulkTools = false
                        confirmDelete = true
                    },
                    enabled = selected.isNotEmpty(),
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                ) {
                    Text(l("Изтрий избраните", "Delete selected"))
                }
            }
        }
    if (filter)
        ModalBottomSheet(
            onDismissRequest = { filter = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = contentGutter())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Heading(l("Изглед по ваш вкус.", "Your list, your way."))
                Section(l("Статус", "Status"))
                MultiChoice(listOf("todo", "in_progress", "done"), statuses, { statuses = it }) {
                    statusLabel(it)
                }
                Section(l("Приоритет", "Priority"))
                MultiChoice(listOf("0", "1", "2", "3"), priorities, { priorities = it }) {
                    priorityLabel(it.toInt())
                }
                if (w.projects.isNotEmpty()) {
                    Section(l("Проект", "Project"))
                    MultiChoice(w.projects.map { it.id }, projects, { projects = it }) { id ->
                        w.projects.find { it.id == id }?.title ?: ""
                    }
                }
                Field(
                    tags,
                    { tags = it },
                    l("Тагове (разделени със запетая)", "Tags (comma separated)"),
                )
                Choice(
                    l("Срок", "Due date"),
                    deadline,
                    listOf(l("Всички", "All"), l("Със срок", "With date"), l("Без срок", "No date")),
                ) {
                    deadline = it
                }
                Choice(
                    l("Подреждане", "Sort"),
                    listOf("due", "priority", "created").indexOf(settings.sort),
                    listOf(
                        l("Срок", "Due date"),
                        l("Приоритет", "Priority"),
                        l("Създаване", "Created"),
                    ),
                ) { i ->
                    vm.run({ repo.setting("sort", listOf("due", "priority", "created")[i]) })
                }
                Row {
                    TextButton(
                        onClick = {
                            statuses = ""
                            priorities = ""
                            projects = ""
                            tags = ""
                            deadline = 0
                            group = 0
                        }
                    ) {
                        Text(l("Изчисти", "Reset"))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { filter = false }, shape = RoundedCornerShape(14.dp)) {
                        Text(l("Покажи задачите", "Show tasks"))
                    }
                }
            }
        }
    bulkComplete?.let { ts ->
        Confirm(
            l("Завършване на избраните задачи?", "Complete selected tasks?"),
            l(
                "Всички незавършени подзадачи също ще бъдат завършени.",
                "All unfinished subtasks will also be completed.",
            ),
            { bulkComplete = null },
        ) {
            vm.run({ ts.forEach { repo.save(completed(it, true)) } })
            selected = emptyList()
            bulkComplete = null
        }
    }
    if (confirmDelete)
        Confirm(
            l("Изтриване на избраните задачи?", "Delete selected tasks?"),
            "${selected.size} · " + l("задачи ще бъдат изтрити.", "tasks will be deleted."),
            { confirmDelete = false },
        ) {
            delete(w.tasks.filter { it.id in selected })
            selected = emptyList()
            confirmDelete = false
        }
}

@Composable
fun MultiChoice(
    values: List<String>,
    selected: String,
    change: (String) -> Unit,
    label: @Composable (String) -> String,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { value ->
            val set = selected.split(',').filter { it.isNotBlank() }.toSet()
            FilterChip(
                selected = value in set,
                onClick = {
                    change((if (value in set) set - value else set + value).joinToString(","))
                },
                label = { Text(label(value)) },
                shape = RoundedCornerShape(12.dp),
                border = null,
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            )
        }
    }
}

@Composable
fun SearchResults(
    w: Workspace,
    q: String,
    task: (Task) -> Unit,
    project: (Project) -> Unit,
    note: (Note) -> Unit,
    navigate: (String) -> Unit,
) {
    val en = english()
    val tasks = w.tasks.filter { (it.title + it.description + it.tags).contains(q, true) }
    val projects = w.projects.filter { (it.title + it.description).contains(q, true) }
    val notes = w.notes.filter { (it.title + it.body).contains(q, true) }
    val foundServices =
        services.filter { (it.title.get(en) + it.description.get(en)).contains(q, true) }
    val articles = guides.filter { (it.title.get(en) + it.body.get(en)).contains(q, true) }
    if (
        tasks.isEmpty() &&
            projects.isEmpty() &&
            notes.isEmpty() &&
            foundServices.isEmpty() &&
            articles.isEmpty()
    )
        Empty(
            l("Нищо не открихме.", "Nothing found just yet."),
            l("Опитайте друга дума или по-кратка фраза.", "Try another word or a shorter phrase."),
        )
    if (tasks.isNotEmpty()) {
        Section(l("Задачи", "Tasks"))
        tasks.forEach { t -> MenuCard(t.title, AppIcons.Checklist) { task(t) } }
    }
    if (projects.isNotEmpty()) {
        Section(l("Проекти", "Projects"))
        projects.forEach { p -> MenuCard(p.title, AppIcons.FolderOpen) { project(p) } }
    }
    if (notes.isNotEmpty()) {
        Section(l("Бележки", "Notes"))
        notes.forEach { n ->
            MenuCard(n.title.ifBlank { l("Без заглавие", "Untitled") }, AppIcons.Description) {
                note(n)
            }
        }
    }
    if (foundServices.isNotEmpty() || articles.isNotEmpty())
        Section(l("От NEW LOOK", "From NEW LOOK"))
    foundServices.forEach { s ->
        MenuCard(s.title.get(en), AppIcons.Business) { navigate("service/${s.id}") }
    }
    articles.forEach { g ->
        MenuCard(g.title.get(en), AppIcons.MenuBook) { navigate("article/${g.id}") }
    }
}
