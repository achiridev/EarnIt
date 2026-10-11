package com.example.earnit.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.usecase.ArchiveHabitUseCase
import com.example.earnit.domain.usecase.GetHabitHistoryUseCase
import com.example.earnit.domain.usecase.ToggleCompletionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HabitDetailViewModel @javax.inject.Inject constructor(
    private val toggleCompletionUseCase: ToggleCompletionUseCase,
    private val getHabitHistoryUseCase: GetHabitHistoryUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase
) : ViewModel() {

    /**
     * El hábito visible es estado observable: si fuera un `var` normal, Compose no se
     * enteraría de los cambios y la pantalla quedaría vacía al restaurarse sin
     * `initialize()` (BUG-004).
     */
    private val _habit = MutableStateFlow<Habit?>(null)
    private var _date: java.time.LocalDate? = null

    private val _history = MutableStateFlow<List<HabitCompletion>>(emptyList())
    val habit: StateFlow<Habit?> = _habit
    val history: StateFlow<List<HabitCompletion>> = _history

    /** Trabajo del colector de historial en curso, para no acumular ninguno (BUG-003). */
    private var historyJob: Job? = null

    fun initialize(habit: Habit, date: java.time.LocalDate) {
        _habit.value = habit
        _date = date
        observeHistory(habit.id)
    }

    private fun observeHistory(habitId: Long) {
        // El colector anterior se cancela siempre: el ViewModel se comparte entre
        // todos los destinos de detalle, así que sin esto cada visita al detalle de
        // un hábito distinto dejaba un colector vivo escribiendo en el mismo `_history`.
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            getHabitHistoryUseCase(habitId).collect { completions ->
                _history.value = completions
            }
        }
    }

    suspend fun toggleCompletion() {
        val habit = _habit.value ?: return
        val date = _date ?: return
        toggleCompletionUseCase(habit.id, date)
    }

    suspend fun archive() {
        val habit = _habit.value ?: return
        archiveHabitUseCase(habit.id)
    }

    fun isCompletedForDate(date: java.time.LocalDate): Boolean {
        return _history.value.firstOrNull { it.date == date }?.completed == true
    }
}
