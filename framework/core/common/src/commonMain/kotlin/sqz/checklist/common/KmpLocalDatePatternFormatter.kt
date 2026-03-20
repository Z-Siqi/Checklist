package sqz.checklist.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlin.math.min

/**
 * KMP-friendly LocalDate formatter:
 * - Parses a "pattern string" (similar to DateTimeFormatter patterns, but only for date)
 * - Builds kotlinx-datetime LocalDate.Format { ... } formatter
 * - Never throws; logs and falls back gracefully
 *
 * Supported (best-effort, case-tolerant):
 *  Year:
 *   - yyyy / yyy / yy / y
 *
 *  Month (treat both M and m as month):
 *   - MMMM -> month full name
 *   - MMM  -> month short name
 *   - MM   -> month number with zero padding (01..12)
 *   - M    -> month number without padding (1..12)
 *   - mmmm / mmm / mm / m are treated the same as M...
 *
 *  Day of month (treat both d and D as day-of-month here):
 *   - dd -> day with zero padding (01..31)
 *   - d  -> day without padding (1..31)
 *
 *  "dddd"/"DDDD" handling:
 *   In many ecosystems it can mean weekday name or day-of-year.
 *   LocalDate.Format DSL doesn't give us a universal cross-locale weekday/day-of-year formatter here,
 *   so we degrade gracefully:
 *    - "dddd" or "DDDD" => treated as "dd" (day of month, zero padded) + log a warning.
 *
 *  Literals:
 *   - Any other character is emitted via char(c)
 *
 * Notes:
 *  - Locale-aware month names are NOT handled here (MonthNames is passed in).
 *  - If pattern contains unsupported constructs, they are treated as literals or degraded, never crashing.
 */
object KmpLocalDatePatternFormatter {

    private fun log(msg: String) {
        println("KmpLocalDatePatternFormatter: $msg")
    }

    private sealed interface DateToken {
        data class Year(val count: Int) : DateToken         // y/yy/yyy/yyyy
        data class Month(val count: Int) : DateToken        // M/MM/MMM/MMMM (also m..)
        data class Day(val count: Int) : DateToken          // d/dd (also D..)
        data class Literal(val c: Char) : DateToken
    }

    /**
     * Format a LocalDate with a Java-like pattern string.
     *
     * @param date The date to format.
     * @param pattern Pattern like "dd MMM yyyy", "yyyy-MM-dd", "yyyy mmm dddd", etc.
     * @param shortMonths Month short names used for MMM.
     * @param fullMonths Month full names used for MMMM.
     * @param preferNumericMonthWhenTextNotLocalized
     *  - true: If pattern used MMM/MMMM (text month)，but not expected English month,
     *          it will downgrade to digital month automatically (MMM/MMMM -> MM)，then notice via log.
     *  - false: normal output MMM/MMMM, corresponding to shortMonths/fullMonths (default English).
     * @return formatted string; never throws.
     */
    fun format(
        date: LocalDate,
        pattern: String,
        shortMonths: MonthNames = MonthNames.Companion.ENGLISH_ABBREVIATED,
        fullMonths: MonthNames = MonthNames.Companion.ENGLISH_FULL,
        preferNumericMonthWhenTextNotLocalized: Boolean = false
    ): String {
        if (pattern.isBlank()) return fallback(date, "blank pattern")

        val formatter = runCatching {
            buildFormatter(
                pattern = pattern,
                shortMonths = shortMonths,
                fullMonths = fullMonths,
                preferNumericMonthWhenTextNotLocalized = preferNumericMonthWhenTextNotLocalized
            )
        }.onFailure {
            log("Failed to build formatter for '$pattern': ${it.message}")
        }.getOrNull() ?: return fallback(date, "formatter build failed")

        return runCatching { formatter.format(date) }
            .onFailure { log("Format failed for '$pattern': ${it.message}") }
            .getOrElse { fallback(date, "format failed") }
    }

    private fun fallback(date: LocalDate, reason: String): String {
        log("Fallback to ISO string. Reason: $reason")
        return date.toString()
    }

    private fun buildFormatter(
        pattern: String,
        shortMonths: MonthNames,
        fullMonths: MonthNames,
        preferNumericMonthWhenTextNotLocalized: Boolean
    ): DateTimeFormat<LocalDate> {

        val tokens = parse(pattern)

        return LocalDate.Format {
            for (t in tokens) {
                when (t) {
                    is DateToken.Year -> {
                        if (t.count < 4) {
                            log("Year token '${"y".repeat(t.count)}' degraded to full year")
                        }
                        year()
                    }

                    is DateToken.Month -> {
                        when (t.count) {
                            1 -> monthNumber(padding = Padding.NONE)
                            2 -> monthNumber(padding = Padding.ZERO)
                            3 -> {
                                if (preferNumericMonthWhenTextNotLocalized) {
                                    log("MMM requested but localized month names not provided; degrading to MM")
                                    monthNumber(padding = Padding.ZERO)
                                } else {
                                    monthName(shortMonths)
                                }
                            }
                            else -> { // 4+
                                if (preferNumericMonthWhenTextNotLocalized) {
                                    log("MMMM requested but localized month names not provided; degrading to MM")
                                    monthNumber(padding = Padding.ZERO)
                                } else {
                                    monthName(fullMonths)
                                }
                            }
                        }
                    }

                    is DateToken.Day -> {
                        when (t.count) {
                            1 -> day(padding = Padding.NONE)
                            2 -> day(padding = Padding.ZERO)
                            else -> {
                                log("Day token '${"d".repeat(t.count)}' ambiguous; degrading to 'dd'")
                                day(padding = Padding.ZERO)
                            }
                        }
                    }

                    is DateToken.Literal -> char(t.c)
                }
            }
        }
    }

    /**
     * Parse pattern into tokens.
     *
     * Best-effort / tolerant:
     * - treat both M and m as month
     * - treat both d and D as day-of-month (and degrade DDDD/dddd safely)
     * - unknown letters become literals (so it won't crash)
     */
    private fun parse(pattern: String): List<DateToken> {
        val out = ArrayList<DateToken>(pattern.length)
        var i = 0

        while (i < pattern.length) {
            val c = pattern[i]

            if (c.isLetter()) {
                val start = i
                var j = i + 1
                while (j < pattern.length && pattern[j] == c) j++
                val count = j - start

                val token = when (c) {
                    'y', 'Y' -> DateToken.Year(min(count, 4))
                    'M', 'm' -> DateToken.Month(min(count, 4))
                    'd', 'D' -> DateToken.Day(min(count, 4))
                    else -> {
                        //log("Unsupported pattern letter '$c' (x$count). Treating as literal.")
                        null
                    }
                }

                if (token != null) {
                    out += token
                } else {
                    repeat(count) { k -> out += DateToken.Literal(pattern[start + k]) }
                }

                i = j
            } else {
                out += DateToken.Literal(c)
                i++
            }
        }

        return out
    }
}
