# Componentes

## Propósito

Catalogar los **componentes de interfaz reutilizables** de EarnIt: qué hacen, qué parámetros aceptan, dónde se usan y qué se debe tener en cuenta al reutilizarlos o extenderlos.

Para el sistema de temas y colores ver [[UI]]. Para el flujo de pantallas ver [[Navegacion]].

## Estado actual

| Componente | Ubicación | ¿Reutilizable fuera de `HabitFormScreen`? |
|---|---|---|
| `HabitItem` | `core/ui/components/HabitItem.kt` | Sí |
| `IconPicker` | `core/ui/components/IconPicker.kt` | No, hoy solo lo usa el formulario |
| `ColorPicker` | `core/ui/components/ColorPicker.kt` | No, ídem |
| `getIcon` | `core/ui/components/IconHelper.kt` | Sí |
| `HabitHeaderCard` | `feature/habit/HabitDetailScreen.kt` | No (vive en `feature/`, no en `core/`) |
| `HistoryItem` | `feature/habit/HabitDetailScreen.kt` | No |
| `FrequencySelector` | `feature/habit/HabitFormScreen.kt` | No |
| `EmptyState` | `feature/home/HomeScreen.kt` | No, y es `private` |

> [!note] `ColorPicker` tiene su propio archivo
> `ColorPicker.kt` y `HabitColors.kt` separan la UI del selector de su
> matemática de color. La paleta vive en constantes de nivel superior, no
> dentro del composable.

## Información encontrada

### `HabitItem` — la fila de la lista principal

`core/ui/components/HabitItem.kt`

```kotlin
@Composable
fun HabitItem(
    name: String,
    icon: ImageVector,
    color: Int,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit = {},
    modifier: Modifier = Modifier
)
```

Fila de la lista de hábitos. Estructura visual:

```text
Card (onClick = onClick)                          ← toda la fila es pulsable
└── Row (padding 16.dp)
    ├── Box 40.dp → Icon 24.dp                   ← icono del hábito
    ├── Spacer 12.dp
    ├── Column → Text(name)                       ← nombre
    └── Box 24.dp (clickable = onToggle)          ← indicador circular
        ├── isCompleted  → Icons.Filled.CheckCircle         (tint = color del hábito)
        └── otherwise    → Icons.Filled.RadioButtonUnchecked (tint = onSurfaceVariant)
```

Puntos clave:

- **Dos zonas táctiles separadas**: el `Card` completo (`onClick` → abrir detalle) y el `Box` del indicador (`onToggle` → marcar). El `Box` es hijo del `Card` y su `clickable` absorbe el toque, de modo que marcar **no** abre el detalle.
- El componente es **puramente presentacional**: no sabe nada de `Habit`, `LocalDate` ni repositorios. Recibe `icon: ImageVector` ya resuelto y `color: Int` ya convertido. Eso lo hace trivial de previsualizar y probar.
- El color del hábito se aplica con transparencias: fondo `copy(alpha = 0.12f)` cuando está completado, `colorScheme.surface` cuando no.
- `onToggle` tiene valor por defecto `{}`, lo que permite usarlo sin acción de marcado.

### `IconPicker` — selector de iconos

`core/ui/components/IconPicker.kt`

```kotlin
@Composable
fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

Rejilla de **20 iconos** en un `LazyHorizontalGrid` de 5 filas, con desplazamiento horizontal.

La lista de claves está **definida dentro del composable**, como lista local:

```kotlin
val icons = listOf(
    "book" to Icons.Filled.MenuBook,
    "water" to Icons.Filled.LocalDrink,
    ... 18 más ...
)
```

> [!warning] Punto de extensión obligatorio: dos listas que deben coincidir
> La app tiene **dos catálogos de iconos que deben mantenerse sincronizados**:
>
> 1. `IconPicker.kt:66-87` — la lista que el usuario puede elegir.
> 2. `IconHelper.kt:27-48` — el `when` que traduce la clave a `ImageVector` al pintar.
>
> Si se añade una clave solo en (1), el usuario la podrá seleccionar pero **`getIcon()` cairá en la rama `else` y pintará siempre el icono de libro**. Ese es el fallo silencioso a evitar. Para añadir un icono hay que tocar **los dos archivos** (y añadir el `import` correspondiente, ver más abajo).

**Altura acotada: requisito obligatorio, no cosmético.** Las constantes de la parte superior del archivo calculan la altura exacta:

```kotlin
private const val IconRows = 5
private val IconSize = 56.dp
private val IconSpacing = 8.dp
private val iconGridHeight = IconSize * IconRows + IconSpacing * (IconRows - 1) + 16.dp
```

y se aplican en el `modifier` del grid. El comentario del código explica por qué:

> Lazy layouts need a bounded height: a `Column` with `verticalScroll` gives infinite constraints and would throw `LazyHorizontalGrid's height should be bound by parent`.

