package com.example.earnit.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.usecase.GetTodaysHabitsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel @javax.inject.Inject constructor(
    getTodaysHabitsUseCase: GetTodaysHabitsUseCase
) : ViewModel() {

    val habits: StateFlow<List<Habit>> = getTodaysHabitsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
