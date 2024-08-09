package dev.mmauro.immichassistant.verify

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.mmauro.immichassistant.verify.consistency.VerifyConsistencyCommand
import dev.mmauro.immichassistant.verify.oprhaned.VerifyOrphanedCommands

class VerifyCommand : CliktCommand(
    name = "verify"
) {

    init {
        subcommands(VerifyConsistencyCommand())
        subcommands(VerifyOrphanedCommands())
    }

    override fun run() = Unit
}