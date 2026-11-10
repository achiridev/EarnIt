# Testing

## Propósito

Documentar **qué pruebas existen realmente**, cómo ejecutarlas, qué verifican y — sobre todo — **qué partes del proyecto no tienen ninguna cobertura**. Para los fallos conocidos ver [[Bugs]].

## Estado actual

Resumen verificado contra los artefactos de la última ejecución (`app/build/test-results/` y `app/build/outputs/androidTest-results/`):

| Tipo | Archivos | Tests | Resultado |
|---|---|---|---|
| **Unitario (JVM)** | 5 | **20** | ✅ 20 pasan |
| **Instrumentado (dispositivo)** | 3 | **6** | ✅ 6 pasan en emulador |
| **Integración** | 0 | 0 | — |
| **UI (Compose)** | 2 de los 3 instrumentados | 5 | ✅ Pasan |

**Cobertura efectiva: baja, pero ya no nula.** El único test unitario original sigue verificando que `2 + 2 == 4`. Las dependencias de testing **sin usar** son ahora cuatro de cinco: `mockito-core`, `turbine`, `robolectric` y `room-testing`. `kotlinx-coroutines-test` **ya se usa**, en `HabitFormViewModelTest` y `HomeViewModelTest`.

## Información encontrada

### Pruebas unitarias

`app/src/test/java/com/example/earnit/ExampleUnitTest.kt` — archivo íntegro:

```kotlin
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
```

Es la prueba de ejemplo que genera Android Studio. **No prueba nada del proyecto.**

Resultado de la última ejecución (`app/build/test-results/testDebugUnitTest/TEST-com.example.earnit.ExampleUnitTest.xml`):

```xml
<testsuite name="com.example.earnit.ExampleUnitTest" tests="1" skipped="0" failures="0" errors="0" />
```

### Pruebas instrumentadas

#### `ExampleInstrumentedTest`

`app/src/androidTest/java/com/example/earnit/ExampleInstrumentedTest.kt` — andamiaje de Android Studio. Comprueba que el nombre del paquete de la app bajo prueba es `com.example.earnit`.

```kotlin
@Test
fun useAppContext() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.example.earnit", appContext.packageName)
}
```

#### `HabitFormScreenTest` — el único test con valor real

`app/src/androidTest/java/com/example/earnit/HabitFormScreenTest.kt`. Es un **test de regresión** y su KDoc explica exactamente qué protege:

> Regression test for the crash: `java.lang.IllegalArgumentException: LazyHorizontalGrid's height should be bound by parent` — IconPicker's grid previously had no fixed height while being hosted inside a `Column` with `verticalScroll`, so the first frame of the habit form crashed the app.

Qué hace:

1. Construye un `HabitFormViewModel` con un `FakeHabitRepository` en memoria (implementación local de `HabitRepository`, definida al final del mismo archivo).
2. Monta `HabitFormScreen` con `createComposeRule()` y `habitId = null`, `habitRepository = null`.
3. Afirma que el nodo con el texto `"Nombre del Höhe del hábito"` está visible.

El test **no necesita base de datos**: `FakeHabitRepository` devuelve `flowOf(habits.toList())` y una `MutableList` en memoria. Eso lo hace rápido y determinista.

> [!important] No elimines la altura acotada de `IconPicker`
> Este test existe **precisamente** para protegerla. Si se quita el `.height(iconGridHeight)` de `LazyHorizontalGrid`, la app vuelve a hacer crash en el primer fotograma del formulario y este test falla. Ver [[Componentes]].

> [!note] El test no cubre el resto del formulario
> Solo verifica que el texto del campo de nombre se dibuja. No prueba el selector de iconos, el de color, el guardado, ni la validación.

#### `HabitDetailScreenTest` — BUG-001 y BUG-004

`app/src/androidTest/java/com/example/earnit/HabitDetailScreenTest.kt`. Archivo nuevo. Dos tests:

