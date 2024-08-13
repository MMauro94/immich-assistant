package dev.mmauro.immichassistant.commands.fix.timezone

import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.transformValues
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.nanoseconds

data class TimezoneRange(
    val timezone: TimeZone,
    override val start: LocalDateTime,
    val endExclusive: LocalDateTime,
): ClosedRange<LocalDateTime> {

    override val endInclusive = (endExclusive.toInstant(TimeZone.UTC) - 1.nanoseconds).toLocalDateTime(TimeZone.UTC)

    init {
        require(start < endExclusive) { "Start of date/time range must be less than end of date/time range" }
    }

    fun intersectsWith(other: TimezoneRange): Boolean {
        return this.endExclusive > other.start && this.start < other.endExclusive
    }
}

fun List<TimezoneRange>.areMutuallyExclusive(): Boolean {
    return sortedBy { it.start }
        .zipWithNext()
        .all { (a, b) ->
            !a.intersectsWith(b)
        }

}

fun NullableOption<String, String>.timezoneRange(): NullableOption<TimezoneRange, String> {
    return transformValues(3) { (tz, from, to) ->
        TimezoneRange(
            timezone = TimeZone.of(tz),
            start = LocalDateTime.parse(from),
            endExclusive = LocalDateTime.parse(to),
        )
    }
}