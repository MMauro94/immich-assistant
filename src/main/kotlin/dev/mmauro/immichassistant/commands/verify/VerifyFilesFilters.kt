package dev.mmauro.immichassistant.commands.verify

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import dev.mmauro.immichassistant.common.FileType
import dev.mmauro.immichassistant.common.toAbsolute
import dev.mmauro.immichassistant.db.model.Asset
import dev.mmauro.immichassistant.db.model.AssetFile
import dev.mmauro.immichassistant.db.model.DbEntity
import dev.mmauro.immichassistant.db.model.Person
import java.net.URLConnection
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.walk

class VerifyFilesFilters : OptionGroup(
    name = "Verify files filters",
    help = "Allows to filter which files to verify".trimIndent(),
) {

    private val verifyOriginals by option(help = "Verifies the original files")
        .flag("--skip-originals", default = true)

    private val verifyThumbs by option(help = "Verifies the thumbnails")
        .flag("--skip-thumbnails", default = true)

    private val verifyPreviews by option(help = "Verifies the previews")
        .flag("--skip-previews", default = true)

    private val verifyEncodedVideos by option(help = "Verifies the encoded videos")
        .flag("--skip-encoded-videos", default = true)

    private val verifyImages by option(help = "Verifies image assets")
        .flag("--skip-images", default = true)

    private val verifyVideos by option(help = "Verifies video assets")
        .flag("--skip-videos", default = true)

    private val verifyPeople by option(help = "Verifies people's profiles")
        .flag("--skip-people", default = true)

    private val limitFiles by option(
        help = "Only the first N specified files will be verified. Useful to understand if the CLI is setup correctly before running for the whole data set."
    ).int()

    private fun Asset.shouldInclude(): Boolean {
        return (verifyImages && type == Asset.Type.IMAGE) || (verifyVideos && type == Asset.Type.VIDEO)
    }

    fun getFilteredTrackedFiles(
        assets: Iterable<Asset>,
        assetFiles: Iterable<AssetFile>,
        people: Iterable<Person>,
        uploadLocation: Path,
    ): List<TrackedFile> {
        fun Path.toTrackedFile(entity: DbEntity, fileType: FileType, checksum: ByteArray?) = TrackedFile(
            entity = entity,
            path = toAbsolute(uploadLocation),
            type = fileType,
            checksum = checksum
        )

        val assetFilesMap = assetFiles.groupBy { it.assetId }
        val allFiles = sequence {
            yieldAll(
                assets.asSequence().filter { it.shouldInclude() }.flatMap {
                    buildList {
                        val files = assetFilesMap[it.id].orEmpty().groupBy { it.type }
                        if (verifyOriginals) {
                            add(it.originalPath.toTrackedFile(it, FileType.ORIGINAL, it.checksum))
                        }
                        val thumbnails = files[AssetFile.Type.THUMBNAIL].orEmpty()
                        if (verifyThumbs) {
                            thumbnails.forEach {
                                add(it.path.toTrackedFile(it, FileType.THUMBNAIL, checksum = null))
                            }
                        }
                        val previews = files[AssetFile.Type.PREVIEW].orEmpty()
                        if (verifyPreviews) {
                            previews.forEach {
                                add(it.path.toTrackedFile(it, FileType.PREVIEW, checksum = null))
                            }
                        }
                        if (verifyEncodedVideos && it.encodedVideoPath != null) {
                            add(it.encodedVideoPath.toTrackedFile(it, FileType.ENCODED_VIDEO, checksum = null))
                        }
                    }
                }
            )
            if (verifyPeople) {
                yieldAll(
                    people
                        .asSequence()
                        .mapNotNull { it.thumbnailPath?.toTrackedFile(it, FileType.PROFILE, checksum = null) }
                )
            }
        }
        return allFiles.take(limitFiles ?: Int.MAX_VALUE).toList()
    }

    @OptIn(ExperimentalPathApi::class)
    fun getFilteredFilesFromFileSystem(uploadLocation: Path): Sequence<Path> {
        val files = sequence {
            if (verifyOriginals) {
                yieldAll((uploadLocation / "library").walk())
            }
            if (verifyThumbs) {
                yieldAll((uploadLocation / "thumbs").walk())
            }
            if (verifyEncodedVideos) {
                yieldAll((uploadLocation / "encoded-video").walk())
            }
        }
        return files.filter {
            val mimeType = URLConnection.guessContentTypeFromName(it.name) ?: ""
            if (mimeType.startsWith("image")) {
                verifyImages
            } else if (mimeType.startsWith("video")) {
                verifyVideos
            } else {
                true
            }
        }.take(limitFiles ?: Int.MAX_VALUE)
    }
}

/**
 * A file tracked by Immich's DB
 */
class TrackedFile(
    val entity: DbEntity,
    val path: Path,
    val type: FileType,
    val checksum: ByteArray?,
)