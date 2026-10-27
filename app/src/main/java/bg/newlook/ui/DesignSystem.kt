package bg.newlook.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 840.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = contentGutter())
                    .padding(top = 20.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}

@Composable
fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge)
}

@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.secondary) {
    Text(text, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = color)
}

@Composable
fun Muted(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun PageHeader(
    title: String,
    kicker: String? = null,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (kicker != null) Eyebrow(kicker)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            action?.invoke()
        }
        if (subtitle != null) Muted(subtitle)
    }
}

@Composable
fun Section(title: String, action: String? = null, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        if (action != null)
            TextButton(onClick = onClick, contentPadding = PaddingValues(start = 12.dp)) {
                Text(action, style = MaterialTheme.typography.labelMedium)
            }
    }
}

@Composable
fun CardBlock(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(
                if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp <= 340)
                    16.dp
                else 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Box(
        Modifier.size(44.dp).background(background, RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(21.dp), tint = tint)
    }
}

@Composable
fun ActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
    ) {
        Icon(icon, label, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun Tag(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Text(
        text,
        Modifier.background(background, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
    )
}

@Composable
fun Segments(labels: List<String>, selected: Int, change: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            val bg by
                animateColorAsState(
                    if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                    label = "segment",
                )
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable { change(index) }
                    .padding(horizontal = 5.dp, vertical = 12.dp)
                    .semantics { this.selected = active },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color =
                        if (active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun Hero(eyebrow: String, title: String, caption: String, progress: Float) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF6251E7), Color(0xFF3E348F))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width * .95f, size.height * .2f)
            drawCircle(Color.White.copy(alpha = .045f), size.width * .42f, center)
            drawCircle(Color.White.copy(alpha = .05f), size.width * .28f, center)
            drawCircle(
                Color(0xFFBDE9D5).copy(alpha = .5f),
                5.dp.toPx(),
                androidx.compose.ui.geometry.Offset(size.width * .85f, size.height * .72f),
            )
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                eyebrow,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.6.sp,
                color = Color(0xFFDCD7FF),
            )
            Text(
                title,
                Modifier.fillMaxWidth(.9f),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFFBDE9D5),
                trackColor = Color.White.copy(alpha = .18f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(caption, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEAE6FF))
        }
    }
}

@Composable
fun Empty(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WorkspaceAnimation(false, Modifier.size(120.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Muted(subtitle)
    }
}

@Composable
fun BackTitle(title: String, back: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ActionIcon(AppIcons.ArrowBack, l("Назад", "Back"), back)
        Heading(title)
    }
}

