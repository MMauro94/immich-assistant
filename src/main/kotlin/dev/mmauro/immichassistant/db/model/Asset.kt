package dev.mmauro.immichassistant.db.model

import dev.mmauro.immichassistant.db.EntityType
import kotliquery.Row
import java.nio.file.Path
import kotlin.io.path.Path

class Asset(
    override val id: String,
    val ownerId: String,
    val type: Type,
    val originalPath: Path,
    val encodedVideoPath: Path?,
    val checksum: ByteArray,
    val isVisible: Boolean,
    val livePhotoVideoId: String?,
    val sidecarPath: Path?,
) : DbEntity {
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
            encodedVideoPath = row.stringOrNull("encodedVideoPath")?.let { Path(it) },
            checksum = row.bytes("checksum"),
            isVisible = row.boolean("isVisible"),
            livePhotoVideoId = row.stringOrNull("livePhotoVideoId"),
            sidecarPath = row.stringOrNull("sidecarPath")?.let { Path(it) },
        )
    }
}
