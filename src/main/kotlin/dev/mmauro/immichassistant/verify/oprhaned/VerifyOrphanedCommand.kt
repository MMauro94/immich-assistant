package dev.mmauro.immichassistant.verify.oprhaned

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.rendering.TextColors.green
import dev.mmauro.immichassistant.common.*
import dev.mmauro.immichassistant.db.connectDb
import dev.mmauro.immichassistant.db.model.Asset
import dev.mmauro.immichassistant.db.model.Person
import dev.mmauro.immichassistant.db.selectAll
import dev.mmauro.immichassistant.verify.VerifyFilesFilters
import kotlinx.coroutines.*
import java.nio.file.Path

class VerifyOrphanedCommands : CliktCommand(
    name = "orphaned",
    help = """
        This command checks that each file in the file system is reflected somewhere in Immich's database.
                
        This command does not verify the checksum or completeness of matching files.
        Furthermore, the "upload" folder (where files are stored temporarily while they are being uploaded) is not checked. 
        
        For that, use the `verify consistency` command.
    """.trimIndent()
), CommonCommand {

    override val commonOptions by CommonOptions()
    private val immichConfig by ImmichConfig()
    private val verifyFilesFilters by VerifyFilesFilters()

    override fun run() = runBlocking {
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
        val filesTask = progress.addDeferredTask("Listing files (may take a while)") {
            started()
            listFiles()
        }

        val orphanedFiles = progress.addDeferredTask("Detecting orphaned files") {
            val assets = assetsTask.await()
            val people = peopleTask.await()
            val files = filesTask.await()
            started()

            val allPaths = assets
                .flatMap {
                    listOfNotNull(
                        it.originalPath,
                        it.encodedVideoPath,
                        it.thumbnailPath,
                        it.sidecarPath,
                        it.previewPath
                    )
                }
                .plus(people.mapNotNull { it.thumbnailPath })
                .map { it.toAbsolute(immichConfig.uploadLocation) }
                .toSet()

            files.filter { it !in allPaths }
        }.await()

        execution.join()

        echo()
        if (orphanedFiles.isEmpty()) {
            echo(green("🎉 All files verified successfully, no orphans found"))
        } else {
            echo(green("⚠️ Found ${orphanedFiles.size} orphaned files!"))

            for (file in orphanedFiles) {
                echo("$file not found in any asset!")
            }
        }
    }

    private fun TaskRunnerScope.listFiles(): List<Path> {
        var count = 0
        return verifyFilesFilters
            .getFilteredFilesFromFileSystem(immichConfig.uploadLocation)
            .onEach {
                count++
                update("($count files discovered)")
            }
            .toList()
    }
}