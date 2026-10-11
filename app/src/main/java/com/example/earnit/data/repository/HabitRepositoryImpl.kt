package com.example.earnit.data.repository

import com.example.earnit.data.local.dao.HabitDao
import com.example.earnit.data.local.entity.HabitEntity
import com.example.earnit.domain.model.Habit
import com.example.earnit.domain.model.Frequency
import com.example.earnit.domain.model.HabitStatus
import com.example.earnit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class HabitRepositoryImpl @javax.inject.Inject constructor(
    private val habitDao: HabitDao
) : HabitRepository {
    override suspend fun insert(habit: Habit): Long {
        val entity = toEntity(habit)
        return habitDao.insert(entity)
    }

    override suspend fun update(habit: Habit) {
        val entity = toEntity(habit)
        habitDao.update(entity)
    }

    override suspend fun delete(habitId: Long) {
        habitDao.delete(habitId)
    }

    override suspend fun getById(habitId: Long): Habit? {
        val entity = habitDao.getById(habitId)
        return entity?.let { toDomain(it) }
    }

    override fun getActiveHabitsForDate(date: LocalDate): Flow<List<Habit>> {
        return habitDao.getActiveHabits().map { entities ->
            entities.map { toDomain(it) }
        }
    }

    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { toDomain(it) }
        }
    }

    private fun toEntity(habit: Habit): HabitEntity {
        return HabitEntity(
            id = habit.id,
            name = habit.name,
            description = habit.description,
            icon = habit.icon,
            color = habit.color,
            frequency = habit.frequency,
            createdAt = habit.createdAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            status = habit.status
        )
    }

    private fun toDomain(entity: HabitEntity): Habit {
        return Habit(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            icon = entity.icon,
            color = entity.color,
            frequency = entity.frequency,
            createdAt = LocalDate.ofEpochDay(entity.createdAt / 86400000),
            status = entity.status
        )
    }
}