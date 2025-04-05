package dev.mmauro.immichassistant.commands.verify

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.mmauro.immichassistant.commands.verify.dir.VerifyDirCommand
import dev.mmauro.immichassistant.commands.verify.consistency.VerifyConsistencyCommand
import dev.mmauro.immichassistant.commands.verify.oprhaned.VerifyOrphanedCommand

class VerifyCommand : CliktCommand(
    name = "verify"
) {

    init {
        subcommands(VerifyDirCommand())
        subcommands(VerifyConsistencyCommand())
        subcommands(VerifyOrphanedCommand())
    }

    override fun run() = Unit
}