Este fue un **crash real de la app** en el primer fotograma del formulario, y está cubierto por un test de regresión: `app/src/androidTest/java/com/example/earnit/HabitFormScreenTest.kt`. **No elimines la altura acotada**; hay un test que lo verifica.

> [!tip] La receta del proyecto
> `LazyHorizontalGrid` usa `rows = GridCells.Fixed(n)`, y `LazyVerticalGrid` usa `columns`. Los layouts perezosos **siempre** necesitan una altura o ancho acotado por el padre.

### `ColorPicker` — selector de color

`core/ui/components/ColorPicker.kt`

```kotlin
@Composable
fun ColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

Estructura:

```text
Column
├── Row  vista previa (círculo del color + "#RRGGBB") + TextButton "Personalizado"
└── FlowRow → 13 muestras de PaletteColors + muestra "Personalizado"
```

- **13 muestras fijas**, una sola paleta: `PaletteColors`, en `HabitColors.kt`, no
  dentro del composable. **Sin encabezado**: el selector no nombra la paleta, solo
  la muestra.
- Se usa `FlowRow` (`androidx.compose.foundation.layout`, **requiere
  `@OptIn(ExperimentalLayoutApi::class)`** en foundation 1.10) en lugar de una
  `Row`: la versión anterior metía ~520.dp de muestras en una `Row` sin
  desplazamiento y se salían de la pantalla.
- Cada muestra es un `Card` circular de 40.dp; la seleccionada sube a 4.dp de
  elevación, lleva borde de 2.dp y muestra un `Check` en el centro.
- El color del check (negro o blanco) lo decide `isLightColor(color)` de
  `HabitColors.kt`, que calcula la luminancia con los coeficientes de Rec. 709
  (`0.2126·R + 0.7152·G + 0.0722·B`) y la compara con `0.5`.
- El valor por defecto del formulario es `DEFAULT_HABIT_COLOR` (`0xFFFF6E61`),
  constante de `HabitColors.kt` que es la **primera muestra de la paleta**: un
  hábito nuevo nace con un color que el usuario ve marcado entre las 13.
- Sustituir la paleta **no cambia ningún hábito guardado**: el color se guarda
  como `Int` en `Habit.color` y no se toca el esquema. Lo único que cambia es que
  un hábito con un color de una paleta anterior ya no aparece con ninguna muestra
  marcada.

> [!tip] El componente es autónomo
> El diálogo de color personalizado se abre y cierra con estado local del propio
> `ColorPicker`. La pantalla que lo usa no declara nada ni gestiona visibilidad:
> solo recibe el color final por `onColorSelected`. `HabitFormScreen` no ha
> tenido que cambiar.

### `CustomColorDialog` — selector de color libre

`core/ui/components/ColorPicker.kt` (privado)

`AlertDialog` con "Cancelar" / "Aplicar" que permite elegir **cualquier** color:

| Elemento | Comportamiento |
|---|---|
| Vista previa | Rectángulo con el color resultante y el icono del hábito encima, más su `#RRGGBB` |
| Área 2D | 150.dp: eje X = saturación, eje Y = valor, sobre el matiz actual |
| Barra de matiz | 32.dp, de 0 a 360 grados |

- Se dibuja con `Canvas` y degradados (`Brush.horizontalGradient` blanco→matiz,
  `Brush.verticalGradient` transparente→negro). **No usa el `Slider` de Material 3**
  a propósito: su API de `track`/`thumb` cambió entre versiones, y así el
  componente no depende de ella.
