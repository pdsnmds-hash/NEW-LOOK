package bg.newlook.ui

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import bg.newlook.BuildConfig
import bg.newlook.MainActivity
import bg.newlook.data.*
import bg.newlook.domain.*
import kotlinx.coroutines.*

@Composable
fun rememberFileExport(mime: String, vm: WorkspaceModel): (String, String) -> Unit {
    val c = LocalContext.current
    var pendingName by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mime)) { uri ->
            val name = pendingName
            if (uri == null) {
                name?.let { java.io.File(c.cacheDir, "exports/$it").delete() }
                pendingName = null
            } else if (name != null)
                vm.run(
                    {
                        val file = java.io.File(c.cacheDir, "exports/$name")
                        require(file.isFile) {
                            "Export snapshot is unavailable. Please export again."
                        }
                        c.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                            file.inputStream().use { it.copyTo(output) }
                        } ?: error("Cannot open file")
                        file.delete()
                    },
                    { pendingName = null },
                )
        }
    return { name, text ->
        if (pendingName == null)
            vm.run({
                val file =
                    java.io.File(c.cacheDir, "exports/${java.util.UUID.randomUUID()}.snapshot")
                file.parentFile!!.mkdirs()
                file.writeText(text, Charsets.UTF_8)
                withContext(Dispatchers.Main) {
                    pendingName = file.name
                    launcher.launch(name)
                }
            })
    }
}

@Composable
fun BackupScreen(repo: Repository, w: Workspace, vm: WorkspaceModel, back: () -> Unit) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val export = rememberFileExport("application/json", vm)
    val exportCsv = rememberFileExport("text/csv", vm)
    var preview by remember { mutableStateOf<Backup?>(null) }
    var offered by remember { mutableStateOf(false) }
    var imported by remember { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                vm.run({
                    val text =
                        c.contentResolver.openInputStream(uri)?.use { stream ->
                            val target = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var count = 0
                            while (true) {
                                val n = stream.read(buffer)
                                if (n < 0) break
                                count += n
                                require(count <= 10 * 1024 * 1024) { "Maximum backup size: 10 MB" }
                                target.write(buffer, 0, n)
                            }
                            val bytes = target.toByteArray()
                            require(bytes.size <= 10 * 1024 * 1024) { "Maximum backup size: 10 MB" }
                            String(bytes, Charsets.UTF_8)
                        } ?: error("Cannot read file")
                    preview = validateBackup(text)
                })
        }
    Page {
        BackTitle(l("Резервни копия", "Backups"), back)
        CardBlock {
            Text(l("Вие управлявате данните си.", "You control your data."))
            Muted(
                l(
                    "JSON копието съдържа работните записи без прикачените файлове и не е шифровано. Запазете оригиналните файлове отделно. Автоматичното облачно архивиране е изключено.",
                    "The JSON backup contains workspace records, without attached files, and is not encrypted. Keep original files separately. Automatic cloud backup is disabled.",
                )
            )
        }
        Button(
            onClick = {
                vm.run({
                    val data = json.toJson(repo.backup())
                    withContext(Dispatchers.Main) {
                        export("newlook-${java.time.LocalDate.now()}.json", data)
                    }
                })
            }
        ) {
            Text(l("Експорт на работните записи", "Export workspace records"))
        }
        OutlinedButton(onClick = { exportCsv("newlook-tasks.csv", csv(w.tasks)) }) {
            Text(l("Експорт на задачи CSV", "Export tasks as CSV"))
        }
        OutlinedButton(onClick = { picker.launch(arrayOf("application/json", "text/plain")) }) {
            Text(l("Импорт на JSON", "Import JSON"))
        }
        Muted(
            l(
                "Импортът заменя текущото пространство само след проверка и потвърждение. Максимум 10 MB и 20 000 записа.",
                "Import replaces the current workspace after validation and confirmation. Maximum 10 MB and 20,000 records.",
            )
        )
        if (imported) Text(l("Работното пространство е възстановено.", "Workspace restored."))
        preview?.let { b ->
            CardBlock {
                Section(l("Предварителен преглед", "Import preview"))
                Text("${b.tasks.size} · " + l("задачи", "tasks"))
                Text("${b.projects.size} · " + l("проекти", "projects"))
                Text("${b.events.size} · " + l("събития", "events"))
                Text("${b.notes.size} · " + l("бележки", "notes"))
                Muted(
                    l(
                        "Първо запазете копие на текущите записи.",
                        "Save a copy of your current records first.",
                    )
                )
                OutlinedButton(
                    onClick = {
                        vm.run({
                            val data = json.toJson(repo.backup())
                            withContext(Dispatchers.Main) {
                                export("newlook-before-import.json", data)
                            }
                        })
                    }
                ) {
                    Text(l("Запази текущо копие", "Back up current workspace"))
                }
                Button(onClick = { offered = true }) {
                    Text(l("Замени текущото пространство", "Replace current workspace"))
                }
                TextButton(onClick = { preview = null }) { Text(l("Отказ", "Cancel")) }
            }
        }
    }
    if (offered)
        Confirm(
            l("Замяна на всички текущи записи?", "Replace all current records?"),
            l(
                "Проверете дали сте запазили резервно копие. Действието заменя цялото пространство.",
                "Make sure you saved a backup. This replaces the entire workspace.",
            ),
            { offered = false },
        ) {
            preview?.let { b ->
                vm.run(
                    { repo.replace(b) },
                    {
                        preview = null
                        offered = false
                        imported = true
                    },
                )
            }
        }
}