1. **`backArrow_notifiesOnBack`** — monta el detalle, pulsa el nodo con `contentDescription = "Volver"` y comprueba que el callback se invoca una vez. Es la regresión de BUG-001, cuyo `onClick` estaba vacío.
2. **`habitIsLoadedFromTheRepositoryById`** — monta el detalle **sin** llamar a `initialize()`, con un repositorio que resuelve un único hábito, y comprueba que el nombre y la descripción se dibujan. Cubre BUG-004.

Usa fakes escritos a mano en el propio archivo (`FakeHabitCompletionRepository`, `SingleHabitRepository`, `FakeDateProvider`). El nombre del hábito aparece **dos veces** en pantalla —barra superior y tarjeta—, así que se usa `onAllNodesWithText(...).onFirst()`; la descripción solo existe en la tarjeta, y es la que demuestra que la carga funcionó.

#### `HabitFormViewModelTest` — BUG-002 y el estado derivado

`app/src/test/java/com/example/earnit/HabitFormViewModelTest.kt`. Archivo nuevo, y **primer test unitario del proyecto que prueba algo real**. Tres tests:

1. **`save_whenEditing_preservesCreatedAtAndStatus`** — carga un hábito con `createdAt = 2020-01-01` y `status = ARCHIVED`, cambia el nombre, guarda y verifica que se conservan. Es la regresión de BUG-002, el bug con consecuencias de datos.
2. **`save_whenCreating_storesTheHabitAsActive`** — verifica el camino de creación: llama a `insert`, no a `update`, y deja `status = ACTIVE`.
3. **`isValid_tracksTheNameIgnoringSurroundingSpaces`** — comprueba que `"   "` no habilita y `"  Leer  "` sí.

> [!warning] `advanceUntilIdle()` es obligatorio entre lectura y escritura
> `isValid` es un `stateIn(…, SharingStarted.Eagerly)` sobre `viewModelScope`. Con `StandardTestDispatcher` el scope no se ejecuta hasta que se avanza el scheduler, así que `isValid.value` sigue valiendo el valor inicial. **Cada `updateName()`/`clear()` debe ir seguido de `advanceUntilIdle()`** antes de leer el valor. Sin eso el test falla con `assertTrue` sin motivo aparente.

Usa `Dispatchers.setMain()` porque `viewModelScope` necesita un dispatcher principal, y un `RecordingHabitRepository` que guarda lo que se le pasa a `insert` y a `update`.

#### `HomeViewModelTest` — estado por fecha

`app/src/test/java/com/example/earnit/feature/home/HomeViewModelTest.kt`. Tres tests con
fakes en memoria (`MutableDateProvider`, `FakeHabitRepository`, `FakeCompletionRepository`):

1. **`selectingPastDate_usesThatDaysCompletionsAndHidesHabitsCreatedLater`** — al seleccionar
   un día pasado, las completiones son las de ese día y los hábitos creados después no
   aparecen.
2. **`selectingFutureDate_isIgnored`** — `selectDate(today + 1)` no cambia la selección.
3. **`refreshToday_advancesTheSelectionOnlyIfItWasShowingToday`** — es la regresión de
   BUG-005: `refreshToday()` avanza la selección si el usuario veía "hoy" y la respeta si
   estaba consultando un día pasado.

Como los flujos usan `stateIn(viewModelScope, WhileSubscribed(5_000))`, cada test lanza un
colector en `backgroundScope` y llama a `advanceUntilIdle()` antes de leer el `StateFlow`.

#### `CalendarDatesTest` — cálculo de semanas y meses

`app/src/test/java/com/example/earnit/core/util/CalendarDatesTest.kt`. Cuatro tests sobre
`CalendarDates.kt`, que es Kotlin puro precisamente para poder verificarse sin dispositivo:

1. `weekDates_startOnTheConfiguredFirstDay` — la semana empieza en lunes o domingo según el
   primer día configurado.
2. `monthCells_padsTheRowsWithNulls` — febrero de 2026 con lunes como primer día deja seis
   huecos al principio.
