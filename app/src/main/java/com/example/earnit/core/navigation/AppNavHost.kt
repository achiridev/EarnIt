package com.example.earnit.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.earnit.domain.repository.HabitCompletionRepository
import com.example.earnit.domain.repository.HabitRepository
import com.example.earnit.domain.usecase.ToggleCompletionUseCase
import com.example.earnit.feature.habit.HabitDetailScreen
import com.example.earnit.feature.habit.HabitDetailViewModel
import com.example.earnit.feature.habit.HabitFormScreen
import com.example.earnit.feature.habit.HabitFormViewModel
import com.example.earnit.feature.home.HomeScreen
import com.example.earnit.feature.home.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel,
    habitFormViewModel: HabitFormViewModel,
    habitDetailViewModel: HabitDetailViewModel,
    habitRepository: HabitRepository,
    completionRepository: HabitCompletionRepository,
    toggleCompletionUseCase: ToggleCompletionUseCase
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(navController, "home", modifier = modifier) {
        composable("home") {
            HomeScreen(
                habitsFlow = homeViewModel.habits,
                completionRepository = completionRepository,
                onAddHabit = {
                    habitFormViewModel.clear()
                    navController.navigate("add_habit")
                },
                onHabitClick = { habit, date ->
                    habitDetailViewModel.initialize(habit, date)
                    navController.navigate("habit_detail/${habit.id}")
                },
                onToggleCompletion = { habitId, date ->
                    scope.launch { toggleCompletionUseCase(habitId, date) }
                }
            )
        }
        composable(
            route = "add_habit",
            arguments = listOf()
        ) {
            HabitFormScreen(
                viewModel = habitFormViewModel,
                habitRepository = habitRepository,
                onSave = { navController.popBackStack() },
                onCancel = {
                    habitFormViewModel.clear()
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "habit_detail/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
            HabitDetailScreen(
                viewModel = habitDetailViewModel,
                habitId = habitId,
                onEdit = { habit ->
                    habitFormViewModel.loadHabit(habit)
                    navController.navigate("edit_habit/${habit.id}")
                },
                onArchive = {
                    scope.launch { habitDetailViewModel.archive() }
                    navController.popBackStack()
                },
                onToggleCompletion = { date ->
                    scope.launch { habitDetailViewModel.toggleCompletion() }
                }
            )
        }
        composable(
            route = "edit_habit/{habitId}",
            arguments = listOf(navArgument("habitId") { type = NavType.LongType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
            HabitFormScreen(
                viewModel = habitFormViewModel,
                habitId = habitId,
                habitRepository = habitRepository,
                onSave = { navController.popBackStack() },
                onCancel = {
                    habitFormViewModel.clear()
                    navController.popBackStack()
                }
            )
        }
    }
}