@Composable
fun SettingsScreen(
    repo: Repository,
    w: Workspace,
    settings: Settings,
    vm: WorkspaceModel,
    back: () -> Unit,
) {
    var introduction by rememberSaveable { mutableStateOf(false) }
    if (introduction) {
        androidx.activity.compose.BackHandler { introduction = false }
        Onboarding(preview = true) { introduction = false }
        return
    }
    val c = LocalContext.current
    val activity = LocalActivity.current as? MainActivity
    var reset by remember { mutableStateOf(false) }
    var demoDelete by remember { mutableStateOf(false) }
    var allowed by remember {
        mutableStateOf(NotificationManagerCompat.from(c).areNotificationsEnabled())
    }
    LifecycleResumeEffect(Unit) {
        allowed = NotificationManagerCompat.from(c).areNotificationsEnabled()
        onPauseOrDispose {}
    }
    Page {
        BackTitle(l("По вашия начин.", "Make it yours."), back)
        Muted(
            l(
                "Малките настройки правят голяма разлика.",
                "Small preferences. A more personal space.",
            )
        )
        MenuCard(l("Запознаване с приложението", "App introduction"), AppIcons.AutoAwesome) {
            introduction = true
        }
        Section(l("Външен вид", "Appearance"))
        Segments(
            listOf(l("Система", "System"), l("Светла", "Light"), l("Тъмна", "Dark")),
            listOf("system", "light", "dark").indexOf(settings.theme),
        ) { i ->
            vm.run({ repo.setting("theme", listOf("system", "light", "dark")[i]) })
        }
        Section(l("Език", "Language"))
        Segments(listOf("Български", "English"), if (english()) 1 else 0) { i ->
            activity?.language(if (i == 0) "bg" else "en")
        }
        Section(l("Вашата седмица", "Your working week"))
        Choice(
            l("Начало на седмицата", "Week starts on"),
            if (settings.weekStart == 1) 0 else 1,
            listOf(l("Понеделник", "Monday"), l("Неделя", "Sunday")),
        ) { i ->
            vm.run({ repo.setting("weekStart", if (i == 0) 1 else 7) })
        }
        Choice(
            l("Стандартно напомняне", "Default reminder"),
            listOf(10, 15, 30, 60).indexOf(settings.reminder),
            listOf("10 min", "15 min", "30 min", "60 min"),
        ) { i ->
            vm.run({ repo.setting("reminder", listOf(10, 15, 30, 60)[i]) })
        }
        Section(l("Напомняния", "Reminders"))
        CardBlock {
            Row {
                IconTile(AppIcons.CalendarMonth)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        l("Локални известия", "Local notifications"),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        if (allowed) l("Разрешени", "Allowed") else l("Изключени", "Not allowed"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Muted(
                l(
                    "Android може да забави доставката. Задачите и календарът работят и без разрешение.",
                    "Android may delay delivery. Tasks and your calendar work without permission too.",
                )
            )
            TextButton(
                onClick = {
                    safeIntent(
                        c,
                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, c.packageName),
                    )
                }
            ) {
                Text(l("Управление на известията", "Manage notifications"))
            }
        }
        Section(l("Работно пространство", "Workspace"))
        CardBlock {
            Text(
                "${w.tasks.size} " +
                    l("задачи", "tasks") +
                    " · ${w.projects.size} " +
                    l("проекта", "projects"),
                style = MaterialTheme.typography.titleMedium,
            )
            Muted(
                l(
                    "Демоданните са обозначени и се премахват отделно от личните ви записи.",
                    "Demo records are labeled and can be removed separately from your own work.",
                )
            )
            TextButton(onClick = { vm.run({ repo.demo() }) }) {
                Icon(AppIcons.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(l("Разгледайте с демоданни", "Explore with demo data"))
            }
            TextButton(onClick = { demoDelete = true }) {
                Text(l("Премахни само демоданните", "Remove demo records only"))
            }
        }
        Row(Modifier.padding(top = 10.dp)) {
            Icon(
                AppIcons.Lock,
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Muted(
                l(
                    "Локално. Лично. Под ваш контрол. Без аналитика и автоматична отправка на работни записи.",
                    "Local. Personal. In your control. No analytics or automatic sharing of workspace records.",
                )
            )
        }
        TextButton(
            onClick = { reset = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(l("Изтрий всички работни данни", "Delete all workspace data"))
        }
        Text(
            "NEW LOOK 1 LTD EOOD · ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (reset)
        Confirm(
            l("Изтриване на всички данни?", "Delete all data?"),
            l(
                "Задачи, проекти, събития, бележки, собствени шаблони и избрани ще бъдат премахнати. Напомнянията ще бъдат отменени.",
                "Tasks, projects, events, notes, custom templates and favorites will be removed. Reminders will be canceled.",
            ),
            { reset = false },
        ) {
            vm.run({ repo.reset() }, { reset = false })
        }
    if (demoDelete)
        Confirm(
            l("Премахване на демоданните?", "Remove demo records?"),
            l(
                "Ще се премахнат само записите с обозначение Демо.",
                "Only records labeled Demo will be removed.",
            ),
            { demoDelete = false },
        ) {
            vm.run({ repo.removeDemo() }, { demoDelete = false })
        }
}
