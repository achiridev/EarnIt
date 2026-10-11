package com.example.earnit.core.util

import java.time.LocalDate
import java.time.ZoneId

class RealDateProvider : DateProvider {
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}