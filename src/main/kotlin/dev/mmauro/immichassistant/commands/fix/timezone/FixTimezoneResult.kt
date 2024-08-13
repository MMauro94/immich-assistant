package dev.mmauro.immichassistant.commands.fix.timezone

import com.github.ajalt.mordant.rendering.TextColors
import dev.mmauro.immichassistant.db.model.Asset
import kotlinx.datetime.TimeZone

sealed interface FixTimezoneResult {

    val message: String

    data object Untouched : FixTimezoneResult {
        override val message = "🙌 Untouched"
    }

    data object MissingExifData : FixTimezoneResult {
        override val message = "💔 Missing EXIF data"
    }

    data object MissingTimezoneRange : FixTimezoneResult {
        override val message = "🕒 Missing timezone range"
    }

    data class TimezoneFixed(
        val oldTimeZone: TimeZone?,
        val newTimezone: TimeZone?
    ) : FixTimezoneResult {
        override val message = run {
            val oldTimezone = oldTimeZone?.toString().orEmpty().ifEmpty { "no timezone" }
            val newTimezone = newTimezone.toString()
            "🛠️ Fixed from ${TextColors.red(oldTimezone)} to ${TextColors.green(newTimezone)}"
        }

        fun logLine(asset: Asset) = buildString {
            append(asset.id)
            append(" (")
            append(oldTimeZone?.toString().orEmpty().ifEmpty { "???" })
            append(" --> ")
            append(newTimezone.toString())
            append("): ")
            append(asset.originalPath)
        }

        companion object {
            private val TZ_COMPARATOR = compareBy<TimeZone?> {
                when (it) {
                    null -> 0
                    else -> 1
                }
            }.thenBy { it.toString() }

            val COMPARATOR = compareBy<TimezoneFixed, _>(TZ_COMPARATOR) {
                it.oldTimeZone
            }.thenBy(TZ_COMPARATOR) { it.newTimezone }
        }
    }

    companion object {
        val COMPARATOR = compareBy<FixTimezoneResult> {
            when (it) {
                Untouched -> 0
                MissingExifData -> 1
                MissingTimezoneRange -> 2
                is TimezoneFixed -> 3
            }
        }.thenBy(TimezoneFixed.COMPARATOR) {
            if (it is TimezoneFixed) {
                it
            } else {
                error("shouldn't have more than 1 data object")
            }
        }
    }
}