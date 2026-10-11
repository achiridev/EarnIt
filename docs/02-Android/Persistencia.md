# Persistencia

## Propósito

Documentar la **capa de persistencia local**: el esquema de Room, los DAOs, los conversores de tipos, el manejo de fechas y la política de migraciones. Es el documento a consultar antes de cambiar cualquier cosa que toque la base de datos.

Para las capas superiores ver [[Arquitectura]].

## Estado actual

| Aspecto | Estado |
|---|---|
| Motor | **Room 2.7.2** con KSP |
| Entidades | 2 (`habits`, `habit_completions`) |
| `version` del esquema | **1** (nunca ha cambiado) |
| `exportSchema` | **`false`** — no se generan esquemas JSON |
| Migraciones | **Ninguna**. `fallbackToDestructiveMigration()` |
| Operaciones reactivas | `Flow` en 5 consultas |
| Transacciones (`@Transaction`) | **Ninguna** |
| Índices adicionales | Solo los implícitos de las claves primarias |
| Clave foránea | **No declarada** entre `habit_completions.habitId` y `habits.id` |

## Información encontrada

### La base de datos

`app/src/main/java/com/example/earnit/data/local/database/AppDatabase.kt`

```kotlin
@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "earnit_database")
                    .fallbackToDestructiveMigration()
                    .build()
                .also { INSTANCE = it }
            }
}
```

- **Nombre del fichero**: `earnit_database`.
- **Patrón singleton** con doble comprobación: `@Volatile` + `synchronized(this)`. Correcto y seguro entre hilos.
- **Sin inyección**: se obtiene por `AppContainer`, que es quien llama a `getInstance(context)`. El acceso directo a `AppDatabase` fuera del contenedor no existe.
- El nombre real del fichero SQLite incluye un sufijo; `earnit_database` es el nombre base declarado.

### Esquema

#### Tabla `habits`

```kotlin
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val icon: String,
    val color: Int,
    val frequency: Frequency,     // ← persistido como TEXT (String)
    val createdAt: Long,          // epoch millis
    val status: HabitStatus       // ← persistido como TEXT (String)
)
```

| Tipo Kotlin | Tipo SQL (inferido por Room) | Notas |
|---|---|---|
| `Long` (PK, autoGenerate) | `INTEGER PRIMARY KEY AUTOINCREMENT` | `0` dispara la autogeneración |
| `String` | `TEXT NOT NULL` | |
| `String?` | `TEXT` | Nullable |
| `String` | `TEXT NOT NULL` | Clave de icono, p. ej. `"book"` |
| `Int` | `INTEGER NOT NULL` | Color ARGB empaquetado |
| `Frequency` (enum) | **`TEXT NOT NULL`** | Vía `Converters` |
| `Long` | `INTEGER NOT NULL` | Milisegundos desde epoch |
| `HabitStatus` (enum) | **`TEXT NOT NULL`** | Vía `Converters` |

**No hay `NOT NULL` explícito en `description`** porque Kotlin lo marca nullable; Room lo traduce correctamente.

#### Tabla `habit_completions`

```kotlin
@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"]
)
data class HabitCompletionEntity(
    val habitId: Long,
    val date: Long,      // epoch millis del inicio del día
    val completed: Boolean,
    val value: Double?
)
```

Clave primaria **compuesta** `(habitId, date)`. Es lo que garantiza que solo exista **un registro por hábito y día**, y es la razón de que el `upsert` use `REPLACE`.

> [!note] Sin clave foránea
> `habitId` **no declara `@ForeignKey`** ni `indices`. Borrar un hábito (posible vía `HabitRepository.delete()`, aunque sin UI) **no borra sus registros**: quedarían completaciones huérfanas. Hoy es inocuo porque nadie llama a `delete`, pero es una trampa para quien lo añada.

### Conversores de enum

`data/local/database/Converters.kt` — 4 métodos, dos pares:

```kotlin
@TypeConverter fun fromFrequency(value: String): Frequency = Frequency.valueOf(value)
@TypeConverter fun toFrequency(value: Frequency): String = value.name
@TypeConverter fun fromHabitStatus(value: String): HabitStatus = HabitStatus.valueOf(value)
@TypeConverter fun toHabitStatus(value: HabitStatus): String = value.name
```

Se persisten **por nombre** (`"DAILY"`, `"ACTIVE"`), no por ordinal. Es la elección correcta: reordenar el enum no corrompe los datos.

> [!warning] `valueOf` lanza excepción
> Si una fila contiene `"WEEKLY"` y el enum no declara ese valor, `valueOf` lanza `IllegalArgumentException` **al leer**, y con `fallbackToDestructiveMigration()` no hay versión anterior a la que recurrir. Renombrar o eliminar un valor del enum es una **ruptura de datos silenciosa** hasta que se lee la fila.

### Manejo de fechas: `DateConverter`

`app/src/main/java/com/example/earnit/core/util/DateConverter.kt`

```kotlin
fun LocalDate.toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
```

Dos funciones de extensión que **sustituyen a `DateConverter` como nombre de clase**: el archivo se llama `DateConverter.kt`, pero no contiene ninguna clase, solo funciones a nivel de fichero.

El KDoc del archivo registra una advertencia que se debe respetar:

> Do NOT convert back with `LocalDate.ofEpochDay(millis / 86_400_000)`: integer division shifts the date by one day in any non-UTC zone.

**Este error existió y fue corregido.** Las versiones anteriores de `HabitRepositoryImpl` y `HabitCompletionRepositoryImpl` usaban exactamente `LocalDate.ofEpochDay(entity.createdAt / 86400000)`, que:

- Asumía que el epoch millis estaba en **UTC**, cuando `toEpochMillis` lo guarda en **hora local del sistema**.
- Dividía entre `86_400_000` con división entera, perdiendo la parte de horas.

En cualquier zona que no sea UTC, el hábito creado a las 18:00 del lunes aparecía con fecha **domingo**. Ambos repositorios usan hoy `toEpochMillis()` / `toLocalDate()` de forma consistente.

> [!important] Regla para cualquier código nuevo
> **Nunca** construyas ni interpretes epoch millis a mano. Usa siempre las extensiones de `core/util/DateConverter.kt`. Son la única fuente de verdad sobre fechas en la app.

Nota de alcance: la conversión se hace con `ZoneId.systemDefault()` **en el momento de la llamada**. Si el usuario cambia de zona horaria, las fechas ya almacenadas siguen apuntando al instante original, que en la nueva zona puede caer en otro día. `ToggleCompletionUseCase` siempre usa `dateProvider.today()` (fecha local *actual*), por lo que no se rompe el flujo normal; el riesgo solo aparece con datos históricos y cambios de zona.

### `DateProvider`: la abstracción de "hoy"

```kotlin
interface DateProvider { fun today(): LocalDate }
class RealDateProvider : DateProvider {
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}
```

Existe **para poder inyectar una fecha falsa en tests**, y se usa en `GetTodaysHabitsUseCase` y `ToggleCompletionUseCase`.

> [!warning] La abstracción solo se respeta a medias
> `DateProvider` se usa en la capa de dominio, pero **`HomeScreen` y `HabitDetailScreen` llaman a `LocalDate.now()` directamente**, sin pasar por él. Ver BUG-005 en [[Bugs]]: la pantalla principal congela la fecha al componer.

### `HabitDao`

`data/local/dao/HabitDao.kt` — 8 métodos:

