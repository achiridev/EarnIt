package com.example.earnit.core.ui.components

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Paleta y matemática de color de los hábitos.
 *
 * Todo es Kotlin puro (sin Compose) para que sea verificable con pruebas locales.
 * El color de un hábito es un `Int` ARGB empaquetado, tal y como lo guarda
 * `HabitEntity.color`; aquí solo se manipulan sus canales.
 */

/**
 * Color por defecto de un hábito nuevo.
 *
 * Es la primera muestra de [PaletteColors], de modo que el formulario siempre
 * arranca con un color que el usuario ve marcado entre las 13 muestras.
 */
val DEFAULT_HABIT_COLOR: Int = 0xFFFF6E61.toInt()

/**
 * Paleta del selector: 13 colores claros repartidos por toda la rueda de matiz,
 * uno cada ~25 grados para que no falte ningún sector.
 *
 * Son tonos suaves pero **no pastel**: mantienen el valor alto (claros, como los
 * pasteles de antes) con la saturación al doble (≈0.60 frente a ≈0.26), así que
 * se distinguen bien en la lista sin volverse oscuros.
 *
 * Todos tienen una luminancia por encima de [MIN_LUMINANCE_TO_DARKEN] —el más
 * bajo, el índigo, da 0.49— para que [Int.accentColor] los oscurezca siempre
 * antes de usarlos como texto o tint. Esa condición la vigila la prueba
 * `accentColor_darkensThePaletteUntilTheyAreLegible`.
 */
val PaletteColors: List<Int> = listOf(
    0xFFFF6E61.toInt(), // Coral
    0xFFFF9D4D.toInt(), // Naranja
    0xFFFFE252.toInt(), // Amarillo
    0xFFBBFA64.toInt(), // Lima
    0xFF6DF299.toInt(), // Verde
    0xFF66F2C8.toInt(), // Menta
    0xFF66F2F2.toInt(), // Turquesa
    0xFF5ECCF7.toInt(), // Cielo
    0xFF61A5FF.toInt(), // Azul
    0xFF7373FF.toInt(), // Índigo
    0xFFB473FF.toInt(), // Violeta
    0xFFFF6BFF.toInt(), // Magenta
    0xFFFF66A6.toInt(), // Rosa
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
 * usarse como tint de icono o de texto.
 *
 * Por debajo quedan los colores que la app ya usaba antes de la paleta actual
 * —los 12 "vivos", cuyo más alto daba 0.36— y los colores oscuros que el usuario
 * elija a mano: ninguno se toca, así que **ningún hábito guardado cambia de
 * aspecto**. Por encima están los 13 colores de [PaletteColors], cuyo más bajo
 * da 0.49.
 *
 * Regla para añadir un color a la paleta: su luminancia debe superar este
 * umbral. Si no, [Int.accentColor] lo devolvería intacto y se usaría sin
 * oscurecer como texto sobre el tema claro.
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
 * Los colores de [PaletteColors] tienen una luminancia demasiado alta para
 * leerse sobre el tema claro. En lugar de recortarlos (lo que los volvería
 * grises) se baja su intensidad manteniendo el matiz y la saturación, hasta
 * entrar en el umbral de [MAX_ACCENT_LUMINANCE]. Los colores ya oscuros se
 * devuelven sin tocar.
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