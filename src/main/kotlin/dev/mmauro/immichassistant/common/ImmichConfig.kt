package dev.mmauro.immichassistant.common

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path

class ImmichConfig : OptionGroup(
    name = "Immich config",
    help = """
        Contains all relevant config options to connect to Immich.
        All options can be specified using either a CLI argument or an environment variable, identical to the one supplied to Immich.
        
        See: https://immich.app/docs/install/environment-variables#database
        See: https://immich.app/docs/install/environment-variables#docker-compose
    """.trimIndent(),
) {

    val dbHostname by option(
        "--db-hostname",
        help = "The hostname of Immich's Postgres database.",
        envvar = "DB_HOSTNAME",
    ).required()
    val dbPort by option(
        "--db-port",
        help = "The port of Immich's Postgres database.",
        envvar = "DB_PORT",
    ).int().default(5432)
    val dbUsername by option(
        "--db-username",
        help = "The username to use when connecting to Immich's Postgres database.",
        envvar = "DB_USERNAME",
    ).required()
    val dbPassword by option(
        "--db-password",
        help = "The password to use when connecting to Immich's Postgres database.",
        envvar = "DB_PASSWORD",
    ).required()
    val dbDatabaseName by option(
        "--db-database-name",
        help = "The Postgres database name to use when connecting to Immich's Postgres database.",
        envvar = "DB_DATABASE_NAME",
    ).required()
    val uploadLocation by option(
        "--upload-location",
        help = "The directory in which Immich is pointed to with its UPLOAD_LOCATION env.",
        envvar = "UPLOAD_LOCATION"
    ).path(mustExist = true, canBeFile = false, mustBeReadable = true).required()
}