package com.example.earnit.feature.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.earnit.core.ui.components.accentColor
import com.example.earnit.core.ui.components.getIcon
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.repository.HabitRepository
import com.example.earnit.ui.theme.EarnItTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: HabitDetailViewModel,
    habitId: Long,
    habitRepository: HabitRepository?,
    onBack: () -> Unit,
    onEdit: (Habit) -> Unit,
    onArchive: () -> Unit,
    onToggleCompletion: (LocalDate) -> Unit
) {
    val habit by viewModel.habit.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val isCompletedToday = history.any { it.date == today && it.completed }

    // Relee el hábito por id, igual que hace `HabitFormScreen`. Sin esto el detalle
    // depende de que `AppNavHost` haya llamado a `initialize()` antes de navegar, así
    // que al restaurar el back stack directamente en `habit_detail/{id}` la pantalla
    // queda vacía, y tras editar muestra los datos antiguos (BUG-004).
    LaunchedEffect(habitId) {
        habitRepository?.getById(habitId)?.let { viewModel.initialize(it, today) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = habit?.name ?: "Hábito", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { habit?.let { onEdit(it) } }) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onArchive) {
                        Icon(imageVector = Icons.Filled.Archive, contentDescription = "Archivar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .fillMaxWidth()
            ,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Habit header card
            habit?.let { h ->
                HabitHeaderCard(
                    habit = h,
                    isCompleted = isCompletedToday,
                    onToggle = { onToggleCompletion(LocalDate.now()) }
                )
            }

            // History
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("Historial", style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(history.sortedByDescending { it.date }) { completion ->
                        HistoryItem(completion = completion)
                    }
                }
            }
        }
    }
}

@Composable
fun HabitHeaderCard(
    habit: Habit,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    val themeColor = Color(habit.color)
    // El fondo usa el color puro; icono y texto, la variante de acento, para que
    // los colores pasteles sigan siendo legibles sobre superficies claras.
    val accentColor = Color(habit.color.accentColor())

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onToggle,
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isCompleted) themeColor.copy(alpha = 0.15f) else themeColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIcon(habit.icon),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(48.dp)
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = habit.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            habit.description?.let { desc ->
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completado",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Completado hoy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                } else {
                    Text(
                        text = "Toca para marcar como completado",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(completion: HabitCompletion) {
    val date = completion.date
    // LocalConfiguration is observable inside compositions, unlike Locale.getDefault().
    val locale = LocalConfiguration.current.locales[0]
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    val dayNumber = date.dayOfMonth
    val monthName = date.month.getDisplayName(TextStyle.SHORT, locale)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "$dayName, $dayNumber $monthName",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (completion.completed) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Completado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}