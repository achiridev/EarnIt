package com.example.earnit.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SwatchSize = 40.dp
private val SwatchSpacing = 8.dp

/**
 * Selector de color del hábito.
 *
 * Muestra dos paletas —[PastelColors] y [VividColors]— y un botón que abre
 * [CustomColorDialog] para elegir cualquier color.
 *
 * El diálogo se gestiona con estado local del propio composable: la pantalla
 * que lo usa no necesita saber nada, solo recibe el color final por
 * [onColorSelected].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectedColorPreview(selectedColor = selectedColor, onEditClick = { showCustomDialog = true })

        Text("Pasteles", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SwatchSpacing),
            verticalArrangement = Arrangement.spacedBy(SwatchSpacing)
        ) {
            PastelColors.forEach { color ->
                ColorSwatch(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = { onColorSelected(color) }
                )
            }
            CustomColorSwatch(onClick = { showCustomDialog = true })
        }

        Text("Vívidos", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SwatchSpacing),
            verticalArrangement = Arrangement.spacedBy(SwatchSpacing)
        ) {
            VividColors.forEach { color ->
                ColorSwatch(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomColorDialog(
            initialColor = selectedColor,
            onDismiss = { showCustomDialog = false },
            onConfirm = {
                onColorSelected(it)
                showCustomDialog = false
            }
        )
    }
}

/** Vista previa del color elegido y acceso al selector de color libre. */
@Composable
private fun SelectedColorPreview(selectedColor: Int, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(selectedColor))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(selectedColor.toHexString(), style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Filled.Colorize,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Personalizado")
        }
    }
}

@Composable
private fun ColorSwatch(color: Int, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(SwatchSize),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        shape = CircleShape,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
        colors = CardDefaults.cardColors(containerColor = Color(color))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Seleccionado",
                    tint = if (isLightColor(color)) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Última muestra de la paleta de pasteles: abre el selector de color libre. */
@Composable
private fun CustomColorSwatch(onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(SwatchSize),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Colorize,
                contentDescription = "Elegir un color personalizado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Selector de color libre: área de saturación y valor más una barra de matiz.
 *
 * Arranca siempre sobre [initialColor], de modo que reabrirlo sin cambiar nada
 * devuelve el mismo color. El resultado solo se emite al confirmar.
 */
@Composable
private fun CustomColorDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val initialHsv = remember(initialColor) { initialColor.toHsv() }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }

    val preview = hsvToColorInt(hue, saturation, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color personalizado") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(preview)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIcon("book"),
                        contentDescription = null,
                        tint = if (isLightColor(preview)) Color.Black else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = preview.toHexString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                SaturationValueArea(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSaturationValueChange = { newSaturation, newValue ->
                        saturation = newSaturation
                        value = newValue
                    }
                )

                HueBar(hue = hue, onHueChange = { hue = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(preview) }) { Text("Aplicar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/**
 * Rectángulo de selección de saturación (eje X) y valor (eje Y) sobre el matiz
 * actual. Responde tanto al toque como al arrastre.
 */
@Composable
private fun SaturationValueArea(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChange: (Float, Float) -> Unit
) {
    var areaSize by remember { mutableStateOf(IntSize.Zero) }
    val pureHue = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .onSizeChanged { areaSize = it }
            .clip(RoundedCornerShape(12.dp))
            .sampleWhilePressed { position ->
                if (areaSize.width > 0 && areaSize.height > 0) {
                    onSaturationValueChange(
                        (position.x / areaSize.width).coerceIn(0f, 1f),
                        (1f - position.y / areaSize.height).coerceIn(0f, 1f)
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pureHue)))
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        }
        ColorPickerCursor(
            x = saturation * areaSize.width,
            y = (1f - value) * areaSize.height
        )
    }
}

/** Barra de matiz de 0 a 360 grados. */
@Composable
private fun HueBar(hue: Float, onHueChange: (Float) -> Unit) {
    val rainbow = remember {
        Brush.horizontalGradient(
            listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, 1f, 1f) }
        )
    }
    val selectedHue = Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f)
    var barWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .onSizeChanged { barWidth = it.width }
            .clip(CircleShape)
            .sampleWhilePressed { position ->
                if (barWidth > 0) {
                    onHueChange((position.x / barWidth * 360f).coerceIn(0f, 360f))
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(rainbow))
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((hue / 360f * barWidth).roundToInt() - 10.dp.roundToPx(), -10.dp.roundToPx()) }
                .size(20.dp)
                .clip(CircleShape)
                .background(selectedHue)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

/**
 * Reporta la posición del dedo mientras siga apoyado, de modo que un toque y un
 * arrastre se comportan igual. No reinicia el gesto en cada recomposición: el
 * callback lee el estado a través de las propiedades que ya capturó.
 */
private fun Modifier.sampleWhilePressed(onSample: (Offset) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            var event = awaitPointerEvent()
            while (event.changes.any { it.pressed }) {
                event.changes.firstOrNull { it.pressed }?.let { onSample(it.position) }
                event = awaitPointerEvent()
            }
        }
    }

/** Círculo que marca la posición actual dentro de un área de color. */
@Composable
private fun ColorPickerCursor(x: Float, y: Float) {
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt() - 11.dp.roundToPx(), y.roundToInt() - 11.dp.roundToPx()) }
            .size(22.dp)
            .border(3.dp, Color.White, CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
    )
}