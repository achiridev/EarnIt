package com.example.earnit.domain.usecase

import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.repository.HabitCompletionRepository
import kotlinx.coroutines.flow.Flow

class GetHabitHistoryUseCase @javax.inject.Inject constructor(
    private val completionRepository: HabitCompletionRepository
) {
    operator fun invoke(habitId: Long): Flow<List<HabitCompletion>> {
        return completionRepository.getForHabit(habitId)
    }
}