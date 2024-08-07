package dev.mmauro.immichassistant.db

import dev.mmauro.immichassistant.common.ImmichConfig
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf

fun ImmichConfig.connectDb(): Session {
    return sessionOf(
        url = "jdbc:postgresql://$dbHostname:$dbPort/$dbDatabaseName",
        user = dbUsername,
        password = dbPassword
    )
}

fun <T> Session.selectAll(entity: EntityType<T>): List<T> {
    return queryOf("SELECT * FROM \"${entity.tableName}\"")
        .map(entity::map)
        .asList
        .let { run(it) }
}