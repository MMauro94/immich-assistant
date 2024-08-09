package dev.mmauro.immichassistant.common

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class CommonOptions: OptionGroup() {

    val debug by option(help = "Prints the stacktrace of errors").flag(default = false)
}