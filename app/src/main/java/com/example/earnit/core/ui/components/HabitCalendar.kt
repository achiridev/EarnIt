package com.example.earnit.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.earnit.core.util.monthCells
import com.example.earnit.core.util.weekDates
import com.example.earnit.core.util.weekDayLabels
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Calendario de la pantalla principal.
 *
 * La tira semanal está siempre visible. El botón de la derecha despliega la
 * cuadrícula del mes; allí las flechas navegan entre meses. En la vista semanal
 * las flechas desplazan la selección una semana, nunca más allá de hoy.
 */
@Composable
fun HabitCalendar(
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var visibleMonthText by rememberSaveable { mutableStateOf(YearMonth.from(selectedDate).toString()) }
    val visibleMonth = remember(visibleMonthText) { YearMonth.parse(visibleMonthText) }

    // Si la selección cambia desde fuera ("Hoy"), la vista mensual vuelve a su mes.
    LaunchedEffect(selectedDate) {
        visibleMonthText = YearMonth.from(selectedDate).toString()
    }

    val titleMonth = if (expanded) visibleMonth else YearMonth.from(selectedDate)
    val title = monthTitle(titleMonth, locale)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (selectedDate != today) {
                        TextButton(
                            onClick = onGoToToday,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text("Hoy", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                IconButton(onClick = { navigateBack(expanded, visibleMonth, selectedDate, onDateSelected) { visibleMonthText = it } }) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = if (expanded) "Mes anterior" else "Semana anterior"
                    )
                }
                IconButton(
                    onClick = {
                        when {
                            expanded -> {
                                if (visibleMonth < YearMonth.from(today)) {
                                    visibleMonthText = visibleMonth.plusMonths(1).toString()
                                }
                            }
                            selectedDate.isBefore(today) -> {
                                onDateSelected(minOf(selectedDate.plusWeeks(1), today))
                            }
                        }
                    },
                    enabled = if (expanded) visibleMonth < YearMonth.from(today) else selectedDate.isBefore(today)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = if (expanded) "Mes siguiente" else "Semana siguiente"
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.CalendarMonth,
                        contentDescription = if (expanded) "Ocultar mes" else "Ver mes"
                    )
                }
            }

            if (expanded) {
                MonthGrid(
                    month = visibleMonth,
                    selectedDate = selectedDate,
                    today = today,
                    firstDayOfWeek = firstDayOfWeek,
                    locale = locale,
                    onDateSelected = onDateSelected
                )
            } else {
                WeekStrip(
                    selectedDate = selectedDate,
                    today = today,
                    firstDayOfWeek = firstDayOfWeek,
                    locale = locale,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

private fun navigateBack(
    expanded: Boolean,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onVisibleMonthChanged: (String) -> Unit
) {
    if (expanded) {
        onVisibleMonthChanged(visibleMonth.minusMonths(1).toString())
    } else {
        onDateSelected(selectedDate.minusWeeks(1))
    }
}

@Composable
private fun WeekStrip(
    selectedDate: LocalDate,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    locale: Locale,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        weekDates(selectedDate, firstDayOfWeek).forEach { date ->
            WeekDayCell(
                date = date,
                selectedDate = selectedDate,
                today = today,
                locale = locale,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun RowScope.WeekDayCell(
    date: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    locale: Locale,
    onDateSelected: (LocalDate) -> Unit
) {
    val enabled = !date.isAfter(today)
    val selected = date == selectedDate
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Button,
                onClick = { onDateSelected(date) }
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        DayNumber(
            day = date.dayOfMonth,
            selected = selected,
            isToday = date == today,
            enabled = enabled
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    locale: Locale,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDayLabels(firstDayOfWeek).forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        monthCells(month, firstDayOfWeek).chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f).height(40.dp))
                    } else {
                        MonthDayCell(
                            date = date,
                            selectedDate = selectedDate,
                            today = today,
                            onDateSelected = onDateSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.MonthDayCell(
    date: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val enabled = !date.isAfter(today)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .selectable(
                selected = date == selectedDate,
                enabled = enabled,
                role = Role.Button,
                onClick = { onDateSelected(date) }
            ),
        contentAlignment = Alignment.Center
    ) {
        DayNumber(
            day = date.dayOfMonth,
            selected = date == selectedDate,
            isToday = date == today,
            enabled = enabled
        )
    }
}

@Composable
private fun DayNumber(
    day: Int,
    selected: Boolean,
    isToday: Boolean,
    enabled: Boolean
) {
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val border = if (isToday && !selected) {
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(background)
            .then(border),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor
        )
    }
}

private fun monthTitle(month: YearMonth, locale: Locale): String {
    val monthName = month.month.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    return "$monthName ${month.year}"
}
