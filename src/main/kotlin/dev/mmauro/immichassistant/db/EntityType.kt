package dev.mmauro.immichassistant.db

import kotliquery.Row

interface EntityType<T> {
    val tableName: String

    fun map(row: Row): T
}