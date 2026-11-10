package com.example.earnit.feature.home

import com.example.earnit.core.util.DateProvider
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.model.HabitStatus
import com.example.earnit.domain.repository.HabitCompletionRepository
import com.example.earnit.domain.repository.HabitRepository
import com.example.earnit.domain.usecase.GetCompletionsForDateUseCase
import com.example.earnit.domain.usecase.GetTodaysHabitsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 11, 10)
    private val past = LocalDate.of(2026, 11, 5)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectingPastDate_usesThatDaysCompletionsAndHidesHabitsCreatedLater() = runTest(dispatcher) {
        val oldHabit = habit(id = 1, createdAt = LocalDate.of(2026, 11, 1))
        val newHabit = habit(id = 2, createdAt = LocalDate.of(2026, 11, 9))
        val todayCompletion = HabitCompletion(habitId = 1, date = today, completed = true)
        val pastCompletion = HabitCompletion(habitId = 1, date = past, completed = true)
        val viewModel = viewModel(
            habits = listOf(oldHabit, newHabit),
            completions = mapOf(today to listOf(todayCompletion), past to listOf(pastCompletion))
        )

        backgroundScope.launch { viewModel.habits.collect {} }
        backgroundScope.launch { viewModel.completions.collect {} }
        advanceUntilIdle()

        assertEquals(today, viewModel.selectedDate.value)
        assertEquals(listOf(oldHabit, newHabit), viewModel.habits.value)
        assertEquals(listOf(todayCompletion), viewModel.completions.value)

        viewModel.selectDate(past)
        advanceUntilIdle()

        assertEquals(past, viewModel.selectedDate.value)
        assertEquals(listOf(oldHabit), viewModel.habits.value)
        assertEquals(listOf(pastCompletion), viewModel.completions.value)
    }

    @Test
    fun selectingFutureDate_isIgnored() = runTest(dispatcher) {
        val viewModel = viewModel(habits = emptyList(), completions = emptyMap())

        viewModel.selectDate(today.plusDays(1))

        assertEquals(today, viewModel.selectedDate.value)
    }

    @Test
    fun refreshToday_advancesTheSelectionOnlyIfItWasShowingToday() = runTest(dispatcher) {
        val dateProvider = MutableDateProvider(today)
        val viewModel = viewModel(habits = emptyList(), completions = emptyMap(), dateProvider = dateProvider)

        dateProvider.current = today.plusDays(1)
        viewModel.refreshToday()

        assertEquals(today.plusDays(1), viewModel.today.value)
        assertEquals(today.plusDays(1), viewModel.selectedDate.value)

        viewModel.selectDate(past)
        dateProvider.current = today.plusDays(2)
        viewModel.refreshToday()

        assertEquals(today.plusDays(2), viewModel.today.value)
        assertEquals(past, viewModel.selectedDate.value)
    }

    private fun viewModel(
        habits: List<Habit>,
        completions: Map<LocalDate, List<HabitCompletion>>,
        dateProvider: MutableDateProvider = MutableDateProvider(today)
    ): HomeViewModel {
        val habitRepository = FakeHabitRepository(habits)
        val completionRepository = FakeCompletionRepository(completions)
        return HomeViewModel(
            getTodaysHabitsUseCase = GetTodaysHabitsUseCase(habitRepository, dateProvider),
            getCompletionsForDateUseCase = GetCompletionsForDateUseCase(completionRepository),
            dateProvider = dateProvider
        )
    }

    private fun habit(id: Long, createdAt: LocalDate) = Habit(
        id = id,
        name = "Hábito $id",
        icon = "book",
        color = 0xFF6650A4.toInt(),
        createdAt = createdAt
    )
}

private class MutableDateProvider(var current: LocalDate) : DateProvider {
    override fun today(): LocalDate = current
}

private class FakeHabitRepository(private val habits: List<Habit>) : HabitRepository {
    override suspend fun insert(habit: Habit): Long = 0L
    override suspend fun update(habit: Habit) = Unit
    override suspend fun delete(habitId: Long) = Unit
    override suspend fun getById(habitId: Long): Habit? = habits.firstOrNull { it.id == habitId }
    override fun getActiveHabitsForDate(date: LocalDate): Flow<List<Habit>> =
        flowOf(habits.filter { it.status == HabitStatus.ACTIVE })
    override fun getAllHabits(): Flow<List<Habit>> = flowOf(habits)
}

private class FakeCompletionRepository(
    private val completionsByDate: Map<LocalDate, List<HabitCompletion>>
) : HabitCompletionRepository {
    override suspend fun upsert(completion: HabitCompletion) = Unit

    override fun getForHabit(habitId: Long): Flow<List<HabitCompletion>> =
        flowOf(completionsByDate.values.flatten().filter { it.habitId == habitId })

    override fun getForDate(date: LocalDate): Flow<List<HabitCompletion>> =
        flowOf(completionsByDate[date].orEmpty())

    override suspend fun getForHabitAndDate(habitId: Long, date: LocalDate): HabitCompletion? =
        completionsByDate[date].orEmpty().firstOrNull { it.habitId == habitId }

    override suspend fun delete(habitId: Long, date: LocalDate) = Unit
}