- El gesto usa `Modifier.sampleWhilePressed()`, un helper privado basado en
  `awaitEachGesture`/`awaitPointerEvent`. Responde igual a un toque que a un
  arrastre. Compose no expone `awaitFirstDown` como extensión importable, de ahí
  que no se use `detectTapGestures`/`detectDragGestures` por separado.
- **Arranca sobre el color ya elegido** (`initialColor`), de modo que reabrirlo
  sin tocar nada devuelve el mismo color, y solo emite al confirmar.

### `HabitColors.kt` — paletas y matemática de color

`core/ui/components/HabitColors.kt`

Kotlin puro, sin Compose, para que sea verificable con pruebas locales:

| Símbolo | Función |
|---|---|
| `DEFAULT_HABIT_COLOR` | `0xFFFF6E61`, el color de un hábito nuevo (la primera muestra de la paleta) |
| `PaletteColors` | Los 13 colores del selector |
| `isLightColor(Int)` | Si sobre el color hay que dibujar negro |
| `Int.accentColor()` | Versión legible del color para iconos y texto |
| `Int.toHexString()` | `#RRGGBB` |
| `Int.toHsv()` / `hsvToColorInt(h, s, v)` | Conversión RGB↔HSV, ambos verificados por prueba |

> [!warning] El inverso de HSV hay que escribirlo a mano
> Compose ofrece `Color.hsv(h, s, v)` pero **no** `Color.toHsv()`. Para poder
> abrir el diálogo sobre el color actual hace falta la conversión inversa, que
> es la razón de que exista `Int.toHsv()`.

> [!tip] Por qué `accentColor()`
> Un color de la paleta tiene una luminancia demasiado alta para leerse sobre el
> tema claro. `accentColor()` baja su intensidad manteniendo matiz y saturación
> hasta dejarla en 0.28, pero **solo si supera 0.45**. Ese corte separa la
> paleta —los 13 colores van de 0.49 a 0.89, todos por encima— de los colores
> oscuros, incluidos los 12 "vivos" que el selector ofrecía antes (el más alto
> daba 0.36) y los que el usuario elija a mano: ninguno se toca, así que ningún
> hábito existente cambia de aspecto.

> [!warning] Regla al añadir un color a la paleta
> Su luminancia tiene que **superar 0.45**. Si no, `accentColor()` lo devolvería
> intacto y se usaría sin oscurecer como texto sobre el tema claro. Lo detecta
> `accentColor_darkensThePaletteUntilTheyAreLegible`, que además comprueba que el
> acento resultante sea más oscuro que el color original.

### `getIcon` —Traductor de clave a `ImageVector`

`core/ui/components/IconHelper.kt`

```kotlin
fun getIcon(imageVectorName: String): ImageVector
```

Función pura, sin Compose, sin estado. 20 ramas en un `when`, con **fallback silencioso**:

```kotlin
else -> Icons.Filled.MenuBook
```

Un nombre desconocido **no lanza excepción**: devuelve el icono de lectura. Es robusto, pero también significa que un error de tipeo en la base de datos no se manifiesta de forma visible.

Mapeo completo:

| Clave | Icono | Clave | Icono |
|---|---|---|---|
| `book` | `MenuBook` | `swim` | `Pool` |
| `water` | `LocalDrink` | `yoga` | `SportsGymnastics` |
| `exercise` | `FitnessCenter` | `study` | `School` |
| `sleep` | `Bedtime` | `work` | `Work` |
| `meditate` | `SelfImprovement` | `music` | `MusicNote` |
| `code` | `Code` | `art` | `Palette` |
| `read` | `ChromeReaderMode` | `cook` | `Restaurant` |
| `run` | `DirectionsRun` | `clean` | `CleaningServices` |
| `walk` | `DirectionsWalk` | `plant` | `Spa` |
| `bike` | `DirectionsBike` | `pet` | `Pets` |

> [!warning] Los iconos de Material son propiedades de extensión
> Para usar un icono hay que **importar la propiedad de extensión**:
> ```kotlin
> import androidx.compose.material.icons.filled.WaterDrop
> // …
> Icons.Filled.WaterDrop
> ```
> El acceso completamente cualificado **no compila**:
> `androidx.compose.material.icons.Icons.Filled.X` → error.
>
> `material-icons-extended` está en el classpath, así que los iconos de cola larga están disponibles.

