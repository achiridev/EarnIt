package com.example.earnit

import com.example.earnit.domain.model.Frequency
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.HabitStatus
import com.example.earnit.domain.repository.HabitRepository
import com.example.earnit.domain.usecase.CreateHabitUseCase
import com.example.earnit.domain.usecase.UpdateHabitUseCase
import com.example.earnit.feature.habit.HabitFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Regression test for BUG-002: `save()` rebuilt the `Habit` without copying `createdAt`
 * nor `status`, so both fell back to the constructor defaults (`LocalDate.now()` and
 * `HabitStatus.ACTIVE`). Because `HabitDao.update()` overwrites the whole row, merely
 * editing a habit reset its creation date — which also moved it to the top of the list,
 * since `getActiveHabits()` is `ORDER BY createdAt DESC`.
 */
class HabitFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // `isValid` is shared with `viewModelScope`, which needs a main dispatcher.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun save_whenEditing_preservesCreatedAtAndStatus() = runTest(dispatcher) {
        val repository = RecordingHabitRepository()
        val viewModel = HabitFormViewModel(
            createHabitUseCase = CreateHabitUseCase(repository),
            updateHabitUseCase = UpdateHabitUseCase(repository)
        )
        val originalDate = LocalDate.of(2020, 1, 1)
        viewModel.loadHabit(
            Habit(
                id = 42L,
                name = "Leer libro",
                description = "30 min",
                icon = "book",
                color = 0xFF6650A4.toInt(),
                frequency = Frequency.DAILY,
                createdAt = originalDate,
                status = HabitStatus.ARCHIVED
            )
        )

        viewModel.updateName("Leer un libro")
        viewModel.save()

        val saved = repository.updated
        assertEquals("no se llamó a update", 1, repository.updates)
        assertEquals(0, repository.inserts)
        assertEquals(originalDate, saved!!.createdAt)
        assertEquals(HabitStatus.ARCHIVED, saved.status)
        assertEquals("Leer un libro", saved.name)
        assertEquals(42L, saved.id)
    }

    @Test
    fun save_whenCreating_storesTheHabitAsActive() = runTest(dispatcher) {
        val repository = RecordingHabitRepository()
        val viewModel = HabitFormViewModel(
            createHabitUseCase = CreateHabitUseCase(repository),
            updateHabitUseCase = UpdateHabitUseCase(repository)
        )
        viewModel.updateName(" Leer libro  ")

        viewModel.save()

        val saved = repository.inserted
        assertEquals(1, repository.inserts)
        assertEquals(0, repository.updates)
        assertEquals("Leer libro", saved!!.name)
        assertEquals(HabitStatus.ACTIVE, saved.status)
        assertEquals(0L, saved.id)
    }

    @Test
    fun isValid_tracksTheNameIgnoringSurroundingSpaces() = runTest(dispatcher) {
        val repository = RecordingHabitRepository()
        val viewModel = HabitFormViewModel(
            createHabitUseCase = CreateHabitUseCase(repository),
            updateHabitUseCase = UpdateHabitUseCase(repository)
        )

        assertFalse(viewModel.isValid.value)

        viewModel.updateName("   ")
        advanceUntilIdle()
        assertFalse(viewModel.isValid.value)

        viewModel.updateName("  Leer  ")
        advanceUntilIdle()
        assertTrue(viewModel.isValid.value)

        viewModel.clear()
        advanceUntilIdle()
        assertFalse(viewModel.isValid.value)
    }
}

/** In-memory repository that records what `save()` actually writes. */
private class RecordingHabitRepository : HabitRepository {
    var inserts = 0
        private set
    var updates = 0
        private set
    var inserted: Habit? = null
        private set
    var updated: Habit? = null
        private set

    override suspend fun insert(habit: Habit): Long {
        inserts++
        inserted = habit
        return 1L
    }

    override suspend fun update(habit: Habit) {
        updates++
        updated = habit
    }

    override suspend fun delete(habitId: Long) = Unit

    override suspend fun getById(habitId: Long): Habit? = updated

    override fun getActiveHabitsForDate(date: LocalDate): Flow<List<Habit>> = flowOf(emptyList())

    override fun getAllHabits(): Flow<List<Habit>> = flowOf(emptyList())
}