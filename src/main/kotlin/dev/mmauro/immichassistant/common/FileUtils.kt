package dev.mmauro.immichassistant.common

import java.nio.file.Path
import kotlin.io.path.div

fun Path.toAbsolute(uploadLocation: Path) = uploadLocation / Constants.ROOT_PATH.relativize(this)