@Composable
fun MenuCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 14.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        Icon(
            AppIcons.ChevronRight,
            null,
            Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SearchField(value: String, change: (String) -> Unit) {
    TextField(
        value,
        change,
        Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp)),
        placeholder = {
            Text(l("Търсете нещо…", "Find something…"), style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = { Icon(AppIcons.Search, null, Modifier.size(20.dp)) },
        trailingIcon = {
            if (value.isNotEmpty())
                IconButton(onClick = { change("") }) {
                    Icon(
                        AppIcons.Close,
                        l("Изчисти търсенето", "Clear search"),
                        Modifier.size(18.dp),
                    )
                }
        },
        singleLine = true,
        shape = RoundedCornerShape(17.dp),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

@Composable
fun QuickAdd(
    placeholder: String = l("Добавете нещо за днес…", "Add something for today…"),
    add: (String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    TextField(
        title,
        { title = it },
        Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(AppIcons.Add, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
            if (title.isNotBlank())
                IconButton(
                    onClick = {
                        add(title.trim())
                        title = ""
                    }
                ) {
                    Icon(AppIcons.ArrowForward, l("Добави задача", "Add task"))
                }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

@Composable
fun TaskRow(
    t: Task,
    w: Workspace,
    toggle: (Task) -> Unit,
    open: (Task) -> Unit,
    selected: Boolean = false,
    select: (() -> Unit)? = null,
) {
    var completing by remember(t.id, t.status) { mutableStateOf(false) }
    LaunchedEffect(completing) {
        if (completing) {
            kotlinx.coroutines.delay(1500)
            completing = false
        }
    }
    val done = t.status == "done" || completing
    val steps = substeps(t)
    val project = w.projects.find { it.id == t.projectId }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable { if (select != null) select() else open(t) }
            .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = if (select != null) selected else done,
            onCheckedChange = {
                if (select != null) select()
                else if (!completing) {
                    completing = t.status != "done" && steps.none { !it.done }
                    toggle(t)
                }
            },
            modifier = Modifier.semantics { contentDescription = t.title },
            colors =
                CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkedColor = MaterialTheme.colorScheme.secondary,
                ),
        )
        Column(
            Modifier.weight(1f).padding(start = 2.dp, end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                t.title,
                style = MaterialTheme.typography.titleSmall,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color =
                    if (done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            )
            val meta =
                listOfNotNull(
                    project?.title,
                    if (t.due != null) displayDate(t.due) else null,
                    t.time,
                    if (steps.isNotEmpty()) "${steps.count { it.done }}/${steps.size}" else null,
                    if (t.demo) l("Демо", "Demo") else null,
                )
            if (meta.isNotEmpty())
                Text(
                    meta.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            if (overdue(t) || t.priority >= 2 || t.status == "in_progress")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (overdue(t))
                        Tag(
                            l("Просрочена", "Overdue"),
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = .5f),
                        )
                    if (t.priority >= 2) Tag(priorityLabel(t.priority))
                    if (t.status == "in_progress")
                        Tag(
                            statusLabel(t.status),
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.secondaryContainer,
                        )
                }
        }
        IconButton(onClick = { open(t) }, modifier = Modifier.size(40.dp)) {
            Icon(
                AppIcons.ChevronRight,
                l("Отвори задача", "Open task"),
                Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
fun statusLabel(status: String) =
    when (status) {
        "done" -> l("Завършена", "Done")
        "in_progress" -> l("В процес", "In progress")
        else -> l("За изпълнение", "To do")
    }

@Composable
fun priorityLabel(value: Int) =
    when (value) {
        0 -> l("Нисък", "Low")
        2 -> l("Висок", "High")
        3 -> l("Спешен", "Urgent")
        else -> l("Нормален", "Normal")
    }

@Composable
fun displayDate(raw: String) =
    LocalDate.parse(raw)
        .format(
            DateTimeFormatter.ofPattern(
                "d MMM",
                if (english()) Locale.ENGLISH else Locale.forLanguageTag("bg"),
            )
        )

@Composable
fun ProjectCard(p: Project, w: Workspace, modifier: Modifier = Modifier, open: () -> Unit) {
    val tasks = w.tasks.filter { it.projectId == p.id }
    val done = tasks.count { it.status == "done" }
    val tint =
        when (p.color) {
            1 -> MaterialTheme.colorScheme.secondary
            2 ->
                if (MaterialTheme.colorScheme.background.luminance() < .5f) Color(0xFFF6C882)
                else Color(0xFF986024)
            else -> MaterialTheme.colorScheme.primary
        }
    Surface(
        modifier.fillMaxWidth().clickable(onClick = open),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.FolderOpen, null, Modifier.size(23.dp), tint = tint)
                Spacer(Modifier.weight(1f))
                Text(
                    if (tasks.isEmpty()) "—" else "${done * 100 / tasks.size}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = tint,
                )
            }
            Text(p.title, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = tint,
                trackColor = tint.copy(alpha = .1f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Row {
                Text(
                    if (tasks.isEmpty()) l("Все още няма задачи", "No tasks yet")
                    else "$done/${tasks.size} · " + l("завършени", "completed"),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (p.target != null)
                    Text(
                        displayDate(p.target),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
    }
}

@Composable
fun EventRow(e: Event, open: (Event) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable { open(e) }
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.widthIn(min = 46.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (e.allDay) l("ДЕН", "DAY")
                else
                    Instant.parse(e.start)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Box(
                Modifier.width(2.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = .25f))
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(e.title, style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(
                        l("Събитие", "Event"),
                        e.location.ifBlank { null },
                        if (e.zone != ZoneId.systemDefault().id) e.zone else null,
                    )
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            AppIcons.ChevronRight,
            null,
            Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

fun newEvent(date: LocalDate = LocalDate.now()): Event {
    val start = date.atTime(LocalTime.now().hour, 0).atZone(ZoneId.systemDefault())
    return Event(
        start = start.toInstant().toString(),
        end = start.plusHours(1).toInstant().toString(),
    )
}

@Composable
fun BrandWordmark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(bg.newlook.R.drawable.newlook_logo),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
        Text("NEW LOOK", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
    }
}
