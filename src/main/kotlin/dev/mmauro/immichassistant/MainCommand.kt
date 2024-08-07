package dev.mmauro.immichassistant

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.mmauro.immichassistant.verify.VerifyCommand
import java.nio.charset.Charset

class MainCommand: CliktCommand() {

    init {
        subcommands(VerifyCommand())
    }
    override fun run() = Unit
}

fun main(args: Array<String>) = MainCommand().main(args)

