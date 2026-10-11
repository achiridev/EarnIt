package com.example.earnit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"]
)
data class HabitCompletionEntity(
    val habitId: Long,
    val date: Long, // Unix timestamp (milliseconds) for the date at midnight
    val completed: Boolean,
    val value: Double?
)