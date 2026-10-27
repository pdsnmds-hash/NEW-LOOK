package bg.newlook.ui

import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import bg.newlook.data.*
import kotlinx.coroutines.*

@Composable
fun ProjectFilesScreen(project: Project, repo: Repository, vm: WorkspaceModel, back: () -> Unit) {
    val all by repo.attachments.items.collectAsState()
    val files = all.filter { it.projectId == project.id }.sortedByDescending { it.createdAt }
    val context = LocalContext.current
    var importing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProjectAttachment?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                importing = true
                vm.run({
                    try {
                        uris.forEach { repo.attach(project.id, it) }
                    } finally {
                        withContext(Dispatchers.Main) { importing = false }
                    }
                })
            }
        }
    fun open(item: ProjectAttachment, share: Boolean) {
        runCatching {
                val file = repo.attachments.file(item)
                check(file.isFile) { "File is unavailable" }
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        file,
                        item.name,
                    )
                val intent =
                    if (share)
                        Intent(Intent.ACTION_SEND)
                            .setType(item.mime)
                            .putExtra(Intent.EXTRA_STREAM, uri)
                    else Intent(Intent.ACTION_VIEW).setDataAndType(uri, item.mime)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.clipData = ClipData.newRawUri(item.name, uri)
                safeIntent(context, Intent.createChooser(intent, item.name))
            }
            .onFailure { vm.error.value = it.message ?: "Unable to open file" }
    }
    Page {
        BackTitle(l("Файлове в проекта", "Project files"), back)
        Text(project.title, style = MaterialTheme.typography.titleMedium)
        Muted(
            l(
                "Документи и снимки, събрани около вашата работа.",
                "Documents and photos, kept close to your work.",
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !importing,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Text(l("Добави файл", "Add files"))
            }
            OutlinedButton(
                onClick = { picker.launch(arrayOf("image/*")) },
                enabled = !importing,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
            ) {
                Text(l("Добави снимка", "Add photos"))
            }
        }
        if (importing) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(l("Копиране на файловете…", "Copying files…"))
        }
        Muted(
            l(
                "Локални копия · до 50 MB на файл. Оригиналите не се променят.",
                "Local copies · up to 50 MB per file. Originals stay unchanged.",
            )
        )
        if (files.isEmpty() && !importing)
            Empty(
                l("Всичко за проекта, на едно място.", "Everything your project needs, together."),
                l("Добавете първия документ или снимка.", "Add your first document or photo."),
            )
        files.forEach { item ->
            key(item.id) {
                CardBlock({ open(item, false) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AttachmentThumbnail(item, repo)
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Muted(
                                android.text.format.Formatter.formatShortFileSize(
                                    context,
                                    item.bytes,
                                ) + " · " + item.mime.substringAfter('/').uppercase()
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = { open(item, false) }) { Text(l("Отвори", "Open")) }
                        TextButton(onClick = { open(item, true) }) { Text(l("Сподели", "Share")) }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { deleting = item },
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                        ) {
                            Text(l("Премахни", "Remove"))
                        }
                    }
                }
            }
        }
        if (files.isNotEmpty())
            Muted(
                "${files.size} " +
                    (if (files.size == 1) l("файл", "file") else l("файла", "files")) +
                    " · " +
                    android.text.format.Formatter.formatShortFileSize(
                        context,
                        files.sumOf { it.bytes },
                    )
            )
        Muted(
            l(
                "JSON архивът съдържа работните записи, без прикачените файлове. Запазете важните оригинали отделно.",
                "The JSON backup includes workspace records, not attached files. Keep important originals separately.",
            )
        )
    }
    deleting?.let { item ->
        Confirm(
            l("Премахване на файла?", "Remove file?"),
            l(
                "Изтрива се само копието в приложението. Оригиналът се запазва.",
                "Only the app's copy will be deleted. The original stays unchanged.",
            ) + "\n\n" + item.name,
            { deleting = null },
        ) {
            vm.run({ repo.attachments.delete(item) }, { deleting = null })
        }
    }
}

@Composable
private fun AttachmentThumbnail(item: ProjectAttachment, repo: Repository) {
    val bitmap by
        produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, item.id) {
            if (item.mime.startsWith("image/"))
                value =
                    withContext(Dispatchers.IO) {
                        runCatching {
                                val path = repo.attachments.file(item).absolutePath
                                val bounds =
                                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeFile(path, bounds)
                                var sample = 1
                                while (
                                    bounds.outWidth / sample > 256 ||
                                        bounds.outHeight / sample > 256
                                ) sample *= 2
                                BitmapFactory.decodeFile(
                                        path,
                                        BitmapFactory.Options().apply { inSampleSize = sample },
                                    )
                                    ?.asImageBitmap()
                            }
                            .getOrNull()
                    }
        }
    if (bitmap != null)
        Image(
            bitmap!!,
            null,
            Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    else IconTile(AppIcons.Description)
}
