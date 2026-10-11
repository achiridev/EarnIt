# UI

## Propósito

Documentar el **sistema visual** de EarnIt: el tema Material 3, la tipografía, el uso de color, los patrones de pantalla y el estado de la internacionalización.

Para el inventario de componentes reutilizables ver [[Componentes]]. Para la estructura de la app ver [[Android]].

## Estado actual

| Aspecto | Estado |
|---|---|
| Material 3 | **En uso completo** (`androidx.compose.material3`) |
| Tema claro y oscuro | Implementado, con **color dinámico** por defecto |
| `Typography` personalizada | **Solo `bodyLarge`**; el resto son los valores por defecto de Material |
| Localización | **No existe**: textos hardcodeados en Kotlin |
| Recursos de texto | **1 solo** (`app_name`) |
| Accesoibilidad | Parcial: hay `contentDescription` en iconos de acción, pero `null` en iconos decorativos |

## Información encontrada

### El tema: `EarnItTheme`

`app/src/main/java/com/example/earnit/ui/theme/Theme.kt`

```kotlin
@Composable
fun EarnItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
)
```

Es el tema estándar generado por Android Studio, con **una decisión de diseño activa**: `dynamicColor = true` por defecto. La lógica de selección es:

| Condición | Resultado |
|---|---|
| `dynamicColor` y `Build.VERSION.SDK_INT >= S` (API 31) | `dynamicDarkColorScheme` / `dynamicLightColorScheme` según el tema del sistema |
| Solo modo oscuro | `DarkColorScheme` |
| Resto | `LightColorScheme` |

> [!important] Consecuencia real del color dinámico
> En **Android 12 o superior, el tema del sistema es el que manda** y los colores `Purple40`/`Purple80` no se ven. Dado que `minSdk = 26`, los usuarios con Android 8–11 sí ven la paleta propia. Cualquier prueba visual o captura de pantalla debe indicar el nivel de API, o el resultado no es reproducible.
>
> `MainActivity` invoca `EarnItTheme` **sin argumentos**, así que siempre usa `dynamicColor = true` y el tema del sistema. No hay forma de forzar la paleta propia desde el código actual.

### Colores del tema

`ui/theme/Color.kt` define 6 colores, todos de la plantilla de Material Design:

| Color | Claro | Oscuro |
|---|---|---|
| `Purple` | `0xFF6650A4` | `0xFFD0BCFF` |
| `PurpleGrey` | `0xFF625B71` | `0xFFCCC2DC` |
| `Pink` | `0xFF7D5260` | `0xFFEFB8C8` |

Solo se redefinen `primary`, `secondary` y `tertiary`. **El resto de la paleta (`background`, `surface`, `onSurface`…) son los valores por defecto de Material 3**, coherentes con los comentarios que permanecen en el código.

El código usa con frecuencia los roles no Primary del esquema:

| Rol | Dónde |
|---|---|
| `surfaceContainerLow` | `TopAppBar` en las **tres** pantallas |
| `surfaceContainerHighest` | Fondo de las tarjetas no seleccionadas del `IconPicker` |
| `onSurfaceVariant` | Textos secundarios y placeholders |
| `primaryContainer` | Tarjeta seleccionada del `IconPicker` |

> [!note] `res/values/colors.xml` no participa
> Los 7 colores de ese archivo (`purple_200`, `teal_700`, `black`…) son residuo de la plantilla y **no se usan en ninguna parte**. Lint los reporta como `UnusedResources`.

### Color por hábito (la decisión visual propia del proyecto)

El rasgo visual diferenciador de EarnIt es que **cada hábito tiene su propio color**, elegido por el usuario: hay **13 muestras fijas** —una única paleta, clara y saturada, sin ningún encabezado que la nombre— más un **selector de color libre** que admite cualquier tonalidad. Ese color se guarda como `Int` ARGB en `Habit.color` y se aplica así:

| Contexto | Aplicación del color | Archivo |
|---|---|---|
| Ícono en la lista | `tint = accentColor` si está completado; si no, `onSurfaceVariant` | `HabitItem.kt:70` |
| Nombre en la lista | `color = accentColor` si está completado; si no, `onSurface` | `HabitItem.kt:80` |
| Fondo de la fila | `themeColor.copy(alpha = 0.12f)` si completado; si no, `surface` | `HabitItem.kt:53` |
| Icono del checklist | `accentColor` si completado; si no, `onSurfaceVariant` | `HabitItem.kt:95,101` |
| Tarjeta del detalle | `themeColor.copy(alpha = 0.15f)` si completado; si no, `0.1f` | `HabitDetailScreen.kt:156` |
| Título del detalle | `color = accentColor`, 24 sp, `Bold` | `HabitDetailScreen.kt:179-180` |
| Marca de completado | `tint = accentColor` | `HabitDetailScreen.kt:200,208` |

