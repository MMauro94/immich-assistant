package dev.mmauro.immichassistant.commands.verify.dir

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.rendering.TextColors.green
import dev.mmauro.immichassistant.common.CommonCommand
import dev.mmauro.immichassistant.common.CommonOptions
import dev.mmauro.immichassistant.common.ImmichConfig
import dev.mmauro.immichassistant.common.sha1
import dev.mmauro.immichassistant.common.task.addDeferredTask
import dev.mmauro.immichassistant.common.task.addFilesProgressTask
import dev.mmauro.immichassistant.common.task.listFiles
import dev.mmauro.immichassistant.db.connectDb
import dev.mmauro.immichassistant.db.model.Asset
import dev.mmauro.immichassistant.db.selectAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.walk

class VerifyDirCommand : CliktCommand(name = "dir"), CommonCommand {

    override fun help(context: Context) = """
        This command compares a given directory to what is stored in Immich's database, and reports on missing files.
        The comparison is made using SHA-1 checksums.
        
        This is useful if you want to double-check that all files in a given folder are present in Immich.
        
        Note: this command computes the SHA-1 checksum
    """.trimIndent()

    override val commonOptions by CommonOptions()
    private val immichConfig by ImmichConfig()

    private val directory by argument().path(mustExist = true, mustBeReadable = true, canBeFile = false)

    @OptIn(ExperimentalPathApi::class)
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

        val filesTask = progress.addDeferredTask("Listing files") {
            started()
            listFiles(directory.walk())
        }

        val files = filesTask.await()
        val assets = assetsTask.await().associateBy { it.checksum.toList() }

        val results = progress.addFilesProgressTask(files).process {
            it.path.verify(assets)
        }

        execution.join()

        val missingFiles = results.filter { !it.found }
        if (missingFiles.isEmpty()) {
            echo(green("🎉 All files verified successfully"))
        } else {
            echo(green("⚠️ Found ${missingFiles.size} files not present in Immich's database!"))

            for (file in missingFiles) {
                echo("${file.file} not found in any asset!")
            }
        }
    }

    private fun Path.verify(assets: Map<List<Byte>, Asset>): VerifiedFile {
        val sha1 = sha1().toList()
        return VerifiedFile(
            file = this,
            found = sha1 in assets,
        )
    }
}

private data class VerifiedFile(
    val file: Path,
    val found: Boolean,
)
