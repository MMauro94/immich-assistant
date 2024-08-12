package dev.mmauro.immichassistant.common

import java.nio.file.Path

data class PathWithSize(
    val path: Path,
    val size: Long,
)

fun Iterable<PathWithSize>.totalSize() = sumOf { it.size }