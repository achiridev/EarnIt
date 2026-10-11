# Navegación

## Propósito

Documentar el **grafo de navegación** de la app: qué rutas existen, cómo se navega entre ellas y qué dependencias recibe cada pantalla. Es el documento a consultar antes de añadir, mover o eliminar una pantalla.

Para el estado y los ViewModel ver [[EstadoYViewModels]]. Para las capas ver [[Arquitectura]].

## Estado actual

| Aspecto | Estado |
|---|---|
| Librería | `androidx.navigation:navigation-compose` **2.8.4** (declarada y en uso) |
| Grafo | Definido en un único sitio: `core/navigation/AppNavHost.kt` |
| Rutas | **4** |
| ViewModel | **Compartidos a nivel de `Activity`**, no por destino |
| Anidamiento | **Ninguno** (sin `nestedNavHost`) |
| Deep links | **Ninguno declarado** |
| Transiciones animadas | **Ninguna personalizada** |
| Barra de atrás del sistema | Funciona por defecto (`popBackStack` implícito del NavHost) |

## Información encontrada

### El grafo

`app/src/main/java/com/example/earnit/core/navigation/AppNavHost.kt` — **105 líneas, un solo `@Composable`, todo el grafo en un `NavHost`.**

```kotlin
NavHost(navController, "home", modifier = modifier) {
    composable("home")                  { … }   // lista principal
    composable("add_habit")             { … }   // crear hábito
    composable("habit_detail/{habitId}"){ … }   // detalle + historial
    composable("edit_habit/{habitId}")  { … }   // editar hábito
}
```

### Tabla de rutas

| Ruta | Tipo argumento | Pantalla | Destino |
|---|---|---|---|
| `home` | — | `HomeScreen` | **Origen** (`startDestination`) |
| `add_habit` | — | `HabitFormScreen` | `home` (FAB "Nuevo hábito") |
| `habit_detail/{habitId}` | `NavType.LongType` | `HabitDetailScreen` | `home` (toque en una fila) |
| `edit_habit/{habitId}` | `NavType.LongType` | `HabitFormScreen` | `habit_detail` (icono editar) |

### Grafo visual

```text
            ┌──────────────────────────────┐
            │            home              │  ← startDestination
            │        (HomeScreen)          │
            └──┬───────────────────────┬───┘
               │ FAB "Nuevo hábito"    │ toque en HabitItem
               ▼                       ▼
        ┌──────────────┐        ┌───────────────────────┐
        │  add_habit   │        │ habit_detail/{habitId}│
        │(HabitFormScreen)      │ (HabitDetailScreen)   │
        └──────┬───────┘        └───┬───────────────┬───┘
               │                    │ "Editar"      │ "Archivar"
               │                    ▼                │
               │            ┌────────────────┐       │
               │            │ edit_habit/{id} │       │
               │            │(HabitFormScreen)│      │
               │            └────────────────┘       │
               │                    │                │
               └────────────────────┴────────────────┘
                                  │
                       popBackStack() en todos los casos
```

**No hay rutas de retorno específicas**: `add_habit` y `edit_habit` siempre hacen `popBackStack()`, lo que devuelve al origen (home o habit_detail respectivamente). Nunca se navega a una ruta de la pila completa con `launchSingleTop`, `popUpTo` ni `restoreState`, así que el estado de scroll de la lista no se preserva al volver.

### Inyección de dependencias por lambda

La decisión de diseño más característica de `AppNavHost`: **el NavHost no inyecta dependencias en las pantallas, sino el comportamiento**.

```kotlin
composable("habit_detail/{habitId}") { backStackEntry ->
    val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
    HabitDetailScreen(
        viewModel = habitDetailViewModel,
        habitId = habitId,
        onEdit = { habit ->
            habitFormViewModel.loadHabit(habit)
            navController.navigate("edit_habit/${habit.id}")
        },
        onArchive = {
            scope.launch { habitDetailViewModel.archive() }
            navController.popBackStack()
        },
        onToggleCompletion = { date ->
            scope.launch { habitDetailViewModel.toggleCompletion() }
        }
    )
}
```

Ventajas reales de este patrón:

- Las pantallas no conocen el `NavController`, lo que las hace previsualizables y testeables (el test instrumentado de `HabitFormScreen` lo demuestra).
- Las escrituras pasan por un `rememberCoroutineScope()` del NavHost, no por `viewModelScope`: la operación se cancela si el NavHost sale de la composición.
- Los ViewModel se crean en `MainActivity` y se pasan ya construidos, así que **las pantallas no saben de dónde vienen**.

Coste real:

- **`AppNavHost` recibe 6 parámetros** (`homeViewModel`, `habitFormViewModel`, `habitDetailViewModel`, `habitRepository`, `completionRepository`, `toggleCompletionUseCase`). Con más pantallas esta firma crece sin límite. Es el punto de tensión del patrón.
- **El orden de las llamadas importa y es implícito**: `habitFormViewModel.clear()` debe ejecutarse *antes* de `navigate("add_habit")`, porque `HabitFormScreen` lee ese estado en su primera composición. Invertirlo mostraría los datos del hábito anterior.

### La tabla de argumentos

| Qué | Tipo | Nota |
|---|---|---|
| `habitId` | `NavType.LongType` | Correcto: los ids son `Long` |
| Valor por defecto | `?: 0L` | Si falta el argumento, se navega a un id inexistente en lugar de fallar |

