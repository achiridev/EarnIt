package com.example.earnit.domain.usecase

import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitStatus
import com.example.earnit.domain.repository.HabitRepository

class ArchiveHabitUseCase @javax.inject.Inject constructor(
    private val habitRepository: HabitRepository
) {
    operator suspend fun invoke(habitId: Long) {
        val habit = habitRepository.getById(habitId)
        if (habit != null && habit.status == HabitStatus.ACTIVE) {
            habitRepository.update(habit.copyWith(status = HabitStatus.ARCHIVED))
        }
    }
}