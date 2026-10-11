package com.example.earnit.domain.repository

import com.example.earnit.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitCompletionRepository {
    suspend fun upsert(completion: HabitCompletion)
    fun getForHabit(habitId: Long): Flow<List<HabitCompletion>>
    fun getForDate(date: java.time.LocalDate): Flow<List<HabitCompletion>>
    suspend fun getForHabitAndDate(habitId: Long, date: java.time.LocalDate): HabitCompletion?
    suspend fun delete(habitId: Long, date: java.time.LocalDate)
}