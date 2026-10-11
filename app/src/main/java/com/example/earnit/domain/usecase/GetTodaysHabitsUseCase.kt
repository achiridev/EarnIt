package com.example.earnit.domain.usecase

import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetTodaysHabitsUseCase @javax.inject.Inject constructor(
    private val habitRepository: HabitRepository,
    private val dateProvider: com.example.earnit.core.util.DateProvider
) {
    operator fun invoke(): Flow<List<Habit>> {
        val today = dateProvider.today()
        return habitRepository.getActiveHabitsForDate(today)
    }
}