# Estado y ViewModels

## Propósito

Documentar **cómo se gestiona el estado** en EarnIt: los tres ViewModel, el uso de `StateFlow`, `viewModelScope` y coroutines, y — sobre todo — **qué ViewModel está compartido entre qué pantallas**, que es donde están los problemas reales de esta área.

Para el flujo entre capas ver [[Arquitectura]]. Para el grafo de navegación ver [[Navegacion]].

## Estado actual

| ViewModel | Estado propio | Ámbito real |
|---|---|---|
| `HomeViewModel` | `StateFlow<List<Habit>>` | Compartido, pero solo lo usa una pantalla |
| `HabitFormViewModel` | 7 `StateFlow` independientes + `isValid` derivado | **Compartido entre `add_habit` y `edit_habit`** |
| `HabitDetailViewModel` | `StateFlow<List<HabitCompletion>>` + `StateFlow<Habit?>` + un `var _date` | **Compartido entre destinos de detalle** |

Ninguno de los tres recibe `SavedStateHandle`.

## Información encontrada

### `HomeViewModel` — el único bien resuelto

`feature/home/HomeViewModel.kt` completo:

```kotlin
class HomeViewModel(getTodaysHabitsUseCase: GetTodaysHabitsUseCase) : ViewModel() {
    val habits: StateFlow<List<Habit>> = getTodaysHabitsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
```

Es el patrón correcto y sin slotted ningún problema:

- El `Flow` del repositorio se comparte mediante `stateIn`, no se recolecta de nuevo en cada recomposición.
- `WhileSubscribed(5_000)` mantiene la suscripción 5 segundos tras la última pantalla: sobrevive a rotaciones sin desperdiciar recursos.
- `initialValue = emptyList()` evita que la primera composición muestre `null`.

**No expone ninguna acción**: todo el comportamiento pasa por los lambdas que `AppNavHost` inyecta en la pantalla. `HomeScreen` recibe `habitsFlow`, no el ViewModel.

### `HabitFormViewModel` — siete `StateFlow` sueltos y uno derivado

`feature/habit/HabitFormViewModel.kt`

```kotlin
private val _name = MutableStateFlow("")
private val _description = MutableStateFlow("")
private val _icon = MutableStateFlow("book")
private val _color = MutableStateFlow(0xFF6650A4.toInt())   // Purple40
private val _frequency = MutableStateFlow(Frequency.DAILY)
private val _isEditing = MutableStateFlow(false)
private val _habitId = MutableStateFlow<Long?>(null)
private val _createdAt = MutableStateFlow(LocalDate.now())
private val _status = MutableStateFlow(HabitStatus.ACTIVE)
```

Cada campo se expone como `StateFlow` público y la pantalla los recolecta uno a uno con `collectAsStateWithLifecycle()`.

Además hay **un `StateFlow` derivado**:

```kotlin
val isValid: StateFlow<Boolean> = _name
    .map { it.trim().isNotBlank() }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

> [!warning] `isValid` debe ser estado, no un método
> Antes era `fun isValid(): Boolean`. `HabitFormScreen` lo leía en la lambda `actions` de la `TopAppBar`, y una lectura de `MutableStateFlow.value` ahí no es una lectura de estado de Compose: ese ámbito de recomposición nunca se invalidaba y **el botón "Guardar" quedaba inutilizable** (BUG-014). Convertirlo en `StateFlow` derivado de `_name` es la corrección; volver a un método reintroduciría el fallo.

| Método | Función |
|---|---|
| `loadHabit(habit)` | Rellena los 9 campos desde un `Habit` y pone `isEditing = true` |
| `clear()` | Restaura los valores iniciales por defecto |
| `updateName/Description/Icon/Color/Frequency` | Escritores de un campo |
| `save(): Long` | Construye el `Habit` y llama a `update` o `create` según `isEditing` |

**El ViewModel es compartido entre las rutas `add_habit` y `edit_habit`**, ambas renderizadas por `HabitFormScreen`. Es lo que permite que el mismo componente sirva para crear y editar. El flujo real es:

```text
"Nuevo hábito" (FAB)
   └─> habitFormViewModel.clear()          ← AppNavHost lo llama ANTES de navegar
       └─> navigate("add_habit")
            └─> HabitFormScreen(isEditing = false)

"Editar" (icono en el detalle)
   └─> habitFormViewModel.loadHabit(habit) ← AppNavHost lo llama ANTES de navegar
       └─> navigate("edit_habit/{id}")
            └─> LaunchedEffect(habitId) { repository.getById(id) → loadHabit(it) }  ← segunda carga
                └─> HabitFormScreen(isEditing = true)
