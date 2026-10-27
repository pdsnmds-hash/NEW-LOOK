@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package bg.newlook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.*

@Composable
fun EventEditor(
    e: Event,
    w: Workspace,
    repo: Repository,
    vm: WorkspaceModel,
    onSaved: (Event) -> Unit = {},
    dismiss: () -> Unit,
) {
    val zone = ZoneId.of(e.zone)
    var title by rememberSaveable { mutableStateOf(e.title) }
    var allDay by rememberSaveable { mutableStateOf(e.allDay) }
    var startDate by rememberSaveable {
        mutableStateOf(
            if (e.allDay) e.start else Instant.parse(e.start).atZone(zone).toLocalDate().toString()
        )
    }
    var endDate by rememberSaveable {
        mutableStateOf(
            if (e.allDay) LocalDate.parse(e.end).minusDays(1).toString()
            else Instant.parse(e.end).atZone(zone).toLocalDate().toString()
        )
    }
    var startTime by rememberSaveable {
        mutableStateOf(
            if (e.allDay) "09:00" else Instant.parse(e.start).atZone(zone).toLocalTime().toString()
        )
    }
    var endTime by rememberSaveable {
        mutableStateOf(
            if (e.allDay) "10:00" else Instant.parse(e.end).atZone(zone).toLocalTime().toString()
        )
    }
    var location by rememberSaveable { mutableStateOf(e.location) }
    var notes by rememberSaveable { mutableStateOf(e.notes) }
    var project by rememberSaveable { mutableStateOf(e.projectId) }
    var reminder by rememberSaveable { mutableStateOf(e.reminder) }
    var delete by remember { mutableStateOf(false) }
    fun value() =
        e.copy(
            title = title,
            allDay = allDay,
            start =
                if (allDay) LocalDate.parse(startDate).toString()
                else
                    LocalDate.parse(startDate)
                        .atTime(LocalTime.parse(startTime))
                        .atZone(zone)
                        .toInstant()
                        .toString(),
            end =
                if (allDay) LocalDate.parse(endDate).plusDays(1).toString()
                else
                    LocalDate.parse(endDate)
                        .atTime(LocalTime.parse(endTime))
                        .atZone(zone)
                        .toInstant()
                        .toString(),
            location = location,
            notes = notes,
            projectId = project,
            reminder = reminder,
        )
    val value = runCatching { value() }.getOrNull()
    val valid =
        value != null &&
            title.isNotBlank() &&
            (if (allDay) LocalDate.parse(value.end) > LocalDate.parse(value.start)
            else Instant.parse(value.end) >= Instant.parse(value.start))
    val exporter = rememberFileExport("text/calendar", vm)
    EditorShell(
        l("Събитие", "Event"),
        value != e,
        dismiss,
        {
            value?.let { saved ->
                vm.run(
                    { repo.save(saved) },
                    {
                        onSaved(saved)
                        dismiss()
                    },
                )
            }
        },
        valid,
    ) {
        Field(title, { title = it }, l("Заглавие *", "Title *"))
        Row {
            Switch(allDay, { allDay = it })
            Text(l("Цял ден", "All day"), Modifier.padding(12.dp))
        }
        DateField(startDate, { startDate = it }, l("Начало", "Start"))
        if (!allDay) TimeField(startTime, { startTime = it }, l("Начален час", "Start time"))
        DateField(endDate, { endDate = it }, l("Край", "End"))
        if (!allDay) TimeField(endTime, { endTime = it }, l("Краен час", "End time"))
        Muted(e.zone)
        Field(location, { location = it }, l("Място", "Location"))
        Field(notes, { notes = it }, l("Бележки", "Notes"), true)
        ProjectChoice(project, w) { project = it }
        ReminderField(reminder, 15) { reminder = it }
        if (!valid)
            Muted(l("Краят не може да е преди началото.", "The end cannot be before the start."))
        if (valid)
            OutlinedButton(onClick = { exporter("event.ics", ics(value!!)) }) {
                Text(l("Експорт .ics", "Export .ics"))
            }
        if (w.events.any { it.id == e.id })
            TextButton(onClick = { delete = true }) { Text(l("Изтрий", "Delete")) }
    }
    if (delete)
        Confirm(l("Изтриване на събитие?", "Delete event?"), e.title, { delete = false }) {
            vm.run(
                {
                    repo.dao.deleteEvent(e)
                    ReminderScheduler.reconcile(repo.context, repo.dao)
                },
                dismiss,
            )
        }
}

