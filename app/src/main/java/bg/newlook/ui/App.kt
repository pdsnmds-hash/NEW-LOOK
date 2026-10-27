@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package bg.newlook.ui

import android.content.*
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class WorkspaceModel : ViewModel() {
    val error = MutableStateFlow<String?>(null)

    fun run(block: suspend () -> Unit, after: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                after()
            } catch (e: Exception) {
                error.value = e.message ?: "Error"
            }
        }
    }
}

@Composable
fun NewLook(
    repo: Repository,
    deepId: String?,
    deepType: String?,
    deepGeneration: Long = 0,
    requestComplete: Boolean = false,
) {
    val vm: WorkspaceModel = viewModel()
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            listOf("task_success.json", "empty_list.json").forEach {
                com.airbnb.lottie.LottieCompositionFactory.fromAssetSync(
                    repo.context,
                    "animations/$it",
                )
            }
        }
    }
    val startupError by repo.startupError.collectAsStateWithLifecycle()
    val storedWorkspace by repo.workspace.collectAsStateWithLifecycle()
    val w = localizedDemo(storedWorkspace, english())
    val settings by repo.settings.collectAsStateWithLifecycle()
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        android.util.Log.d("NewLookStartup", "Awaiting repository: ${repo.loaded.isCompleted}")
        repo.loaded.await()
        ready = true
        android.util.Log.d("NewLookStartup", "UI ready")
    }
    LookTheme(settings.theme) {
        val activity = LocalActivity.current
        val view = LocalView.current
        val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
        SideEffect {
            activity?.let {
                WindowCompat.getInsetsController(it.window, view).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }
        }
        if (!ready) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                BrandSplashContent()
            }
            return@LookTheme
        }
        startupError?.let { message ->
            Page {
                Heading(l("Пространството не се отвори.", "Your workspace couldn’t open."))
                Muted(
                    l(
                        "Записите не са изтрити. Опитайте отново.",
                        "Your records haven’t been deleted. Please try again.",
                    )
                )
                Muted(message)
                Button(onClick = { vm.run({ repo.initialize() }) }) {
                    Text(l("Опитай отново", "Try again"))
                }
            }
            return@LookTheme
        }
        if (!settings.onboarded) {
            Onboarding { demo ->
                vm.run({
                    if (demo) repo.demo()
                    repo.setting("onboarded", true)
                })
            }
            return@LookTheme
        }
        val nav = rememberNavController()
        val back by nav.currentBackStackEntryAsState()
        val route = back?.destination?.route ?: "today"
        var taskJson by rememberSaveable { mutableStateOf<String?>(null) }
        var eventJson by rememberSaveable { mutableStateOf<String?>(null) }
        var planningEventId by rememberSaveable { mutableStateOf<String?>(null) }
        var projectJson by rememberSaveable { mutableStateOf<String?>(null) }
        var noteJson by rememberSaveable { mutableStateOf<String?>(null) }
        var createMenu by remember { mutableStateOf(false) }
        var search by rememberSaveable { mutableStateOf("") }
        val snack = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val error by vm.error.collectAsStateWithLifecycle()
        val en = english()

        var pendingDone by remember { mutableStateOf<Task?>(null) }
        var deepConsumed by rememberSaveable(deepId, deepGeneration) { mutableStateOf(false) }
        LaunchedEffect(deepId, deepGeneration, w.tasks.size, w.events.size) {
            if (deepId != null && !deepConsumed) {
                if (deepType == "event")
                    w.events
                        .find { it.id == deepId }
                        ?.let {
                            eventJson = json.toJson(it)
                            deepConsumed = true
                        }
                else
                    w.tasks
                        .find { it.id == deepId }
                        ?.let {
                            if (requestComplete) {
                                if (substeps(it).any { step -> !step.done }) pendingDone = it
                                else vm.run({ repo.save(completed(it, true)) })
                            } else taskJson = json.toJson(it)
                            deepConsumed = true
                        }
            }
        }
        var previousStatuses by remember { mutableStateOf<Map<String, String>?>(null) }
        var completionSequence by remember { mutableIntStateOf(0) }
        var showTaskSuccess by remember { mutableStateOf(false) }
        LaunchedEffect(storedWorkspace.tasks) {
            val previous = previousStatuses
            if (
                previous != null &&
                    storedWorkspace.tasks.any {
                        it.status == "done" && previous[it.id] != null && previous[it.id] != "done"
                    }
            ) {
                completionSequence++
                showTaskSuccess = true
            }
            previousStatuses = storedWorkspace.tasks.associate { it.id to it.status }
        }
        LaunchedEffect(completionSequence) {
            if (showTaskSuccess) {
                delay(2300)
                showTaskSuccess = false
            }
        }
        val openTask: (Task) -> Unit = { taskJson = json.toJson(it) }
        val openEvent: (Event) -> Unit = { eventJson = json.toJson(it) }
        val toggle: (Task) -> Unit = { t ->
            if (t.status != "done" && substeps(t).any { !it.done }) pendingDone = t
            else
                vm.run({
                    if (t.status != "done") delay(550)
                    repo.save(completed(t, t.status != "done"))
                })
        }
        val showFloatingCreate = route in listOf("tasks", "notes", "projects")
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                Column {
                    Box(
                        Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            showTaskSuccess,
                            enter =
                                fadeIn(tween(300)) +
                                    androidx.compose.animation.slideInVertically(tween(300)) {
                                        it / 8
                                    },
                            exit = fadeOut(tween(300)),
                        ) {
                            key(completionSequence) { TaskSuccessNotice() }
                        }
                    }
                    SnackbarHost(snack)
                }
            },
            bottomBar = {
                WorkspaceDock(route) { r ->
                    search = ""
                    if (r == "today") {
                        if (!nav.popBackStack("today", false))
                            nav.navigate("today") {
                                popUpTo(nav.graph.id) { inclusive = false }
                                launchSingleTop = true
                            }
                    } else {
                        nav.navigate(r) {
                            popUpTo("today") {
                                inclusive = false
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showFloatingCreate) {
                    FloatingActionButton(
                        onClick = {
                            when {
                                route == "notes" -> noteJson = json.toJson(Note())
                                route == "projects" -> projectJson = json.toJson(Project())
                                route.startsWith("calendar") -> eventJson = json.toJson(newEvent())
                                else ->
                                    taskJson =
                                        json.toJson(
                                            Task(
                                                projectId =
                                                    if (route == "project/{id}")
                                                        back?.arguments?.getString("id")
                                                    else null,
                                                due =
                                                    if (route == "today") LocalDate.now().toString()
                                                    else null,
                                            )
                                        )
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            AppIcons.Add,
                            when {
                                route == "notes" -> l("Нова бележка", "New note")
                                route == "projects" -> l("Нов проект", "New project")
                                route.startsWith("calendar") -> l("Ново събитие", "New event")
                                else -> l("Нова задача", "New task")
                            },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "today",
                modifier =
                    Modifier.padding(padding)
                        .padding(bottom = if (showFloatingCreate) 76.dp else 0.dp),
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(120)) },
            ) {
                composable("search") {
                    Page {
                        BackTitle(l("Търсене навсякъде", "Search everything")) {
                            nav.popBackStack()
                        }
                        SearchField(search) { search = it }
                        if (search.isNotBlank())
                            SearchResults(
                                w,
                                search,
                                openTask,
                                { projectJson = json.toJson(it) },
                                { noteJson = json.toJson(it) },
                            ) {
                                nav.navigate(it)
                            }
                    }
                }
                composable("today") {
                    TodayScreen(
                        w,
                        repo,
                        vm,
                        openTask,
                        toggle,
                        openEvent,
                        { nav.navigate(it) },
                        { createMenu = true },
                        { projectJson = json.toJson(Project()) },
                    )
                }
                composable("tasks") {
                    TasksScreen(w, settings, repo, vm, search, { search = it }, openTask, toggle) {
                        selected ->
                        scope.launch {
                            val old = selected.toList()
                            withContext(Dispatchers.IO) { old.forEach { repo.delete(it) } }
                            if (
                                snack.showSnackbar(
                                    if (en) "Tasks deleted" else "Задачите са изтрити",
                                    if (en) "Undo" else "Отмяна",
                                ) == SnackbarResult.ActionPerformed
                            )
                                withContext(Dispatchers.IO) { old.forEach { repo.save(it) } }
                        }
                    }
                }
                composable(
                    "calendar?day={day}",
                    arguments =
                        listOf(
                            androidx.navigation.navArgument("day") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        ),
                ) { entry ->
                    CalendarScreen(
                        w,
                        settings,
                        repo,
                        vm,
                        openTask,
                        toggle,
                        openEvent,
                        initialDate = entry.arguments?.getString("day"),
                    ) {
                        val meeting = newEvent(it)
                        planningEventId = meeting.id
                        eventJson = json.toJson(meeting)
                    }
                }
                composable("company") { CompanyScreen(w, repo, vm) { nav.navigate(it) } }
                composable("more") {
                    MoreScreen(
                        w,
                        search,
                        { search = it },
                        openTask,
                        { projectJson = json.toJson(it) },
                        { noteJson = json.toJson(it) },
                    ) {
                        nav.navigate(it)
                    }
                }
                composable("projects") {
                    Page {
                        BackTitle(l("Проекти", "Projects")) { nav.popBackStack() }
                        var archive by rememberSaveable { mutableStateOf(false) }
                        Row {
                            FilterChip(
                                selected = !archive,
                                onClick = { archive = false },
                                label = { Text(l("Активни", "Active")) },
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = archive,
                                onClick = { archive = true },
                                label = { Text(l("Архив", "Archive")) },
                            )
                        }
                        w.projects
                            .filter { it.archived == archive }
                            .forEach { ProjectCard(it, w) { nav.navigate("project/${it.id}") } }
                        Button(onClick = { projectJson = json.toJson(Project()) }) {
                            Text(l("Нов проект", "New project"))
                        }
                    }
                }
                composable("project/{id}") { entry ->
                    val p = w.projects.find { it.id == entry.arguments?.getString("id") }
                    if (p != null)
                        ProjectScreen(
                            p,
                            w,
                            repo,
                            vm,
                            openTask,
                            toggle,
                            { projectJson = json.toJson(p) },
                            { noteJson = json.toJson(Note(projectId = p.id)) },
                            { noteJson = json.toJson(it) },
                            openEvent,
                            history = { nav.navigate("project/${p.id}/history") },
                            files = { nav.navigate("project/${p.id}/files") },
                        ) {
                            if (!nav.popBackStack()) nav.navigate("projects")
                        }
                }
                composable("project/{id}/files") { entry ->
                    val p = w.projects.find { it.id == entry.arguments?.getString("id") }
                    if (p != null) ProjectFilesScreen(p, repo, vm) { nav.popBackStack() }
                }
                composable("weekly-review") {
                    WeeklyReviewScreen(
                        w,
                        settings,
                        repo,
                        vm,
                        { nav.popBackStack() },
                        { nav.navigate("project/$it") },
                        openTask,
                    )
                }
                composable("project/{id}/history") { entry ->
                    val p = w.projects.find { it.id == entry.arguments?.getString("id") }
                    if (p != null)
                        ProjectHistoryScreen(p, w, openTask, toggle) {
                            if (!nav.popBackStack()) nav.navigate("projects")
                        }
                }
                composable("notes") {
                    NotesScreen(
                        w,
                        search,
                        { search = it },
                        { noteJson = json.toJson(it) },
                        { noteJson = json.toJson(Note()) },
                    ) {
                        nav.popBackStack()
                    }
                }
                composable("templates") {
                    TemplateList(w, search, { search = it }, { nav.navigate("templates/new") }) {
                        nav.navigate("template/$it")
                    }
                }
                composable("templates/new") {
                    CreateTemplateScreen(repo, vm, { nav.popBackStack() }) { id ->
                        nav.navigate("template/$id") {
                            popUpTo("templates/new") { inclusive = true }
                        }
                    }
                }
                composable("template/{id}") {
                    TemplateDetail(
                        entryId = it.arguments?.getString("id") ?: "",
                        w = w,
                        repo = repo,
                        vm = vm,
                        back = { nav.popBackStack() },
                        openService = { id -> nav.navigate("service/$id") },
                        openProject = { id -> nav.navigate("project/$id") },
                    )
                }
                composable("guides") {
                    GuidesScreen(search, { search = it }, { nav.popBackStack() }) {
                        nav.navigate(it)
                    }
                }
                composable("article/{id}") {
                    val g = guides.find { g -> g.id == it.arguments?.getString("id") }
                    if (g != null)
                        ArticleScreen(g, w, repo, vm, { nav.popBackStack() }) { nav.navigate(it) }
                }
                composable("service/{id}") {
                    val service = services.find { s -> s.id == it.arguments?.getString("id") }
                    if (service != null)
                        ServiceScreen(service, w, repo, vm, { nav.popBackStack() }) {
                            nav.navigate(it)
                        }
                }
                composable("finder") { FinderScreen { nav.navigate(it) } }
                composable("contacts") { ContactsScreen { nav.popBackStack() } }
                composable("favorites") {
                    Page {
                        BackTitle(l("Избрани", "Favorites")) { nav.popBackStack() }
                        if (w.favorites.isEmpty())
                            Empty(
                                l("Запазете полезното.", "Keep what helps."),
                                l(
                                    "Добавете услуга, статия или шаблон в избрани.",
                                    "Save a service, guide or template.",
                                ),
                            )
                        w.favorites.forEach { f ->
                            val title =
                                when (f.type) {
                                    "service" ->
                                        services.find { it.id == f.targetId }?.title?.get(en)
                                    "article" -> guides.find { it.id == f.targetId }?.title?.get(en)
                                    else ->
                                        templates.find { it.id == f.targetId }?.title?.get(en)
                                            ?: w.templates.find { it.id == f.targetId }?.title
                                }
                            title?.let {
                                MenuCard(it, AppIcons.Bookmark) {
                                    nav.navigate(
                                        "${if(f.type=="service")"service" else if(f.type=="article")"article" else "template"}/${f.targetId}"
                                    )
                                }
                            }
                        }
                    }
                }
                composable("settings") {
                    SettingsScreen(repo, w, settings, vm) { nav.popBackStack() }
                }
                composable("backup") { BackupScreen(repo, w, vm) { nav.popBackStack() } }
            }
        }
        if (createMenu)
            CreateSheet({ createMenu = false }) { type ->
                when (type) {
                    0 ->
                        taskJson =
                            json.toJson(
                                Task(
                                    due = if (route == "today") LocalDate.now().toString() else null
                                )
                            )
                    1 -> projectJson = json.toJson(Project())
                    2 -> noteJson = json.toJson(Note())
                    3 -> eventJson = json.toJson(newEvent())
                }
            }
        taskJson?.let { raw ->
            TaskEditor(
                json.fromJson(raw, Task::class.java),
                w,
                settings,
                repo,
                vm,
                onDismiss = { taskJson = null },
                onDuplicate = { taskJson = json.toJson(it) },
                onDelete = { t ->
                    vm.run(
                        { repo.delete(t) },
                        {
                            taskJson = null
                            scope.launch {
                                if (
                                    snack.showSnackbar(
                                        if (en) "Task deleted" else "Задачата е изтрита",
                                        if (en) "Undo" else "Отмяна",
                                    ) == SnackbarResult.ActionPerformed
                                )
                                    vm.run({ repo.save(t) })
                            }
                        },
                    )
                },
            )
        }
        eventJson?.let {
            EventEditor(
                json.fromJson(it, Event::class.java),
                w,
                repo,
                vm,
                onSaved = { saved ->
                    if (planningEventId == saved.id) {
                        val day =
                            if (saved.allDay) LocalDate.parse(saved.start)
                            else
                                Instant.parse(saved.start)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                        nav.navigate("calendar?day=$day") {
                            popUpTo("today") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            ) {
                eventJson = null
                planningEventId = null
            }
        }
        projectJson?.let {
            ProjectEditor(
                json.fromJson(it, Project::class.java),
                w,
                repo,
                vm,
                onSaved = { saved ->
                    if (json.fromJson(it, Project::class.java).title.isBlank())
                        nav.navigate("project/${saved.id}")
                },
            ) {
                projectJson = null
            }
        }
        noteJson?.let {
            NoteEditor(json.fromJson(it, Note::class.java), w, repo, vm) { noteJson = null }
        }
        error?.let { message ->
            AlertDialog(
                onDismissRequest = { vm.error.value = null },
                title = { Text(l("Неуспешно действие", "Action failed")) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { vm.error.value = null }) { Text(l("Затвори", "Close")) }
                },
            )
        }
        pendingDone?.let { t ->
            AlertDialog(
                onDismissRequest = { pendingDone = null },
                title = { Text(l("Има незавършени подзадачи", "Unfinished subtasks")) },
                text = {
                    Text(l("Да завършим ли всички подзадачи?", "Complete all subtasks too?"))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.run({ repo.save(completed(t, true)) })
                            pendingDone = null
                        }
                    ) {
                        Text(l("Завърши всички", "Complete all"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDone = null }) { Text(l("Назад", "Go back")) }
                },
            )
        }
    }
}
