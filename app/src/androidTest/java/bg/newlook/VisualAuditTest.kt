package bg.newlook

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Captures the real rendered app on the QA emulator, including its system bars. */
class VisualAuditTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val repo get() = (instrumentation.targetContext.applicationContext as NewLookApp).repo
    private fun tap(text: String) {
        val textNodes=compose.onAllNodesWithText(text).fetchSemanticsNodes()
        if(textNodes.isNotEmpty())compose.onNodeWithText(text).performClick()else compose.onNodeWithContentDescription(text).performClick()
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "redesign/$name.png")
        file.parentFile!!.mkdirs()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle() }
    }
    @Test fun captureBothThemesAndKeyScreens() {
        runBlocking { repo.setting("onboarded", true); repo.setting("theme", "light") }
        compose.runOnUiThread { compose.activity.language("bg") }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Днес").fetchSemanticsNodes().isNotEmpty() }
        capture("today-bg-light")
        tap("Задачи");capture("tasks-bg-light")
        compose.onNodeWithContentDescription("Нова задача").performClick();capture("task-editor-bg-light")
        compose.onNodeWithContentDescription("Затвори").performClick()
        tap("Календар");capture("calendar-bg-light")
        tap("NEW LOOK");capture("company-bg-light")
        tap("Още");capture("more-bg-light")
        tap("Шаблони");capture("templates-bg-light")
        compose.onNodeWithText("Подготовка на среща").performScrollTo().performClick();capture("template-bg-light")
        compose.onNodeWithContentDescription("Назад").performClick()
        tap("Още")
        compose.onNodeWithText("Настройки").performScrollTo().performClick();capture("settings-bg-light")
        runBlocking { repo.setting("theme", "dark") }
        compose.runOnUiThread { compose.activity.language("en") }
        compose.waitUntil(15000) { compose.onAllNodesWithText("Make it yours.").fetchSemanticsNodes().isNotEmpty() }
        capture("settings-en-dark")
        tap("Today");capture("today-en-dark")
        tap("Tasks");capture("tasks-en-dark")
        tap("Calendar");capture("calendar-en-dark")
        tap("NEW LOOK");capture("company-en-dark")
        tap("More");tap("More");capture("more-en-dark")
        tap("Templates");capture("templates-en-dark")
        tap("More")
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        runBlocking { repo.setting("theme", "light") }
        compose.runOnUiThread { compose.activity.language("bg") }
    }
    @Test fun captureNarrowLargeType() {
        runBlocking { repo.setting("onboarded",true);repo.setting("theme","light") }
        compose.runOnUiThread { compose.activity.language("bg") }
        compose.waitUntil(15000){compose.onAllNodesWithText("Днес").fetchSemanticsNodes().isNotEmpty()}
        capture("today-360dp-font200")
        tap("Задачи");capture("tasks-360dp-font200")
        compose.onNodeWithContentDescription("Нова задача").performClick()
        compose.onNodeWithText("Заглавие *").performTextInput("Проверка на голям шрифт")
        compose.onNodeWithText("Запази").assertIsDisplayed().assertIsEnabled()
        capture("editor-360dp-font200")
        compose.onNodeWithContentDescription("Затвори").performClick()
        compose.onNodeWithText("Отхвърли").performClick()
        tap("Календар");capture("calendar-360dp-font200")
        tap("NEW LOOK");capture("company-360dp-font200")
        tap("Днес")
    }
}
