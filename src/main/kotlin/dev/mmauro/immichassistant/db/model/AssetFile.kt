package dev.mmauro.immichassistant.db.model

import dev.mmauro.immichassistant.db.EntityType
import kotliquery.Row
import java.nio.file.Path
import kotlin.io.path.Path

class AssetFile(
    override val id: String,
    val assetId: String,
    val type: Type,
    val path: Path,
) : DbEntity {

    enum class Type {
        PREVIEW, THUMBNAIL
    }

    override fun equals(other: Any?) = other is AssetFile && id == other.id
    override fun hashCode() = id.hashCode()

    companion object : EntityType<AssetFile> {

        override val tableName = "assets_files"

        override fun map(row: Row) = AssetFile(
            id = row.string("id"),
            assetId = row.string("assetId"),
            type = row.string("type").let { type ->
                Type.entries.single { it.name.equals(type, ignoreCase = true) }
            },
            path = Path(row.string("path")),
        )
    }
}
