package dev.mmauro.immichassistant.verify

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.mmauro.immichassistant.verify.consistency.VerifyConsistencyCommand

class VerifyCommand : CliktCommand(
    name = "verify"
) {

    init {
        subcommands(VerifyConsistencyCommand())
    }

    override fun run() = Unit
}