# Arquitectura

## Propósito

Explicar **cómo está organizado el código y cómo fluyen los datos** de la base de datos a la pantalla. Es el documento central del proyecto: antes de tocar cualquier capa conviene leerlo completo.

Para el detalle de cada pieza ver [[EstadoYViewModels]], [[Persistencia]] y [[Navegacion]]. Para el porqué de las decisiones ver [[DecisionesTecnicas]].

## Estado actual

Arquitectura **Clean Architecture de tres capas**, implementada de forma real y consistente en su mayor parte. No es una aspiración: los límites existen y se ven en el código.

| Capa | Paquete | Depende de |
|---|---|---|
| Dominio | `domain/` | Nada. Kotlin puro. |
| Datos | `data/` | `domain/` + Room |
| Presentación | `feature/`, `core/ui/` | `domain/`… y `data/` en dos casos |

## Información encontrada

### Las tres capas

#### 1. Dominio (`domain/`) — Kotlin puro, sin dependencias de Android

```
domain/
├── model/       Habit, HabitCompletion, Frequency, HabitStatus
├── repository/  HabitRepository, HabitCompletionRepository  (interfaces)
└── usecase/     7 casos de uso
```

Reglas que se cumplen:
- Ningún archivo de `domain/` importa nada de `android.*`, `androidx.*` ni `java.time.ZoneId`. Solo `java.time.LocalDate` y `kotlinx.coroutines.flow.Flow`.
- Los repositorios son **interfaces** definidas en el dominio; las implementaciones viven en `data/`. Esto permite invertir la dependencia: el dominio no sabe que existe Room.
- Los casos de uso son clases con un único `operator fun invoke(...)`, sin estado.

Los 7 casos de uso y su firma:

| Caso de uso | Firma | Qué hace |
|---|---|---|
| `CreateHabitUseCase` | `suspend operator fun invoke(habit: Habit): Long` | Inserta y devuelve el id generado |
| `UpdateHabitUseCase` | `suspend operator fun invoke(habit: Habit)` | Actualiza por clave primaria |
| `ArchiveHabitUseCase` | `suspend operator fun invoke(habitId: Long)` | Lee, y solo si está `ACTIVE`, pasa a `ARCHIVED` |
| `GetTodaysHabitsUseCase` | `operator fun invoke(): Flow<List<Habit>>` | Resuelve "hoy" con `DateProvider` y filtra |
| `GetCompletionsForDateUseCase` | `operator fun invoke(date: LocalDate): Flow<List<HabitCompletion>>` | Completiones de un día concreto |
| `ToggleCompletionUseCase` | `suspend operator fun invoke(habitId: Long, date: LocalDate? = null): Boolean` | Alterna `completed` y persiste |
| `GetHabitHistoryUseCase` | `operator fun invoke(habitId: Long): Flow<List<HabitCompletion>>` | Historial completo del hábito |

> [!note] Convención de suspending
> Solo son `suspend` las operaciones que **escriben o leen un valor puntual** (`insert`, `update`, `delete`, `getById`, `upsert`, `getForHabitAndDate`). Las que **devuelven un `Flow`** son métodos normales no suspendidos, porque la lectura es laxo. Esta convención se respeta en toda la cadena: DAO → repositorio → caso de uso.

#### 2. Datos (`data/`) — el único lugar que conoce Room

```
data/
├── local/
│   ├── dao/        HabitDao, HabitCompletionDao
│   ├── entity/     HabitEntity, HabitCompletionEntity
│   └── database/   AppDatabase, Converters
└── repository/     HabitRepositoryImpl, HabitCompletionRepositoryImpl
```

Los repositorios implementan las interfaces del dominio aplicando el mapeo `Entity` ↔ modelo. El mapeo es **explícito y privado**:

- `HabitRepositoryImpl.toEntity()` / `toDomain()` (líneas 52 y 65)
- `HabitCompletionRepositoryImpl.toEntity()` / `toDomain()` (líneas 42 y 51)

Único cambio de tipo en el mapeo: `createdAt` es `LocalDate` en el dominio y `Long` (epoch ms) en la entidad. `frequency` y `status` no cambian de tipo; los traduce `Converters`.

