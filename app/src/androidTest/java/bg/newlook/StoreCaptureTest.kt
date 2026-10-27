package bg.newlook

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import bg.newlook.data.*
import java.io.File
import java.time.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Real release UI, dedicated disposable marketing workspace; no user records. */
class StoreCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val repo get() = (instrumentation.targetContext.applicationContext as NewLookApp).repo
    private fun tap(text: String) { compose.onAllNodes(hasText(text) and hasClickAction()).onLast().performClick();compose.waitForIdle() }
    private fun scroll(text: String) { compose.onNodeWithText(text).performScrollTo().performClick();compose.waitForIdle() }
    private fun back() { val node=compose.onNodeWithContentDescription("Back");if(!node.isDisplayed())node.performScrollTo();node.performClick();compose.waitForIdle() }
    private fun shot(name: String) {
        compose.waitForIdle();Thread.sleep(500)
        val file=File(instrumentation.targetContext.getExternalFilesDir(null),"store-captures/$name.png");file.parentFile!!.mkdirs()
        instrumentation.uiAutomation.takeScreenshot().let { b -> file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) };b.recycle() }
    }
    @Test fun captureMarketingScreens() {
        runBlocking { repo.reset();repo.setting("onboarded",true);repo.setting("theme","dark");repo.setting("filters","") }
        compose.runOnUiThread { compose.activity.language("en") }
        compose.waitUntil(20000) { compose.onAllNodesWithContentDescription("Create").fetchSemanticsNodes().isNotEmpty() }
        val launch=Project(title="Product launch",description="A thoughtful launch, one clear step at a time.")
        val client=Project(title="Client onboarding",description="A warm welcome and a confident start.")
        val today=LocalDate.now();val zone=ZoneId.systemDefault();val monday=today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        runBlocking {
            repo.dao.project(launch);repo.dao.project(client)
            listOf("Define the launch goals","Map the customer journey","Prepare the creative brief","Approve the brand direction","Confirm the delivery plan").forEachIndexed { i,title ->
                repo.save(Task(title=title,projectId=launch.id,status="done",completedAt=monday.plusDays((i%2).toLong()).atTime(9,10+i*5).atZone(zone).toInstant().toEpochMilli(),due=(if(i>=3) today else today.minusDays(1)).toString()))
            }
            repo.save(Task(title="Confirm the launch checklist",projectId=launch.id,due=today.toString(),priority=2))
            repo.save(Task(title="Review final assets",projectId=launch.id,status="in_progress",due=today.toString()))
            repo.save(Task(title="Prepare the welcome pack",projectId=client.id,due=today.toString()))
            repo.save(Task(title="Send the project overview",projectId=client.id,status="done",due=today.toString(),completedAt=today.atTime(9,15).atZone(zone).toInstant().toEpochMilli()))
            repo.save(Task(title="Introduce the project team",projectId=client.id,status="done",due=today.toString(),completedAt=today.atTime(9,45).atZone(zone).toInstant().toEpochMilli()))
            repo.save(Event(title="Design review",projectId=launch.id,start=today.atTime(14,0).atZone(zone).toInstant().toString(),end=today.atTime(14,45).atZone(zone).toInstant().toString(),location="Studio meeting room"))
            repo.save(Event(title="Client kickoff",projectId=client.id,start=today.atTime(16,0).atZone(zone).toInstant().toString(),end=today.atTime(16,45).atZone(zone).toInstant().toString()))
            val bytes=instrumentation.context.assets.open("marketing-launch-brief.pdf").use { it.readBytes() }
            repo.attachments.importStream(launch.id,"Launch brief.pdf","application/pdf",java.io.ByteArrayInputStream(bytes))
            val image=java.io.ByteArrayOutputStream()
            android.graphics.BitmapFactory.decodeResource(compose.activity.resources,bg.newlook.R.drawable.newlook_logo).compress(Bitmap.CompressFormat.PNG,100,image)
            repo.attachments.importStream(launch.id,"Brandmark.png","image/png",java.io.ByteArrayInputStream(image.toByteArray()))
        }
        shot("01-today")
        scroll("Weekly review");shot("05-weekly-review");back()
        tap("More");scroll("Projects");scroll(launch.title);scroll("Board")
        compose.onNodeWithText("Confirm the launch checklist").performScrollTo();shot("02-project-board")
        scroll("Project files · 2");shot("06-project-files");back()
        tap("Calendar");scroll("My meetings");shot("03-meetings")
        tap("More");scroll("Templates");shot("04-templates")
    }
}