`habitId` **se usa en las dos pantallas de hábito**: en `HabitFormScreen` dentro de un `LaunchedEffect`, y en `HabitDetailScreen` desde la corrección de BUG-004, que relee el hábito por id en lugar de depender solo de `_habit`.

### Detalle importante: `MainActivity` no usa `by navGraphViewModels`

Los tres ViewModel se crean a nivel de `Activity`:

```kotlin
private val homeViewModel: HomeViewModel by viewModels { HomeViewModelFactory(...) }
private val habitFormViewModel: HabitFormViewModel by viewModels { HabitFormViewModelFactory(...) }
private val habitDetailViewModel: HabitDetailViewModel by viewModels { HabitDetailViewModelFactory(...) }
```

y se pasan a `AppNavHost`, que los reparte entre destinos. Consecuencias:

1. **Sobreviven a los cambios de destino**: ir de un hábito a otro reutiliza la misma instancia.
2. **Se comparten entre rutas**: `HabitFormViewModel` es la misma instancia en `add_habit` y en `edit_habit`; `HabitDetailViewModel` es la misma en todos los detalles.
3. **No hay `SavedStateHandle`** en ninguno, así que no sobreviven a la muerte del proceso. Es la causa de BUG-004.

Lo que **no** pasa: los ViewModel no usan `navArgument` para recibir datos, no usan `viewModel()` de navigation-compose, y no hay `Navigation Compose` con `hiltViewModel()`.

### Comportamientos que funcionan

Conviene registrarlos para no "arreglarlos" por error:

- **Volver desde el formulario** usa `onCancel`, que hace `habitFormViewModel.clear()` + `popBackStack()`. Limpia el formulario para la próxima apertura.
- **Volver desde el detalle** usa el botón atrás del sistema, que funciona por defecto del `NavHost`.
- **Guardar** hace `popBackStack()` sin más acciones, porque la pantalla de lista ya está suscrita al `Flow` de Room y se actualiza sola.
- **Archivar** lanza la corrutina y hace `popBackStack()` de inmediato, sin esperar. La lista se actualiza sola al volver.

### La flecha "volver" del detalle ya está conectada

`feature/habit/HabitDetailScreen.kt`. Antes tenía un `onClick` vacío —el comentario lo delata: era un `navController.popBackStack()` que nunca se conectó— y la pantalla no recibía ningún callback de navegación atrás. Era el BUG-001 de [[Bugs]], la incoherencia más visible de la app.

**Resuelto.** `HabitDetailScreen` recibe ahora `onBack: () -> Unit`, conectado al `navigationIcon`, y `AppNavHost` le pasa `navController.popBackStack()`:

```kotlin
HabitDetailScreen(
    viewModel = habitDetailViewModel,
    habitId = habitId,
    habitRepository = habitRepository,
    onBack = { navController.popBackStack() },
    onEdit = { habit -> … },
    …
)
```

La flecha se comporta como el `onCancel` de `HabitFormScreen`, que ya funcionaba.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

| # | Pendiente | Ubicación |
|---|---|---|
| 1 | `onToggleCompletion(date)` ignora el `date` que recibe | `AppNavHost.kt:83-85` |
| 2 | Doble carga del hábito en modo edición (BUG-010) | `AppNavHost.kt:76` + `HabitFormScreen.kt:67` |
| 3 | `AppNavHost` recibe 7 parámetros y crece sin límite | `AppNavHost.kt:22-31` |

### Mejoras recomendadas (no decididas)

- **Reemplazar los ViewModel de `Activity` por ViewModel por destino** (`viewModel { … }` de navigation-compose o `by navGraphViewModels`), para que cada instancia quede asociada a su ruta y se pueda usar `SavedStateHandle`. Cerraría la causa de fondo de BUG-004.
- **Extraer el tipo de pantalla en un `sealed interface`** y reducir los 7 parámetros de `AppNavHost` agrupándolos.
- **Añadir transiciones** entre rutas (`AnimatedContentTransitionScope`) — hoy el cambio de pantalla es instantáneo.
- **Preservar el estado de scroll** de `HomeScreen` al volver del detalle (`popUpTo("home") { saveState = true }`).
- **Declarar deep links** si en el futuro se quiere abrir un hábito desde una notificación o un enlace externo. Requeriría rethink del paso de `habitId`.

## Preguntas abiertas

- ¿Debe poder editarse un hábito desde la pantalla principal sin pasar por el detalle? El flujo actual obliga a pasar por el detalle.
- ¿Se quiere un diálogo de confirmación al archivar? Hoy la acción es inmediata e irreversible desde la UI.
- ¿El `ViewModel` compartido entre `add_habit` y `edit_habit` es intencionado? Es lo que permite un único `HabitFormScreen`, pero obliga a los `clear()` / `loadHabit()` explícitos.
- ¿Qué debe ocurrir al volver desde `edit_habit` con cambios sin guardar? Hoy se pierden sin preguntar, porque el estado vive en el ViewModel compartido y `onCancel` hace `clear()`.
- ¿Se necesita navegación anidada (por ejemplo, una pestaña de ajustes con subpáginas)? Hoy no hay tabs; `HomeScreen` es la única raíz.
- ¿Sigue siendo aceptable que `HabitDetailScreen` reciba `habitRepository` como parámetro? Se añadió para corregir BUG-004, y es la misma fuga de capas que ya arrastra `HabitFormScreen` (ver BUG-010).