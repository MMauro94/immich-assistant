package dev.mmauro.immichassistant.commands.fix.timezone

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.Tuple2
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime

private val TIME_1 = LocalDateTime.parse("2024-01-01T10:00")
private val TIME_2 = LocalDateTime.parse("2024-01-01T12:00")
private val TIME_3 = LocalDateTime.parse("2024-01-01T14:00")
private val TIME_4 = LocalDateTime.parse("2024-01-01T16:00")

class TimezoneRangeTest : FunSpec({
    context("constructor") {
        context("works with valid values") {
            withData(
                nameFn = { it.str() },
                tuple(TIME_1, TIME_2),
                tuple(TIME_1, TIME_3),
                tuple(TIME_2, TIME_3),
            ) { (from, to) ->
                shouldNotThrowAny {
                    range(from, to)
                }
            }
        }
        context("doesn't work with invalid values") {
            withData(
                nameFn = { it.str() },
                tuple(TIME_1, TIME_1),
                tuple(TIME_2, TIME_1),
                tuple(TIME_3, TIME_2),
            ) { (from, to) ->
                shouldThrow<IllegalArgumentException> {
                    range(from, to)
                }
            }
        }
    }

    context("intersectsWith()") {
        withData(
            nameFn = { (r1, r2) -> "${r1.str()} ∩ ${r2.str()}"},
            // Intersecting ranges
            tuple(range(TIME_1, TIME_2), range(TIME_1, TIME_2), true),
            tuple(range(TIME_1, TIME_3), range(TIME_2, TIME_4), true),
            tuple(range(TIME_1, TIME_4), range(TIME_2, TIME_3), true),

            // Non-intersecting ranges
            tuple(range(TIME_1, TIME_2), range(TIME_2, TIME_3), false),
            tuple(range(TIME_1, TIME_2), range(TIME_3, TIME_4), false),
        ) { (r1, r2, expected) ->
            r1.intersectsWith(r2) shouldBe expected
            r2.intersectsWith(r1) shouldBe expected
        }
    }

    context("areMutuallyExclusive()") {
        withData(
            nameFn = { (ranges, _) -> ranges.joinToString(" ∩ ") { it.str() } },
            tuple(
                listOf(
                    range(TIME_1, TIME_2),
                    range(TIME_2, TIME_3),
                    range(TIME_3, TIME_4),
                ),
                true
            ),
            tuple(
                listOf(
                    range(TIME_1, TIME_3),
                    range(TIME_2, TIME_4),
                ),
                false
            ),
        ) { (list, expected) ->
            list.areMutuallyExclusive() shouldBe expected
        }
    }
})

private fun range(start: LocalDateTime, endExclusive: LocalDateTime) = TimezoneRange(
    // we don't care about the timezone for these tests
    timezone = mockk(),
    start = start,
    endExclusive = endExclusive,
)

private fun Tuple2<LocalDateTime, LocalDateTime>.str(): String {
    return "$a..<$b"
}

private fun TimezoneRange.str(): String {
    return "$start..<$endExclusive"
}