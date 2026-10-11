package com.example.earnit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.repository.HabitRepository
import com.example.earnit.domain.usecase.CreateHabitUseCase
import com.example.earnit.domain.usecase.UpdateHabitUseCase
import com.example.earnit.feature.habit.HabitFormScreen
import com.example.earnit.feature.habit.HabitFormViewModel
import com.example.earnit.ui.theme.EarnItTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Regression test for the crash:
 * "java.lang.IllegalArgumentException: LazyHorizontalGrid's height should be
 * bound by parent" — IconPicker's grid previously had no fixed height while
 * being hosted inside a Column with verticalScroll, so the first frame of the
 * habit form crashed the app.
 */
@RunWith(AndroidJUnit4::class)
class HabitFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun habitFormScreen_composesIconPickerWithoutCrashing() {
        val fakeRepository = FakeHabitRepository()
        val viewModel = HabitFormViewModel(
            createHabitUseCase = CreateHabitUseCase(fakeRepository),
            updateHabitUseCase = UpdateHabitUseCase(fakeRepository)
        )

        composeRule.setContent {
            EarnItTheme {
                HabitFormScreen(
                    viewModel = viewModel,
                    habitId = null,
                    habitRepository = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }

        // setContent above throws if the IconPicker grid has an unbounded height.
        // Assert part of the form actually rendered.
        composeRule.onNodeWithText("Nombre del hábito").assertIsDisplayed()
    }

    /**
     * Regression test for BUG-014: the "Guardar" button stayed permanently disabled,
     * so a new habit could never be created.
     *
     * The button read `viewModel.isValid()`, a plain read of `MutableStateFlow.value`,
     * from inside the `TopAppBar`'s `actions` lambda. That is not a Compose snapshot
     * read, so the recomposition scope holding it was never invalidated: the button
     * kept the value computed on the very first frame (empty name → disabled).
     */
    @Test
    fun saveButton_enablesOnceTheNameIsNotBlank() {
        val viewModel = HabitFormViewModel(
            createHabitUseCase = CreateHabitUseCase(FakeHabitRepository()),
            updateHabitUseCase = UpdateHabitUseCase(FakeHabitRepository())
        )

        composeRule.setContent {
            EarnItTheme {
                HabitFormScreen(
                    viewModel = viewModel,
                    habitId = null,
                    habitRepository = null,
                    onSave = {},
                    onCancel = {}
                )
            }
        }

        // Precondition: with an empty name there is nothing to save.
        composeRule.onNodeWithText("Guardar").assertIsNotEnabled()

        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Leer libro")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Guardar").assertIsEnabled()
    }

    @Test
    fun saveButton_savesAndNotifiesWhenPressed() {
        val repository = FakeHabitRepository()
        val viewModel = HabitFormViewModel(
            createHabitUseCase = CreateHabitUseCase(repository),
            updateHabitUseCase = UpdateHabitUseCase(repository)
        )
        var saveCalls = 0

        composeRule.setContent {
            EarnItTheme {
                HabitFormScreen(
                    viewModel = viewModel,
                    habitId = null,
                    habitRepository = null,
                    onSave = { saveCalls++ },
                    onCancel = {}
                )
            }
        }

        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Leer libro")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertTrue("onSave no se invocó", saveCalls == 1) }
    }
}

/** Minimal in-memory implementation; enough to construct the use cases. */
private class FakeHabitRepository : HabitRepository {
    private val habits = mutableListOf<Habit>()

    override suspend fun insert(habit: Habit): Long {
        habits.add(habit.copy(id = habits.size.toLong() + 1))
        return habits.size.toLong()
    }

    override suspend fun update(habit: Habit) {
        val index = habits.indexOfFirst { it.id == habit.id }
        if (index >= 0) habits[index] = habit
    }

    override suspend fun delete(habitId: Long) {
        habits.removeAll { it.id == habitId }
    }

    override suspend fun getById(habitId: Long): Habit? =
        habits.firstOrNull { it.id == habitId }

    override fun getActiveHabitsForDate(date: LocalDate): Flow<List<Habit>> =
        flowOf(habits.toList())

    override fun getAllHabits(): Flow<List<Habit>> =
        flowOf(habits.toList())
}