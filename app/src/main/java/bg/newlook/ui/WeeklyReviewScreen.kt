package bg.newlook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeeklyReviewScreen(
    w: Workspace,
    settings: Settings,
    repo: Repository,
    vm: WorkspaceModel,
    back: () -> Unit,
    openProject: (String) -> Unit,
    openTask: (Task) -> Unit,
) {
    val current = weekStart(LocalDate.now(), settings.weekStart)
    var selected by rememberSaveable { mutableStateOf(current.toString()) }
    val start = LocalDate.parse(selected)
    val review = weeklyReview(w.tasks, start, ZoneId.systemDefault())
    val locale = if (english()) Locale.ENGLISH else Locale.forLanguageTag("bg")
    var reschedule by remember { mutableStateOf(false) }
    var destination by rememberSaveable {
        mutableStateOf(maxOf(LocalDate.now(), start.plusDays(7)).toString())
    }
    var moving by remember { mutableStateOf(false) }
    Page {
        BackTitle(l("Седмицата в перспектива", "Your week in perspective"), back)
        Muted(
            l(
                "Малки стъпки. Вижте колко далеч стигнахте.",
                "Small steps. See how far you have come.",
            )
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { selected = start.minusWeeks(1).toString() }) {
                Icon(AppIcons.ChevronLeft, l("Предишна седмица", "Previous week"))
            }
            Text(
                "${start.format(DateTimeFormatter.ofPattern("d MMM", locale))} — ${start.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))}",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            IconButton(
                onClick = { selected = start.plusWeeks(1).toString() },
                enabled = start < current,
            ) {
                Icon(AppIcons.ChevronRight, l("Следваща седмица", "Next week"))
            }
        }
        Hero(
            l("ВАШИЯТ НАПРЕДЪК", "YOUR PROGRESS"),
            "${review.completed.size} " +
                if (review.completed.size == 1) l("завършена задача", "task completed")
                else l("завършени задачи", "tasks completed"),
            "${review.projectCounts.size} " +
                if (review.projectCounts.size == 1) l("проект с напредък", "project moved forward")
                else l("проекта с напредък", "projects moved forward"),
            if (review.completed.size + review.unfinished.size == 0) 0f
            else review.completed.size.toFloat() / (review.completed.size + review.unfinished.size),
        )
        CardBlock {
            Text(l("Стъпка по стъпка", "Day by day"), style = MaterialTheme.typography.titleMedium)
            val max = review.dailyCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                review.dailyCounts.forEachIndexed { index, count ->
                    val date = start.plusDays(index.toLong())
                    Column(
                        Modifier.weight(1f).semantics { contentDescription = "$date · $count" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("$count", style = MaterialTheme.typography.labelMedium)
                        Box(
                            Modifier.height(90.dp).fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                Modifier.fillMaxWidth(.65f)
                                    .height((6 + 84f * count / max).dp)
                                    .background(
                                        if (count > 0) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(6.dp),
                                    )
                            )
                        }
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EE", locale)),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
        if (review.projectCounts.isNotEmpty()) {
            Section(l("Проекти с напредък", "Projects moved forward"))
            review.projectCounts.entries
                .sortedByDescending { it.value }
                .forEach { (id, count) ->
                    w.projects
                        .find { it.id == id }
                        ?.let { p ->
                            CardBlock({ openProject(id) }) {
                                Text(p.title, style = MaterialTheme.typography.titleMedium)
                                Muted(
                                    "$count " + l("завършени тази седмица", "completed this week")
                                )
                                val all = w.tasks.filter { it.projectId == id }
                                val done = all.count { it.status == "done" }
                                LinearProgressIndicator(
                                    progress = {
                                        if (all.isEmpty()) 0f else done.toFloat() / all.size
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Muted(
                                    l("Общ напредък сега", "Overall progress now") +
                                        ": $done / ${all.size}"
                                )
                            }
                        }
                }
        }
        Section(l("Завършено през седмицата", "Completed this week"))
        if (review.completed.isEmpty())
            Empty(
                l("Всяка стъпка има значение.", "Every step counts."),
                l(
                    "Завършените през тази седмица задачи ще се появят тук.",
                    "Tasks completed during this week will appear here.",
                ),
            )
        review.completed.forEach { task ->
            CardBlock({ openTask(task) }) {
                Text(task.title, style = MaterialTheme.typography.titleSmall)
                Muted(
                    listOfNotNull(
                            w.projects.find { it.id == task.projectId }?.title,
                            Instant.ofEpochMilli(task.completedAt!!)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("d MMM · HH:mm", locale)),
                        )
                        .joinToString(" · ")
                )
            }
        }
        Section(l("Остава от тази седмица", "Still on your list"))
        Muted(
            l(
                "Незавършени задачи със срок в избраната седмица.",
                "Unfinished tasks due during the selected week.",
            )
        )
        if (review.unfinished.isEmpty())
            Text(
                l("Няма оставащи задачи за тази седмица.", "No unfinished tasks for this week."),
                style = MaterialTheme.typography.bodyLarge,
            )
        review.unfinished.forEach { task ->
            CardBlock({ openTask(task) }) {
                Text(task.title, style = MaterialTheme.typography.titleSmall)
                Muted(displayDate(task.due!!))
            }
        }
        if (review.unfinished.isNotEmpty())
            OutlinedButton(
                onClick = {
                    destination = maxOf(LocalDate.now(), start.plusDays(7)).toString()
                    reschedule = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(l("Планирай оставащите задачи", "Reschedule unfinished tasks"))
            }
    }
    if (reschedule)
        AlertDialog(
            onDismissRequest = { if (!moving) reschedule = false },
            title = { Text(l("Нова дата за задачите", "A new date for your tasks")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${review.unfinished.size} " +
                            l(
                                "задачи ще получат нов срок. Часовете и проектите се запазват.",
                                "tasks will get a new due date. Times and projects stay the same.",
                            )
                    )
                    DateField(destination, { destination = it }, l("Нов срок", "New due date"))
                }
            },
            confirmButton = {
                Button(
                    enabled = !moving && runCatching { LocalDate.parse(destination) }.isSuccess,
                    onClick = {
                        moving = true
                        val ids = review.unfinished.map { it.id }.toSet()
                        val newDate = destination
                        vm.run(
                            {
                                try {
                                    repo.rescheduleUnfinished(ids, LocalDate.parse(newDate))
                                } finally {
                                    kotlinx.coroutines.withContext(
                                        kotlinx.coroutines.Dispatchers.Main
                                    ) {
                                        moving = false
                                    }
                                }
                            },
                            { reschedule = false },
                        )
                    },
                ) {
                    Text(l("Премести", "Reschedule"))
                }
            },
            dismissButton = {
                TextButton(onClick = { reschedule = false }, enabled = !moving) {
                    Text(l("Отказ", "Cancel"))
                }
            },
        )
}
