package dev.mmauro.immichassistant.db.model

import dev.mmauro.immichassistant.db.EntityType
import kotliquery.Row
import java.nio.file.Path
import kotlin.io.path.Path

class Asset(
    val id: String,
    val ownerId: String,
    val type: Type,
    val originalPath: Path,
    val previewPath: Path?,
    val thumbnailPath: Path?,
    val encodedVideoPath: Path?,
    val isVisible: Boolean,
    val livePhotoVideoId: String?,
    val checksum: ByteArray,
) {
    enum class Type {
        IMAGE, VIDEO
    }

    override fun equals(other: Any?) = other is Asset && id == other.id
    override fun hashCode() = id.hashCode()

    companion object : EntityType<Asset> {
        override val tableName = "assets"

        override fun map(row: Row) = Asset(
            id = row.string("id"),
            ownerId = row.string("id"),
            type = Type.valueOf(row.string("type")),
            originalPath = Path(row.string("originalPath")),
            previewPath = row.stringOrNull("previewPath")?.let { Path(it) },
            thumbnailPath = row.stringOrNull("thumbnailPath")?.let { Path(it) },
            encodedVideoPath = row.stringOrNull("encodedVideoPath")?.let { Path(it) },
            isVisible = row.boolean("isVisible"),
            livePhotoVideoId = row.stringOrNull("livePhotoVideoId"),
            checksum = row.bytes("checksum"),
        )
    }
}
