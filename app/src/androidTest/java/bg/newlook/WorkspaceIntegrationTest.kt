package bg.newlook

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bg.newlook.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceIntegrationTest {
    private val context
        get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun bundledLottieAnimationsParseWithoutExternalAssets() {
        listOf(
                "task_success.json",
                "empty_list.json",
                "onboarding_0.json",
                "onboarding_1.json",
                "onboarding_2.json",
            )
            .forEach { name ->
                val result =
                    com.airbnb.lottie.LottieCompositionFactory.fromAssetSync(
                        context,
                        "animations/$name",
                    )
                assertNull(result.exception)
                val composition = requireNotNull(result.value)
                assertTrue(composition.duration > 0)
                assertTrue(composition.layers.isNotEmpty())
                assertFalse(composition.hasImages())
                val drawable = com.airbnb.lottie.LottieDrawable()
                drawable.setComposition(composition)
                drawable.setBounds(0, 0, 240, 240)
                drawable.progress = .6f
                val bitmap =
                    android.graphics.Bitmap.createBitmap(
                        240,
                        240,
                        android.graphics.Bitmap.Config.ARGB_8888,
                    )
                drawable.draw(android.graphics.Canvas(bitmap))
                val pixels = IntArray(240 * 240)
                bitmap.getPixels(pixels, 0, 240, 0, 0, 240, 240)
                assertTrue(
                    "$name renders visible artwork",
                    pixels.any { android.graphics.Color.alpha(it) > 0 },
                )
                val file = java.io.File(context.getExternalFilesDir(null), "$name.png")
                file.outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
    }

    @Test
    fun foreignKeysKeepDetachedRecords() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkspaceDb::class.java).build()
        try {
            val dao = db.dao()
            val project = Project(title = "Project")
            dao.project(project)
            val t = Task(title = "Task", projectId = project.id)
            val n = Note(title = "Note", projectId = project.id)
            val e =
                Event(
                    title = "Event",
                    allDay = true,
                    start = "2026-10-26",
                    end = "2026-10-27",
                    projectId = project.id,
                )
            dao.task(t)
            dao.note(n)
            dao.event(e)
            dao.deleteProject(project)
            assertNull(dao.allTasks().single().projectId)
            assertNull(dao.allNotes().single().projectId)
            assertNull(dao.allEvents().single().projectId)
        } finally {
            db.close()
        }
    }

    @Test
    fun occurrenceUniquenessIsEnforced() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkspaceDb::class.java).build()
        try {
            val dao = db.dao()
            val t = Task(title = "Instance", seriesId = uuid(), occurrenceKey = "2026-10-26")
            dao.occurrence(t)
            dao.occurrence(t.copy(id = uuid()))
            assertEquals(1, dao.allTasks().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrationPreservesExistingData() = runBlocking {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val schema =
            JSONObject(
                    InstrumentationRegistry.getInstrumentation()
                        .context
                        .assets
                        .open("schema-2.json")
                        .bufferedReader()
                        .use { it.readText() }
                )
                .getJSONObject("database")
        val legacy = context.openOrCreateDatabase(name, 0, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            legacy.execSQL(
                entity
                    .getString("createSql")
                    .replace("\${TABLE_NAME}", table)
                    .replace(", `demo` INTEGER NOT NULL", "")
                    .replace(", `excludedKeys` TEXT NOT NULL DEFAULT ''", "")
            )
            val indices = entity.optJSONArray("indices")
            for (j in 0 until (indices?.length() ?: 0)) legacy.execSQL(
                indices!!.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table)
            )
        }
        val id = uuid()
        legacy.execSQL(
            "INSERT INTO projects (id,title,description,color,target,archived) VALUES (?,?,?,0,NULL,0)",
            arrayOf(id, "Keep me", "Existing data"),
        )
        legacy.version = 1
        legacy.close()
        val db =
            Room.databaseBuilder(context, WorkspaceDb::class.java, name)
                .addMigrations(WorkspaceDb.MIGRATION_1_2)
                .build()
        try {
            val p = db.dao().allProjects().single()
            assertEquals(id, p.id)
            assertEquals("Keep me", p.title)
            assertFalse(p.demo)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
