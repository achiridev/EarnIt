package com.example.earnit.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.usecase.ArchiveHabitUseCase
import com.example.earnit.domain.usecase.GetHabitHistoryUseCase
import com.example.earnit.domain.usecase.ToggleCompletionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HabitDetailViewModel @javax.inject.Inject constructor(
    private val toggleCompletionUseCase: ToggleCompletionUseCase,
    private val getHabitHistoryUseCase: GetHabitHistoryUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase
) : ViewModel() {

    private var _habit: Habit? = null
    private var _date: java.time.LocalDate? = null

    private val _history = MutableStateFlow<List<HabitCompletion>>(emptyList())
    val history: StateFlow<List<HabitCompletion>> = _history

    fun initialize(habit: Habit, date: java.time.LocalDate) {
        _habit = habit
        _date = date
        observeHistory(habit.id)
    }

    private fun observeHistory(habitId: Long) {
        viewModelScope.launch {
            getHabitHistoryUseCase(habitId).collect { completions ->
                _history.value = completions
            }
        }
    }

    fun getHabit(): Habit? = _habit

    suspend fun toggleCompletion() {
        val habit = _habit ?: return
        val date = _date ?: return
        toggleCompletionUseCase(habit.id, date)
    }

    suspend fun archive() {
        val habit = _habit ?: return
        archiveHabitUseCase(habit.id)
    }

    fun isCompletedForDate(date: java.time.LocalDate): Boolean {
        return _history.value.firstOrNull { it.date == date }?.completed == true
    }
}