Detalle completo en [[Persistencia]].

#### 3. Presentación (`feature/`, `core/ui/`) — Compose

```
feature/
├── home/    HomeScreen, HomeViewModel
└── habit/   HabitDetailScreen, HabitDetailViewModel, HabitFormScreen, HabitFormViewModel
```

Cada *feature* agrupa pantalla y ViewModel en el mismo paquete. Los componentes reutilizables entre pantallas viven aparte, en `core/ui/components/`; la lógica pura de calendario (semanas y cuadrícula mensual), en `core/util/CalendarDates.kt`.

### Flujo de datos: lectura y escritura

**Lectura (reactiva, con `Flow`)** — dos cadenas que convergen en `HomeViewModel`

```text
Room (InvalidationTracker)
      ↓  Flow<List<HabitEntity>>
HabitDao.getActiveHabits()
      ↓  .map { toDomain(it) }
HabitRepositoryImpl.getActiveHabitsForDate()
      ↓
GetTodaysHabitsUseCase()                (devuelve activos; no filtra por fecha)
      ↓  .combine(selectedDate) { filtra createdAt ≤ día }
HomeViewModel.habits : StateFlow<List<Habit>>

Room (InvalidationTracker)
      ↓  Flow<List<HabitCompletionEntity>>
HabitCompletionDao.getForDate(date)
      ↓  .map { toDomain(it) }
HabitCompletionRepositoryImpl
      ↓
GetCompletionsForDateUseCase(date)
      ↓  .flatMapLatest sobre `selectedDate`
HomeViewModel.completions : StateFlow<List<HabitCompletion>>
      ↓  collectAsStateWithLifecycle()
HomeScreen → recomposición
```

La cadena es unidireccional y sin sorpresas: la base de datos es la única fuente de verdad, y cualquier cambio propaga solo hasta la pantalla.

**Escritura (unidireccional, desde los callbacks)**

```text
toque del usuario en HabitItem.onToggle
      ↓
HomeScreen onToggleCompletion(habitId, selectedDate)   ← lambda, no ViewModel
      ↓
AppNavHost: scope.launch { toggleCompletionUseCase(habitId, date) }
      ↓
ToggleCompletionUseCase → HabitCompletionRepository.upsert()
      ↓
HabitCompletionDao.upsert()  (@Insert REPLACE sobre clave compuesta)
      ↓
Room invalida el Flow → la pantalla se actualiza sola
```

Detalle importante: **la escritura no actualiza estado localmente**. No hay `MutableStateFlow` de "optimistic update"; la UI espera a que Room notifique el cambio. Eso elimina de raíz las inconsistencias entre UI y datos.

### Puntos de entrada y cableado

El cableado es **totalmente manual** (ver [[ADR-0001]]):

```text
EarnItApplication.onCreate()
      ↓  new AppContainer(this)
      ↓
MainActivity  ── by viewModels { HomeViewModelFactory(...) }          (3 ViewModels)
      ↓  setContent { EarnItTheme { Scaffold { AppNavHost(...) } } }
      ↓
AppNavHost    ── rememberNavController() + rememberCoroutineScope()
      ↓
HomeScreen / HabitFormScreen / HabitDetailScreen
```

`AppContainer` (`core/di/AppContainer.kt`) construye todo el grafo en 35 líneas: base de datos, `DateProvider`, 2 repositorios y 7 casos de uso. Se expone como `lateinit var appContainer` en `EarnItApplication`.

`MainActivity` obtiene los tres ViewModel con `by viewModels { ... }`, cada uno con su factory de `core/di/ViewModelFactories.kt`.

### Matriz de dependencias reales

| Desde | Puede importar | ¿Se cumple? |
|---|---|---|
| `domain/` | `domain/`, `kotlinx.coroutines`, `java.time` | ✅ Sí, totalmente |
| `data/` | `domain/`, Room, `core/util` | ✅ Sí |
| `feature/` | `domain/`, `core/ui`, Compose | ⚠️ **Excepción**: `HabitFormScreen` importa `domain.repository.HabitRepository` y lo recibe como parámetro desde `AppNavHost` |