3. `monthCells_handlesAMonthStartingOnTheFirstWeekDay` — junio de 2026 empieza en lunes y
   deja cinco huecos al final.
4. `weekDayLabels_followTheConfiguredFirstDay` — las etiquetas siguen el mismo orden.

### Comandos

```bash
# Todas las pruebas unitarias (JVM) — rápido, no necesita dispositivo
./gradlew test

# Solo la variante debug
./gradlew testDebugUnitTest

# Pruebas instrumentadas — REQUIERE dispositivo o emulador conectado
./gradlew connectedAndroidTest

# Ver el informe HTML de las unitarias
open app/build/reports/tests/testDebugUnitTest/index.html

# Ver el informe HTML de las instrumentadas
open app/build/reports/androidTests/connected/debug/index.html
```

El orden recomendado en el repositorio es `lintDebug` → `test` → `assembleDebug`; ver [[Build]].

> [!warning] `connectedAndroidTest` puede salir con código 1
> En una ejecución anterior `app/build/outputs/androidTest-results/connected/debug/test-result-exit-code.txt` contenía `1` pese a pasar los tests, por haber **dos dispositivos conectados**. Es BUG-006. Con un único emulador la tarea termina en `BUILD SUCCESSFUL`.

### Dispositivos usados en la última ejecución

Extraído de `app/build/outputs/androidTest-results/connected/debug/`:

| Identificador | Tipo | Resultado |
|---|---|---|
| `Medium_Phone(AVD) - 17` | Emulador | 2/2 tests pasan (`TEST-Medium_Phone(AVD) - 17.xml`) |
| `M2101K7BL - 13` | Dispositivo físico | Solo `device-info.pb`; sin resultados |

> [!note] No se documentan IPs ni puertos
> Los artefactos contienen una dirección de conexión ADB. No se reproduce aquí porque **pertenece al entorno de desarrollo, no al repositorio**. Ver [[Instalacion]] para el procedimiento genérico.

### Qué NO está probado

Este listado es el más útil del documento. **No hay ninguna prueba** de:

| Área | Sin cubrir |
|---|---|
| **Casos de uso** | `CreateHabitUseCase`, `UpdateHabitUseCase`, `ArchiveHabitUseCase`, `ToggleCompletionUseCase`, `GetTodaysHabitsUseCase`, `GetHabitHistoryUseCase`, `GetCompletionsForDateUseCase` — **0 tests directos** (algunos se ejercitan desde `HomeViewModelTest`) |
| **`DateConverter`** | `toEpochMillis` / `toLocalDate` — **0 tests**. Es la pieza que ya tuvo un bug real de zona horaria |
| **`RealDateProvider` / `DateProvider`** | **0 tests** |
| **Repositorios** | `HabitRepositoryImpl`, `HabitCompletionRepositoryImpl` — **0 tests** |
| **DAO / Room** | `HabitDao`, `HabitCompletionDao` — **0 tests**. `room-testing` está declarada y sin usar |
| **`Converters`** | **0 tests**. `Frequency.valueOf` lanza excepción ante un valor desconocido y eso no está verificado |
| **`AppDatabase`** | **0 tests**. Nadie verifica que la migración destructiva funciona |
| **`HomeViewModel`** | **3 tests**. Selección de días pasados/futuros, filtro por `createdAt` y refresco de "hoy" (BUG-005) |
| **`HabitFormViewModel`** | **3 tests**. Cubren la preservación de `createdAt`/`status` al editar (BUG-002) y el estado derivado `isValid` |
| **`HabitDetailViewModel`** | **1 test indirecto**. `habitIsLoadedFromTheRepositoryById` lo monta sin `initialize()`; el `Job` del colector (BUG-003) no tiene test propio |
| **`HomeScreen`** | **0 tests** |
| **`HabitDetailScreen`** | **2 tests**. La flecha "Volver" (BUG-001) y la carga por id (BUG-004) |
| **`getIcon`** | **0 tests**. El fallback silencioso no está verificado |
| **`IconPicker` / `ColorPicker`** | Solo `IconPicker` indirectamente, por el test del formulario. El selector de color libre y el diálogo **no tienen ninguna prueba** |

