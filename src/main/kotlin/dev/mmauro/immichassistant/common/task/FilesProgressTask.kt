package dev.mmauro.immichassistant.common.task

import com.github.ajalt.mordant.animation.progress.ProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.ProgressTask
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.widgets.progress.*
import dev.mmauro.immichassistant.common.PathWithSize
import dev.mmauro.immichassistant.common.totalSize
import kotlinx.coroutines.*
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.fileSize

val FILES_PROGRESS_LAYOUT = progressBarLayout(alignColumns = false) {
    verticalLayout {
        percentage()
        progressBar(width = 20, finishedStyle = green)
        completed(" files")
        speed(suffix = " files/s")
    }
}

class FilesProgressTask<T>(
    private val task: ProgressTask<Int>,
    private val files: List<T>,
    private val path: T.() -> Path,
    private val totalBytes: Long?,
) {

    suspend fun <R> process(process: suspend (T) -> R): List<R> {
        // advance() can only be called sequentially, so we'll do it on a fixed thread pool of size 1
        return Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { advanceDispatcher ->
            val deferredResults = withContext(Dispatchers.IO) {
                files.map {
                    async {
                        val result = process(it)
                        withContext(advanceDispatcher) {
                            task.update {
                                if (totalBytes != null) {
                                    completed += it.path().fileSize()
                                } else {
                                    completed++
                                }
                                context++
                            }
                        }
                        result
                    }
                }
            }
            deferredResults.awaitAll()
        }
    }
}

fun <T> ProgressBarAnimation.addFilesProgressTask(
    files: List<T>,
    path: (T) -> Path,
    totalBytes: Long? = null,
): FilesProgressTask<T> {
    val task = addTask(
        definition = filesProgressLayout(count = files.size.toLong(), totalBytes = totalBytes),
        context = 0,
        completed = 0,
        total = totalBytes ?: files.size.toLong(),
    )
    return FilesProgressTask(
        task = task,
        files = files,
        path = path,
        totalBytes = totalBytes,
    )
}

fun ProgressBarAnimation.addFilesProgressTask(files: List<PathWithSize>) = addFilesProgressTask(
    files = files,
    path = { it.path },
    totalBytes = files.totalSize()
)

private fun filesProgressLayout(count: Long, totalBytes: Long?) = progressBarContextLayout<Int>(alignColumns = false) {
    percentage()
    progressBar(width = 20, finishedStyle = green)
    if (totalBytes != null) {
        bytes(totalBytes = totalBytes) { completed }
    }
    itemsCount(total = count, suffix = " files") { context.toLong() }
    speed(suffix = "B/s")
}
