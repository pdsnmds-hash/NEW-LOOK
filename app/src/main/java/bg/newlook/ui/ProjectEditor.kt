@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package bg.newlook.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import bg.newlook.data.*
import bg.newlook.domain.*
import java.time.LocalDate

@Composable
fun ProjectEditor(
    p: Project,
    w: Workspace,
    repo: Repository,
    vm: WorkspaceModel,
    onSaved: (Project) -> Unit = {},
    dismiss: () -> Unit,
) {
    var raw by rememberSaveable(p.id) { mutableStateOf(json.toJson(p)) }
    val value = json.fromJson(raw, Project::class.java)
    var remove by remember { mutableStateOf(false) }
    val save = {
        vm.run(
            {
                value.target?.let(LocalDate::parse)
                repo.dao.project(value)
            },
            {
                onSaved(value)
                dismiss()
            },
        )
    }
    EditorShell(
        l("Проект", "Project"),
        raw != json.toJson(p),
        dismiss,
        save,
        value.title.isNotBlank(),
    ) {
        Field(value.title, { raw = json.toJson(value.copy(title = it)) }, l("Име *", "Name *"))
        Field(
            value.description,
            { raw = json.toJson(value.copy(description = it)) },
            l("Описание", "Description"),
            true,
        )
        DateField(
            value.target ?: "",
            { raw = json.toJson(value.copy(target = it.ifBlank { null })) },
            l("Целева дата", "Target date"),
        )
        Choice(
            l("Цвят", "Color"),
            value.color,
            listOf(l("Индиго", "Indigo"), l("Тюркоаз", "Teal"), l("Кехлибар", "Amber")),
        ) {
            raw = json.toJson(value.copy(color = it))
        }
        if (w.projects.any { it.id == p.id })
            TextButton(onClick = { remove = true }) { Text(l("Изтрий проект", "Delete project")) }
    }
    if (remove)
        AlertDialog(
            onDismissRequest = { remove = false },
            title = { Text(l("Изтриване на проект", "Delete project")) },
            text = {
                Text(
                    l(
                        "Преместването запазва задачите във входящи, а бележките и събитията — без проект. Изтриването на съдържанието премахва всички свързани записи. И двата избора изтриват копията на прикачените файлове. Оригиналите се запазват.",
                        "Moving keeps tasks in Inbox and detaches notes and events. Deleting content removes all related records. Both options delete the project’s attached file copies; originals stay unchanged.",
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.run({ repo.deleteProject(p, false) }, dismiss) }) {
                    Text(l("Премести във входящи", "Move to Inbox"))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.run({ repo.deleteProject(p, true) }, dismiss) }) {
                    Text(l("Изтрий със съдържанието", "Delete with content"))
                }
            },
        )
}

