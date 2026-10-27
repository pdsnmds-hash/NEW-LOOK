package bg.newlook.domain

import bg.newlook.data.Workspace

/** Only known, explicitly labeled demo text is localized. User edits stay verbatim. */
private val demoCopy =
    listOf(
        "Демо · Адаптация на служител" to "Demo · Employee onboarding",
        "Демонстрационен проект" to "Demonstration project",
        "Подготовка на среща" to "Meeting preparation",
        "Проверка на CRM" to "CRM review",
        "Преглед на документи" to "Document review",
        "Планиране на въвеждането" to "Plan the introduction",
        "Демо · Работна среща" to "Demo · Working meeting",
    )

fun demoText(text: String, english: Boolean): String {
    val pair = demoCopy.firstOrNull { text == it.first || text == it.second } ?: return text
    return if (english) pair.second else pair.first
}

fun localizedDemo(workspace: Workspace, english: Boolean) =
    workspace.copy(
        projects =
            workspace.projects.map {
                if (it.demo)
                    it.copy(
                        title = demoText(it.title, english),
                        description = demoText(it.description, english),
                    )
                else it
            },
        tasks =
            workspace.tasks.map {
                if (it.demo)
                    it.copy(
                        title = demoText(it.title, english),
                        description = demoText(it.description, english),
                    )
                else it
            },
        events =
            workspace.events.map {
                if (it.demo)
                    it.copy(
                        title = demoText(it.title, english),
                        notes = demoText(it.notes, english),
                    )
                else it
            },
        notes =
            workspace.notes.map {
                if (it.demo)
                    it.copy(title = demoText(it.title, english), body = demoText(it.body, english))
                else it
            },
    )
