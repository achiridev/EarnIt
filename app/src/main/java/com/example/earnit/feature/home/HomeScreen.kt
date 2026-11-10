package com.example.earnit.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.earnit.core.ui.components.AppVersionButton
import com.example.earnit.core.ui.components.HabitCalendar
import com.example.earnit.core.ui.components.HabitItem
import com.example.earnit.core.ui.components.getIcon
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitCompletion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.TextStyle as JavaTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    habitsFlow: Flow<List<Habit>>,
    completionsFlow: Flow<List<HabitCompletion>>,
    selectedDateFlow: StateFlow<LocalDate>,
    todayFlow: StateFlow<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onGoToToday: () -> Unit,
    onRefreshToday: () -> Unit,
    onAddHabit: () -> Unit,
    onHabitClick: (Habit, LocalDate) -> Unit,
    onToggleCompletion: (Long, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val completions by completionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedDate by selectedDateFlow.collectAsStateWithLifecycle()
    val today by todayFlow.collectAsStateWithLifecycle()
    val completedHabitIds = remember(completions) {
        completions.filter { it.completed }.map { it.habitId }.toSet()
    }

    // Mantiene "hoy" al día si la app permanece abierta cruzando medianoche.
    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
            onRefreshToday()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onRefreshToday()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Hábitos", fontWeight = FontWeight.Medium) },
                actions = { AppVersionButton() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddHabit,
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo hábito") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HabitCalendar(
                selectedDate = selectedDate,
                today = today,
                onDateSelected = onDateSelected,
                onGoToToday = onGoToToday,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SelectedDateLabel(selectedDate = selectedDate, today = today)

            if (habits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    EmptyState(isToday = selectedDate == today, onAddHabit = onAddHabit)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(habits) { habit ->
                        val isCompleted = completedHabitIds.contains(habit.id)
                        HabitItem(
                            name = habit.name,
                            icon = getIcon(habit.icon),
                            color = habit.color,
                            isCompleted = isCompleted,
                            onClick = { onHabitClick(habit, selectedDate) },
                            onToggle = { onToggleCompletion(habit.id, selectedDate) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDateLabel(selectedDate: LocalDate, today: LocalDate) {
    val locale = LocalConfiguration.current.locales[0]
    val dayName = selectedDate.dayOfWeek.getDisplayName(JavaTextStyle.FULL, locale)
    val monthName = selectedDate.month.getDisplayName(JavaTextStyle.FULL, locale)
    val prefix = if (selectedDate == today) "Hoy" else dayName.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
    }

    Text(
        text = "$prefix, ${selectedDate.dayOfMonth} de $monthName",
        modifier = Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EmptyState(isToday: Boolean, onAddHabit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.TrackChanges,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isToday) "No hay hábitos hoy" else "No hay hábitos este día",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isToday) "Añade tu primer hábito para empezar"
            else "No hay hábitos que existieran en esta fecha",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddHabit) {
            Text("Crear hábito")
        }
    }
}
