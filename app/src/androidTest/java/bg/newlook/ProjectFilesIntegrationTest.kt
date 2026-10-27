package bg.newlook

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import bg.newlook.data.ProjectFiles
import bg.newlook.data.uuid
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProjectFilesIntegrationTest {
    @Test
    fun documentUriCopiesSurviveSourceRemovalAndProjectDeletionCleansUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = (context.applicationContext as NewLookApp).repo
        repo.loaded.await()
        val project = bg.newlook.data.Project(title = "Attachment provider test")
        repo.dao.project(project)
        val root = File(context.filesDir, "project-files").apply { mkdirs() }
        val source = File(root, uuid())
        val bytes = "%PDF-1.4 sample document".toByteArray()
        source.writeBytes(bytes)
        try {
            val uri =
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    source,
                    "Sample.pdf",
                )
            repo.attach(project.id, uri)
            val copy = repo.attachments.items.value.single { it.projectId == project.id }
            assertEquals("Sample.pdf", copy.name)
            assertEquals("application/pdf", copy.mime)
            source.delete()
            assertArrayEquals(bytes, repo.attachments.file(copy).readBytes())
            val copyUri =
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    repo.attachments.file(copy),
                    copy.name,
                )
            context.contentResolver.openInputStream(copyUri)!!.use {
                assertArrayEquals(bytes, it.readBytes())
            }
            repo.deleteProject(project, false)
            assertFalse(repo.attachments.file(copy).exists())
            assertTrue(repo.attachments.items.value.none { it.projectId == project.id })
        } finally {
            source.delete()
            if (repo.dao.allProjects().any { it.id == project.id })
                repo.deleteProject(project, true)
        }
    }

    @Test
    fun copiesPersistAndDeletionIsProjectScoped() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(base.cacheDir, "file-store-test-${uuid()}").apply { mkdirs() }
        val context =
            object : ContextWrapper(base) {
                override fun getFilesDir() = folder
            }
        try {
            val store = ProjectFiles(context)
            val bytes = ByteArray(8192) { (it % 127).toByte() }
            val first =
                store.importStream(
                    "project-a",
                    "../../contract.pdf",
                    "application/pdf",
                    ByteArrayInputStream(bytes),
                )
            val other =
                store.importStream(
                    "project-b",
                    "Photo.png",
                    "image/png",
                    ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                )
            assertArrayEquals(bytes, store.file(first).readBytes())
            assertFalse(first.name.contains('/'))
            assertEquals(bytes.size.toLong(), first.bytes)
            val reloaded = ProjectFiles(context)
            reloaded.retainProjects(setOf("project-a", "project-b"))
            assertEquals(2, reloaded.items.value.size)
            reloaded.retainProjects(setOf("project-b"))
            assertFalse(reloaded.file(first).exists())
            assertTrue(reloaded.file(other).exists())
            reloaded.delete(other)
            assertTrue(reloaded.items.value.isEmpty())
            assertFalse(reloaded.file(other).exists())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun failedImportLeavesNoRecordOrPartialFile() = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(base.cacheDir, "file-store-failure-${uuid()}").apply { mkdirs() }
        val context =
            object : ContextWrapper(base) {
                override fun getFilesDir() = folder
            }
        try {
            val store = ProjectFiles(context)
            var closed = false
            val stream =
                object : java.io.InputStream() {
                    override fun read(): Int = throw java.io.IOException("Provider disconnected")

                    override fun close() {
                        closed = true
                    }
                }
            try {
                store.importStream("p", "Broken.pdf", "application/pdf", stream)
                fail("Must reject failed copies")
            } catch (_: java.io.IOException) {}
            assertTrue(closed)
            assertTrue(store.items.value.isEmpty())
            assertTrue(File(folder, "project-files").listFiles().isNullOrEmpty())
        } finally {
            folder.deleteRecursively()
        }
    }
}
