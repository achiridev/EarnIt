package com.example.earnit.domain.model

import java.time.LocalDate

enum class Frequency {
    DAILY
}

enum class HabitStatus {
    ACTIVE,
    ARCHIVED
}

data class Habit(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val icon: String,
    val color: Int,
    val frequency: Frequency = Frequency.DAILY,
    val createdAt: LocalDate = LocalDate.now(),
    val status: HabitStatus = HabitStatus.ACTIVE
) {
    fun copyWith(
        name: String? = null,
        description: String? = null,
        icon: String? = null,
        color: Int? = null,
        frequency: Frequency? = null,
        status: HabitStatus? = null
    ): Habit = copy(
        name = name ?: this.name,
        description = description ?: this.description,
        icon = icon ?: this.icon,
        color = color ?: this.color,
        frequency = frequency ?: this.frequency,
        status = status ?: this.status
    )
}

data class HabitCompletion(
    val habitId: Long,
    val date: LocalDate,
    val completed: Boolean,
    val value: Double? = null
)