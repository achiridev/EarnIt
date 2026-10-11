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

    /**
     * Los 12 colores "vivos" que el selector ofrecía antes de la paleta actual.
     *
     * Ya no se ofrecen, pero pueden estar guardados en hábitos de usuarios
     * anteriores, así que `accentColor()` no debe alterarlos: si el umbral de
     * luminancia se moviera, todos esos hábitos cambiarían de aspecto.
     */
    private val legacyColors = listOf(
        0xFF6650A4.toInt(), // Purple
        0xFF006E54.toInt(), // Teal
        0xFF8B5000.toInt(), // Brown
        0xFF7D5260.toInt(), // Pink
        0xFF4A626A.toInt(), // Blue Grey
        0xFF3B4D10.toInt(), // Green
        0xFF8B0000.toInt(), // Red
        0xFF6B5800.toInt(), // Yellow
        0xFF006B7C.toInt(), // Cyan
        0xFF5C2E7C.toInt(), // Deep Purple
        0xFF1A7538.toInt(), // Green
        0xFF7C4D00.toInt(), // Orange
    )

    @Test
    fun hsv_roundTripReproducesTheOriginalColor() {
        val samples = PaletteColors + legacyColors + listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())

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
    fun accentColor_darkensThePaletteUntilTheyAreLegible() {
        PaletteColors.forEach { color ->
            val accent = color.accentColor()
            assertNotEquals(
                "El color ${color.toHexString()} debería oscurecerse: si no supera el " +
                    "umbral de 0.45 se usaría como texto sin oscurecer",
                color,
                accent
            )
            assertTrue(
                "El acento ${accent.toHexString()} debería ser más oscuro que ${color.toHexString()}",
                (accent shr 16 and 0xFF) + (accent shr 8 and 0xFF) + (accent and 0xFF) <
                    (color shr 16 and 0xFF) + (color shr 8 and 0xFF) + (color and 0xFF)
            )
        }
    }

    @Test
    fun accentColor_keepsColorsThatAreAlreadyDarkEnough() {
        // Los colores que la app ya usaba son legibles: ningún hábito existente debe cambiar de aspecto.
        legacyColors.forEach { legacy ->
            assertEquals("El color ${legacy.toHexString()} no debería cambiar", legacy, legacy.accentColor())
        }
    }

    @Test
    fun isLightColor_agreesWithTheLuminanceThreshold() {
        assertTrue(isLightColor(0xFFFFFFFF.toInt())) // blanco
        assertTrue(isLightColor(0xFFF8BBD0.toInt())) // rosa claro
        assertTrue(!isLightColor(0xFF000000.toInt())) // negro
        assertTrue(!isLightColor(0xFF6650A4.toInt())) // Purple40
    }

    @Test
    fun toHexString_formatsSixDigits() {
        assertEquals("#FF6E61", DEFAULT_HABIT_COLOR.toHexString())
        assertEquals("#000000", 0xFF000000.toInt().toHexString())
        assertEquals("#FFFFFF", 0xFFFFFFFF.toInt().toHexString())
    }

    @Test
    fun palette_hasThirteenDistinctColors() {
        assertEquals("La paleta debe tener 13 muestras", 13, PaletteColors.size)
        val repeated = PaletteColors.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue("Colores repetidos: ${repeated.keys.map { it.toHexString() }}", repeated.isEmpty())
    }

    @Test
    fun defaultColorIsPartOfThePalette() {
        assertTrue(
            "El color por defecto debe ser una de las 13 muestras del selector",
            DEFAULT_HABIT_COLOR in PaletteColors
        )
    }
}