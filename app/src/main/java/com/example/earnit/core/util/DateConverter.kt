package com.example.earnit.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Shared conversions between [LocalDate] and the epoch millis stored in the Room
 * database (always "midnight of the local day", e.g. `2026-10-10` -> startOfDay
 * in the system zone).
 *
 * Do NOT convert back with `LocalDate.ofEpochDay(millis / 86_400_000)`: integer
 * division shifts the date by one day in any non-UTC zone (fixed in
 * HabitCompletionRepositoryImpl / HabitRepositoryImpl).
 */
fun LocalDate.toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()