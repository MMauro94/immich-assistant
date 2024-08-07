package dev.mmauro.immichassistant.common

import java.io.OutputStream
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.io.path.inputStream

fun Path.sha1() : ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    DigestInputStream(inputStream().buffered(), digest).use { input ->
        // Just consume the stream in order to calculate the digest
        input.transferTo(OutputStream.nullOutputStream())
    }
    return digest.digest()
}