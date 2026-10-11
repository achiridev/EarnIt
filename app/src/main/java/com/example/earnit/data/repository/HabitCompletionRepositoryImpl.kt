package com.example.earnit.data.repository

import com.example.earnit.data.local.dao.HabitCompletionDao
import com.example.earnit.data.local.entity.HabitCompletionEntity
import com.example.earnit.domain.model.HabitCompletion
import com.example.earnit.domain.repository.HabitCompletionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

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
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return completionDao.getForDate(dateMillis).map { entities ->
            entities.map { toDomain(it) }
        }
    }

    override suspend fun getForHabitAndDate(habitId: Long, date: LocalDate): HabitCompletion? {
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val entity = completionDao.getForHabitAndDate(habitId, dateMillis)
        return entity?.let { toDomain(it) }
    }

    override suspend fun delete(habitId: Long, date: LocalDate) {
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        completionDao.delete(habitId, dateMillis)
    }

    private fun toEntity(completion: HabitCompletion): HabitCompletionEntity {
        val dateMillis = completion.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return HabitCompletionEntity(
            habitId = completion.habitId,
            date = dateMillis,
            completed = completion.completed,
            value = completion.value
        )
    }

    private fun toDomain(entity: HabitCompletionEntity): HabitCompletion {
        return HabitCompletion(
            habitId = entity.habitId,
            date = LocalDate.ofEpochDay(entity.date / 86400000),
            completed = entity.completed,
            value = entity.value
        )
    }
}