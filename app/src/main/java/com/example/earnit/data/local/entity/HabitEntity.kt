package com.example.earnit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.earnit.domain.model.Frequency
import com.example.earnit.domain.model.HabitStatus

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String?,
    val icon: String,
    val color: Int,
    val frequency: Frequency,
    val createdAt: Long, // Unix timestamp (milliseconds)
    val status: HabitStatus
)