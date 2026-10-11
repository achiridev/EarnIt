package com.example.earnit.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val IconRows = 5
private val IconSize = 56.dp
private val IconSpacing = 8.dp

// 5 rows + 4 gaps + vertical content padding (2 * 8.dp)
private val iconGridHeight = IconSize * IconRows + IconSpacing * (IconRows - 1) + 16.dp

@Composable
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = listOf(
        "book" to Icons.Filled.MenuBook,
        "water" to Icons.Filled.LocalDrink,
        "exercise" to Icons.Filled.FitnessCenter,
        "sleep" to Icons.Filled.Bedtime,
        "meditate" to Icons.Filled.SelfImprovement,
        "code" to Icons.Filled.Code,
        "read" to Icons.Filled.ChromeReaderMode,
        "run" to Icons.Filled.DirectionsRun,
        "walk" to Icons.Filled.DirectionsWalk,
        "bike" to Icons.Filled.DirectionsBike,
        "swim" to Icons.Filled.Pool,
        "yoga" to Icons.Filled.SportsGymnastics,
        "study" to Icons.Filled.School,
        "work" to Icons.Filled.Work,
        "music" to Icons.Filled.MusicNote,
        "art" to Icons.Filled.Palette,
        "cook" to Icons.Filled.Restaurant,
        "clean" to Icons.Filled.CleaningServices,
        "plant" to Icons.Filled.Spa,
        "pet" to Icons.Filled.Pets,
    )

    LazyHorizontalGrid(
        // Lazy layouts need a bounded height: a Column with verticalScroll gives
        // infinite constraints and would throw
        // "LazyHorizontalGrid's height should be bound by parent".
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(iconGridHeight),
        rows = GridCells.Fixed(IconRows),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        items(icons) { (name, imageVector) ->
            val isSelected = selectedIcon == name
            Card(
                modifier = Modifier.size(56.dp),
                onClick = { onIconSelected(name) },
                elevation = androidx.compose.material3.CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 4.dp else 1.dp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// `ColorPicker` e `isLightColor` viven en ColorPicker.kt y HabitColors.kt.
