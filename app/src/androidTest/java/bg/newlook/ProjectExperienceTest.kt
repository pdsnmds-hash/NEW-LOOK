package bg.newlook

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import bg.newlook.data.*
import bg.newlook.domain.completed
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Regression coverage for the five issues reported with screenshots. QA workspace is snapshotted.
 */
class ProjectExperienceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val repo
        get() = (instrumentation.targetContext.applicationContext as NewLookApp).repo

    private fun prepare() {
        compose.runOnUiThread { compose.activity.language("en") }
        runBlocking {
            repo.reset()
            repo.setting("onboarded", true)
            repo.setting("theme", "dark")
            repo.setting("filters", "")
        }
        compose.waitUntil(15000) {
            compose.onAllNodesWithContentDescription("Create").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(text: String) {
        compose.onAllNodes(hasText(text) and hasClickAction()).onLast().performClick()
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        val file =
            File(instrumentation.targetContext.getExternalFilesDir(null), "five-fixes/$name.png")
        file.parentFile!!.mkdirs()
        Thread.sleep(300)
        instrumentation.uiAutomation.takeScreenshot().let { b ->
            file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            b.recycle()
        }
    }

    private fun openProject(title: String) {
        compose.onNodeWithText(title).performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun completedTasksGoToTheirOwnProjectHistoryAndCanReopen() {
        prepare()
        val p = Project(title = "History project")
        val other = Project(title = "Other project")
        val task = Task(title = "Finish this", projectId = p.id)
        runBlocking {
            repo.dao.project(p)
            repo.dao.project(other)
            repo.save(task)
            repo.save(completed(Task(title = "Other completion", projectId = other.id), true))
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText(p.title).fetchSemanticsNodes().isNotEmpty()
        }
        openProject(p.title)
        compose.onNodeWithContentDescription(task.title).performScrollTo()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription(task.title).performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithContentDescription(task.title).assertIsOn().assertIsDisplayed()
        compose.waitUntil(10000) {
            runBlocking { repo.dao.allTasks().find { it.id == task.id }!!.status == "done" }
        }
        compose.mainClock.advanceTimeBy(300)
        compose.onAllNodesWithContentDescription(task.title).assertCountEquals(0)
        compose.onNodeWithText("Task completed!").assertIsDisplayed()
        shot("task-success-lottie")
        compose.mainClock.autoAdvance = true
        compose.onNodeWithContentDescription("Project history").performClick()
        compose.onNodeWithText(task.title).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Other completion").assertCountEquals(0)
        shot("project-history")
        compose.onNodeWithContentDescription(task.title).performClick()
        compose.waitUntil(10000) {
            runBlocking { repo.dao.allTasks().find { it.id == task.id }!!.status == "todo" }
        }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText(task.title).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun weeklyReviewAndProjectFilesAreUsableFromHome() {
        prepare()
        val project = Project(title = "Weekly delivery")
        val done =
            Task(
                title = "Delivered milestone",
                projectId = project.id,
                due = java.time.LocalDate.now().toString(),
                status = "done",
                completedAt = System.currentTimeMillis(),
            )
        val remaining =
            Task(
                title = "Review next steps",
                projectId = project.id,
                due = java.time.LocalDate.now().toString(),
            )
        runBlocking {
            repo.dao.project(project)
            repo.save(done)
            repo.save(remaining)
            repo.attachments.importStream(
                project.id,
                "Contract.pdf",
                "application/pdf",
                java.io.ByteArrayInputStream("%PDF-1.4 sample".toByteArray()),
            )
        }
        compose.onNodeWithText("Weekly review").performScrollTo().performClick()
        compose.onNodeWithText("Your week in perspective").assertExists()
        shot("weekly-review")
        compose.onNodeWithText(done.title).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(remaining.title).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reschedule unfinished tasks").performScrollTo().performClick()
        compose.onNodeWithText("Reschedule", substring = false).performClick()
        compose.waitUntil(10000) {
            runBlocking {
                repo.dao.allTasks().single { it.id == remaining.id }.due != remaining.due
            }
        }
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        openProject(project.title)
        compose.onNodeWithText("Project files · 1").performScrollTo().performClick()
        compose.onNodeWithText("Contract.pdf").assertIsDisplayed()
        compose.onNodeWithText("Add files").assertIsDisplayed()
        compose.onNodeWithText("Add photos").assertIsDisplayed()
        shot("project-files")
        compose.onNodeWithText("Remove").performClick()
        compose.onNodeWithText("Remove file?").assertIsDisplayed()
        compose.onNodeWithText("Confirm").performClick()
        compose.waitUntil(10000) { repo.attachments.items.value.isEmpty() }
        compose.onNodeWithText("Everything your project needs, together.").assertExists()
        runBlocking {
            assertEquals(project.id, repo.dao.allTasks().single { it.id == remaining.id }.projectId)
            assertEquals("done", repo.dao.allTasks().single { it.id == done.id }.status)
        }
    }

    @Test
    fun animatedIntroductionCanBeReplayedWithoutChangingWorkspace() {
        prepare()
        val project = Project(title = "Keep my workspace")
        runBlocking { repo.dao.project(project) }
        tap("More")
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        compose.onNodeWithText("App introduction").performScrollTo().performClick()
        compose.onNodeWithText("One goal.\nA clear plan.").assertIsDisplayed()
        shot("onboarding-projects")
        compose.onNodeWithText("Continue").performScrollTo().performClick()
        compose.onNodeWithText("Make room for\nmeaningful meetings.").assertIsDisplayed()
        shot("onboarding-meetings")
        compose.onNodeWithText("Continue").performScrollTo().performClick()
        compose.onNodeWithText("Your rhythm.\nYour templates.").assertIsDisplayed()
        shot("onboarding-templates")
        compose.onNodeWithText("Back to app").performScrollTo().performClick()
        compose.onNodeWithText("App introduction").assertExists()
        runBlocking { assertEquals(listOf(project), repo.dao.allProjects()) }
    }

    @Test
    fun createOwnTemplateFromHomeAndReuseIt() {
        prepare()
        compose.onNodeWithText("Create your own template").performScrollTo().performClick()
        compose.onNodeWithText("Save template").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Template name").performScrollTo().performTextInput("Client launch")
        compose.onNodeWithText("Task 1").performScrollTo().performTextInput("Collect requirements")
        compose.onNodeWithText("Add step").performScrollTo().performClick()
        compose.onNodeWithText("Task 2").performScrollTo().performTextInput("Review delivery")
        compose
            .onAllNodesWithText("Days from start (0–3650)")
            .onLast()
            .performScrollTo()
            .performTextReplacement("3")
        compose.onNodeWithText("Save template").performScrollTo().performClick()
        compose.waitUntil(10000) { runBlocking { repo.dao.allTemplates().size == 1 } }
        compose.onNodeWithText("Create your plan").performScrollTo().performClick()
        compose.waitUntil(10000) { runBlocking { repo.dao.allTasks().size == 2 } }
        runBlocking {
            val template = repo.dao.allTemplates().single()
            assertEquals("Client launch", template.title)
            val project = repo.dao.allProjects().single()
            assertEquals(template.title, project.title)
            val tasks = repo.dao.allTasks()
            assertTrue(tasks.all { it.projectId == project.id })
            val first =
                java.time.LocalDate.parse(tasks.single { it.title == "Collect requirements" }.due)
            assertEquals(
                first.plusDays(3).toString(),
                tasks.single { it.title == "Review delivery" }.due,
            )
        }
    }

    @Test
    fun templateCanAddTasksToAnExistingProject() {
        prepare()
        val project = Project(title = "Existing client project")
        val original = Task(title = "Keep existing work", projectId = project.id)
        runBlocking {
            repo.dao.project(project)
            repo.save(original)
        }
        tap("More")
        tap("Templates")
        compose.onNodeWithText("Meeting preparation").performScrollTo().performClick()
        compose.onNodeWithText("Add to project").performScrollTo().performClick()
        tap(project.title)
        compose.onNodeWithText("Create your plan").performScrollTo().performClick()
        compose.waitUntil(10000) { runBlocking { repo.dao.allTasks().size == 9 } }
        runBlocking {
            assertEquals(listOf(project), repo.dao.allProjects())
            val tasks = repo.dao.allTasks()
            assertTrue(tasks.all { it.projectId == project.id })
            assertEquals(original, tasks.single { it.id == original.id })
            assertEquals(8, tasks.filter { it.id != original.id }.map { it.due }.toSet().size)
        }
    }

    @Test
    fun demoProjectSelectorUsesEnglishAndPreservesOriginalData() {
        prepare()
        runBlocking { repo.demo() }
        compose.waitUntil(10000) {
            compose
                .onAllNodesWithText("Demo · Employee onboarding")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        tap("Tasks")
        compose.onNodeWithContentDescription("New task").performClick()
        tap("Project")
        compose.onNodeWithText("Demo · Employee onboarding").assertIsDisplayed()
        compose.onAllNodesWithText("Демо · Адаптация на служител").assertCountEquals(0)
        shot("demo-project-selector-en")
        runBlocking {
            assertEquals("Демо · Адаптация на служител", repo.dao.allProjects().single().title)
        }
    }

    @Test
    fun phoneBoardShowsAllStagesAndMovesRealTasks() {
        prepare()
        val p = Project(title = "Phone board")
        val t = Task(title = "Write outline", projectId = p.id)
        runBlocking {
            repo.dao.project(p)
            repo.save(t)
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText(p.title).fetchSemanticsNodes().isNotEmpty()
        }
        openProject(p.title)
        compose.onNodeWithText("Board").performScrollTo().performClick()
        compose.onNodeWithText("To do").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("In progress").assertIsDisplayed()
        compose.onNodeWithText("Done").assertIsDisplayed()
        shot("phone-kanban")
        compose.onNodeWithContentDescription("Move: ${t.title}").performScrollTo().performClick()
        compose.onAllNodesWithText("In progress").onLast().performClick()
        compose.waitUntil(10000) {
            runBlocking { repo.dao.allTasks().single().status == "in_progress" }
        }
        compose.onNodeWithContentDescription("Move: ${t.title}").performClick()
        compose.onAllNodesWithText("Done").onLast().performClick()
        compose.waitUntil(10000) { runBlocking { repo.dao.allTasks().single().status == "done" } }
        compose.onAllNodesWithText(t.title).assertCountEquals(0)
    }

    @Test
    fun meetingHubOpensPlanningAndMarkedDates() {
        prepare()
        val day = java.time.LocalDate.now().plusDays(1)
        val e =
            Event(
                title = "Booked meeting",
                allDay = true,
                start = day.toString(),
                end = day.plusDays(1).toString(),
            )
        runBlocking { repo.save(e) }
        tap("Calendar")
        compose.onNodeWithText("Schedule a meeting").assertIsDisplayed()
        compose.onNodeWithText("My meetings").assertIsDisplayed()
        shot("meeting-hub")
        tap("My meetings")
        compose.onNodeWithContentDescription("${day} · 1 meetings").performClick()
        compose.onNodeWithText(e.title).performScrollTo().assertIsDisplayed()
        shot("my-meetings")
        compose.onNodeWithContentDescription("Back").performClick()
        tap("Schedule a meeting")
        compose.onNodeWithContentDescription(day.toString()).performClick()
        compose.onAllNodesWithText(e.title).assertCountEquals(0)
        compose.onAllNodesWithText("Day with meetings", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Schedule for this date").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Schedule for this date").assertIsDisplayed()
        compose.onAllNodesWithText(e.title).assertCountEquals(0)
        shot("planning-only")
        compose.onNodeWithText("Schedule for this date").performClick()
        compose.onNodeWithText("Title *").performTextInput("New planned meeting")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10000) { runBlocking { repo.dao.allEvents().size == 2 } }
        compose.onNodeWithText("My meetings").assertIsDisplayed()
        compose.onNodeWithText("New planned meeting").performScrollTo().assertIsDisplayed()
        shot("created-meeting-after-save")
        runBlocking {
            val created = repo.dao.allEvents().first { it.title == "New planned meeting" }
            org.junit.Assert.assertEquals(
                day,
                java.time.Instant.parse(created.start)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate(),
            )
        }
    }

    @Test
    fun companyTemplateProjectAlwaysAllowsBackAndRootTabs() {
        prepare()
        tap("NEW LOOK")
        compose.onNodeWithText("A little support goes a long way").performScrollTo()
        compose.onNodeWithText("HR & Payroll Assistance").performScrollTo().performClick()
        compose.onNodeWithText("Employee onboarding").performScrollTo().performClick()
        compose.onNodeWithText("Create your plan").performScrollTo().performClick()
        compose.waitUntil(10000) { runBlocking { repo.dao.allProjects().isNotEmpty() } }
        compose.onNodeWithText("Open the new project").performScrollTo().performClick()
        compose.onNodeWithText("Project tasks").performScrollTo()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        shot("project-sticky-back")
        tap("Calendar")
        compose.onNodeWithText("My meetings").assertIsDisplayed()
        tap("NEW LOOK")
        compose.onNodeWithContentDescription("Contact").assertIsDisplayed()
        tap("More")
        compose.onNodeWithText("Projects").assertIsDisplayed()
        tap("Tasks")
        compose.onNodeWithText("MY TASKS").assertIsDisplayed()
        tap("Today")
        compose.onNodeWithContentDescription("Create").assertIsDisplayed()
    }
}
