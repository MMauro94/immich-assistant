package dev.mmauro.immichassistant.common.task

import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.progress.ProgressLayoutScope
import com.github.ajalt.mordant.widgets.progress.ProgressState
import dev.mmauro.immichassistant.common.byteSizeToString

fun <T> ProgressLayoutScope<T>.itemsCount(
    total: Long,
    suffix: String,
    completed: ProgressState<T>.() -> Long,
) {
    val totalLength = total.toString().length
    cell(width = ColumnWidth.Fixed(totalLength * 2 + 1 + suffix.length)) {
        Text("${completed()}/$total$suffix")
    }
}

fun <T> ProgressLayoutScope<T>.bytes(totalBytes: Long, bytes: ProgressState<T>.() -> Long) {
    val total = totalBytes.byteSizeToString()
    cell(width = ColumnWidth.Fixed(19)) {
        val current = bytes().byteSizeToString()
        Text("$current/$total")
    }
}