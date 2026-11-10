package com.example.earnit.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earnit.core.util.DateProvider
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.usecase.GetCompletionsForDateUseCase
import com.example.earnit.domain.usecase.GetTodaysHabitsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Estado de la pantalla principal: el día seleccionado en el calendario manda
 * sobre las completiones que se muestran y sobre la fecha con la que se marca.
 *
 * `GetTodaysHabitsUseCase` devuelve los hábitos activos (la frecuencia es solo
 * `DAILY`), así que aquí se filtra por fecha de creación: un hábito no debe
 * aparecer como pendiente en un día anterior a su existencia.
 */
class HomeViewModel @javax.inject.Inject constructor(
    getTodaysHabitsUseCase: GetTodaysHabitsUseCase,
    getCompletionsForDateUseCase: GetCompletionsForDateUseCase,
    private val dateProvider: DateProvider
) : ViewModel() {

    private val _today = MutableStateFlow(dateProvider.today())
    val today: StateFlow<LocalDate> = _today

    private val _selectedDate = MutableStateFlow(dateProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val activeHabits = getTodaysHabitsUseCase()

    val habits: StateFlow<List<Habit>> =
        combine(activeHabits, _selectedDate) { habits, date ->
            habits.filter { !it.createdAt.isAfter(date) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val completions: StateFlow<List<HabitCompletion>> =
        _selectedDate
            .flatMapLatest { date -> getCompletionsForDateUseCase(date) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Selecciona un día. Las fechas futuras no se pueden marcar. */
    fun selectDate(date: LocalDate) {
        if (date.isAfter(_today.value)) return
        _selectedDate.value = date
    }

    fun goToToday() {
        _selectedDate.value = _today.value
    }

    /**
     * Se llama al volver a la pantalla y al cruzar medianoche. Si el usuario
     * estaba viendo "hoy", la selección avanza con el día; si estaba consultando
     * un día pasado, se respeta su elección.
     */
    fun refreshToday() {
        val previousToday = _today.value
        val currentToday = dateProvider.today()
        _today.value = currentToday
        if (_selectedDate.value == previousToday) {
            _selectedDate.value = currentToday
        }
    }
}
