package com.example.earnit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.earnit.core.util.DateProvider
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.repository.HabitCompletionRepository
import com.example.earnit.domain.repository.HabitRepository
import com.example.earnit.domain.usecase.ArchiveHabitUseCase
import com.example.earnit.domain.usecase.GetHabitHistoryUseCase
import com.example.earnit.domain.usecase.ToggleCompletionUseCase
import com.example.earnit.feature.habit.HabitDetailScreen
import com.example.earnit.feature.habit.HabitDetailViewModel
import com.example.earnit.ui.theme.EarnItTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Regression test for BUG-001: the "Volver" arrow of `HabitDetailScreen` had an empty
 * `onClick` because the screen had no navigation callback at all, so the only way out
 * was the system back gesture.
 */
@RunWith(AndroidJUnit4::class)
class HabitDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backArrow_notifiesOnBack() {
        val habit = sampleHabit()
        val viewModel = HabitDetailViewModel(
            toggleCompletionUseCase = ToggleCompletionUseCase(
                FakeHabitCompletionRepository(),
                FakeDateProvider()
            ),
            getHabitHistoryUseCase = GetHabitHistoryUseCase(FakeHabitCompletionRepository()),
            archiveHabitUseCase = ArchiveHabitUseCase(SingleHabitRepository(habit))
        )
        viewModel.initialize(habit, LocalDate.now())

        var backCalls = 0
        composeRule.setContent {
            EarnItTheme {
                HabitDetailScreen(
                    viewModel = viewModel,
                    habitId = habit.id,
                    habitRepository = null,
                    onBack = { backCalls++ },
                    onEdit = {},
                    onArchive = {},
                    onToggleCompletion = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Volver").performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals("onBack no se invocó", 1, backCalls) }
    }

    /**
     * The screen reads the habit from the repository by id, so a detail screen restored
     * without `initialize()` is not empty (BUG-004).
     */
    @Test
    fun habitIsLoadedFromTheRepositoryById() {
        val habit = sampleHabit()
        val viewModel = HabitDetailViewModel(
            toggleCompletionUseCase = ToggleCompletionUseCase(
                FakeHabitCompletionRepository(),
                FakeDateProvider()
            ),
            getHabitHistoryUseCase = GetHabitHistoryUseCase(FakeHabitCompletionRepository()),
            archiveHabitUseCase = ArchiveHabitUseCase(SingleHabitRepository(habit))
        )

        composeRule.setContent {
            EarnItTheme {
                HabitDetailScreen(
                    viewModel = viewModel,
                    habitId = habit.id,
                    habitRepository = SingleHabitRepository(habit),
                    onBack = {},
                    onEdit = {},
                    onArchive = {},
                    onToggleCompletion = {}
                )
            }
        }
        composeRule.waitForIdle()

        // El nombre se dibuja dos veces (barra superior y tarjeta), así que se
        // comprueba la primera aparición. La descripción solo existe dentro de la
        // tarjeta: si el hábito no se hubiera cargado, ese nodo no existiría.
        composeRule.onAllNodesWithText(habit.name).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(habit.description!!).assertIsDisplayed()
    }

    private fun sampleHabit() = Habit(
        id = 7L,
        name = "Leer libro",
        description = "30 minutos",
        icon = "book",
        color = 0xFF6650A4.toInt()
    )
}

private class FakeHabitCompletionRepository : HabitCompletionRepository {
    override suspend fun upsert(completion: HabitCompletion) = Unit

    override fun getForHabit(habitId: Long): Flow<List<HabitCompletion>> = flowOf(emptyList())

    override fun getForDate(date: LocalDate): Flow<List<HabitCompletion>> = flowOf(emptyList())

    override suspend fun getForHabitAndDate(habitId: Long, date: LocalDate): HabitCompletion? = null

    override suspend fun delete(habitId: Long, date: LocalDate) = Unit
}

/** Repository that resolves a single known habit, used to prove the id-based load. */
private class SingleHabitRepository(private val habit: Habit) : HabitRepository {
    override suspend fun insert(habit: Habit): Long = error("no usado")
    override suspend fun update(habit: Habit) = error("no usado")
    override suspend fun delete(habitId: Long) = error("no usado")
    override suspend fun getById(habitId: Long): Habit? =
        if (habitId == this.habit.id) this.habit else null

    override fun getActiveHabitsForDate(date: LocalDate): Flow<List<Habit>> = flowOf(listOf(habit))
    override fun getAllHabits(): Flow<List<Habit>> = flowOf(listOf(habit))
}

private class FakeDateProvider : DateProvider {
    override fun today(): LocalDate = LocalDate.now()
}