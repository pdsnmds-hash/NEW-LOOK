package bg.newlook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bg.newlook.data.*
import bg.newlook.domain.*

@Composable
fun CreateTemplateScreen(
    repo: Repository,
    vm: WorkspaceModel,
    back: () -> Unit,
    saved: (String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var names by rememberSaveable { mutableStateOf(listOf("")) }
    var offsets by rememberSaveable { mutableStateOf(listOf("0")) }
    var saving by remember { mutableStateOf(false) }
    val valid =
        title.isNotBlank() &&
            names.isNotEmpty() &&
            names.all { it.isNotBlank() } &&
            offsets.all { it.toIntOrNull()?.let { n -> n in 0..3650 } == true }
    Page {
        BackTitle(l("Нов собствен шаблон", "Create your own template"), back)
        Muted(
            l(
                "Запазете стъпките си и ги използвайте отново с нова начална дата и избран проект.",
                "Save your steps and reuse them with a new start date and a project of your choice.",
            )
        )
        Field(title, { title = it }, l("Име на шаблона", "Template name"))
        Field(description, { description = it }, l("Описание", "Description"), multiline = true)
        Section(l("Стъпки", "Steps"))
        names.indices.forEach { index ->
            CardBlock {
                Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                Field(
                    names[index],
                    { value ->
                        names = names.mapIndexed { i, old -> if (i == index) value else old }
                    },
                    l("Задача", "Task") + " ${index + 1}",
                )
                Field(
                    offsets[index],
                    { value ->
                        offsets = offsets.mapIndexed { i, old -> if (i == index) value else old }
                    },
                    l("Дни от началото (0–3650)", "Days from start (0–3650)"),
                )
                if (names.size > 1)
                    OutlinedButton(
                        onClick = {
                            names = names.filterIndexed { i, _ -> i != index }
                            offsets = offsets.filterIndexed { i, _ -> i != index }
                        },
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                    ) {
                        Text(l("Премахни стъпката", "Remove step"))
                    }
            }
        }
        OutlinedButton(
            onClick = {
                names = names + ""
                offsets = offsets + "0"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(l("Добави стъпка", "Add step"))
        }
        Button(
            onClick = {
                if (valid && !saving) {
                    saving = true
                    val template =
                        UserTemplate(
                            title = title.trim(),
                            description = description.trim(),
                            steps =
                                json.toJson(
                                    names.mapIndexed { i, name ->
                                        TemplateStep(name.trim(), offsets[i].toInt())
                                    }
                                ),
                        )
                    vm.run(
                        {
                            try {
                                repo.dao.template(template)
                            } finally {
                                saving = false
                            }
                        },
                        { saved(template.id) },
                    )
                }
            },
            enabled = valid && !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(l("Запази шаблона", "Save template"))
        }
    }
}