Patrón consistente: **el color del hábito solo aparece cuando está completado**. Pendiente, el elemento se ve neutro (`onSurface` / `onSurfaceVariant`); completado, se tiñe. Es una señal de estado clara y aditiva.

#### Fondo con color puro, texto con color de acento

Hay **dos** colores en juego, y no es un descuido:

| | Color | Por qué |
|---|---|---|
| Fondos (`copy(alpha = …)`) | `themeColor`, el color elegido | Se ve tal cual, que es justo lo que se pidió |
| Iconos y texto | `accentColor` (`color.accentColor()`) | Un color claro no se lee sobre superficie clara |

`Int.accentColor()` baja la intensidad del color manteniendo matiz y saturación **solo si su luminancia supera 0.45**. Por debajo quedan los colores que la app ya usaba —los 12 "vivos" de la paleta anterior, cuyo más alto daba 0.36— y los oscuros que elija el usuario: se devuelven intactos, así que **ningún hábito ya guardado cambia de aspecto**. Los 13 de la paleta actual van de 0.49 a 0.89 y se oscurecen hasta llegar a 0.28. Ver [[Componentes]].

### Tipografía

`ui/theme/Type.kt` define `Typography` con **un único estilo sobrescrito**, `bodyLarge` (16 sp, `lineHeight` 24 sp, `letterSpacing` 0.5 sp, `FontWeight.Normal`).

**Casi todo el texto de la app no usa `Typography`**: las pantallas fijan `fontSize` y `fontWeight` directamente con `.sp` y `dp`. Ejemplos:

| Texto | Configuración inline |
|---|---|
| Título de las barras superiores | `fontWeight = FontWeight.Medium` (sin tamaño) |
| Nombre del hábito en la lista | `16.sp`, `Medium` |
| Nombre en la tarjeta del detalle | `24.sp`, `Bold` |
| "Historial" | `typography.titleMedium` (usa el tema) |
| Textos secundarios | `14.sp`, peso normal |
| Icono de estado vacío | `64.dp` |

Consecuencia práctica: **cambiar la tipografía del tema apenas afecta a la app**. Solo "Historial" y los cuerpos de texto largos se verían alterados.

### Los tres patrones de pantalla

Todas las pantallas comparten la misma estructura, lo que da consistencia visual:

```text
Scaffold
├── topBar = TopAppBar
│     ├── title (FontWeight.Medium)
│     ├── navigationIcon (flecha atrás)   ← solo en detalle y formulario
│     ├── actions (editar / archivar / guardar)
│     └── colors = surfaceContainerLow
├── snackbarHost                            ← solo en HomeScreen, y nunca se usa
└── contenido con Modifier.padding(innerPadding)
```

Definiciones concretas:

| Pantalla | `TopAppBar` | Acciones | Contenido |
|---|---|---|---|
| `HomeScreen` | Título "Hábitos" | — | `Column` + `LazyColumn`, o `EmptyState` |
| `HabitDetailScreen` | Título = nombre del hábito | Editar, Archivar | `Column` con `HabitHeaderCard` + `LazyColumn` de historial |
| `HabitFormScreen` | "Editar hábito" / "Nuevo hábito" | Botón "Guardar" | `Column` con `verticalScroll` |

### Estado vacío

`HomeScreen.kt:126-156` define un `EmptyState` composable privado con icono `TrackChanges` de 64 dp, dos textos explicativos y un botón "Crear hábito". Es el único estado vacío de la app; **las otras dos pantallas no definen ninguno**: si el historial está vacío, la `LazyColumn` simplemente no dibuja nada.

### Internacionalización

**No existe.** Evidencia:

- `res/values/strings.xml` contiene **una única** cadena: `app_name` = "EarnIt".
- No hay ningún directorio `values-xx/`.
- Todas las cadenas visibles están escritas directamente en los Composables.

Inventario de cadenas hardcodeadas (parcial, para dimensionar el trabajo):

| Pantalla | Cadenas |
|---|---|
| `HomeScreen` | "Hábitos", "Nuevo hábito", "No hay hábitos hoy", "Añade tu primer hábito para empezar", "Crear hábito" |
| `HabitDetailScreen` | "Hábito", "Historial", "Completado hoy", "Toca para marcar como completado", "Completado", "Volver", "Editar", "Archivar" |
| `HabitFormScreen` | "Editar hábito", "Nuevo hábito", "Volver", "Guardar", "Nombre del hábito", "Descripción (opcional)", "Icono", "Color", "Frecuencia", "Diario", "Seleccionado" |
| `HabitItem` | "Completed", "Not completed" |
| `IconPicker` | "Selected" |
| `ColorPicker` | "Personalizado", "Color personalizado", "Aplicar", "Cancelar", "Seleccionado", "Elegir un color personalizado" |

