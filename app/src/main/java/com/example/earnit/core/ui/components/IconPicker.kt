package com.example.earnit.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        rows = GridCells.Fixed(5),
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

@Composable
fun ColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        0xFF6650A4.toInt(), // Purple
        0xFF006E54.toInt(), // Teal
        0xFF8B5000.toInt(), // Brown
        0xFF7D5260.toInt(), // Pink
        0xFF4A626A.toInt(), // Blue Grey
        0xFF3B4D10.toInt(), // Green
        0xFF8B0000.toInt(), // Red
        0xFF6B5800.toInt(), // Yellow
        0xFF006B7C.toInt(), // Cyan
        0xFF5C2E7C.toInt(), // Deep Purple
        0xFF1A7538.toInt(), // Green
        0xFF7C4D00.toInt(), // Orange
    )

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { color ->
            val isSelected = selectedColor == color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Card(
                    modifier = Modifier.size(36.dp),
                    onClick = { onColorSelected(color) },
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 4.dp else 1.dp
                    ),
                    shape = RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(color)
                    )
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = if (isLightColor(color)) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isLightColor(color: Int): Boolean {
    val r = ((color shr 16) and 0xFF) / 255f
    val g = ((color shr 8) and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    return luminance > 0.5
}