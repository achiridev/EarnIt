package com.example.earnit.data.local.database

import androidx.room.TypeConverter
import com.example.earnit.domain.model.Frequency
import com.example.earnit.domain.model.HabitStatus

class Converters {
    @TypeConverter
    fun fromFrequency(value: String): Frequency {
        return Frequency.valueOf(value)
    }

    @TypeConverter
    fun toFrequency(value: Frequency): String {
        return value.name
    }

    @TypeConverter
    fun fromHabitStatus(value: String): HabitStatus {
        return HabitStatus.valueOf(value)
    }

    @TypeConverter
    fun toHabitStatus(value: HabitStatus): String {
        return value.name
    }
}