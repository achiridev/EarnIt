package com.example.earnit.core.di

import android.content.Context
import com.example.earnit.core.util.DateProvider
import com.example.earnit.core.util.RealDateProvider
import com.example.earnit.data.local.database.AppDatabase
import com.example.earnit.data.repository.HabitCompletionRepositoryImpl
import com.example.earnit.data.repository.HabitRepositoryImpl
import com.example.earnit.domain.repository.HabitCompletionRepository
import com.example.earnit.domain.repository.HabitRepository
import com.example.earnit.domain.usecase.ArchiveHabitUseCase
import com.example.earnit.domain.usecase.CreateHabitUseCase
import com.example.earnit.domain.usecase.GetHabitHistoryUseCase
import com.example.earnit.domain.usecase.GetTodaysHabitsUseCase
import com.example.earnit.domain.usecase.ToggleCompletionUseCase
import com.example.earnit.domain.usecase.UpdateHabitUseCase

class AppContainer(private val context: Context) {
    private val database = AppDatabase.getInstance(context)

    // Core
    val dateProvider: DateProvider = RealDateProvider()

    // Data
    val habitRepository: HabitRepository = HabitRepositoryImpl(database.habitDao())
    val habitCompletionRepository: HabitCompletionRepository = HabitCompletionRepositoryImpl(database.habitCompletionDao())

    // Domain - Use Cases
    val getTodaysHabitsUseCase = GetTodaysHabitsUseCase(habitRepository, dateProvider)
    val createHabitUseCase = CreateHabitUseCase(habitRepository)
    val updateHabitUseCase = UpdateHabitUseCase(habitRepository)
    val archiveHabitUseCase = ArchiveHabitUseCase(habitRepository)
    val toggleCompletionUseCase = ToggleCompletionUseCase(habitCompletionRepository, dateProvider)
    val getHabitHistoryUseCase = GetHabitHistoryUseCase(habitCompletionRepository)
}