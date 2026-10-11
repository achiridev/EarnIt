package com.example.earnit.core.util

import java.time.LocalDate

interface DateProvider {
    fun today(): LocalDate
}