### Sí está probado: `HabitColors`

`app/src/test/java/com/example/earnit/core/ui/components/HabitColorsTest.kt` — **9 pruebas locales**. Es el único bloque de lógica de UI con cobertura real, y existe porque `HabitColors.kt` se escribió deliberadamente **sin Compose** para que `toHsv`, `hsvToColorInt`, `accentColor`, `isLightColor` y `toHexString` fueran verificables en `./gradlew test`, sin dispositivo.

Lo que fija:

| Prueba | Qué garantiza |
|---|---|
| `hsv_roundTripReproducesTheOriginalColor` | `toHsv` y `hsvToColorInt` son inversos exactos sobre **todas** las muestras de la paleta y de los colores heredados. Si alguien toca la conversión, el diálogo abriría sobre un color distinto |
| `toHsv_reportsTheExpectedComponents` | El matiz, la saturación y el valor del azul puro salen 240 / 1 / 1 |
| `hsvToColorInt_alwaysReturnsAnOpaqueColorInRange` | Un matiz o saturación fuera de rango se recorta, nunca lanza |
| `accentColor_darkensThePaletteUntilTheyAreLegible` | Los 13 colores de la paleta se oscurecen de verdad: **protege el umbral de 0.45**, que un color nuevo por debajo dejaría sin oscurecer |
| `accentColor_keepsColorsThatAreAlreadyDarkEnough` | Los 12 colores que la app ya usaba **no cambian**: ningún hábito guardado altera su aspecto |
| `isLightColor_agreesWithTheLuminanceThreshold` | Umbral de 0.5, para que el `Check`_negro o blanco siga siendo legible |
| `toHexString_formatsSixDigits` | Formato `#RRGGBB` |
| `palette_hasThirteenDistinctColors` | La paleta tiene 13 muestras y ninguna está duplicada |
| `defaultColorIsPartOfThePalette` | `DEFAULT_HABIT_COLOR` es una de las 13 muestras: el formulario nunca arranca con un color que no se vea marcado |

> [!warning] Un test rojo aquí es una regresión visual
> Si `accentColor_keepsColorsThatAreAlreadyDarkEnough` falla, significa que un color que la app ya usaba va a cambiar de aspecto para todos sus usuarios. Es el test que protege esa decisión de umbral. Los colores heredados viven **dentro del archivo de test**, no en `HabitColors.kt`: el selector ya no los ofrece, pero pueden estar guardados en hábitos de usuarios anteriores.

### Dependencias de testing declaradas y su uso real

| Dependencia | Versión | ¿Se usa? | Nota |
|---|---|---|---|
| `junit` | 4.13.2 | ✅ Sí | Los 5 archivos de test unitario |
| `kotlinx-coroutines-test` | 1.8.1 | ✅ **Sí** (desde 2026-10-10) | `HabitFormViewModelTest`, `HomeViewModelTest` |
| `mockito-core` | 5.18.0 | ❌ **No** | Solo se usa un `Fake` escrito a mano |
| `turbine` | 1.0.0 | ❌ **No** | La herramienta ideal para los `Flow` de los repositorios |
| `robolectric` | 4.11 | ❌ **No** | Permite tests de Room sin dispositivo |
| `androidx.room:room-testing` | 2.7.2 | ❌ **No** | Para probar migraciones |
| `androidx.compose.ui:ui-test-junit4` | vía BOM | ✅ Sí | `createComposeRule()` |
| `androidx.test.ext:junit` | 1.3.0 | ✅ Sí | `@RunWith(AndroidJUnit4::class)` |
| `espresso-core` | 3.7.0 | ❌ **No** | La app no tiene widgets ni vistas |
| `ui-test-manifest` | vía BOM | ✅ (debug) | Necesario para `createComposeRule` |

