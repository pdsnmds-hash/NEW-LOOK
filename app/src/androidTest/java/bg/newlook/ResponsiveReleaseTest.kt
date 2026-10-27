package bg.newlook

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import bg.newlook.data.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ResponsiveReleaseTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val repo
        get() = (instrumentation.targetContext.applicationContext as NewLookApp).repo

    private val en
        get() = InstrumentationRegistry.getArguments().getString("locale", "en") == "en"

    private fun label(bg: String, english: String) = if (en) english else bg

    private val failures = mutableListOf<String>()

    private fun tap(text: String) {
        compose.onAllNodes(hasText(text) and hasClickAction()).onLast().performClick()
        compose.waitForIdle()
    }

    private fun scroll(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun back() {
        val node = compose.onNodeWithContentDescription(label("Назад", "Back"))
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun capture(screen: String) {
        compose.waitForIdle()
        val width = InstrumentationRegistry.getArguments().getString("width", "unknown")
        compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),
                useUnmergedTree = true,
            )
            .fetchSemanticsNodes()
            .forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
                results.forEach { result ->
                    val ellipsis = (0 until result.lineCount).any { result.isLineEllipsized(it) }
                    if (
                        ((0 until result.lineCount).any {
                            result.getLineRight(it) - result.multiParagraph.width > 1f ||
                                result.getLineLeft(it) < -1f
                        } || result.multiParagraph.height - result.size.height > 1f) &&
                            !ellipsis &&
                            node.boundsInRoot.width > 0 &&
                            node.boundsInRoot.height > 0
                    ) {
                        failures +=
                            "$width/${if(en)"en" else "bg"}/$screen: ${result.layoutInput.text.text}"
                    }
                }
            }
        Thread.sleep(200)
        val folder =
            File(
                    instrumentation.targetContext.getExternalFilesDir(null),
                    "release-widths/$width-${if(en)"en" else "bg"}",
                )
                .apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { b ->
            File(folder, "$screen.png").outputStream().use {
                b.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            b.recycle()
        }
    }

    @Test
    fun narrowBoardLanesRemainReadable() {
        runBlocking { repo.reset(); repo.setting("onboarded", true); repo.setting("theme", if(en) "dark" else "light") }
        compose.runOnUiThread { compose.activity.language(if(en) "en" else "bg") }
        compose.waitUntil(20000) { compose.onAllNodesWithContentDescription(label("Създаване", "Create")).fetchSemanticsNodes().isNotEmpty() }
        val p = Project(title = "Mobile board QA")
        runBlocking {
            repo.dao.project(p)
            repo.save(Task(title=label("Проверете документите преди предаване", "Review all documents before delivery"),projectId=p.id))
            repo.save(Task(title=label("Подгответе работното пространство", "Prepare the working environment"),projectId=p.id,status="in_progress"))
            repo.save(Task(title="Done",projectId=p.id,status="done",completedAt=System.currentTimeMillis()))
        }
        tap(label("Още", "More"));scroll(label("Проекти", "Projects"));scroll(p.title);scroll(label("Табло", "Board"))
        compose.onNodeWithText(label("Проверете документите преди предаване", "Review all documents before delivery")).performScrollTo()
        capture("board-lanes")
        compose.onNodeWithText(label("За изпълнение", "To do")).assertIsDisplayed()
        compose.onNodeWithText(label("В процес", "In progress")).assertIsDisplayed()
        compose.onNodeWithText(label("Готово", "Done")).assertIsDisplayed()
        assertTrue(failures.joinToString(" | "), failures.isEmpty())
    }

    @androidx.test.filters.SdkSuppress(minSdkVersion = 30)
    @Test
    fun firstLaunchUsesSafeInsets() {
        runBlocking { repo.setting("onboarded", false) }
        compose.runOnUiThread { compose.activity.language(if (en) "en" else "bg") }
        compose.waitUntil(20000) {
            compose.onAllNodesWithText(label("Пропусни", "Skip")).fetchSemanticsNodes().isNotEmpty()
        }
        capture("first-launch-onboarding")
        val skip =
            compose.onNodeWithText(label("Пропусни", "Skip")).fetchSemanticsNode().boundsInRoot
        val statusHeight =
            androidx.core.view.ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                ?.top ?: 0
        assertTrue("Skip must be below the status bar", skip.top >= statusHeight)
        tap(label("Пропусни", "Skip"))
        scroll(label("Започнете на чисто", "Start fresh"))
        compose.waitUntil(20000) {
            compose
                .onAllNodesWithContentDescription(label("Създаване", "Create"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertTrue(failures.joinToString(" | "), failures.isEmpty())
    }

    @Test
    fun auditPhoneSectionsAndEditors() {
        runBlocking {
            repo.reset()
            repo.setting("onboarded", true)
            repo.setting("theme", if (en) "dark" else "light")
            repo.setting("filters", "")
        }
        compose.runOnUiThread { compose.activity.language(if (en) "en" else "bg") }
        compose.waitUntil(20000) {
            compose
                .onAllNodesWithContentDescription(label("Създаване", "Create"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val title = label("Мобилна проверка на проекта", "Mobile project review")
        val p =
            Project(
                title = title,
                description =
                    label(
                        "Документи, срещи и следващи стъпки за екипа.",
                        "Documents, meetings and next steps for the team.",
                    ),
            )
        runBlocking {
            repo.dao.project(p)
            repo.save(
                Task(
                    title =
                        label(
                            "Преглед на документите преди предаване",
                            "Review supporting documents before delivery",
                        ),
                    projectId = p.id,
                    due = LocalDate.now().toString(),
                )
            )
            repo.save(
                Task(
                    title = label("Проверка на достъпа", "Check required access"),
                    projectId = p.id,
                    status = "in_progress",
                )
            )
            repo.save(
                Task(
                    title = label("Подготовка на работното пространство", "Prepare the workspace"),
                    projectId = p.id,
                    status = "done",
                    completedAt = System.currentTimeMillis(),
                )
            )
            repo.save(
                Event(
                    title = label("Работна среща", "Working meeting"),
                    projectId = p.id,
                    allDay = true,
                    start = LocalDate.now().toString(),
                    end = LocalDate.now().plusDays(1).toString(),
                )
            )
            repo.attachments.importStream(
                p.id,
                "Project agreement.pdf",
                "application/pdf",
                java.io.ByteArrayInputStream("%PDF-1.4 sample".toByteArray()),
            )
        }
        capture("today")
        scroll(label("Седмичен обзор", "Weekly review"))
        capture("weekly-review")
        back()
        tap(label("Задачи", "Tasks"))
        capture("tasks")
        compose.onNodeWithContentDescription(label("Нова задача", "New task")).performClick()
        capture("task-editor")
        scroll(label("Подзадачи, напомняния и още", "Subtasks, reminders & more"))
        capture("task-options")
        compose.onNodeWithContentDescription(label("Затвори", "Close")).performClick()
        tap(label("Календар", "Calendar"))
        capture("meeting-hub")
        scroll(label("Моите срещи", "My meetings"))
        capture("meetings")
        back()
        scroll(label("Планирай среща", "Schedule a meeting"))
        capture("meeting-planning")
        back()
        tap(label("Още", "More"))
        capture("more")
        scroll(label("Бележки", "Notes"))
        capture("notes")
        tap(label("Още", "More"))
        scroll(label("Проекти", "Projects"))
        scroll(title)
        capture("project")
        scroll(label("Табло", "Board"))
        capture("board")
        scroll(label("Файлове в проекта", "Project files") + " · 1")
        capture("project-files")
        back()
        compose
            .onAllNodesWithContentDescription(label("История на проекта", "Project history"))
            .onFirst()
            .performClick()
        capture("history")
        back()
        tap(label("Още", "More"))
        scroll(label("Шаблони", "Templates"))
        capture("templates")
        scroll(label("Нов собствен шаблон", "Create your own template"))
        capture("template-editor")
        back()
        tap(label("Още", "More"))
        scroll(label("Настройки", "Settings"))
        capture("settings")
        scroll(label("Запознаване с приложението", "App introduction"))
        capture("onboarding-1")
        scroll(label("Продължи", "Continue"))
        capture("onboarding-2")
        scroll(label("Продължи", "Continue"))
        capture("onboarding-3")
        scroll(label("Към приложението", "Back to app"))
        tap("NEW LOOK")
        capture("company")
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "release-widths")
        File(
                folder,
                "overflow-${InstrumentationRegistry.getArguments().getString("width")}-${if(en)"en" else "bg"}.txt",
            )
            .writeText(failures.joinToString("\n"))
        assertTrue("Text overflow: ${failures.joinToString(" | ")}", failures.isEmpty())
    }
}
