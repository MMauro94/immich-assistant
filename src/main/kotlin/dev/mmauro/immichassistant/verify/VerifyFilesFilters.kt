package dev.mmauro.immichassistant.verify

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.mmauro.immichassistant.common.Constants
import dev.mmauro.immichassistant.common.FileType
import dev.mmauro.immichassistant.db.model.Asset
import java.nio.file.Path
import kotlin.io.path.div

class VerifyFilesFilters : OptionGroup(
    name = "Verify files filters",
    help = "Allows to filter which files to verify".trimIndent(),
) {

    val verifyOriginals by option(help = "Verifies the original file")
        .flag("--skip-originals", default = true)

    val verifyThumbs by option(help = "Verifies the thumbnails")
        .flag("--skip-thumbnails", default = true)

    val verifyEncodedVideos by option(help = "Verifies the encoded videos")
        .flag("--skip-encoded-videos", default = true)

    val verifyImages by option(help = "Verifies image assets")
        .flag("--skip-images", default = true)

    val verifyVideos by option(help = "Verifies video assets")
        .flag("--skip-videos", default = true)


    private fun Asset.shouldInclude(): Boolean {
        return (verifyImages && type == Asset.Type.IMAGE) || (verifyVideos && type == Asset.Type.VIDEO)
    }

    fun getFilteredFiles(assets: Iterable<Asset>, uploadLocation: Path, limit: Int): Map<Asset, List<FilteredFile>> {
        fun Path.toAbsolute() = uploadLocation / Constants.ROOT_PATH.relativize(this)

        return assets.filter { it.shouldInclude() }.take(limit).associateWith {
            buildList {
                if (verifyOriginals) {
                    add(FilteredFile(it.originalPath.toAbsolute(), FileType.ORIGINAL, it.checksum))
                }
                if (verifyThumbs && it.thumbnailPath != null) {
                    add(FilteredFile(it.thumbnailPath.toAbsolute(), FileType.THUMBNAIL, checksum = null))
                }
                if (verifyEncodedVideos && it.encodedVideoPath != null) {
                    add(FilteredFile(it.encodedVideoPath.toAbsolute(), FileType.ENCODED_VIDEO, checksum = null))
                }
            }
        }
    }
}

class FilteredFile(
    val path: Path,
    val type: FileType,
    val checksum: ByteArray?,
)