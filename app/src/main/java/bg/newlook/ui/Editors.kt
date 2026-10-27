@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package bg.newlook.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, multiline: Boolean = false) {
    TextField(
        value,
        onChange,
        Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp)),
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        shape = RoundedCornerShape(17.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
    )
}

@Composable
fun DateField(value: String, onChange: (String) -> Unit, label: String) {
    val c = LocalContext.current
    val locale = if (english()) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("bg")
    val pick = {
        val d = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }
        DatePickerDialog(
                c,
                { _, y, m, day -> onChange(LocalDate.of(y, m + 1, day).toString()) },
                d.year,
                d.monthValue - 1,
                d.dayOfMonth,
            )
            .show()
        Unit
    }
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = pick),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.CalendarMonth,
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (value.isBlank()) l("Изберете дата", "Choose a date")
                    else
                        runCatching {
                                LocalDate.parse(value)
                                    .format(
                                        java.time.format.DateTimeFormatter.ofPattern(
                                            "d MMMM yyyy",
                                            locale,
                                        )
                                    )
                            }
                            .getOrDefault(value),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (value.isNotBlank())
                IconButton(onClick = { onChange("") }) {
                    Icon(AppIcons.Close, l("Изчисти датата", "Clear date"), Modifier.size(17.dp))
                }
            else
                IconButton(onClick = pick) {
                    Icon(
                        AppIcons.ChevronRight,
                        l("Избери дата", "Choose date"),
                        Modifier.size(18.dp),
                    )
                }
        }
    }
}

@Composable
fun TimeField(value: String, onChange: (String) -> Unit, label: String) {
    val c = LocalContext.current
    val pick = {
        val time = runCatching { LocalTime.parse(value) }.getOrElse { LocalTime.of(9, 0) }
        TimePickerDialog(
                c,
                { _, hour, minute -> onChange(LocalTime.of(hour, minute).toString()) },
                time.hour,
                time.minute,
                android.text.format.DateFormat.is24HourFormat(c),
            )
            .show()
        Unit
    }
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = pick),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value.ifBlank { l("Без час", "No time") },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            IconButton(
                onClick =
                    if (value.isBlank()) pick
                    else {
                        { onChange("") }
                    }
            ) {
                Icon(
                    if (value.isBlank()) AppIcons.Add else AppIcons.Close,
                    l("Промени часа", "Change time"),
                    Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun Choice(label: String, selected: Int, options: List<String>, change: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        Modifier.fillMaxWidth().clickable { expanded = true },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (LocalDensity.current.fontScale > 1.3f) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        options.getOrElse(selected) { "—" },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Icon(
                        AppIcons.ExpandMore,
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    options.getOrElse(selected) { "—" },
                    Modifier.weight(1.1f),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    AppIcons.ExpandMore,
                    null,
                    Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
    }
    if (expanded)
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Heading(label)
                options.forEachIndexed { i, text ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(17.dp))
                            .clickable {
                                expanded = false
                                change(i)
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = i == selected,
                            onClick = {
                                expanded = false
                                change(i)
                            },
                        )
                        Text(
                            text,
                            Modifier.weight(1f).padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
}

@Composable
fun ProjectChoice(id: String?, w: Workspace, change: (String?) -> Unit) {
    val ps = w.projects
    Choice(
        l("Проект", "Project"),
        ps.indexOfFirst { it.id == id } + 1,
        listOf(l("Входящи", "Inbox")) + ps.map { it.title },
    ) {
        change(if (it == 0) null else ps[it - 1].id)
    }
}

@Composable
fun Confirm(title: String, body: String, dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = confirm) { Text(l("Потвърди", "Confirm")) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(l("Отказ", "Cancel")) } },
    )
}

@Composable
fun EditorShell(
    title: String,
    dirty: Boolean,
    dismiss: () -> Unit,
    save: () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    val close = { if (dirty) confirm = true else dismiss() }
    Dialog(
        onDismissRequest = close,
        properties =
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val dialogView = androidx.compose.ui.platform.LocalView.current
        val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
        SideEffect {
            (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.let {
                window ->
                androidx.core.view.WindowCompat.getInsetsController(window, dialogView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BackHandler { close() }
            Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionIcon(AppIcons.Close, l("Затвори", "Close"), close)
                    Text(
                        title,
                        Modifier.weight(1f).padding(horizontal = 14.dp),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Button(
                        onClick = save,
                        enabled = enabled,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Text(l("Запази", "Save"))
                    }
                }
                Page(content)
            }
        }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(l("Незапазени промени", "Unsaved changes")) },
            text = { Text(l("Да запазим ли промените?", "Save your changes?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        save()
                    },
                    enabled = enabled,
                ) {
                    Text(l("Запази", "Save"))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            confirm = false
                            dismiss()
                        }
                    ) {
                        Text(l("Отхвърли", "Discard"))
                    }
                    TextButton(onClick = { confirm = false }) { Text(l("Остани", "Stay")) }
                }
            },
        )
}

@Composable
fun ReminderField(value: Int?, default: Int, change: (Int?) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var explain by remember { mutableStateOf(false) }
    Row {
        Switch(
            checked = value != null,
            onCheckedChange = {
                if (it) {
                    explain = true
                } else change(null)
            },
        )
        Text(l("Напомняне", "Reminder"), Modifier.padding(12.dp))
    }
    if (value != null) {
        Choice(
            l("Минути преди срока", "Minutes before"),
            listOf(0, 10, 15, 30, 60, 1440).indexOf(value),
            listOf("0", "10", "15", "30", "60", "1440"),
        ) {
            change(listOf(0, 10, 15, 30, 60, 1440)[it])
        }
        Muted(
            l(
                "Приблизително време; Android може да забави известието.",
                "Approximate timing; Android may delay delivery.",
            )
        )
    }
    if (explain)
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text(l("Напомняния за важните неща", "Reminders for what matters")) },
            text = {
                Text(
                    l(
                        "Разрешете известия, за да получавате локални напомняния. Отказът не пречи на работата.",
                        "Allow notifications for local reminders. You can keep using the app if you decline.",
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        explain = false
                        change(default)
                        if (Build.VERSION.SDK_INT >= 33)
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text(l("Продължи", "Continue"))
                }
            },
            dismissButton = {
                TextButton(onClick = { explain = false }) { Text(l("Отказ", "Cancel")) }
            },
        )
}
