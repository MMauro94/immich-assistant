package dev.mmauro.immichassistant.db.model

import dev.mmauro.immichassistant.db.EntityType
import kotliquery.Row
import java.nio.file.Path
import kotlin.io.path.Path

class Person(
    override val id: String,
    val ownerId: String,
    val thumbnailPath: Path?,
) : DbEntity {
    override fun equals(other: Any?) = other is Person && id == other.id
    override fun hashCode() = id.hashCode()

    companion object : EntityType<Person> {
        override val tableName = "person"

        override fun map(row: Row) = Person(
            id = row.string("id"),
            ownerId = row.string("id"),
            thumbnailPath = row.stringOrNull("thumbnailPath")?.let { Path(it) },
        )
    }
}