```

> [!warning] La edición carga el hábito dos veces
> `AppNavHost.onEdit` llama a `loadHabit(habit)`, y además `HabitFormScreen` tiene un `LaunchedEffect(habitId)` que consulta `habitRepository.getById(habitId)` y vuelve a llamar a `loadHabit()`. Es redundante: dos lecturas para el mismo dato. Detalle en [[Bugs]] (BUG-010).

> [!warning] `save()` perdía `createdAt` y `status` — **corregido**
> `HabitFormViewModel.save()` construía el modelo así:
> ```kotlin
> val habit = Habit(
>     id = _habitId.value ?: 0,
>     name = _name.value.trim(),
>     description = _description.value.trim().takeIf { it.isNotBlank() },
>     icon = _icon.value,
>     color = _color.value,
>     frequency = _frequency.value
> )
> ```
> **No copiaba `createdAt` ni `status`.** Como ambos tienen valor por defecto en el constructor (`LocalDate.now()` y `HabitStatus.ACTIVE`), cada edición:
>
> 1. **Ponía `createdAt` a hoy**, lo que hacía que el hábito saltase al principio de la lista (`ORDER BY createdAt DESC`) y **perdía su antigüedad visible**.
> 2. **Reactivaba un hábito archivado**, porque `@Update` sobrescribe toda la fila.
>
> Era el BUG-002 de [[Bugs]]. **Resuelto**: el ViewModel guarda ahora `_createdAt` y `_status`, `loadHabit()` los rellena, `clear()` los restablece y `save()` los pasa explícitos. Cubierto por `HabitFormViewModelTest`.

### `HabitDetailViewModel` — el hábito ya es estado observable

`feature/habit/HabitDetailViewModel.kt`

```kotlin
private val _habit = MutableStateFlow<Habit?>(null)   // ← observable
private var _date: LocalDate? = null                 // ← sigue siendo un var
private val _history = MutableStateFlow<List<HabitCompletion>>(emptyList())
private var historyJob: Job? = null                  // ← BUG-003

