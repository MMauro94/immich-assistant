package dev.mmauro.immichassistant.commands.verify.oprhaned

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.rendering.TextColors.green
import dev.mmauro.immichassistant.commands.verify.VerifyFilesFilters
import dev.mmauro.immichassistant.common.CommonCommand
import dev.mmauro.immichassistant.common.CommonOptions
import dev.mmauro.immichassistant.common.ImmichConfig
import dev.mmauro.immichassistant.common.task.addDeferredTask
import dev.mmauro.immichassistant.common.task.listFiles
import dev.mmauro.immichassistant.common.toAbsolute
import dev.mmauro.immichassistant.db.connectDb
import dev.mmauro.immichassistant.db.model.Asset
import dev.mmauro.immichassistant.db.model.AssetFile
import dev.mmauro.immichassistant.db.model.Person
import dev.mmauro.immichassistant.db.selectAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class VerifyOrphanedCommand : CliktCommand(name = "orphaned"), CommonCommand {

    override fun help(context: Context) = """
        This command checks that each file in the file system is reflected somewhere in Immich's database.
                
        This command does not verify the checksum or completeness of matching files.
        Furthermore, the "upload" folder (where files are stored temporarily while they are being uploaded) is not checked. 
        
        For that, use the `verify consistency` command.
    """.trimIndent()

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
        val assetFilesTask = progress.addDeferredTask("Listing asset files") {
            val db = dbTask.await()
            started()
            db.selectAll(AssetFile)
        }
        val peopleTask = progress.addDeferredTask("Listing people") {
            val db = dbTask.await()
            started()
            db.selectAll(Person)
        }
        val filesTask = progress.addDeferredTask("Listing files (may take a while)") {
            started()
            listFiles(verifyFilesFilters.getFilteredFilesFromFileSystem(immichConfig.uploadLocation))
        }

        val orphanedFiles = progress.addDeferredTask("Detecting orphaned files") {
            val assets = assetsTask.await()
            val assetFiles = assetFilesTask.await().groupBy { it.assetId }
            val people = peopleTask.await()
            val files = filesTask.await()
            started()

            val allPaths = assets
                .flatMap { asset ->
                    buildList {
                        add(asset.originalPath)
                        add(asset.encodedVideoPath)
                        add(asset.sidecarPath)
                        addAll(assetFiles[asset.ownerId]?.map { it.path }.orEmpty())
                    }.filterNotNull()
                }
                .plus(people.mapNotNull { it.thumbnailPath })
                .map { it.toAbsolute(immichConfig.uploadLocation) }
                .toSet()

            files.filter { it.path !in allPaths }
        }.await()

        execution.join()

        echo()
        if (orphanedFiles.isEmpty()) {
            echo(green("🎉 All files verified successfully, no orphans found"))
        } else {
            echo(green("⚠️ Found ${orphanedFiles.size} orphaned files!"))

            for (file in orphanedFiles) {
                echo("${file.path} not found in any asset!")
            }
        }
    }
}