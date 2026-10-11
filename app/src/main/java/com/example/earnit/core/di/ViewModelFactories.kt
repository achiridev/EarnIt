package com.example.earnit.core.di

import androidx.lifecycle.ViewModelProvider
import com.example.earnit.domain.usecase.ArchiveHabitUseCase
import com.example.earnit.domain.usecase.CreateHabitUseCase
import com.example.earnit.domain.usecase.GetHabitHistoryUseCase
import com.example.earnit.domain.usecase.GetTodaysHabitsUseCase
import com.example.earnit.domain.usecase.ToggleCompletionUseCase
import com.example.earnit.domain.usecase.UpdateHabitUseCase
import com.example.earnit.feature.habit.HabitDetailViewModel
import com.example.earnit.feature.habit.HabitFormViewModel
import com.example.earnit.feature.home.HomeViewModel

/**
 * Manual ViewModel factories. The app does not use Hilt, so every factory receives
 * its dependencies straight from [AppContainer].
 */
class HomeViewModelFactory(
    private val getTodaysHabitsUseCase: GetTodaysHabitsUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return HomeViewModel(getTodaysHabitsUseCase) as T
    }
}

class HabitFormViewModelFactory(
    private val createHabitUseCase: CreateHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HabitFormViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return HabitFormViewModel(createHabitUseCase, updateHabitUseCase) as T
    }
}

class HabitDetailViewModelFactory(
    private val toggleCompletionUseCase: ToggleCompletionUseCase,
    private val getHabitHistoryUseCase: GetHabitHistoryUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HabitDetailViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return HabitDetailViewModel(
            toggleCompletionUseCase,
            getHabitHistoryUseCase,
            archiveHabitUseCase
        ) as T
    }
}
