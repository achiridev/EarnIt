package com.example.earnit.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Pets
import androidx.compose.ui.graphics.vector.ImageVector

fun getIcon(imageVectorName: String): ImageVector {
    return when (imageVectorName) {
        "book" -> Icons.Filled.MenuBook
        "water" -> Icons.Filled.LocalDrink
        "exercise" -> Icons.Filled.FitnessCenter
        "sleep" -> Icons.Filled.Bedtime
        "meditate" -> Icons.Filled.SelfImprovement
        "code" -> Icons.Filled.Code
        "read" -> Icons.Filled.ChromeReaderMode
        "run" -> Icons.Filled.DirectionsRun
        "walk" -> Icons.Filled.DirectionsWalk
        "bike" -> Icons.Filled.DirectionsBike
        "swim" -> Icons.Filled.Pool
        "yoga" -> Icons.Filled.SportsGymnastics
        "study" -> Icons.Filled.School
        "work" -> Icons.Filled.Work
        "music" -> Icons.Filled.MusicNote
        "art" -> Icons.Filled.Palette
        "cook" -> Icons.Filled.Restaurant
        "clean" -> Icons.Filled.CleaningServices
        "plant" -> Icons.Filled.Spa
        "pet" -> Icons.Filled.Pets
        else -> Icons.Filled.MenuBook
    }
}