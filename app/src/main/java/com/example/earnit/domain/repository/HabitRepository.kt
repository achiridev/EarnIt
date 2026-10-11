package com.example.earnit.domain.repository

import com.example.earnit.domain.model.Habit
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    suspend fun insert(habit: Habit): Long
    suspend fun update(habit: Habit)
    suspend fun delete(habitId: Long)
    suspend fun getById(habitId: Long): Habit?
    fun getActiveHabitsForDate(date: java.time.LocalDate): Flow<List<Habit>>
    fun getAllHabits(): Flow<List<Habit>>
}