| Método | Tipo | Consulta | ¿Se usa? |
|---|---|---|---|
| `insert(habit)` | `suspend` | `@Insert(IGNORE)` → `Long` | ✅ `CreateHabitUseCase` |
| `update(habit)` | `suspend` | `@Update` | ✅ `UpdateHabitUseCase`, `ArchiveHabitUseCase` |
| `delete(habitId)` | `suspend` | `DELETE WHERE id = :habitId` | ⚠️ Solo vía repositorio, **sin UI** |
| `getById(habitId)` | `suspend` | `SELECT WHERE id = :habitId` | ✅ `HabitFormScreen` |
| `getActiveHabits()` | `Flow` | `WHERE status = 'ACTIVE' ORDER BY createdAt DESC` | ✅ `HomeScreen` |
| `getActiveHabitsList()` | `suspend` | Misma consulta, sin Flow | ❌ **Código muerto** |
| `getAllHabits()` | `Flow` | `ORDER BY createdAt DESC` | ❌ **Código muerto** |
| `getAllHabitsList()` | `suspend` | Misma consulta, sin Flow | ❌ **Código muerto** |

Detalles relevantes:

- `insert` usa **`OnConflictStrategy.IGNORE`**: si la clave primaria ya existe, no hace nada y devuelve `-1`. Con `id = 0` y autogeneración nunca ocurre, pero es un detalle a tener en cuenta si algún día se fuerza un id.
- `@Update` **sobrescribe la fila entera** por clave primaria. Es la causa de que editar un hábito losing `createdAt` y `status` (BUG-002).
- Las consultas filtran por `status = 'ACTIVE'` **comparando con el literal**, no con el parámetro del converter. Funciona porque el converter escribe el `name` del enum, pero es frágil ante un cambio de estrategia de conversión.

### `HabitCompletionDao`

`data/local/dao/HabitCompletionDao.kt` — 7 métodos:

| Método | Tipo | Consulta | ¿Se usa? |
|---|---|---|---|
| `upsert(completion)` | `suspend` | `@Insert(REPLACE)` | ✅ `ToggleCompletionUseCase` |
| `update(completion)` | `suspend` | `@Update` | ❌ **Código muerto** |
| `delete(habitId, date)` | `suspend` | `DELETE WHERE habitId AND date` | ⚠️ Solo vía repositorio, **sin UI** |
| `getForHabit(habitId)` | `Flow` | `WHERE habitId ORDER BY date DESC` | ✅ `HabitDetailScreen` |
| `getForHabitList(habitId)` | `suspend` | Misma, sin Flow | ❌ **Código muerto** |
| `getForDate(date)` | `Flow` | `WHERE date ORDER BY habitId` | ✅ `HomeScreen` |
| `getForHabitAndDate(...)` | `suspend` | `SELECT WHERE habitId AND date` | ✅ `ToggleCompletionUseCase` |

> [!note] `upsert` con `REPLACE` es lo correcto aquí
> Dado que la clave primaria es `(habitId, date)`, `REPLACE` hace que marcar y desmarcar el mismo día sea una única operación idempotente. Por eso `@Update` sobra: `REPLACE` ya cubre ambos casos.

### Mapeo Entity ↔ dominio

Los repositorios hacen el mapeo explícito con funciones privadas:

| Repositorio | `toEntity` | `toDomain` |
|---|---|---|
| `HabitRepositoryImpl` | líneas 52-63 | líneas 65-76 |
| `HabitCompletionRepositoryImpl` | líneas 42-49 | líneas 51-58 |

El único cambio de tipo es `createdAt` / `date` (`LocalDate` ↔ `Long`). `frequency` y `status` pasan sin tocar porque `Converters` los traduce en la frontera de Room.

> [!warning] `copyWith` no puede limpiar `description`
> `Habit.copyWith()` (`Habit.kt:24-38`) usa `description ?: this.description`. Pasar `description = null` **conserva el valor anterior**. No hay forma de borrar una descripción mediante `copyWith`. Ninguna ruta actual de la UI lo necesita, pero es una trampa para quien use `ArchiveHabitUseCase` u otras combinaciones de `copyWith`.
> `HabitFormViewModel.save()` no usa `copyWith`: reconstruye el modelo, y por eso pierde `createdAt` y `status`.

### `value: Double?` — un campo muerto

`HabitCompletionEntity.value` existe en la tabla, se persiste y se lee, pero:

