package dev.mmauro.immichassistant.common.task

import dev.mmauro.immichassistant.common.PathWithSize
import dev.mmauro.immichassistant.common.byteSizeToString
import java.nio.file.Path
import kotlin.io.path.fileSize

fun TaskRunnerScope.listFiles(sequence: Sequence<Path>): List<PathWithSize> {
    var count = 0
    var size = 0L
    return sequence
        .map { PathWithSize(it, it.fileSize()) }
        .onEach {
            count++
            size += it.size
            update("($count files discovered, ${size.byteSizeToString()})")
        }
        .toList()
}

