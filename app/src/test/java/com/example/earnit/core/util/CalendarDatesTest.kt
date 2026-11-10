package com.example.earnit.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pruebas locales del cálculo de semanas y de la cuadrícula mensual. `CalendarDates.kt`
 * es Kotlin puro precisamente para poder verificarlo sin dispositivo.
 */
class CalendarDatesTest {

    @Test
    fun weekDates_startOnTheConfiguredFirstDay() {
        val date = LocalDate.of(2026, 11, 10) // martes

        val mondayWeek = weekDates(date, DayOfWeek.MONDAY)
        val sundayWeek = weekDates(date, DayOfWeek.SUNDAY)

        assertEquals(7, mondayWeek.size)
        assertEquals(LocalDate.of(2026, 11, 9), mondayWeek.first())
        assertEquals(LocalDate.of(2026, 11, 15), mondayWeek.last())

        assertEquals(LocalDate.of(2026, 11, 8), sundayWeek.first())
        assertEquals(LocalDate.of(2026, 11, 14), sundayWeek.last())
    }

    @Test
    fun monthCells_padsTheRowsWithNulls() {
        val cells = monthCells(YearMonth.of(2026, 2), DayOfWeek.MONDAY)

        // Febrero de 2026 empieza en domingo: con lunes como primer día sobran 6 huecos.
        assertEquals(35, cells.size)
        assertEquals(List(6) { null }, cells.take(6))
        assertEquals(LocalDate.of(2026, 2, 1), cells[6])
        assertEquals(LocalDate.of(2026, 2, 28), cells[33])
        assertNull(cells.last())
    }

    @Test
    fun monthCells_handlesAMonthStartingOnTheFirstWeekDay() {
        val cells = monthCells(YearMonth.of(2026, 6), DayOfWeek.MONDAY)

        // Junio de 2026 empieza en lunes y tiene 30 días: 5 filas y 5 huecos al final.
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 6, 1), cells.first())
        assertEquals(LocalDate.of(2026, 6, 30), cells[29])
        assertEquals(List(5) { null }, cells.takeLast(5))
    }

    @Test
    fun weekDayLabels_followTheConfiguredFirstDay() {
        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
            ),
            weekDayLabels(DayOfWeek.MONDAY)
        )
    }
}