> [!warning] `AGENTS.md` afirma que estas herramientas están en uso
> Dice: *"Mockito es usado para mocking; Robolectric para Android context simulation; Turbine para Flow testing"*. **No es correcto**: ninguna se usa. La afirmación queda registrada como discrepancia en [[Bugs]] (BUG-011).
>
> También afirma que `Turbine` debe quedarse en `1.0.0` porque `1.0.1` no existe en Maven Central. Esa restricción **sí es verificable** y conviene respetarla si algún día se añade la dependencia.

### Verificación de la configuración de tests

| Aspecto | Estado |
|---|---|
| `testInstrumentationRunner` | `androidx.test.runner.AndroidJUnitRunner` ✅ |
| `testOptions { unitTests.isIncludeAndroidResources } | **No configurado.** Con Robolectric declarado pero sin usar, esto aún no ha afectado |
| `testOptions { unitTests.isReturnDefaultValues }` | **No configurado** |
| `robolectric.properties` / `@Config` | **No existe**. Robolectric usaría el `targetSdk` por defecto |
| Etiquetas o filtros de test | Ninguno |

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

1. **`connectedAndroidTest` sale con código 1** cuando hay más de un dispositivo conectado (BUG-006).
2. **`AGENTS.md` describe herramientas de testing que no se usan** (BUG-011) — solo se ha corregido parcialmente: `kotlinx-coroutines-test` ya se usa, pero Mockito, Turbine y Robolectric siguen sin usarse.
3. **No hay `testOptions` configurado**, lo que limita las opciones de test unitario con recursos.
4. Las dependencias `mockito-core`, `turbine`, `robolectric` y `room-testing` **no se usan** (BUG-009).

### Mejoras recomendadas (no decididas)

Ordenadas por relación esfuerzo/beneficio. Las tres primeras ya están hechas:

1. ~~**`DateConverterTest`**~~ — sigue pendiente; es la pieza que ya rompió la app una vez.
2. **Tests de los casos de uso con `kotlinx-coroutines-test` y Turbine.** Son funciones puras sobre interfaces; no necesitan Android ni base de datos. Cubren la lógica de alternancia de `ToggleCompletionUseCase` y la transición de estado de `ArchiveHabitUseCase`.
3. ~~**Test de regresión para BUG-002**~~ — **hecho** (`HabitFormViewModelTest`).
4. **Tests de DAO con `room-testing` + Robolectric.** Verifican las consultas reales (`status = 'ACTIVE'`, el `ORDER BY`, la clave compuesta) sin necesidad de dispositivo.
5. **Tests de `Converters`**, en particular el comportamiento de `Frequency.valueOf` ante un valor desconocido.
6. **Tests de UI para `HomeScreen`**, siguiendo el patrón ya establecido. `HabitDetailScreen` está cubierto; `HomeScreen` es el hueco que queda.
7. **Test del `Job` de `observeHistory()`** (BUG-003): comprobar que abrir dos detalles seguidos deja un solo colector.
8. **Configurar `testOptions`** si se adopta Robolectric.

### Eliminar o justificar las dependencias sin uso

Cada dependencia sin usar es superficie de mantenimiento y riesgo de resolución. Alternativas: usarlas (tests reales) o retirarlas.

## Preguntas abiertas

- ¿Se adoptará Robolectric? Es útil para probar Room sin dispositivo, pero añade complejidad y requiere configurar `testOptions`. Si la estrategia es probar los casos de uso en la JVM y dejar Room para instrumentado, podría sobrar.
- ¿Se quiere un umbral de cobertura? Hoy no hay ningún plugin de cobertura (`jacoco` / `kover`) declarado.
- ¿Debería `connectedAndroidTest` filtrar por dispositivo para evitar el fallo con varios conectados?
- ¿Se ejecutan las pruebas en integración continua? **No hay configuración de CI/CD en el repositorio**, así que las pruebas dependen de que alguien las lance manualmente (ver [[Build]]).
- ¿Cuál es la prioridad: cubrir la lógica de dominio (casos de uso, fechas) o la de persistencia (DAO, migraciones)? La primera es mucho más barata de probar.