package dev.mmauro.immichassistant.db

import com.github.ajalt.mordant.animation.coroutines.CoroutineProgressAnimator
import dev.mmauro.immichassistant.common.CommonCommand
import dev.mmauro.immichassistant.common.ImmichConfig
import dev.mmauro.immichassistant.common.task.addDeferredTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
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

context(CommonCommand, CoroutineScope)
fun CoroutineProgressAnimator.connectDbTask(immichConfig: ImmichConfig): Deferred<Session> {
    return addDeferredTask("Connecting to DB") {
        started()
        immichConfig.connectDb()
    }
}

context(CommonCommand, CoroutineScope)
fun <T> CoroutineProgressAnimator.addUseDbTask(
    dbTask: Deferred<Session>,
    name: String,
    block: (db: Session) -> T,
): Deferred<T> {
    return addDeferredTask(name) {
        val db = dbTask.await()
        started()
        delay(1000)
        block(db)
    }
}