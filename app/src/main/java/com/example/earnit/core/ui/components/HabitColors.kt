package com.example.earnit.core.ui.components

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Paletas y matemática de color de los hábitos.
 *
 * Todo es Kotlin puro (sin Compose) para que sea verificable con pruebas locales.
 * El color de un hábito es un `Int` ARGB empaquetado, tal y como lo guarda
 * `HabitEntity.color`; aquí solo se manipulan sus canales.
 */

/** Color por defecto de un hábito nuevo. Coincide con `Purple40` del tema. */
val DEFAULT_HABIT_COLOR: Int = 0xFF6650A4.toInt()

/** Paleta suave: colores claros y desaturados. */
val PastelColors: List<Int> = listOf(
    0xFFF8BBD0.toInt(), // Rosa
    0xFFF48FB1.toInt(), // Rosa intenso
    0xFFFFD3B6.toInt(), // Melocotón
    0xFFFFAB91.toInt(), // Salmón
    0xFFFFE0B2.toInt(), // Naranja suave
    0xFFFFF9C4.toInt(), // Amarillo
    0xFFFFF59D.toInt(), // Amarillo intenso
    0xFFE6EE9C.toInt(), // Lima
    0xFFC5E1A5.toInt(), // Verde lima
    0xFFB9F6CA.toInt(), // Menta
    0xFFC8E6C9.toInt(), // Salvia
    0xFFA5D6A7.toInt(), // Verde suave
    0xFFB2DFDB.toInt(), // Agua
    0xFF80CBC4.toInt(), // Turquesa
    0xFFB2EBF2.toInt(), // Turquesa claro
    0xFFB3E5FC.toInt(), // Cielo
    0xFFBBDEFB.toInt(), // Azul
    0xFFC5CAE9.toInt(), // Lavanda
    0xFFD1C4E9.toInt(), // Periwinkle
    0xFFE1BEE7.toInt(), // Lila
    0xFFF3E5F5.toInt(), // Magenta muy claro
)

/** Paleta original: los colores vivos que el selector ofrecía antes de los pasteles. */
val VividColors: List<Int> = listOf(
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

/**
 * Luminancia relativa con los coeficientes de Rec. 709, igual que usa el tema
 * para decidir el color de contenido sobre una superficie.
 */
private fun relativeLuminance(color: Int): Float {
    val r = ((color shr 16) and 0xFF) / 255f
    val g = ((color shr 8) and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/** `true` si sobre este color hay que dibujar contenido oscuro (negro). */
fun isLightColor(color: Int): Boolean = relativeLuminance(color) > 0.5f

/**
 * Luminancia a partir de la cual un color se considera demasiado claro para
 * usarse como tint de icono o de texto. Los colores vivos originales quedan
 * todos por debajo (el más alto, `Purple40`, da 0.36) y los pasteles por encima
 * (el más bajo, 0.65), así que el corte separa ambas paletas sin tocar los
 * colores que la app ya usaba.
 */
private const val MIN_LUMINANCE_TO_DARKEN = 0.45f

/**
 * Luminancia máxima que se admite en el color de acento resultante. Está por
 * debajo de la anterior a propósito: al entrar en el rango se sigue bajando la
 * intensidad hasta caer aquí, que es lo que hace legible el texto.
 */
private const val MAX_ACCENT_LUMINANCE = 0.28f

/**
 * Versión del color del hábito apta para iconos y texto.
 *
 * Los pasteles tienen una luminancia demasiado alta para leerse sobre el tema
 * claro. En lugar de recortarlos (lo que los volvería grises) se baja su
 * intensidad manteniendo el matiz y la saturación, hasta entrar en el umbral de
 * [MAX_ACCENT_LUMINANCE]. Los colores vivos ya son legibles y se devuelven sin
 * tocar, de modo que ningún hábito existente cambia de aspecto.
 *
 * Para fondos (`copy(alpha = …)`) hay que seguir usando el color original.
 */
fun Int.accentColor(): Int {
    if (relativeLuminance(this) <= MIN_LUMINANCE_TO_DARKEN) return this
    val hsv = toHsv()
    for (step in 1..ACCENT_STEPS) {
        val value = hsv[2] * (1f - step.toFloat() / ACCENT_STEPS)
        val candidate = hsvToColorInt(hsv[0], hsv[1], value)
        if (relativeLuminance(candidate) <= MAX_ACCENT_LUMINANCE) return candidate
    }
    return 0xFF000000.toInt()
}

private const val ACCENT_STEPS = 64

/** Representación hexadecimal `#RRGGBB` del color, para mostrar en la interfaz. */
fun Int.toHexString(): String =
    "#%06X".format(0xFFFFFF and this)

/**
 * Convierte el color a HSV.
 *
 * @return `[matiz 0..360, saturación 0..1, valor 0..1]`. Compose no ofrece el
 * inverso (`Color.toHsv()` no existe), así que se calcula a mano para poder
 * abrir el selector de color personalizado sobre el color ya elegido.
 */
fun Int.toHsv(): FloatArray {
    val r = ((this shr 16) and 0xFF) / 255f
    val g = ((this shr 8) and 0xFF) / 255f
    val b = (this and 0xFF) / 255f

    val value = max(r, max(g, b))
    val chroma = value - min(r, min(g, b))
    val saturation = if (value == 0f) 0f else chroma / value

    var hue = when {
        chroma == 0f -> 0f
        value == r -> 60f * (((g - b) / chroma) % 6f)
        value == g -> 60f * (((b - r) / chroma) + 2f)
        else -> 60f * (((r - g) / chroma) + 4f)
    }
    if (hue < 0f) hue += 360f

    return floatArrayOf(hue, saturation, value)
}

/**
 * Color opaco a partir de sus componentes HSV, siempre en rango válido.
 * Es el gemelo de [Int.toHsv]; ambos reconstruyen exactamente el mismo color.
 */
fun hsvToColorInt(hue: Float, saturation: Float, value: Float): Int {
    val h = hue.coerceIn(0f, 360f)
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)

    val chroma = v * s
    val sector = h / 60f
    val x = chroma * (1f - abs(sector % 2f - 1f))

    val (red, green, blue) = when {
        sector < 1f -> Triple(chroma, x, 0f)
        sector < 2f -> Triple(x, chroma, 0f)
        sector < 3f -> Triple(0f, chroma, x)
        sector < 4f -> Triple(0f, x, chroma)
        sector < 5f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }

    val m = v - chroma
    return argb(red + m, green + m, blue + m, 1f)
}

/** Empaqueta canales normalizados en un `Int` ARGB. */
private fun argb(r: Float, g: Float, b: Float, alpha: Float): Int {
    fun channel(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).roundToInt()
    return (channel(alpha) shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
}