Son **~33 cadenas**. Externalizarlas es trabajo mecánico, no difícil.

> [!warning] Restricción de Lint que conviene conocer
> El proyecto ya evitó un fallo de compilación por Lint: **no se puede llamar a `Locale.getDefault()` dentro de un `@Composable`** (se dispara `NonObservableLocale`). El patrón correcto ya está aplicado en `HistoryItem` (`HabitDetailScreen.kt:213`):
> ```kotlin
> val locale = LocalConfiguration.current.locales[0]
> ```
> Cualquier formateo nuevo de fechas debe seguir ese patrón.

### Accesibilidad

| Elemento | `contentDescription` |
|---|---|
| Iconos de acción (volver, editar, archivar) | **Correcto**: "Volver", "Editar", "Archivar" |
| Icono de la FAB "Nuevo hábito" | `null` — correcto, el texto "Nuevo hábito" ya etiqueta el botón |
| Icono de `HabitItem` | `null` — correcto, decorativo |
| Checkbox de `HabitItem` | **"Completed" / "Not completed"** en inglés, mientras el resto de la app está en español |
| Check del `IconPicker` | `null` |
| Check del `ColorPicker` | **"Seleccionado"**, en español |
| Botón de color personalizado | "Elegir un color personalizado" |
| `HistoryItem` | **"Completado"** en español |

Existe una inconsistencia real de idioma en los `contentDescription`: la app está en español pero dos de ellos siguen en inglés ("Completed" / "Not completed" de `HabitItem`). Registrado como **BUG-013** en [[Bugs]]. El `Check` del `ColorPicker` ya no suma a esa lista: pasó a "Seleccionado" al reescribir el selector.

### Contraste de los colores de hábito

Los colores de la paleta son claros pero no legibles como texto: su luminancia va de 0.49 a 0.89 y sobre superficie clara desaparecen. `Int.accentColor()` los oscurece para icono y texto y deja el color intacto para el fondo (ver la tabla anterior). El umbral está calibrado para que **ninguno de los colores que la app ya usaba cambie de aspecto**, de modo que no hay regresión visual en los hábitos ya guardados.

Lo que **no** está verificado: la app no se auditó contra WCAG, y el contraste en tema oscuro (donde el color de acento se dibuja sobre superficies oscuras) no se ha comprobado en ningún sitio.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- `HomeScreen.kt:22,70` — se crea un `SnackbarHostState` y un `SnackbarHost`, pero **nunca se muestra ningún Snackbar**. Infraestructura montada y sin usar.
- `HabitItem.kt:38` — el parámetro `modifier` no es el primer opcional; Lint lo señala (`ModifierParameter`).
- `HomeScreen.kt:58` — Lint: el parámetro `modifier` opcional debe tener `Modifier` como valor por defecto (`ModifierParameter`).
- Importaciones sin usar de `EarnItTheme` en `HomeScreen.kt:40`, `HabitDetailScreen.kt:45`, `HabitFormScreen.kt:46` y `HabitItem.kt:18`.
- Lint: `AndroidManifest.xml:18` — `android:label` redundante.

### Mejoras recomendadas (no decididas)

- **Externalizar las ~35 cadenas** a `strings.xml` y añadir `values-es/`. Es el requisito previo para cualquier localización.
- **Traducir los `contentDescription`** "Completed", "Not completed" y "Selected" al español, o mejor, hacerlos resources.
- **Sustituir los `fontSize`/`fontWeight` inline por estilos de `Typography`**, para que el tema tenga efecto real.
- **Decidir si se mantiene el color dinámico.** Fija la identidad visual pero hace que la app se vea distinta según el fabricante.
- **Añadir estados vacíos** a `HabitDetailScreen` (historial vacío) y a `HabitFormScreen`.
- **Eliminar el `SnackbarHostState` no usado** o implementar realmente los mensajes de confirmación (hoy no hay ningún mensaje tras guardar).

## Preguntas abiertas

- ¿Se busca una identidad visual propia (desactivando el color dinámico) o se prefiere delegar en el tema del sistema?
- ¿Habrá soporte para tipografías personalizadas o escalado de fuente del sistema? No hay nada preparado.
- ¿La app debe ser accesible según WCAG? El contraste de los colores de hábito está corregido para el tema claro mediante `accentColor()`, pero **la app no se ha auditado contra WCAG** y el tema oscuro no está comprobado.
- ¿Se internacionalizará a inglés u otros idiomas? Hoy no hay ningún `values-xx` que sugiera intención.