La pantalla principal ya no recibe repositorios: las completiones llegan por
`GetCompletionsForDateUseCase` y `HomeViewModel`. La única excepción que queda es la de
`HabitFormScreen`.

### Cómo añadir una funcionalidad

Procedimiento derivado de las capas anteriores. Cada paso cita su archivo real:

1. **Modelo** en `domain/model/` — el `data class` y sus enums, sin dependencias de Android.
2. **Interfaz de repositorio** en `domain/repository/` + **implementación** en `data/repository/`, con las conversiones `toEntity()` / `toDomain()` privadas.
3. **Caso de uso** en `domain/usecase/` — una clase con un único `operator fun invoke(...)`. `suspend` solo si escribe o lee un valor puntual; si devuelve un `Flow`, método normal no suspendido (ver [[Arquitectura#Información encontrada]]).
4. **Cablear** en `AppContainer` (`core/di/AppContainer.kt`).
5. **UI** en `feature/` y una ruta en `core/navigation/AppNavHost.kt`. Si hay ViewModel nuevo, **tres** archivos: `AppContainer`, `core/di/ViewModelFactories.kt` y `MainActivity`.
6. **Verificar** con `./gradlew lintDebug test assembleDebug` y actualizar los documentos afectados.

> [!note] El punto 5 es el que más se olvida
> Una pantalla nueva sin ViewModel propio salta `ViewModelFactories` y `MainActivity`. Si el ViewModel existe, `HabitFormViewModel` y `HabitDetailViewModel` son instancias compartidas a nivel de `Activity`, no por destino. Ver [[Navegacion]] y [[EstadoYViewModels]].

### Ausencias arquitectónicas notables

Para evitar buscar lo que no existe:

| Tecnología | ¿Presente? |
|---|---|
| Hilt / Dagger | **No.** Solo anotaciones `@javax.inject.Inject`, que ningún procesador consume |
| DataStore | **No** |
| WorkManager | **No** |
| Retrofit / OkHttp / cliente HTTP | **No** |
| kotlinx.serialization / Moshi | **No** |
| Múltiples módulos Gradle | **No.** Un solo `:app` |
| `Abstract SavedStateHandle` en ViewModels | **No.** Los ViewModel no reciben `SavedStateHandle` (consecuencia: BUG-004) |

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- El único `TODO` estructural: `TODO(frequency)` en `HabitRepositoryImpl.kt:38`, que documenta que `getActiveHabitsForDate(date)` **descarta su parámetro**. El filtrado por fecha no está implementado y no hay código de mapeo para ello.

### Mejoras recomendadas (no decididas)

- **Cerrar la fuga de capas que queda:** mover la carga del hábito de `HabitFormScreen` al `HabitDetailViewModel` (o a un caso de uso), eliminando el último parámetro de repositorio de la capa de presentación.
- **Añadir `SavedStateHandle` a los ViewModel** para sobrevivir a la muerte del proceso sin depender de un `initialize()` previo. Resolvería BUG-004 de raíz.
- **Acotar el alcance de los ViewModel.** `habitDetailViewModel` y `habitFormViewModel` se crean en el `Activity` y se comparten entre destinos; ver [[Navegacion]].

## Preguntas abiertas

- ¿Se prevé migrar a Hilt? El contenedor manual escala bien para 3 ViewModel, pero cada pantalla nueva exige tocar `AppContainer` **y** `ViewModelFactories` **y** `MainActivity`.
- ¿El paquete `core/ui/components/` va a crecer con componentes compartidos entre futuras pantallas, o conviene mover `IconPicker` y `ColorPicker` a `feature/habit/`? Hoy ambos solo los usa `HabitFormScreen`.
- ¿Debe `domain/` seguir siendo Kotlin puro? Es lo más sano para este tamaño, pero nada impide usar `SavedStateHandle` en los ViewModel sin tocar el dominio.
- ¿Se aceptará la dependencia en `HabitDetailScreen` de un `Habit` completo pasado por lambda desde el NavHost, en lugar de releerlo por id? Hoy el patrón obliga a inicializar el ViewModel a mano antes de navegar.