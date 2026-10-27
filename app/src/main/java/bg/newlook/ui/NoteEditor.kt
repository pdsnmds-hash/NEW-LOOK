@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package bg.newlook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import bg.newlook.data.*
import bg.newlook.domain.*
import kotlinx.coroutines.*

@Composable
fun NoteEditor(n: Note, w: Workspace, repo: Repository, vm: WorkspaceModel, dismiss: () -> Unit) {
    var raw by rememberSaveable(n.id) { mutableStateOf(json.toJson(n)) }
    val value = json.fromJson(raw, Note::class.java)
    var saved by remember { mutableStateOf(true) }
    var delete by remember { mutableStateOf(false) }
    var removed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun update(next: Note) {
        saved = false
        raw = json.toJson(next.copy(updatedAt = System.currentTimeMillis()))
    }
    LaunchedEffect(raw, removed) {
        if (!removed && raw != json.toJson(n)) {
            saved = false
            delay(450)
            try {
                withContext(Dispatchers.IO) { repo.dao.note(value) }
                saved = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                vm.error.value = e.message
                saved = false
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !removed && raw != json.toJson(n))
                repo.scope.launch { repo.dao.note(json.fromJson(raw, Note::class.java)) }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (!removed && raw != json.toJson(n))
                repo.scope.launch { repo.dao.note(json.fromJson(raw, Note::class.java)) }
        }
    }
    EditorShell(
        l("Бележка", "Note"),
        false,
        { vm.run({ if (!removed && raw != json.toJson(n)) repo.dao.note(value) }, dismiss) },
        { vm.run({ repo.dao.note(value) }, dismiss) },
    ) {
        Eyebrow(if (saved) l("ЗАПАЗЕНО", "SAVED") else l("ЗАПАЗВАНЕ…", "SAVING…"))
        Field(value.title, { update(value.copy(title = it)) }, l("Заглавие", "Title"))
        Field(
            value.body,
            { update(value.copy(body = it)) },
            l("Вашите мисли и идеи", "Your thoughts and ideas"),
            true,
        )
        TextButton(
            onClick = {
                update(
                    value.copy(
                        body = value.body + (if (value.body.isBlank()) "" else "\n") + "[ ] "
                    )
                )
            }
        ) {
            Icon(AppIcons.Checklist, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(l("Добави точка в списък", "Add checklist item"))
        }
        Field(value.tags, { update(value.copy(tags = it)) }, l("Тагове", "Tags"))
        ProjectChoice(value.projectId, w) { update(value.copy(projectId = it)) }
        Row {
            Switch(value.pinned, { update(value.copy(pinned = it)) })
            Text(l("Закрепи", "Pin note"), Modifier.padding(12.dp))
        }
        TextButton(onClick = { delete = true }) { Text(l("Изтрий", "Delete")) }
    }
    if (delete)
        Confirm(l("Изтриване на бележка?", "Delete note?"), value.title, { delete = false }) {
            removed = true
            vm.run({ repo.dao.deleteNote(value) }, dismiss)
        }
}

