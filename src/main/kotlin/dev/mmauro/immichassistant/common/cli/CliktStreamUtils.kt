package dev.mmauro.immichassistant.common.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.types.isCliktParameterDefaultStdout
import java.io.OutputStream
import java.io.Writer

interface OutputScope {
    fun outputLine(line: String)
}

private class EchoOutputScope(val command: CliktCommand) : OutputScope {
    override fun outputLine(line: String) {
        command.echo(line)
    }
}

private class WriterOutputScope(val writer: Writer) : OutputScope, AutoCloseable {
    override fun outputLine(line: String) {
        writer.appendLine(line)
    }

    override fun close() {
        writer.close()
    }
}

context(CliktCommand)
fun OutputStream.cliktAwareOutput(block: OutputScope.() -> Unit) {
    if (isCliktParameterDefaultStdout) {
        block(EchoOutputScope(this@CliktCommand))
    } else {
        WriterOutputScope(bufferedWriter()).use(block)
    }
}