val habit: StateFlow<Habit?> = _habit
val history: StateFlow<List<HabitCompletion>> = _history
```

**`_habit` pasó a ser observable.** Antes era un `var` normal rellenado por `initialize()`, y `getHabit()` lo devolvía tal cual: Compose no se enteraba de los cambios (BUG-004). La pantalla ahora lo recolecta con `collectAsStateWithLifecycle()`.

**`_date` sigue siendo un `var` normal**, porque solo lo consume `toggleCompletion()`, que es `suspend` y se invoca desde el NavHost: no hay ninguna lectura en composición que lo disparase.

**El colector de historial ya no se acumula.** `observeHistory()` cancela el trabajo anterior:

```kotlin
historyJob?.cancel()
historyJob = viewModelScope.launch { … }
```

Ver BUG-003 de [[Bugs]].

**Inicialización imperativa.** El ViewModel no se puede construir con datos: hay que llamar a `initialize(habit, date)` **antes** de navegar. Lo hace `AppNavHost.onHabitClick`:

```kotlin
onHabitClick = { habit, date ->
    habitDetailViewModel.initialize(habit, date)
    navController.navigate("habit_detail/${habit.id}")
}
```

Como se relanza en cada `initialize()`, ahora cancela el colector anterior, de modo que **siempre hay exactamente uno**.

Además, `HabitDetailScreen` relee el hábito por id en un `LaunchedEffect(habitId)`, igual que hace el formulario. Eso cubre la restauración del back stack y, de paso, el caso de volver de editar y ver el nombre antiguo. `initialize()` sigue siendo necesaria para fijar `_date`.

**Los métodos `suspend` son para el NavHost, no para la pantalla.** `toggleCompletion()` y `archive()` son `suspend` porque `AppNavHost` los invoca desde su propio `rememberCoroutineScope()`:

```kotlin
onToggleCompletion = { date -> scope.launch { habitDetailViewModel.toggleCompletion() } }
onArchive = { scope.launch { habitDetailViewModel.archive() }; navController.popBackStack() }
```

> [!note] El parámetro `date` de `onToggleCompletion` se descarta
> `HabitDetailScreen` declara `onToggleCompletion: (LocalDate) -> Unit` y lo llama con `LocalDate.now()`. Pero `AppNavHost` **ignora ese parámetro**: llama a `habitDetailViewModel.toggleCompletion()`, que usa el `_date` fijado en `initialize()`. El parámetro existe en la firma y no tiene efecto.

`isCompletedForDate(date)` (línea 53) es un método público que **nadie llama** — ver BUG-009.

### Coroutines y su alcance en la app

| Ámbito | Dónde se crea | Para qué |
|---|---|---|
| `viewModelScope` | Automático en los ViewModel | `HomeViewModel.stateIn`, `observeHistory` |
| `rememberCoroutineScope()` | `AppNavHost` | Lanzar escrituras desde los callbacks de navegación |
| `rememberCoroutineScope()` | `HomeScreen` | Creado en la línea 71 pero **nunca usado** |
| `rememberCoroutineScope()` | `HabitFormScreen` | `scope.launch { viewModel.save(); onSave() }` |

Patrón consistente y correcto: **los Composables nunca llaman a un caso de uso `suspend` directamente**; lo hacen dentro de `scope.launch`, y la actualización de UI llega por el `Flow` de Room.

Excepción: `HabitFormScreen.save()` sí invoca casos de uso, pero lo hace dentro de `rememberCoroutineScope()`, que es el patrón correcto.

### Colección de estado en las pantallas

Todas las pantallas usan la misma API:

```kotlin
val habits by habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
```

`collectAsStateWithLifecycle()` (de `androidx.lifecycle:lifecycle-runtime-compose`) **observa el ciclo de vida**: la recolección se detiene cuando la pantalla está en segundo plano. Es la elección correcta frente a `collectAsState()` a secas.

> [!warning] Los parámetros `initialValue = emptyList()` ocultan el estado de carga
> No hay estado de carga ni de error en ninguna parte de la app. Cuando la base de datos aún no ha emitido, las pantallas dibujan la lista vacía, que es **visualmente indistinguible** de "no tienes hábitos". Afecta a `HomeScreen` (lista) y a `HabitDetailScreen` (historial).

### Doble carga de datos en `HomeScreen`

`HomeScreen` combina **dos fuentes distintas** para construir una sola vista:

```kotlin
val habits by habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())     // del ViewModel
val completionsFlow = remember { completionRepository.getForDate(today) }          // del repositorio
val completions by completionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
val completedHabitIds = remember(completions) {
    completions.filter { it.completed }.map { it.habitId }.toSet()
}
```

Dos consecuencias:

1. **Fuga de capas:** la pantalla recibe `HabitCompletionRepository` —una interfaz de `domain/repository/`— directamente como parámetro, desde `AppNavHost`. Es la excepción documentada en [[Arquitectura]].
2. **La fecha se congela:** `today` se calcula con `LocalDate.now()` y `remember { … }` no tiene claves. Si la app queda abierta al cruzar medianoche, sigue consultando el día anterior y los toggles se escriben con la fecha obsoleta. Es el BUG-005 de [[Bugs]].

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

| # | Pendiente | Ubicación |
|---|---|---|
| 1 | Fecha congelada en la pantalla principal (BUG-005) | `HomeScreen.kt:61,64` |
| 2 | Doble carga del hábito en modo edición (BUG-010) | `AppNavHost.kt:76` + `HabitFormScreen.kt:67` |
| 3 | `onToggleCompletion(date)` recibe un parámetro que se ignora | `AppNavHost.kt:83-85` |
| 4 | `isCompletedForDate()` nunca se usa | `HabitDetailViewModel.kt:53` |
| 5 | `scope` y `snackbarHostState` creados y no usados | `HomeScreen.kt:70-71` |
| 6 | Sin estado de carga ni de error en ninguna pantalla | Las 3 pantallas |
| 7 | Ningún ViewModel recibe `SavedStateHandle`; `initialize()` sigue siendo obligatorio | `HabitDetailViewModel.kt` |

### Mejoras recomendadas (no decididas)

- **Modelar el estado de la pantalla principal con una sealed interface** (`Loading` / `Success` / `Empty` / `Error`) en lugar de `emptyList()` como valor inicial. Es lo que permitiría distinguir "cargando" de "sin hábitos" sin cambiar la UI.
- **Usar `flatMapLatest` o guardar el `Job` del colector** en `observeHistory()` para cancelar el colector anterior. `flatMapLatest` resuelve BUG-003 en dos líneas.
- **Un único `StateFlow` con un `data class`** en lugar de siete `StateFlow` en `HabitFormViewModel`. Menos recolectores y una imagen consistente del formulario.
- **Añadir `SavedStateHandle`** a los ViewModel y navegar solo con el `habitId`, eliminando `initialize()` y el parámetro `habit` del callback. Simplifica `AppNavHost`: hoy sigue siendo obligatorio porque `_date` se fija ahí.
- **Extraer `GetCompletionsForDateUseCase`** para que `HomeScreen` deje de recibir un repositorio (cierra la fuga de capas).

## Preguntas abiertas

- ¿Se acepta el patrón de "ViewModel compartido entre destinos" o se prefiere un ViewModel por destino con `viewModel()` de navigation-compose? Hoy obliga a inicializar antes de navegar, lo que genera los bugs 003 y 004.
- ¿Los dos Screen de hábito deberían compartir ViewModel o tener instancias separadas para creación y edición? Hoy comparten, y el `clear()` explícito en el FAB es la contraprestación.
- ¿Debe existir un estado de error? Hoy cualquier excepción de Room se propagaría al `viewModelScope` y **se cancelaría el scope entero**, sin surfacing al usuario.
- ¿Se quiere soporte de reloj de 12/24 horas o de zonas horarias distintas? `DateConverter` usa siempre `ZoneId.systemDefault()`.