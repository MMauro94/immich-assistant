package dev.mmauro.immichassistant.common

import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.widgets.progress.ProgressLayoutScope
import com.github.ajalt.mordant.widgets.progress.ProgressState

fun <T> ProgressLayoutScope<T>.itemsCount(
    completed: ProgressState<T>.() -> Long,
    total: Long,
    suffix: String,
) {
    val totalLength = total.toString().length
    cell(width = ColumnWidth.Fixed(totalLength * 2 + 1 + suffix.length)) {
        Text("${completed()}/$total$suffix")
    }
}
