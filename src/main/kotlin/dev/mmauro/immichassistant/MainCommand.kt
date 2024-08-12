package dev.mmauro.immichassistant

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.mmauro.immichassistant.commands.verify.VerifyCommand

class MainCommand: CliktCommand(name = "immich-assistant") {

    init {
        subcommands(VerifyCommand())
    }
    override fun run() = Unit
}

fun main(args: Array<String>) = MainCommand().main(args)

