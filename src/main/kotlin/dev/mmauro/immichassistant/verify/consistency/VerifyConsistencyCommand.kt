package dev.mmauro.immichassistant.verify.consistency

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.addTask
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.UnorderedList
import com.github.ajalt.mordant.widgets.progress.*
import dev.mmauro.immichassistant.common.*
import dev.mmauro.immichassistant.db.connectDb
import dev.mmauro.immichassistant.db.model.Asset
import dev.mmauro.immichassistant.db.model.Person
import dev.mmauro.immichassistant.db.selectAll
import dev.mmauro.immichassistant.verify.TrackedFile
import dev.mmauro.immichassistant.verify.VerifyFilesFilters
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import kotlin.io.path.exists

class VerifyConsistencyCommand : CliktCommand(
    name = "consistency",
    help = """
        Helps verify consistency between of what is stored in Immich's database compared to the filesystem.
        
        This command is helpful as the file system might get out of sync, either due to manual action, a software bug or
        a hardware failure.
        
        By default, all files in the DB are checked. The SHA-1 checksums are verified as well.
        
        This command does not verify that each file in the file system is reflected in Immich's DB.
        For that, use the `verify orphaned` command.
    """.trimIndent()
), CommonCommand {

    override val commonOptions by CommonOptions()
    private val immichConfig by ImmichConfig()
    private val verifyFilesFilters by VerifyFilesFilters()
    private val verifyChecksums by option(
        help = "Compares the SHA-1 checksum of the verified files. Currently only original files are checked, as that's the only file Immich checksums."
    ).flag("--skip-checksums", default = true)

    override fun run(): Unit = runBlocking {
        val multiProgressBarAnimation = MultiProgressBarAnimation(terminal)
        val progress = multiProgressBarAnimation.animateInCoroutine()
        val execution = launch { progress.execute() }

        val dbTask = progress.addDeferredTask("Connecting to DB") {
            started()
            immichConfig.connectDb()
        }
        val assetsTask = progress.addDeferredTask("Listing assets") {
            val db = dbTask.await()
            started()
            db.selectAll(Asset)
        }
        val peopleTask = progress.addDeferredTask("Listing people") {
            val db = dbTask.await()
            started()
            db.selectAll(Person)
        }
        val assets = assetsTask.await()
        val people = peopleTask.await()

        // Get a map where the asset is the key and each value is the list of files to validate
        // Assets that don't pass the filter are discarded
        val filesToValidate = verifyFilesFilters.getFilteredTrackedFiles(
            assets = assets,
            people = people,
            uploadLocation = immichConfig.uploadLocation,
        )

        if (filesToValidate.isEmpty()) {
            execution.join()
            echo()
            echo("Nothing to validate 🫤")
            return@runBlocking
        }

        val verifyTaskLayout = progressBarLayout(alignColumns = false) {
            percentage()
            progressBar(width = 20, finishedStyle = green)
            completed(" files")
            speed(suffix = " files/s")
        }
        val verifyTask = progress.addTask(
            definition = verifyTaskLayout,
            completed = 0L,
            total = filesToValidate.size.toLong(),
        )

        // advance() can only be called sequentially, so we'll do it on a fixed thread pool of size 1
        val results = Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { advanceDispatcher ->
            val deferredResults = withContext(Dispatchers.IO) {
                filesToValidate.map {
                    async {
                        VerifiedFile(it, it.verify()).also {
                            withContext(advanceDispatcher) {
                                verifyTask.advance()
                            }
                        }
                    }
                }
            }
            deferredResults.awaitAll()
        }

        execution.join()

        val failedVerifications = results.filter { it.result != VerifyResult.OK }

        echo("")
        if (failedVerifications.isEmpty()) {
            echo(green("🎉 All files verified successfully"))
            printResults(results)
        } else {
            echo(green("⚠️ Some files (${failedVerifications.size}) failed verification!"))
            printResults(results)

            for (verifiedFile in failedVerifications) {
                echo(
                    listOf(
                        verifiedFile.file.type,
                        "of",
                        verifiedFile.file.entity::class.simpleName,
                        verifiedFile.file.entity.id,
                        "failed due to",
                        verifiedFile.result,
                        ":",
                        verifiedFile.file.path,
                    ).joinToString(separator = " ")
                )
            }
        }
    }

    private fun printResults(results: List<VerifiedFile>) {
        val flattened = results.groupingBy { it.result }.eachCount()

        fun MutableList<Widget>.resultLine(emoji: String, result: VerifyResult, suffix: String) {
            add(
                Text(
                    buildString {
                        append(emoji)
                        append(" ")
                        append(flattened[result] ?: 0)
                        append(" files ")
                        append(suffix)
                    }
                )
            )
        }

        val list = UnorderedList(
            buildList {
                resultLine("✔️", VerifyResult.OK, "exist" + if (verifyChecksums) " and checksums match" else "")
                resultLine("❗", VerifyResult.FILE_MISSING, "are missing")

                if (verifyChecksums) {
                    resultLine("🔢", VerifyResult.CHECKSUM_MISMATCH, "have mismatched checksums")
                }
            }
        )
        terminal.println(widget = list)
    }

    private fun TrackedFile.verify(): VerifyResult {
        if (!path.exists()) {
            return VerifyResult.FILE_MISSING
        }
        if (verifyChecksums && checksum != null) {
            val sha1 = path.sha1()
            if (!checksum.contentEquals(sha1)) {
                return VerifyResult.CHECKSUM_MISMATCH
            }
        }
        return VerifyResult.OK
    }
}

private data class VerifiedFile(
    val file: TrackedFile,
    val result: VerifyResult,
)

private enum class VerifyResult {
    FILE_MISSING, CHECKSUM_MISMATCH, OK
}
