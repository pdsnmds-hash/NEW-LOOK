package bg.newlook.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    w: Workspace,
    settings: Settings,
    repo: Repository,
    vm: WorkspaceModel,
    open: (Task) -> Unit,
    toggle: (Task) -> Unit,
    event: (Event) -> Unit,
    initialDate: String? = null,
    create: (LocalDate) -> Unit,
) {
    val initial = runCatching { initialDate?.let(LocalDate::parse) }.getOrNull() ?: LocalDate.now()
    var mode by
        rememberSaveable(initialDate) {
            mutableStateOf(if (initialDate == null) "home" else "meetings")
        }
    var monthText by
        rememberSaveable(initialDate) { mutableStateOf(YearMonth.from(initial).toString()) }
    var selectedText by rememberSaveable(initialDate) { mutableStateOf(initial.toString()) }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.parse(selectedText)
    val locale = if (english()) Locale.ENGLISH else Locale.forLanguageTag("bg")
    BackHandler(enabled = mode != "home") { mode = "home" }
    if (mode == "home") {
        Page {
            PageHeader(
                l("Време за среща.", "Make time to meet."),
                l("КАЛЕНДАР", "CALENDAR"),
                l(
                    "Планирайте следващата среща или вижте вече запазените дати.",
                    "Plan your next meeting or see the dates you’ve already booked.",
                ),
            )
            Spacer(Modifier.height(8.dp))
            MeetingAction(
                l("Планирай среща", "Schedule a meeting"),
                l("Изберете дата и добавете подробностите.", "Choose a date and add the details."),
                AppIcons.Add,
                true,
            ) {
                mode = "plan"
            }
            MeetingAction(
                l("Моите срещи", "My meetings"),
                l(
                    "Вижте срещите си и заетите дати в календара.",
                    "See your meetings and the booked dates on your calendar.",
                ),
                AppIcons.CalendarMonth,
                false,
            ) {
                mode = "meetings"
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionIcon(AppIcons.ArrowBack, l("Назад", "Back")) { mode = "home" }
                Text(
                    if (mode == "plan") l("Планирай среща", "Schedule a meeting")
                    else l("Моите срещи", "My meetings"),
                    Modifier.weight(1f).padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Box(Modifier.weight(1f)) {
                Page {
                    if (mode == "plan")
                        Muted(
                            l("Изберете ден за новата среща.", "Choose a day for your new meeting.")
                        )
                    else
                        Muted(
                            l(
                                "Отбелязаните дни имат запазени срещи. Докоснете дата, за да ги видите.",
                                "Marked days have meetings. Tap a date to see them.",
                            )
                        )
                    CardBlock {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                month
                                    .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                                    .replaceFirstChar { it.titlecase(locale) },
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            IconButton(
                                onClick = {
                                    monthText = month.minusMonths(1).toString()
                                    selectedText = month.minusMonths(1).atDay(1).toString()
                                }
                            ) {
                                Icon(
                                    AppIcons.ChevronLeft,
                                    l("Предишен месец", "Previous month"),
                                    Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    monthText = month.plusMonths(1).toString()
                                    selectedText = month.plusMonths(1).atDay(1).toString()
                                }
                            ) {
                                Icon(
                                    AppIcons.ChevronRight,
                                    l("Следващ месец", "Next month"),
                                    Modifier.size(20.dp),
                                )
                            }
                        }
                        Row {
                            repeat(7) { i ->
                                Text(
                                    DayOfWeek.of((settings.weekStart - 1 + i) % 7 + 1)
                                        .getDisplayName(java.time.format.TextStyle.SHORT, locale)
                                        .take(2),
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        val offset = (month.atDay(1).dayOfWeek.value - settings.weekStart + 7) % 7
                        repeat((offset + month.lengthOfMonth() + 6) / 7) { row ->
                            Row {
                                repeat(7) { col ->
                                    val number = row * 7 + col - offset + 1
                                    if (number !in 1..month.lengthOfMonth())
                                        Spacer(Modifier.weight(1f))
                                    else {
                                        val day = month.atDay(number)
                                        val active = day == selected
                                        val count =
                                            if (mode == "meetings")
                                                w.events.count { eventOn(it, day) }
                                            else 0
                                        val description =
                                            if(mode=="plan")day.toString()else day.toString() +
                                                " · " +
                                                count +
                                                " " +
                                                l("срещи", "meetings")
                                        Column(
                                            Modifier.weight(1f)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(
                                                    if (active) MaterialTheme.colorScheme.primary
                                                    else if (count > 0)
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                    else Color.Transparent
                                                )
                                                .clickable { selectedText = day.toString() }
                                                .semantics {
                                                    contentDescription = description
                                                    this.selected = active
                                                }
                                                .padding(vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Text(
                                                number.toString(),
                                                style = MaterialTheme.typography.titleSmall,
                                                color =
                                                    if (active) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurface,
                                            )
                                            Box(
                                                Modifier.size(4.dp)
                                                    .background(
                                                        if (count > 0)
                                                            (if (active) Color(0xFFBDE9D5)
                                                            else
                                                                MaterialTheme.colorScheme.secondary)
                                                        else Color.Transparent,
                                                        CircleShape,
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (mode == "meetings")
                            Text(
                                "• " + l("Ден със срещи", "Day with meetings"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            selected
                                .format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale))
                                .replaceFirstChar { it.titlecase(locale) },
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(
                            onClick = {
                                selectedText = LocalDate.now().toString()
                                monthText = YearMonth.now().toString()
                            }
                        ) {
                            Text(l("Днес", "Today"))
                        }
                    }
                    if (mode == "plan")
                        Button(
                            onClick = { create(selected) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            shape = RoundedCornerShape(17.dp),
                        ) {
                            Icon(AppIcons.Add, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(l("Планирай за тази дата", "Schedule for this date"))
                        }
                    if (mode == "meetings") {
                        val meetings =
                            w.events.filter { eventOn(it, selected) }.sortedBy { it.start }
                        if (meetings.isEmpty())
                            Empty(
                                l("Няма срещи за тази дата.", "No meetings on this date."),
                                l(
                                    "Изберете отбелязан ден или планирайте нова среща.",
                                    "Choose a marked day or schedule a new meeting.",
                                ),
                            )
                        meetings.forEach { EventRow(it, event) }
                        OutlinedButton(
                            onClick = { mode = "plan" },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Text(l("Планирай среща", "Schedule a meeting"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetingAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    primary: Boolean,
    click: () -> Unit,
) {
    val bg = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg =
        if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = click),
        shape = RoundedCornerShape(26.dp),
        color = bg,
        border = if (primary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(28.dp), tint = fg)
                Spacer(Modifier.weight(1f))
                Icon(AppIcons.ArrowForward, null, Modifier.size(22.dp), tint = fg)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = fg)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (primary) fg.copy(alpha = .86f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
