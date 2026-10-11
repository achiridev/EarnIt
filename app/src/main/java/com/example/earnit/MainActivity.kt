package com.example.earnit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.earnit.core.di.AppContainer
import com.example.earnit.core.di.HabitDetailViewModelFactory
import com.example.earnit.core.di.HabitFormViewModelFactory
import com.example.earnit.core.di.HomeViewModelFactory
import com.example.earnit.core.navigation.AppNavHost
import com.example.earnit.feature.habit.HabitDetailViewModel
import com.example.earnit.feature.habit.HabitFormViewModel
import com.example.earnit.feature.home.HomeViewModel
import com.example.earnit.ui.theme.EarnItTheme

class MainActivity : ComponentActivity() {
    private val appContainer: AppContainer by lazy {
        (application as EarnItApplication).appContainer
    }

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(appContainer.getTodaysHabitsUseCase)
    }
    private val habitFormViewModel: HabitFormViewModel by viewModels {
        HabitFormViewModelFactory(appContainer.createHabitUseCase, appContainer.updateHabitUseCase)
    }
    private val habitDetailViewModel: HabitDetailViewModel by viewModels {
        HabitDetailViewModelFactory(
            appContainer.toggleCompletionUseCase,
            appContainer.getHabitHistoryUseCase,
            appContainer.archiveHabitUseCase
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EarnItTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        homeViewModel = homeViewModel,
                        habitFormViewModel = habitFormViewModel,
                        habitDetailViewModel = habitDetailViewModel,
                        habitRepository = appContainer.habitRepository,
                        completionRepository = appContainer.habitCompletionRepository,
                        toggleCompletionUseCase = appContainer.toggleCompletionUseCase
                    )
                }
            }
        }
    }
}
