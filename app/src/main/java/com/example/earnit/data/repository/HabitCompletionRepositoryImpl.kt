package com.example.earnit.data.repository

import com.example.earnit.core.util.toEpochMillis
import com.example.earnit.core.util.toLocalDate
import com.example.earnit.data.local.dao.HabitCompletionDao
import com.example.earnit.data.local.entity.HabitCompletionEntity
import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.repository.HabitCompletionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class HabitCompletionRepositoryImpl @javax.inject.Inject constructor(
    private val completionDao: HabitCompletionDao
) : HabitCompletionRepository {
    override suspend fun upsert(completion: HabitCompletion) {
        val entity = toEntity(completion)
        completionDao.upsert(entity)
    }

    override fun getForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return completionDao.getForHabit(habitId).map { entities ->
            entities.map { toDomain(it) }
        }
    }

    override fun getForDate(date: LocalDate): Flow<List<HabitCompletion>> {
        return completionDao.getForDate(date.toEpochMillis()).map { entities ->
            entities.map { toDomain(it) }
        }
    }

    override suspend fun getForHabitAndDate(habitId: Long, date: LocalDate): HabitCompletion? {
        val entity = completionDao.getForHabitAndDate(habitId, date.toEpochMillis())
        return entity?.let { toDomain(it) }
    }

    override suspend fun delete(habitId: Long, date: LocalDate) {
        completionDao.delete(habitId, date.toEpochMillis())
    }

    private fun toEntity(completion: HabitCompletion): HabitCompletionEntity {
        return HabitCompletionEntity(
            habitId = completion.habitId,
            date = completion.date.toEpochMillis(),
            completed = completion.completed,
            value = completion.value
        )
    }

    private fun toDomain(entity: HabitCompletionEntity): HabitCompletion {
        return HabitCompletion(
            habitId = entity.habitId,
            date = entity.date.toLocalDate(),
            completed = entity.completed,
            value = entity.value
        )
    }
}