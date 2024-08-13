package dev.mmauro.immichassistant.commands.fix.timezone

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.isCliktParameterDefaultStdout
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.addTask
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import dev.mmauro.immichassistant.common.CommonCommand
import dev.mmauro.immichassistant.common.CommonOptions
import dev.mmauro.immichassistant.common.ImmichConfig
import dev.mmauro.immichassistant.common.cli.cliktAwareOutput
import dev.mmauro.immichassistant.common.task.customProgresBar
import dev.mmauro.immichassistant.common.task.itemsCount
import dev.mmauro.immichassistant.db.addUseDbTask
import dev.mmauro.immichassistant.db.connectDbTask
import dev.mmauro.immichassistant.db.model.Asset
import dev.mmauro.immichassistant.db.model.ExifData
import dev.mmauro.immichassistant.db.selectAll
import kotlinx.coroutines.*
import kotlinx.datetime.*

class FixTimezoneCommand : CliktCommand(
    name = "timezone",
    help = "TODO", // TODO
), CommonCommand {

    // TODO: add user filter
    // TODO: understand if it's better to switch to calling the API instead of dealing with the DB directly

    override val commonOptions by CommonOptions()
    private val immichConfig by ImmichConfig()

    private val includeInvisible by option(
        help = """
            Some assets are not visible, because they are "dependant" on another asset.
            An example of this are assets which are the videos generated from a live photo.
            
            By default they are excluded, as their exif date is not used, but you can force their inclusion with this 
            flag.
        """.trimIndent()
    ).flag("--exclude-invisible", default = false)

    // TODO docs
    // TODO understand how to deal with "home" time zone without having to specify each gap
    private val timezoneRanges by option(
        names = arrayOf("--timezone-range", "-tz"),
        help = """
            TODO()
        """.trimIndent(),
    )
        .timezoneRange()
        .multiple(required = true)
        .check("Timezone ranges must be mutually exclusive") { it.areMutuallyExclusive() }

    // TODO doc
    private val listEditedAssets by option(
        help = """
            DOC
        """.trimIndent()
    ).outputStream()

    private enum class OverrideTimezones {
        NEVER, IF_DIFFERENT_OFFSET, IF_DIFFERENT;

        val cliName = name.lowercase().replace('_', '-')
    }

    private val overrideTimezones by option(
        help = """
            This option specifies what to do with assets whose timezone is set, but differs from what is specified in
            the --timezone-range options.
            
            * `${OverrideTimezones.NEVER.cliName}`: don't override the timezone if it's present already. 
              This is the default.
            * `${OverrideTimezones.IF_DIFFERENT_OFFSET.cliName}`: override the timezone only if offset of the two 
              timezones is different. For example, `America/Toronto` and `America/New_York`, while being two different 
              timezones, have always the same offset (-05:00 or -04:00 during DST). This setting would not replace 
              `America/Toronto` with `America/New_York` or vice-versa.
            * `${OverrideTimezones.IF_DIFFERENT.cliName}`: override the timezone if it is different in any way. This
              includes timezone which have different names but otherwise represent the same offset.
        """.trimIndent()
    ).enum<OverrideTimezones> { it.cliName }.default(OverrideTimezones.NEVER)

    private val dryRun by option(help = "Do not perform any change, just pretend to run and print summary")
        .flag(default = false)


    override fun run() = runBlocking<Unit> {
        val multiProgressBarAnimation = MultiProgressBarAnimation(terminal)
        val progress = multiProgressBarAnimation.animateInCoroutine()
        val execution = launch { progress.execute() }

        val dbTask = progress.connectDbTask(immichConfig)
        val assetsTask = progress.addUseDbTask(dbTask, "Listing assets") { db ->
            db.selectAll(Asset)
        }
        val exifTask = progress.addUseDbTask(dbTask, "Listing exif data") { db ->
            db.selectAll(ExifData)
        }

        val assets = assetsTask.await()
        val exif = exifTask.await().associateBy { it.assetId }

        val assetsWithExif = assets
            .filter { it.shouldInclude() }
            .map { AssetWithExif(asset = it, exif = exif[it.id]) }

        val total = assetsWithExif.size.toLong()
        val progressLayout = progressBarLayout {
            customProgresBar()
            itemsCount(total = total, suffix = " assets") { completed }
        }
        val task = progress.addTask(
            definition = progressLayout,
            completed = 0,
            total = total,
        )

        val results = withContext(Dispatchers.IO) {
            async {
                buildList {
                    for (assetWithExif in assetsWithExif) {
                        add(
                            AssetResult(
                                asset = assetWithExif,
                                result = assetWithExif.process()
                            )
                        )
                        task.advance()
                    }
                }
            }.await()
        }

        execution.join()

        val groupedResults: Map<FixTimezoneResult, List<AssetResult>> = results.groupBy { it.result }

        echo()

        listEditedAssets?.cliktAwareOutput {
            groupedResults.entries
                .sortedWith(compareBy(FixTimezoneResult.COMPARATOR) { it.key })
                .forEach { (result, assets) ->
                    echo("${result.message}: ${assets.size}")
                    if (result is FixTimezoneResult.TimezoneFixed) {
                        for (fixedAsset in assets) {
                            outputLine(result.logLine(fixedAsset.asset.asset))
                        }
                    }
                }
        }
    }

    /**
     * @return whether this [Asset] should be included in the list of assets to process, based on CLI options.
     */
    private fun Asset.shouldInclude(): Boolean {
        return isVisible || includeInvisible
    }

    /**
     * Processes a single asset. Should not perform mutating operations if [dryRun] is `true`.
     *
     * @return the result of processing this item (or what would have the result been) as a [FixTimezoneResult].
     */
    private fun AssetWithExif.process(): FixTimezoneResult {
        if (exif == null) {
            return FixTimezoneResult.MissingExifData
        }
        val exifTimezone = exif.timeZone?.let { TimeZone.of(it) }
        val detectedTimezone = asset.localDateTime.findTimezone()

        if (exifTimezone != null && !shouldOverrideTimezone(asset.localDateTime, exifTimezone, detectedTimezone)) {
            return FixTimezoneResult.Untouched
        }

        return if (detectedTimezone != null) {
            // TODO actually fix the timezone
            FixTimezoneResult.TimezoneFixed(oldTimeZone = exifTimezone, newTimezone = detectedTimezone)
        } else {
            FixTimezoneResult.MissingTimezoneRange
        }
    }

    /**
     * Returns whether an asset with the given [localDateTime] and [exifTimezone] should have its timezone overridden by
     * the given [detectedTimezone].
     *
     * This is based on CLI options.
     */
    private fun shouldOverrideTimezone(
        localDateTime: LocalDateTime,
        exifTimezone: TimeZone,
        detectedTimezone: TimeZone?
    ): Boolean {
        return when (overrideTimezones) {
            OverrideTimezones.NEVER -> false
            OverrideTimezones.IF_DIFFERENT_OFFSET -> {
                // If detectedTimezone == null we return true so that we can return the Result.MissingTimezoneRange
                detectedTimezone == null ||
                    localDateTime.toInstant(exifTimezone) != localDateTime.toInstant(detectedTimezone)
            }

            OverrideTimezones.IF_DIFFERENT -> {
                // If detectedTimezone == null we return true so that we can return the Result.MissingTimezoneRange
                detectedTimezone == null || detectedTimezone != exifTimezone
            }
        }
    }

    /**
     * @return the timezone that an asset with this [LocalDateTime] should have, or `null` if it's unknown
     *
     * This is based on timezone ranges given in the CLI.
     */
    private fun LocalDateTime.findTimezone(): TimeZone? {
        return timezoneRanges
            .find { this in it }
            ?.timezone
    }
}

private data class AssetWithExif(
    val asset: Asset,
    val exif: ExifData?,
)

private data class AssetResult(
    val asset: AssetWithExif,
    val result: FixTimezoneResult,
)