- `ToggleCompletionUseCase` construye `HabitCompletion(habitId, date, completed = newCompleted)` **sin `value`** → siempre `null`.
- Ningún caso de uso escribe ni lee `value`.
- Ninguna pantalla lo muestra.

Es una columna preparada para una funcionalidad que no existe. No causa daño, pero es infraestructura speculativa.

### Migraciones: la política actual

```kotlin
.fallbackToDestructiveMigration()
```

Consecuencias, que conviene tener presentes:

1. **Cualquier cambio de esquema borra todos los datos del usuario.** No hay aviso ni copia previa.
2. **`exportSchema = false`** significa que **no se genera el esquema JSON** en `app/schemas/`. Sin historial de esquema no hay forma de escribir una `Migration` correcta a posteriori: habría que deducir el esquema anterior.
3. Con `version = 1` y ningún cambio todavía, **el riesgo aún no se ha materializado**.

Si alguna vez hay que cambiar el esquema, la secuencia correcta es: subir `exportSchema = true`, generar el esquema, escribir la `Migration`, y sustituir `fallbackToDestructiveMigration()` por `.addMigrations(...)`.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

| # | Pendiente | Ubicación |
|---|---|---|
| 1 | `TODO(frequency)`: `getActiveHabitsForDate(date)` **ignora `date`** porque `Frequency` solo tiene `DAILY` | `HabitRepositoryImpl.kt:38` |
| 2 | `exportSchema = false` — sin historial de esquema | `AppDatabase.kt:16` |
| 3 | `fallbackToDestructiveMigration()` — pérdida de datos en cualquier cambio de esquema | `AppDatabase.kt:33` |
| 4 | Sin clave foránea entre `habit_completions.habitId` y `habits.id` | `HabitCompletionEntity.kt` |
| 5 | `value: Double?` sin uso | `HabitCompletionEntity.kt:14` |
| 6 | Código muerto: `HabitDao.getActiveHabitsList()`, `getAllHabits()`, `getAllHabitsList()`, `HabitCompletionDao.update()`, `getForHabitList()` | varios |
| 7 | `HabitRepository.delete()` y `HabitCompletionRepository.delete()` sin ruta de UI | — |

### Mejoras recomendadas (no decididas)

- **Activar `exportSchema = true`** antes del primer cambio de esquema. Es barato ahora y costoso después.
- **Escribir un test de regresión para `DateConverter`.** Es la pieza con más historia de bugs del proyecto y hoy tiene **cero cobertura**. Un test en una zona no UTC habría atrapado el error original.
- **Añadir `indices = ["habitId"]`** en `HabitCompletionEntity`, porque `getForHabit` filtra por esa columna.
- **Declarar la clave foránea** con `onDelete = CASCADE`, para que borrar un hábito borre su historial de forma coherente.
- **Eliminar las variantes `suspend` de los DAO** (`getActiveHabitsList`, `getAllHabitsList`, `getForHabitList`): duplican las consultas sin aportan nada.
- **Extraer el nombre de la tabla a constantes** si se añaden más entidades.

## Preguntas abiertas

- ¿Debe `ARCHIVED` conservar los datos históricos? Hoy sí (el registro queda), pero la UI no permite verlos. ¿Es intencionado?
- ¿Se quiere una tabla de "ajustes" o "perfil"? No hay ninguna, y con `minSdk 26` se podría usar DataStore si hiciera falta.
- ¿Qué ocurre con las fechas si el usuario viaja y cambia de zona horaria? `DateConverter` usa siempre `ZoneId.systemDefault()` en el momento de la llamada.
- ¿Es aceptable perder todos los datos al cambiar el esquema, o la decisión de usar `fallbackToDestructiveMigration()` debería revisarse antes del primer release?
- ¿Se prevé una estrategia de exportación/importación? Hoy no hay ninguna, y `allowBackup="true"` es lo único que preserva los datos fuera del dispositivo (ver [[Permisos]]).