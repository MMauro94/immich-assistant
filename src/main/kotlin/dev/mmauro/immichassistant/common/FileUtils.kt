package dev.mmauro.immichassistant.common

import java.nio.file.Path
import kotlin.io.path.div

fun Path.toAbsolute(uploadLocation: Path) = uploadLocation / Constants.ROOT_PATH.relativize(this)

fun Long.byteSizeToString(): String {
    val bytes = toDouble()
    return when {
        bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30))
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20))
        bytes >= 1 shl 10 -> "%.0f kB".format(bytes / (1 shl 10))
        else -> "$bytes bytes"
    }
}
