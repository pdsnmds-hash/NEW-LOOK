package bg.newlook.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import bg.newlook.domain.json
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ProjectAttachment(
    val id: String = uuid(),
    val projectId: String,
    val name: String,
    val mime: String,
    val bytes: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Private copies, independent of the original document provider's lifetime. */
class ProjectFiles(private val context: Context) {
    companion object {
        const val MAX_BYTES = 50L * 1024 * 1024
    }

    private val root = File(context.filesDir, "project-files")
    private val index = AtomicFile(File(root, "index.json"))
    private val lock = Mutex()
    private var loaded = false
    private val records = MutableStateFlow<List<ProjectAttachment>>(emptyList())
    val items = records.asStateFlow()

    private fun load() {
        if (loaded) return
        check(root.isDirectory || root.mkdirs()) { "Unable to access project files" }
        root.listFiles()?.filter { it.name.endsWith(".part") }?.forEach { it.delete() }
        if (index.baseFile.exists()) {
            records.value =
                json.fromJson(
                    index.readFully().toString(Charsets.UTF_8),
                    object : TypeToken<List<ProjectAttachment>>() {}.type,
                )
        }
        loaded = true
    }

    private fun persist(next: List<ProjectAttachment>) {
        val stream = index.startWrite()
        try {
            stream.write(json.toJson(next).toByteArray())
            index.finishWrite(stream)
            records.value = next
        } catch (e: Exception) {
            index.failWrite(stream)
            throw e
        }
    }

    fun file(item: ProjectAttachment): File {
        require(runCatching { java.util.UUID.fromString(item.id) }.isSuccess)
        return File(root, item.id)
    }

    suspend fun importUri(projectId: String, uri: Uri): ProjectAttachment =
        withContext(Dispatchers.IO) {
            var name = "Document"
            context.contentResolver
                .query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null,
                )
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val n = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (n >= 0 && !cursor.isNull(n)) name = cursor.getString(n)
                        val size = cursor.getColumnIndex(OpenableColumns.SIZE)
                        require(
                            size < 0 || cursor.isNull(size) || cursor.getLong(size) <= MAX_BYTES
                        ) {
                            "Maximum file size: 50 MB"
                        }
                    }
                }
            val reportedType = context.contentResolver.getType(uri)
            val mime =
                reportedType?.takeUnless { it == "application/octet-stream" }
                    ?: android.webkit.MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(
                            name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
                        )
                    ?: "application/octet-stream"
            val input =
                requireNotNull(context.contentResolver.openInputStream(uri)) {
                    "Unable to open file"
                }
            input.use { importStream(projectId, name, mime, it) }
        }

    suspend fun importStream(
        projectId: String,
        name: String,
        mime: String,
        input: InputStream,
    ): ProjectAttachment =
        withContext(Dispatchers.IO) {
            lock.withLock {
                load()
                val id = uuid()
                val temp = File(root, "$id.part")
                val destination = File(root, id)
                try {
                    var count = 0L
                    input.use { source ->
                        temp.outputStream().use { target ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = source.read(buffer)
                                if (n < 0) break
                                count += n
                                require(count <= MAX_BYTES) { "Maximum file size: 50 MB" }
                                target.write(buffer, 0, n)
                            }
                        }
                    }
                    check(temp.renameTo(destination)) { "Unable to store file" }
                    val item =
                        ProjectAttachment(
                            id,
                            projectId,
                            name.replace(Regex("[\\p{Cntrl}/\\\\]"), "_").take(180).ifBlank {
                                "Document"
                            },
                            mime,
                            count,
                        )
                    persist(records.value + item)
                    item
                } catch (e: Exception) {
                    temp.delete()
                    destination.delete()
                    throw e
                }
            }
        }

    suspend fun delete(item: ProjectAttachment) =
        withContext(Dispatchers.IO) {
            lock.withLock {
                load()
                persist(records.value.filterNot { it.id == item.id })
                file(item).delete()
                Unit
            }
        }

    suspend fun retainProjects(ids: Set<String>) =
        withContext(Dispatchers.IO) {
            lock.withLock {
                load()
                val removed = records.value.filter { it.projectId !in ids }
                persist(records.value.filter { it.projectId in ids })
                removed.forEach { file(it).delete() }
            }
        }
}
