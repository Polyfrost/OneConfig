package org.polyfrost.oneconfig.utils.v1

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.platform.v1.Platform

@ApiStatus.Experimental
public object Formatters {
    private const val TIME = "oneconfig.format.time."
    private const val COUNT = "oneconfig.format.count."
    private const val SIZE = "oneconfig.format.size."

    private val english: Map<String, String> = mapOf(
        "${TIME}just_now" to "just now",
        "${TIME}moment" to "in a moment",
        "${TIME}seconds_ago" to "%s seconds ago",
        "${TIME}minute_ago" to "%s minute ago",
        "${TIME}minutes_ago" to "%s minutes ago",
        "${TIME}hour_ago" to "%s hour ago",
        "${TIME}hours_ago" to "%s hours ago",
        "${TIME}day_ago" to "%s day ago",
        "${TIME}days_ago" to "%s days ago",
        "${TIME}month_ago" to "%s month ago",
        "${TIME}months_ago" to "%s months ago",
        "${TIME}year_ago" to "%s year ago",
        "${TIME}years_ago" to "%s years ago",
        "${TIME}in_seconds" to "in %s seconds",
        "${TIME}in_minute" to "in %s minute",
        "${TIME}in_minutes" to "in %s minutes",
        "${TIME}in_hour" to "in %s hour",
        "${TIME}in_hours" to "in %s hours",
        "${TIME}in_day" to "in %s day",
        "${TIME}in_days" to "in %s days",
        "${TIME}in_month" to "in %s month",
        "${TIME}in_months" to "in %s months",
        "${TIME}in_year" to "in %s year",
        "${TIME}in_years" to "in %s years",
        "${TIME}today_at" to "Today, %s",
        "${TIME}yesterday_at" to "Yesterday, %s",
        "${COUNT}thousand" to "%sK",
        "${COUNT}million" to "%sM",
        "${COUNT}billion" to "%sB",
        "${COUNT}trillion" to "%sT",
        "${SIZE}bytes" to "%s B",
        "${SIZE}kilobytes" to "%s KB",
        "${SIZE}megabytes" to "%s MB",
        "${SIZE}gigabytes" to "%s GB",
        "${SIZE}terabytes" to "%s TB",
    )

    @Volatile
    private var cachedLocale: Pair<String, Locale>? = null

    @JvmStatic
    @JvmOverloads
    public fun relativeTime(instant: Instant, now: Instant = Instant.now()): String {
        val elapsed = Duration.between(instant, now).seconds
        val future = elapsed < 0
        val seconds = abs(elapsed)
        if (seconds < 10) return translate(if (future) "${TIME}moment" else "${TIME}just_now")
        val days = seconds / 86_400
        return when {
            seconds < 60 -> relative(future, "second", seconds)
            seconds < 3_600 -> relative(future, "minute", seconds / 60)
            seconds < 86_400 -> relative(future, "hour", seconds / 3_600)
            days < 30 -> relative(future, "day", days)
            days < 365 -> relative(future, "month", days / 30)
            else -> relative(future, "year", days / 365)
        }
    }

    @JvmStatic
    @JvmOverloads
    public fun time(instant: Instant, zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): String {
        val locale = locale()
        val dateTime = instant.atZone(zone)
        val today = now.atZone(zone).toLocalDate()
        val clock = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(dateTime)
        val text = when (dateTime.toLocalDate()) {
            today -> translate("${TIME}today_at", clock)
            today.minusDays(1) -> translate("${TIME}yesterday_at", clock)
            else -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale).format(dateTime)
        }
        return text.replace(' ', ' ')
    }

    @JvmStatic
    public fun count(n: Long): String {
        val magnitude = abs(n.toDouble())
        return when {
            magnitude < 1e3 -> NumberFormat.getIntegerInstance(locale()).format(n)
            magnitude < 1e6 -> translate("${COUNT}thousand", scaled(n / 1e3))
            magnitude < 1e9 -> translate("${COUNT}million", scaled(n / 1e6))
            magnitude < 1e12 -> translate("${COUNT}billion", scaled(n / 1e9))
            else -> translate("${COUNT}trillion", scaled(n / 1e12))
        }
    }

    @JvmStatic
    public fun count(n: Int): String = count(n.toLong())

    @JvmStatic
    public fun size(bytes: Long): String {
        val magnitude = abs(bytes.toDouble())
        return when {
            magnitude < KIB -> translate("${SIZE}bytes", NumberFormat.getIntegerInstance(locale()).format(bytes))
            magnitude < MIB -> translate("${SIZE}kilobytes", scaled(bytes / KIB))
            magnitude < GIB -> translate("${SIZE}megabytes", scaled(bytes / MIB))
            magnitude < TIB -> translate("${SIZE}gigabytes", scaled(bytes / GIB))
            else -> translate("${SIZE}terabytes", scaled(bytes / TIB))
        }
    }

    private const val KIB = 1024.0
    private const val MIB = KIB * 1024.0
    private const val GIB = MIB * 1024.0
    private const val TIB = GIB * 1024.0

    private fun relative(future: Boolean, unit: String, amount: Long): String {
        val plural = amount != 1L
        val key = if (future) {
            "${TIME}in_$unit${if (plural) "s" else ""}"
        } else {
            "${TIME}$unit${if (plural) "s" else ""}_ago"
        }
        return translate(key, NumberFormat.getIntegerInstance(locale()).format(amount))
    }

    private fun scaled(value: Double): String {
        val pattern = if (abs(value) < 100.0) "#,##0.#" else "#,##0"
        val format = DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale()))
        format.roundingMode = RoundingMode.DOWN
        return format.format(value)
    }

    private fun translate(key: String, vararg args: Any): String {
        val translated = runCatching { Platform.i18n().translateString(key, *args) }.getOrNull()
        if (translated != null && translated != key) return translated
        val fallback = english[key] ?: return key
        return if (args.isEmpty()) fallback else String.format(locale(), fallback, *args)
    }

    private fun locale(): Locale {
        val code = runCatching { Platform.i18n().translateString("language.code") }.getOrNull()
        if (code.isNullOrBlank() || code == "language.code") return Locale.getDefault()
        cachedLocale?.let { (cachedCode, locale) -> if (cachedCode == code) return locale }
        val locale = Locale.forLanguageTag(code.replace('_', '-'))
        cachedLocale = code to locale
        return locale
    }
}
