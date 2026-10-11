package com.example.earnit.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earnit.core.ui.components.DEFAULT_HABIT_COLOR
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.Frequency
import com.example.earnit.domain.model.HabitStatus
import com.example.earnit.domain.usecase.CreateHabitUseCase
import com.example.earnit.domain.usecase.UpdateHabitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HabitFormViewModel @javax.inject.Inject constructor(
    private val createHabitUseCase: CreateHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase
) : ViewModel() {

    private val _name = MutableStateFlow("")
    private val _description = MutableStateFlow("")
    private val _icon = MutableStateFlow("book")
    private val _color = MutableStateFlow(DEFAULT_HABIT_COLOR) // primera muestra de PaletteColors
    private val _frequency = MutableStateFlow(Frequency.DAILY)
    private val _isEditing = MutableStateFlow(false)
    private val _habitId = MutableStateFlow<Long?>(null)
    private val _createdAt = MutableStateFlow(LocalDate.now())
    private val _status = MutableStateFlow(HabitStatus.ACTIVE)

    val name: StateFlow<String> = _name
    val description: StateFlow<String> = _description
    val icon: StateFlow<String> = _icon
    val color: StateFlow<Int> = _color
    val frequency: StateFlow<Frequency> = _frequency
    val isEditing: StateFlow<Boolean> = _isEditing
    val habitId: StateFlow<Long?> = _habitId

    /**
     * Validez del formulario como estado observable.
     *
     * Debe ser un `StateFlow` y no un método: `HabitFormScreen` lo lee en la lambda
     * `actions` de la `TopAppBar`, y una lectura de `MutableStateFlow.value` allí no
     * es una lectura de estado de Compose, así que ese ámbito de recomposición nunca
     * se invalidaba y el botón "Guardar" se quedaba deshabilitado para siempre al
     * crear un hábito (BUG-014).
     */
    val isValid: StateFlow<Boolean> = _name
        .map { it.trim().isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun loadHabit(habit: Habit) {
        _habitId.value = habit.id
        _name.value = habit.name
        _description.value = habit.description ?: ""
        _icon.value = habit.icon
        _color.value = habit.color
        _frequency.value = habit.frequency
        _isEditing.value = true
        _createdAt.value = habit.createdAt
        _status.value = habit.status
    }

    fun clear() {
        _habitId.value = null
        _name.value = ""
        _description.value = ""
        _icon.value = "book"
        _color.value = DEFAULT_HABIT_COLOR
        _frequency.value = Frequency.DAILY
        _isEditing.value = false
        _createdAt.value = LocalDate.now()
        _status.value = HabitStatus.ACTIVE
    }

    fun updateName(name: String) { _name.value = name }
    fun updateDescription(description: String) { _description.value = description }
    fun updateIcon(icon: String) { _icon.value = icon }
    fun updateColor(color: Int) { _color.value = color }
    fun updateFrequency(frequency: Frequency) { _frequency.value = frequency }

    suspend fun save(): Long {
        val habit = Habit(
            id = _habitId.value ?: 0,
            name = _name.value.trim(),
            description = _description.value.trim().takeIf { it.isNotBlank() },
            icon = _icon.value,
            color = _color.value,
            frequency = _frequency.value,
            // El formulario no edita estos dos campos. Sin pasarlos explícitamente
            // adoptan los valores por defecto del constructor y `@Update` reescribe
            // la fila entera: `createdAt` saltaba a hoy y un hábito archivado
            // volvía a estar ACTIVE (BUG-002).
            createdAt = _createdAt.value,
            status = _status.value
        )

        return if (_isEditing.value) {
            updateHabitUseCase(habit)
            habit.id
        } else {
            createHabitUseCase(habit)
        }
    }
}