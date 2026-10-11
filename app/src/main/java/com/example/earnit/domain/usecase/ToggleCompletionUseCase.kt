package com.example.earnit.domain.usecase

import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.repository.HabitCompletionRepository
import com.example.earnit.core.util.DateProvider

class ToggleCompletionUseCase @javax.inject.Inject constructor(
    private val completionRepository: HabitCompletionRepository,
    private val dateProvider: DateProvider
) {
    operator suspend fun invoke(habitId: Long, date: java.time.LocalDate? = null): Boolean {
        val targetDate = date ?: dateProvider.today()
        val existing = completionRepository.getForHabitAndDate(habitId, targetDate)
        val newCompleted = existing?.completed != true
        val completion = HabitCompletion(
            habitId = habitId,
            date = targetDate,
            completed = newCompleted
        )
        completionRepository.upsert(completion)
        return newCompleted
    }
}