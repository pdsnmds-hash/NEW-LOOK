package bg.newlook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import bg.newlook.data.Project
import bg.newlook.data.Task
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** These tests run on the isolated QA emulator; workspace is snapshotted before the suite. */
class WorkspaceSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app
        get() =
            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                as NewLookApp

    private fun prepare() {
        compose.runOnUiThread { compose.activity.language("bg") }
        runBlocking {
            app.repo.reset()
            app.repo.setting("onboarded", true)
            app.repo.setting("filters", "")
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithContentDescription("Създаване").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun createAndCompleteTask() {
        prepare()
        compose.onNodeWithContentDescription("Създаване").performClick()
        compose.onNodeWithText("Задача").performClick()
        compose.onNodeWithText("Заглавие *").performTextInput("Smoke · local task")
        compose.onNodeWithText("Запази").performClick()
        compose.onNodeWithText("Задачи").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Smoke · local task").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Smoke · local task").performClick()
        compose.waitUntil(10000){runBlocking{app.repo.dao.allTasks().single().status=="done"}}
        compose.onAllNodesWithContentDescription("Smoke · local task").assertCountEquals(0)
        compose.onNodeWithText("Завършени").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Smoke · local task").assertIsOn()
        runBlocking { assertEquals("done", app.repo.dao.allTasks().single().status) }
    }

    @Test
    fun createProjectThenAddTaskInsideIt() {
        prepare()
        compose.onNodeWithText("Нов проект").performScrollTo().performClick()
        compose.onNodeWithText("Име *").performTextInput("Smoke · project workspace")
        compose.onNodeWithText("Запази").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Нова задача в проекта…").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Нова задача в проекта…").performScrollTo().performTextInput("Smoke · project task")
        compose.onNodeWithContentDescription("Добави задача").performClick()
        compose.waitUntil(10000){runBlocking{app.repo.dao.allTasks().isNotEmpty()}}
        runBlocking{assertEquals(app.repo.dao.allProjects().single().id,app.repo.dao.allTasks().single().projectId)}
        compose.onNodeWithText("Smoke · project task").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Днес").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Моите проекти").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Smoke · project workspace").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Smoke · project task").assertCountEquals(0)
    }

    @Test
    fun changingBulkDateKeepsTheProject() {
        prepare()
        val project = Project(title = "Smoke project")
        val task =
            Task(
                title = "Smoke · keep project",
                projectId = project.id,
                due = LocalDate.now().plusDays(5).toString(),
            )
        runBlocking {
            app.repo.dao.project(project)
            app.repo.save(task)
        }
        compose.onNodeWithText("Задачи").performClick()
        compose.onNodeWithText("Избор").performClick()
        compose
            .onNode(hasScrollToIndexAction())
            .performScrollToNode(hasContentDescription(task.title))
        compose.onNodeWithContentDescription(task.title).performClick()
        compose.onNodeWithText("Действия").performScrollTo().performClick()
        compose.onNodeWithText("Нов срок").performScrollTo().performClick()
        onView(withId(android.R.id.button1)).perform(click())
        compose.onNodeWithText("Промени само срока").performScrollTo().performClick()
        compose.waitUntil(10000) {
            runBlocking { app.repo.dao.allTasks().single().due == LocalDate.now().toString() }
        }
        runBlocking { assertEquals(project.id, app.repo.dao.allTasks().single().projectId) }
    }

    @Test
    fun applyingTemplateTwiceCreatesIndependentProjects() {
        prepare()
        compose.onNodeWithText("Още").performClick()
        compose.onNodeWithText("Шаблони").performClick()
        compose.onNodeWithText("Подготовка на среща").performScrollTo().performClick()
        compose.onNodeWithText("Създайте вашия план").performScrollTo().performClick()
        compose.waitUntil(10000) { runBlocking { app.repo.dao.allProjects().size == 1 } }
        compose.onNodeWithText("Създайте вашия план").performScrollTo().performClick()
        compose.waitUntil(10000) { runBlocking { app.repo.dao.allProjects().size == 2 } }
        runBlocking {
            val projects = app.repo.dao.allProjects()
            val tasks = app.repo.dao.allTasks()
            assertEquals(2, projects.map { it.id }.toSet().size)
            assertEquals(16, tasks.size)
            assertTrue(projects.all { project -> tasks.count { it.projectId == project.id } == 8 })
            assertEquals(tasks.size, tasks.map { it.id }.toSet().size)
        }
    }
}