### Componentes privados de pantallas

No están en `core/ui/components/` pero conviene conocerlos:

| Componente | Archivo | Visibilidad | Función |
|---|---|---|---|
| `EmptyState` | `HomeScreen.kt:126` | `private` | Estado vacío de la lista de hábitos |
| `HabitHeaderCard` | `HabitDetailScreen.kt:128` | `public` | Tarjeta superior del detalle, pulsable para marcar |
| `HistoryItem` | `HabitDetailScreen.kt:210` | `public` | Fila del historial con fecha localizada |
| `FrequencySelector` | `HabitFormScreen.kt:182` | `public` | Selector de frecuencia (hoy solo `DAILY`) |

Dos detalles de `HistoryItem`:

- Formatea la fecha con `LocalConfiguration.current.locales[0]` (ver la restricción de Lint en [[UI]]).
- Solo dibuja el icono de completado cuando `completion.completed == true`. **Los registros falsos no muestran nada**, así que en el historial es imposible distinguir "no completado" de "sin registro".

`HabitHeaderCard` recibe `onClick` a nivel de `Card` (una `Card` con `onClick`, no con `.clickable`), y el click conmuta el completado.

### `FrequencySelector` y su límite actual

```kotlin
Frequency.DAILY.let { freq -> … }
```

Recorre el enum y renderiza cada valor. La estructura ya soporta N frecuencias: añadir `WEEKLY` al enum lo hace aparecer automáticamente. Pero **el filtrado en la capa de datos no existe** (ver `TODO(frequency)` en `HabitRepositoryImpl.kt:38` y [[Persistencia]]), de modo que añadir un valor al enum **no haría que la app funcionase**: el hábito se mostraría los 7 días.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- `HabitItem.kt:39` — `modifier` no es el primer parámetro opcional (aviso de Lint `ModifierParameter`).
- `HabitItem.kt:18` — importación de `EarnItTheme` sin usar.
- `HabitItem.kt:92,98` — acceso a `androidx.compose.material3.Icon` completamente cualificado pese a estar ya importado en el archivo.
- `IconPicker.kt:35-47` — importaciones de Compose sin usar (`Column`, `FontWeight`, `sp`, etc.).
- Los catálogos de `IconPicker` y `IconHelper` están duplicados y pueden desincronizarse.

### Mejoras recomendadas (no decididas)

- **Extraer las claves de icono a una única fuente de verdad.** Por ejemplo un `IconOption(name, imageVector)` en `core/ui/components/`, consumido tanto por `IconPicker` como por `getIcon`. Eliminaría la duplicación y el fallo silencioso.
- **Dar contenido visual a los registros no completados** en `HistoryItem`, para que el historial sea legible.
- **Mover `HabitHeaderCard`, `HistoryItem` y `FrequencySelector`** a `core/ui/components/` si se espera reutilizarlos.
- **Añadir estado vacío al historial** del detalle.
- **Hacer que `getIcon` sea más rico** (por ejemplo, con `Icons.Outlined.Check` para los no completados) para reforzar la señal de estado.

## Preguntas abiertas

- ¿Se quiere un catálogo de iconos más grande o se mantiene fijo en 20? No hay ningún mecanismo de carga dinámica.
- ¿La paleta de colores debería derivarse del tema (y por tanto cambiar con el color dinámico) o ser una identidad fija de EarnIt? **Hoy es una identidad fija**: vive en `HabitColors.kt` y no cambia con el color dinámico.
- ¿`FrequencySelector` se mantiene aunque solo haya una frecuencia? El nombre es plural y el enum tiene un único valor; conviene saber si habrá más frecuencias antes de consolidar la API.
- ¿Debería `getIcon()` lanzar una excepción para claves desconocidas en vez de devolver el icono de libro? Hoy el fallo es silencioso.
- ¿Los componentes de `core/ui/components/` se moverán a un módulo Gradle propio si el proyecto crece? Hoy no hay más de un módulo.