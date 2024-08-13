package dev.mmauro.immichassistant.db.model

import dev.mmauro.immichassistant.db.EntityType
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotliquery.Row

data class ExifData(
    val assetId: String,
    val dateTimeOriginal: Instant?,
    val timeZone: String?,
) : DbEntity {
    override val id: String get() = assetId

    companion object : EntityType<ExifData> {
        override val tableName = "exif"

        override fun map(row: Row) = ExifData(
            assetId = row.string("assetId"),
            dateTimeOriginal = row.instantOrNull("dateTimeOriginal")?.toKotlinInstant(),
            timeZone = row.stringOrNull("timeZone"),
        )
    }
}