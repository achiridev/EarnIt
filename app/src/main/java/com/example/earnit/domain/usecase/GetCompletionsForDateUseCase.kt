package com.example.earnit.domain.usecase

import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.repository.HabitCompletionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Completa la fuga de capas de la pantalla principal: la UI deja de recibir el
 * repositorio y pide las completiones de un día concreto a través de este caso de uso.
 */
class GetCompletionsForDateUseCase @javax.inject.Inject constructor(
    private val completionRepository: HabitCompletionRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<HabitCompletion>> =
        completionRepository.getForDate(date)
}
