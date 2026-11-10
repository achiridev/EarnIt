package com.example.earnit.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Primera fecha de la semana que contiene [date], según [firstDayOfWeek].
 *
 * Se mantiene como Kotlin puro (sin Compose) para poder verificar el cálculo de
 * semanas y meses en `./gradlew test`, sin dispositivo.
 */
fun startOfWeek(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate {
    val daysSinceStart = (date.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    return date.minusDays(daysSinceStart.toLong())
}

/** Las siete fechas de la semana que contiene [date]. */
fun weekDates(date: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val start = startOfWeek(date, firstDayOfWeek)
    return (0L..6L).map(start::plusDays)
}

/**
 * Celdas necesarias para dibujar [month], rellenando con `null` los huecos
 * anteriores y posteriores para que cada fila empiece en [firstDayOfWeek].
 */
fun monthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val rowCount = (leading + month.lengthOfMonth() + 6) / 7
    val gridStart = first.minusDays(leading.toLong())

    return (0 until rowCount * 7).map { index ->
        val date = gridStart.plusDays(index.toLong())
        date.takeIf { YearMonth.from(it) == month }
    }
}

/** Los siete días de la semana en el orden de [firstDayOfWeek]. */
fun weekDayLabels(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
    (0L..6L).map(firstDayOfWeek::plus)
