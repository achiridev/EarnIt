package com.example.earnit.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.Frequency
import com.example.earnit.domain.usecase.CreateHabitUseCase
import com.example.earnit.domain.usecase.UpdateHabitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HabitFormViewModel @javax.inject.Inject constructor(
    private val createHabitUseCase: CreateHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase
) : ViewModel() {

    private val _name = MutableStateFlow("")
    private val _description = MutableStateFlow("")
    private val _icon = MutableStateFlow("book")
    private val _color = MutableStateFlow(0xFF6650A4.toInt()) // Purple40
    private val _frequency = MutableStateFlow(Frequency.DAILY)
    private val _isEditing = MutableStateFlow(false)
    private val _habitId = MutableStateFlow<Long?>(null)

    val name: StateFlow<String> = _name
    val description: StateFlow<String> = _description
    val icon: StateFlow<String> = _icon
    val color: StateFlow<Int> = _color
    val frequency: StateFlow<Frequency> = _frequency
    val isEditing: StateFlow<Boolean> = _isEditing
    val habitId: StateFlow<Long?> = _habitId

    fun loadHabit(habit: Habit) {
        _habitId.value = habit.id
        _name.value = habit.name
        _description.value = habit.description ?: ""
        _icon.value = habit.icon
        _color.value = habit.color
        _frequency.value = habit.frequency
        _isEditing.value = true
    }

    fun clear() {
        _habitId.value = null
        _name.value = ""
        _description.value = ""
        _icon.value = "book"
        _color.value = 0xFF6650A4.toInt()
        _frequency.value = Frequency.DAILY
        _isEditing.value = false
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
            frequency = _frequency.value
        )

        return if (_isEditing.value) {
            updateHabitUseCase(habit)
            habit.id
        } else {
            createHabitUseCase(habit)
        }
    }

    fun isValid(): Boolean = _name.value.trim().isNotBlank()
}