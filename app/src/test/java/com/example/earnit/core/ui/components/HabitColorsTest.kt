package com.example.earnit.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas locales de la matemática de color de los hábitos.
 *
 * Son deliberadamente locales (no instrumentadas) porque `HabitColors.kt` es
 * Kotlin puro: así se verifican de verdad en `./gradlew test`, sin dispositivo.
 */
class HabitColorsTest {

    @Test
    fun hsv_roundTripReproducesTheOriginalColor() {
        val samples = PastelColors + VividColors + listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())

        samples.forEach { original ->
            val hsv = original.toHsv()
            assertEquals("IdHSV de ${original.toHexString()}", original, hsvToColorInt(hsv[0], hsv[1], hsv[2]))
        }
    }

    @Test
    fun toHsv_reportsTheExpectedComponents() {
        val hsv = 0xFF0000FF.toInt().toHsv() // azul puro
        assertEquals(240f, hsv[0], 0.01f) // matiz
        assertEquals(1f, hsv[1], 0.001f) // saturación
        assertEquals(1f, hsv[2], 0.001f) // valor
    }

    @Test
    fun hsvToColorInt_alwaysReturnsAnOpaqueColorInRange() {
        // Fuera de rango: debe recortarse, nunca lanzar.
        listOf(
            Triple(-40f, 1.5f, 2f),
            Triple(400f, -1f, 0f)
        ).forEach { (hue, saturation, value) ->
            val color = hsvToColorInt(hue, saturation, value)
            assertEquals("El alfa debe ser opaco", 0xFF, (color ushr 24) and 0xFF)
        }
    }

    @Test
    fun accentColor_darkensPastelsUntilTheyAreLegible() {
        PastelColors.forEach { pastel ->
            val accent = pastel.accentColor()
            assertNotEquals("El pastel ${pastel.toHexString()} debería oscurecerse", pastel, accent)
            assertTrue(
                "El acento ${accent.toHexString()} debería ser más oscuro que ${pastel.toHexString()}",
                (accent shr 16 and 0xFF) + (accent shr 8 and 0xFF) + (accent and 0xFF) <
                    (pastel shr 16 and 0xFF) + (pastel shr 8 and 0xFF) + (pastel and 0xFF)
            )
        }
    }

    @Test
    fun accentColor_keepsColorsThatAreAlreadyDarkEnough() {
        // Los colores vivos son legibles: ningún hábito existente debe cambiar de aspecto.
        VividColors.forEach { vivid ->
            assertEquals("El color ${vivid.toHexString()} no debería cambiar", vivid, vivid.accentColor())
        }
        assertEquals(DEFAULT_HABIT_COLOR, DEFAULT_HABIT_COLOR.accentColor())
    }

    @Test
    fun isLightColor_agreesWithTheLuminanceThreshold() {
        assertTrue(isLightColor(0xFFFFFFFF.toInt())) // blanco
        assertTrue(isLightColor(0xFFF8BBD0.toInt())) // pastel rosa
        assertTrue(!isLightColor(0xFF000000.toInt())) // negro
        assertTrue(!isLightColor(0xFF6650A4.toInt())) // Purple40
    }

    @Test
    fun toHexString_formatsSixDigits() {
        assertEquals("#6650A4", DEFAULT_HABIT_COLOR.toHexString())
        assertEquals("#000000", 0xFF000000.toInt().toHexString())
        assertEquals("#FFFFFF", 0xFFFFFFFF.toInt().toHexString())
    }

    @Test
    fun palettes_haveNoRepeatedColors() {
        val repeated = (PastelColors + VividColors).groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue("Colores repetidos: ${repeated.keys.map { it.toHexString() }}", repeated.isEmpty())
    }
}