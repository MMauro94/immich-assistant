package dev.mmauro.immichassistant.commands.fix

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.mmauro.immichassistant.commands.fix.timezone.FixTimezoneCommand

class FixCommand : CliktCommand(
    name = "fix",
) {

    init {
        subcommands(FixTimezoneCommand())
    }

    override fun run() = Unit

    override fun aliases() = mapOf(
        "tz" to listOf("timezone"),
    )
}