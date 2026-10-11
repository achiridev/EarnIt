package com.example.earnit.domain.usecase

import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.repository.HabitRepository

class UpdateHabitUseCase @javax.inject.Inject constructor(
    private val habitRepository: HabitRepository
) {
    operator suspend fun invoke(habit: Habit) {
        habitRepository.update(habit)
    }
}