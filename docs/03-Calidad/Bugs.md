# Bugs

## Propósito

Registrar **fallos confirmados y verificables** del proyecto, con evidencia concreta que permita reproducirlos y comprobarlos. Para las carencias de cobertura ver [[Testing]].

> [!important] Criterio de inclusión
> Solo se registra aquí lo que se puede **demostrar leyendo el código** o **reproduciendo** con la evidencia recogida. Las mejoras potenciales, los riesgos y las observaciones de estilo van en la sección de pendientes al final de cada documento, o en [[Roadmap]]. **No todo defecto potencial es un bug.**

## Estado actual

| Severidad | Cantidad |
|---|---|
| 🔴 Crítico (pérdida o corrupción de datos) | 1 |
| 🟠 Alto (funcionalidad rota visible) | 4 |
| 🟡 Medio (degradación o incoherencia) | 3 |
| 🔵 Configuración / documentación | 7 |

**Total: 15 incidencias registradas.** Cinco corregidas: BUG-001, BUG-002, BUG-003, BUG-004 y BUG-014.

## Información encontrada

### BUG-001 — El botón "volver" de la pantalla de detalle no hace nada

| Campo | Valor |
|---|---|
| **Severidad** | 🟠 Alto |
| **Estado** | **Corregido** |
| **Ubicación** | `app/src/main/java/com/example/earnit/feature/habit/HabitDetailScreen.kt:73` |

**Descripción**

La flecha de "volver" de la `TopAppBar` de `HabitDetailScreen` tiene un `onClick` vacío. El botón se dibuja y responde al toque, pero no ejecuta ninguna acción.

```kotlin
navigationIcon = {
    IconButton(onClick = { /* navController.popBackStack() */ }) {
        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Volver")
    }
}
```

**Causa raíz**

La pantalla no recibe ningún callback de navegación atrás. Su firma es:

```kotlin
fun HabitDetailScreen(
    viewModel: HabitDetailViewModel,
    habitId: Long,
    onEdit: (Habit) -> Unit,
    onArchive: () -> Unit,
    onToggleCompletion: (LocalDate) -> Unit
)
```

No hay `onBack`, y `HabitDetailScreen` no conoce el `NavController` (por diseño, para mantener las pantallas puras). El comentario sugiere que en origen iba a ser `navController.popBackStack()`, pero el patrón de inyección por lambda nunca se completó para este caso.

**Pasos para reproducir**

1. Lanzar la app y crear al menos un hábito.
2. Tocar sobre el hábito para abrir su detalle.
3. Pulsar la flecha "←" de la barra superior.

**Esperado** — se vuelve a la pantalla principal.
**Observado** — no ocurre nada. Hay que usar el gesto del sistema o el botón físico atrás.

**Contraste**: el mismo botón en `HabitFormScreen.kt:83` **sí funciona**, porque recibe `onCancel: () -> Unit` y `AppNavHost` le pasa `popBackStack()`.

**Impacto**

Funcionalidad rota visible para el usuario. En un dispositivo con barra de navegación por gestos es el único indicador de "atrás" que hay en pantalla.

**Solución**

Añadir `onBack: () -> Unit` a la firma y conectarlo en `AppNavHost` con `navController.popBackStack()`.

**Resolución** (2026-10-10)

- **Causa final:** la pantalla no tenía ningún callback de navegación atrás; el `onClick` quedó vacío cuando se adoptó la inyección por lambda.
- **Cambio aplicado:** `HabitDetailScreen` recibe `onBack: () -> Unit`, conectado al `navigationIcon`; `AppNavHost` le pasa `navController.popBackStack()`.
- **Cómo se verifica:** `HabitDetailScreenTest.backArrow_notifiesOnBack`, y a mano en el emulador (del detalle a la lista con la flecha).

---

### BUG-002 — Editar un hábito reinicia su fecha de creación y reactiva los archivados

| Campo | Valor |
|---|---|
| **Severidad** | 🔴 Crítico |
| **Estado** | **Corregido** |
| **Ubicación** | `app/src/main/java/com/example/earnit/feature/habit/HabitFormViewModel.kt:61-68` |

**Descripción**

Al guardar un hábito existente, `HabitFormViewModel.save()` reconstruye el modelo **sin copiar `createdAt` ni `status`**, por lo que ambos adoptan sus valores por defecto del constructor.

```kotlin
val habit = Habit(
    id = _habitId.value ?: 0,
    name = _name.value.trim(),
    description = _description.value.trim().takeIf { it.isNotBlank() },
    icon = _icon.value,
    color = _color.value,
    frequency = _frequency.value
)
// createdAt y status NO se especifican → LocalDate.now() y HabitStatus.ACTIVE
```

**Causa raíz**

Dos efectos combinados:

1. `Habit.createdAt` tiene valor por defecto `LocalDate.now()` y `Habit.status` tiene `HabitStatus.ACTIVE` (`Habit.kt:21-22`).
2. `HabitDao.update()` usa `@Update`, que **sobrescribe la fila entera** de la tabla `habits` cuya clave primaria coincide.

El ViewModel no lee el hábito original antes de reconstruirlo: `loadHabit()` solo rellena los campos del formulario (`name`, `description`, `icon`, `color`, `frequency`) y **no guarda `createdAt` ni `status` en ningún sitio**.

**Pasos para reproducir**

*Para el efecto sobre `createdAt`:*

1. Crear un hábito hoy.
2. Abrir su detalle y pulsar "Editar".
3. Cambiar solo el nombre y guardar.
4. Volver a la lista principal.

**Esperado** — el hábito conserva su fecha de creación original y mantiene su posición en la lista.
**Observado** — `createdAt` pasa a ser hoy. Como la consulta de `HabitDao.getActiveHabits()` es `ORDER BY createdAt DESC`, el hábito edited **salta al principio de la lista**.

*Para el efecto sobre `status`:*

1. Crear un hábito y archivarlo desde su detalle (desaparece de la lista).
2. Editarlo… **no es posible por la UI actual**, porque no hay pantalla de hábitos archivados (ver BUG-012). El defecto es alcanzable en cuanto se añada esa pantalla, o mediante `UpdateHabitUseCase` desde código.

**Impacto**

- **Pérdida silenciosa de información**: la fecha de creación del hábito se pierde de forma permanente tras cualquier edición.
- Un hábito archivado puede volver a aparecer en la lista principal si se actualiza su fila.

**Relación con tests**

`HabitFormViewModel` **no tiene ningún test**. Un test que verificara que editar conserva `createdAt` y `status` habría atrapado esto. Ver [[Testing]].

**Solución**

Preservar los campos que no se editan: leer el hábito existente (o guardarlos en `loadHabit()`) y pasarlos explícitamente al construir el modelo.

**Resolución** (2026-10-10)

- **Causa final:** `loadHabit()` rellenaba solo los campos del formulario y no guardaba `createdAt` ni `status`, así que `save()` los heredaba de los valores por defecto del constructor.
- **Cambio aplicado:** `HabitFormViewModel` mantiene `_createdAt` y `_status` como `StateFlow`; `loadHabit()` los rellena, `clear()` los restablece y `save()` los pasa explícitos al construir el `Habit`.
- **Cómo se verifica:** `HabitFormViewModelTest.save_whenEditing_preservesCreatedAtAndStatus`, y a mano forzando `createdAt` a 2026-01-15 en la base del emulador: tras editar, el valor seguía en 2026-01-15.
- **Prueba añadida:** `HabitFormViewModelTest.save_whenCreating_storesTheHabitAsActive` cubre el camino de creación.

> [!note] El efecto sobre `status` sigue sin ser alcanzable por la UI
> No hay pantalla de hábitos archivados (BUG-012), así que el caso de resurrectar un archivado sigue sin poder provocarse desde la app. La corrección lo preventa para cuando esa pantalla exista.

---

### BUG-003 — El historial acumulado de colectores en `HabitDetailViewModel`

| Campo | Valor |
|---|---|
| **Severidad** | 🟠 Alto |
| **Estado** | **Corregido** |
| **Ubicación** | `app/src/main/java/com/example/earnit/feature/habit/HabitDetailViewModel.kt:26-38` |

**Descripción**

Cada vez que se abre el detalle de un hábito, `initialize()` lanza un **colector nuevo** que nunca se cancela. Todos ellos escriben en el mismo `_history`.

```kotlin
fun initialize(habit: Habit, date: LocalDate) {
    _habit = habit
    _date = date
    observeHistory(habit.id)
}

private fun observeHistory(habitId: Long) {
    viewModelScope.launch {                      // ← trabajo nuevo, nunca cancelado
        getHabitHistoryUseCase(habitId).collect { completions ->
            _history.value = completions
        }
    }
}
```

**Causa raíz**

- `viewModelScope.launch` crea un `Job` hijo que sobrevive hasta que se cancela o termina el `viewModelScope`.
- El `collect` sobre un `Flow` de Room **nunca termina** mientras el ViewModel viva.
- `observeHistory()` no guarda el `Job` devuelto, así que no hay forma de cancelarlo.

El ViewModel es además **compartido entre todos los destinos de detalle** (ver [[Navegacion]]), así que acumula colectores a lo largo de toda la sesión.

**Pasos para reproducir**

1. Crear los hábitos A y B.
2. Abrir el detalle de A (crea el colector 1).
3. Volver a la lista.
4. Abrir el detalle de B (crea el colector 2).
5. Marcar hoy el hábito A desde la lista principal.
6. Mirar el historial que se muestra en el detalle de B.

**Esperado** — el historial de B no cambia.
**Observado** — el colector de A vuelve a escribir en `_history` porque su `Flow` de Room se ha invalidado. El valor mostrado depende de **qué colector emita el último**, que no está garantizado.

Cuantos más hábitos se visiten en una sesión, más colectores vivos habrá.

**Impacto**

Datos mostrados incorrectos y trabajo de CPU y de base de datos innecesario y creciente.

**Solución**

Guardar el `Job` y cancelarlo antes de lanzar el siguiente, o usar `flatMapLatest` sobre un `MutableStateFlow<Long>` de `habitId`, que resuelve el problema en dos líneas.

**Resolución** (2026-10-10)

- **Cambio aplicado:** se optó por guardar el `Job`, por ser el cambio menor y no alterar la firma de `initialize()`.
- **Cómo se verifica:** al abrir un detalle distinto, `observeHistory()` cancela el colector anterior; queda siempre exactamente uno escribiendo en `_history`.

```kotlin
historyJob?.cancel()
historyJob = viewModelScope.launch { … }
```

---

### BUG-004 — La pantalla de detalle queda vacía si se restaura sin `initialize()`

| Campo | Valor |
|---|---|
| **Severidad** | 🟡 Medio |
| **Estado** | **Corregido** |
| **Ubicación** | `app/src/main/java/com/example/earnit/feature/habit/HabitDetailViewModel.kt:20-21` |

**Descripción**

El hábito que se muestra en el detalle **no es estado observable**. Es un campo normal que se rellena con `initialize()`, llamado manualmente por `AppNavHost` antes de navegar.

```kotlin
private var _habit: Habit? = null       // no es StateFlow
private var _date: LocalDate? = null   // no es StateFlow

fun getHabit(): Habit? = _habit         // la pantalla lo llama en cada recomposición
```

**Causa raíz**

Ningún ViewModel recibe `SavedStateHandle`, y el `habitId` de la ruta **no se usa para releer el hábito**. La única vía de datos es el objeto `Habit` completo, que viaja por el lambda `onHabitClick`:

```kotlin
onHabitClick = { habit, date ->
    habitDetailViewModel.initialize(habit, date)   // ← única inicialización
    navController.navigate("habit_detail/${habit.id}")
}
```

Si Android restaura el back stack directamente en `habit_detail/{id}` — porque el proceso murió y se recreó la pila de tareas — `initialize()` **nunca se llama**.

**Pasos para reproducir**

1. Abrir el detalle de un hábito.
2. Forzar el cierre del proceso de la app (comando de developer, o "no mantener actividades" en las opciones del desarrollador).
3. Volver a abrir la app.

**Esperado** — el detalle del hábito se muestra con normalidad.
**Observado** — la pantalla aparece con el título genérico "Hábito", **sin tarjeta de cabecera, sin nombre, sin color y sin icono**. El historial también queda sin fuente de datos.

**Contexto adicional**

`HabitFormScreen` **sí** tiene un mecanismo de recuperación (`LaunchedEffect(habitId)` que consulta el repositorio), pero `HabitDetailScreen` no lo tiene. La asimetría es la causa: el formulario puede sobrevivir, el detalle no.

**Solución**

Leer el hábito por `habitId` dentro del detalle (igual que hace el formulario) o inyectar `SavedStateHandle` en el ViewModel.

**Resolución** (2026-10-10)

- **Cambio aplicado:** se eligió la primera vía, la que ya usa `HabitFormScreen`, por ser la menor. `HabitDetailScreen` recibe `habitRepository` y relee el hábito en un `LaunchedEffect(habitId)`; además `_habit` pasó de `var` a `MutableStateFlow` expuesto como `StateFlow<Habit?>`.
- **Efecto secundario comprobado:** como el `LaunchedEffect` se relanza al reentrar en la pantalla, el detalle dejó de mostrar datos obsoletos al volver de editar.
- **Cómo se verifica:** `HabitDetailScreenTest.habitIsLoadedFromTheRepositoryById` monta el detalle **sin** llamar a `initialize()` y comprueba que el nombre y la descripción se dibujan.
- **Pendiente:** `initialize()` sigue siendo necesario para fijar `_date`, así que se conserva. La causa de fondo —que ningún ViewModel usa `SavedStateHandle`— sigue abierta.

---

### BUG-005 — La pantalla principal congela la fecha y muestra datos obsoletos al cruzar medianoche

| Campo | Valor |
|---|---|
| **Severidad** | 🟡 Medio |
| **Estado** | Confirmado por lectura de código |
| **Ubicación** | `app/src/main/java/com/example/earnit/feature/home/HomeScreen.kt:61,64` |

**Descripción**

`HomeScreen` calcula "hoy" una sola vez por composición y memoriza el `Flow` de completiones **sin claves**, de modo que nunca vuelve a consultar la fecha correcta.

```kotlin
val today = java.time.LocalDate.now()                              // línea 61

val completionsFlow = remember { completionRepository.getForDate(today) }   // línea 64
val completions by completionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
```

**Causa raíz**

- `remember { … }` sin claves se calcula **una vez** mientras la composición permanezca viva.
- Si la app queda abierta al cruzar medianoche, `today` sigue siendo el día anterior.
- El `Flow` sigue observando las completiones de la fecha antigua.
- Además, `onToggleCompletion(habit.id, today)` **escribe con esa misma fecha obsoleta**.

**Pasos para reproducir**

1. Dejar la app abierta en la pantalla principal antes de medianoche.
2. Mantener la app en primer plano y esperar a que pase medianoche.
3. Marcar un hábito.

**Esperado** — al marcar, el registro se guarda con la fecha de hoy y la lista refleja el nuevo día.
**Observado** — el registro se guarda con la fecha de ayer, y el estado mostrado corresponde al día anterior.

**Causa relacionada**: el mismo patrón aparece en `HabitDetailScreen.kt:61` con `val today = LocalDate.now()`, y ese `today` se usa para calcular `isCompletedToday` mientras que el toggle usa el `_date` del ViewModel. **Son dos referencias al mismo día que pueden divergir.**

Además, `HomeScreen` y `HabitDetailScreen` llaman a `LocalDate.now()` directamente, **saltándose `DateProvider`** que el resto de la app sí usa. Ver [[EstadoYViewModels]].

**Solución**

Usar `DateProvider` y un `remember` con `today` como clave, o exponer la fecha actual como `StateFlow` desde el ViewModel.

---

### BUG-006 — `connectedAndroidTest` termina con código 1 cuando hay varios dispositivos

| Campo | Valor |
|---|---|
| **Severidad** | 🟡 Medio |
| **Estado** | Observado en artefactos (por reconfirmar) |
| **Ubicación** | Configuración de la tarea Gradle `connectedAndroidTest` |

**Descripción**

En la última ejecución registrada, **todos los tests pasaron** en el emulador, pero la tarea devolvió código de salida `1`.

Evidencia:

| Artefacto | Contenido |
|---|---|
| `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 17.xml` | `tests="2" failures="0" errors="0" skipped="0"` |
| `app/build/outputs/androidTest-results/connected/debug/test-result-exit-code.txt` | `1` |
| `app/build/outputs/androidTest-results/connected/debug/M2101K7BL - 13/` | Solo `device-info.pb`, **sin resultados de test** |

**Causa raíz**

**No confirmada con certeza.** La hipótesis más probable: había **dos dispositivos conectados** a la vez (un emulador y un teléfono físico). La tarea se ejecutó correctamente en el emulador, pero el segundo dispositivo no completó la ejecución y la tarea agregado reportó fallo.

Alternativa no descartada: el dispositivo físico se desconectó a mitad de la ejecución.

**Pasos para reproducir** (a confirmar)

1. Conectar un emulador **y** un dispositivo físico simultáneamente.
2. Ejecutar `./gradlew connectedAndroidTest`.
3. Revisar `test-result-exit-code.txt`.

**Impacto**

Fallos falsos en negativo en flujos que ejecutan pruebas instrumentadas. Dificulta distinguir un fallo real de un problema de entorno.

**Solución**

Restringir la ejecución a un dispositivo, por ejemplo con `-Pandroid.testInstrumentationRunnerArguments` o usando `ANDROID_SERIAL`, o filtrando por unmanaged devices.

---

### BUG-007 — `AGENTS.md` afirma que hay configuración de firma que no existe

| Campo | Valor |
|---|---|
| **Severidad** | 🔵 Documentación |
| **Estado** | Confirmado |
| **Ubicación** | `AGENTS.md` (tabla de comandos) vs `app/build.gradle.kts` |

**Descripción**

`AGENTS.md` incluye esta fila en su tabla de comandos:

> `./gradlew assembleRelease` — Build the release APK (**requires sign config**)

Pero `app/build.gradle.kts` **no contiene ningún bloque `signingConfig`**. La sección `buildTypes` solo declara:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

**Impacto**

Una persona o un agente que siga `AGENTS.md` esperará que `assembleRelease` falle por falta de firma. El estado real de `assembleRelease` **no se ha verificado ejecutando la tarea** (ver preguntas abiertas).

**Solución**

Documentar la realidad: o bien añadir la configuración de firma (y entonces `AGENTS.md` sería correcto), o bien corregir `AGENTS.md` para indicar que la variante release **no está configurada para publicar**. La documentación se ha actualizado en [[Build]] con el estado real.

---

### BUG-008 — Referencia a `proguard-rules.pro`, que no existe en el repositorio

| Campo | Valor |
|---|---|
| **Severidad** | 🔵 Configuración |
| **Estado** | Confirmado |
| **Ubicación** | `app/build.gradle.kts:24` |

**Descripción**

```kotlin
release {
    isMinifyEnabled = false
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

**El archivo `app/proguard-rules.pro` no existe.** La raíz de `app/` contiene únicamente `.gitignore`, `build.gradle.kts`, `build/` y `src/`.

Existe en cambio `app/src/main/keepRules/rules.keep`, que es el mecanismo de reglas de R8 de AGP 9 y contiene únicamente comentarios.

**Por qué no rompe hoy**

Con `isMinifyEnabled = false`, R8 **no se ejecuta** en la variante release, y el archivo nunca se lee. El impacto es latente: en cuanto alguien active `isMinifyEnabled = true`, la build fallará o se usará un archivo inexistente.

**Pendiente de verificación**

No se ha ejecutado `assembleRelease`, así que **no está confirmado** que la tarea falle hoy. El análisis se basa en que el archivo no existe y en que `isMinifyEnabled = false`.

**Impacto**

Bajo ahora; puede romper una build de release futura.

**Solución**

Crear `app/proguard-rules.pro` (vacío si no hay reglas propias) o eliminar la línea y apoyarse en `src/main/keepRules/`.

---

### BUG-009 — Código muerto y importaciones sin usar

| Campo | Valor |
|---|---|
| **Severidad** | 🔵 Configuración |
| **Estado** | Confirmado |
| **Ubicación** | Múltiples archivos |

**Descripción**

Hay miembros públicos que nadie invoca e importaciones que no se usan. Lint **no** los detecta (solo detecta `UnusedResources`), por eso no aparecen en el informe.

**Miembros nunca llamados**

| Miembro | Ubicación | Único llamador |
|---|---|---|
| `HabitDao.getActiveHabitsList()` | `HabitDao.kt:29` | Nadie |
| `HabitDao.getAllHabits()` | `HabitDao.kt:32` | Solo la interfaz e impl, nunca una pantalla |
| `HabitDao.getAllHabitsList()` | `HabitDao.kt:35` | Nadie |
| `HabitCompletionDao.update()` | `HabitCompletionDao.kt:17` | Nadie |
| `HabitCompletionDao.getForHabitList()` | `HabitCompletionDao.kt:26` | Nadie |
| `HabitRepository.delete()` | `HabitRepository.kt:9` | Nadie (sin UI) |
| `HabitCompletionRepository.delete()` | `HabitCompletionRepository.kt:11` | Nadie (sin UI) |
| `HabitDetailViewModel.isCompletedForDate()` | `HabitDetailViewModel.kt:53` | Nadie |

**Importaciones sin usar**

| Archivo | Importación |
|---|---|
| `HomeScreen.kt:40` | `com.example.earnit.ui.theme.EarnItTheme` |
| `HabitDetailScreen.kt:35` | `android.graphics.Color` |
| `HabitDetailScreen.kt:45` | `com.example.earnit.ui.theme.EarnItTheme` |
| `HabitFormScreen.kt:46` | `com.example.earnit.ui.theme.EarnItTheme` |
| `HabitItem.kt:18` | `com.example.earnit.ui.theme.EarnItTheme` |

**Estado local no usado**

| Ubicación | Elemento |
|---|---|
| `HomeScreen.kt:70` | `snackbarHostState` creado y nunca mostrado |
| `HomeScreen.kt:71` | `scope` creado y nunca usado |

**Impacto**

Ruido que dificulta la lectura y oculta qué está realmente en uso. `getAllHabits()` en particular es la puerta de entrada a la funcionalidad de hábitos archivados, que no existe.

**Solución**

Eliminar lo que no se vaya a usar, o implementar la funcionalidad correspondiente. Para las importaciones, el IDE las elimina automáticamente.

---

### BUG-010 — La edición carga el hábito dos veces

| Campo | Valor |
|---|---|
| **Severidad** | 🔵 Configuración |
| **Estado** | Confirmado por lectura de código |
| **Ubicación** | `AppNavHost.kt:76` + `HabitFormScreen.kt:67` |

**Descripción**

Al entrar en la edición de un hábito, el mismo dato se lee por dos vías independientes:

```kotlin
// 1) En AppNavHost, con el Habit que ya viene del callback onEdit
onEdit = { habit ->
    habitFormViewModel.loadHabit(habit)
    navController.navigate("edit_habit/${habit.id}")
}
```

```kotlin
// 2) En HabitFormScreen, consultando el repositorio por id
LaunchedEffect(habitId) {
    if (habitId != null && habitRepository != null) {
        habitRepository.getById(habitId)?.let { viewModel.loadHabit(it) }
    }
}
```

**Causa raíz**

El segundo mecanismo (el `LaunchedEffect`) se implementó para que el formulario pudiera cargar el hábito por sí mismo, pero el primero no se eliminó. Ambos terminan llamando a `loadHabit()` con el mismo contenido.

**Efecto secundario a considerar**

Es la razón por la que `HabitFormScreen` **recibe un repositorio como parámetro** (`habitRepository: HabitRepository?`), lo que constituye la fuga de capas documentada en [[Arquitectura]].

**Impacto**

Lectura duplicada y un acoplamiento innecesario entre la pantalla y la capa de datos. Si solo se conservara el `LaunchedEffect`, `HabitFormScreen` seguiría necesitando el repositorio; si solo se conservara el `onEdit`, **podría dejar de necesitarlo**.

**Solución**

Eliminar una de las dos vías. La más limpia es conservar el `LaunchedEffect` (recupera del proceso muerto) y simplificar `AppNavHost.onEdit` a una navegación sin argumentos.

---

### BUG-011 — `AGENTS.md` describe herramientas de testing que no se usan

| Campo | Valor |
|---|---|
| **Severidad** | 🔵 Documentación |
| **Estado** | Confirmado |
| **Ubicación** | `AGENTS.md` (sección "Testing Quirks") vs `app/src/test/` |

**Descripción**

`AGENTS.md` afirma:

> Mockito es usado para mocking; Robolectric para Android context simulation; Turbine para Flow testing.

**Ninguna de las tres se usa.** El único test unitario del proyecto es `ExampleUnitTest`, que solo importa `org.junit.Test` y `org.junit.Assert`. La búsqueda de `mock`, `Turbine`, `RunWith(RobolectricTestRunner)` y `app.cash.turbine` en `app/src/test/` no devuelve resultados.

**Lo que sí es correcto en ese mismo apartado de `AGENTS.md`**

- La restricción de fijar Turbine en `1.0.0` porque `1.0.1` no existe en Maven Central. **Verificable y correcta.**
- La observación de que `connectedAndroidTest` normalmente se omite por no haber dispositivo. Coincide con lo observado.

**Impacto**

Un lector podría dar por hecho que existe una base de pruebas con mocks y Turbine, y no buscar la cobertura que realmente falta.

**Solución**

Corregir `AGENTS.md` para que describa el estado real, o empezar a usar esas herramientas. El detalle del estado actual está en [[Testing]].

---

### BUG-012 — Los hábitos archivados son inalcanzables desde la interfaz

| Campo | Valor |
|---|---|
| **Severidad** | 🟠 Alto |
| **Estado** | Confirmado por lectura de código |
| **Ubicación** | `HabitDao.getAllHabits()` / `HabitRepository.getAllHabits()` |

**Descripción**

Archivar un hábito funciona: `ArchiveHabitUseCase` pone `HabitStatus.ARCHIVED` y el hábito desaparece de la lista principal (cuyo `Flow` filtra por `status = 'ACTIVE'`).

Pero **no existe ninguna forma de volver a verlo**:

- Ninguna pantalla llama a `getAllHabits()`, que ya existe y está implementado.
- No hay ningún filtro, pestaña o pantalla de archivados.
- No hay función de "restaurar" en ningún sitio.

**Pasos para reproducir**

1. Crear un hábito.
2. Abrir su detalle y pulsar "Archivar".
3. Buscar cualquier forma de recuperarlo.

**Esperado** — alguna forma de consultar o restaurar los hábitos archivados.
**Observado** — no existe. El hábito queda en la base de datos, pero es inalcanzable. Solo se puede eliminar borrando los datos de la app.

**Impacto**

Función de archivo **irreversible en la práctica**. Un archivado accidental es pérdida de datos para el usuario, sin aviso.

**Relación con BUG-002**

El defecto de BUG-002 (`status` se reinicia a `ACTIVE` al editar) solo es alcanzable **una vez que exista esta pantalla**. Conviene corregir ambos a la vez.

**Solución**

Añadir una vista de archivados que use `getAllHabits()` y un caso de uso `RestoreHabitUseCase`.

---

### BUG-013 — Inconsistencia de idioma en los `contentDescription`

| Campo | Valor |
|---|---|
| **Severidad** | 🔵 Documentación |
| **Estado** | Confirmado |
| **Ubicación** | `HabitItem.kt:92`, `HabitItem.kt:98`, `IconPicker.kt:183` |

**Descripción**

La interfaz está íntegramente en español, pero tres `contentDescription` están en inglés:

| Texto | Ubicación |
|---|---|
| `"Completed"` / `"Not completed"` | `HabitItem.kt:92` y `HabitItem.kt:98` |
| `"Selected"` | `IconPicker.kt:183` |

Mientras tanto, `HabitDetailScreen` usa `"Completado"` en español (línea 187 y 237), lo que demuestra la inconsistencia dentro del propio proyecto.

**Impacto**

Los lectores de pantalla anuncian esos elementos en un idioma distinto al del resto de la app. Es también un problema de accesibilidad, no solo de estilo.

**Solución**

Externalizarlos a `strings.xml` con el resto de textos, o corregirlos directamente al español.

### BUG-014 — No se puede crear ningún hábito: el botón "Guardar" nunca se habilita

| Campo | Valor |
|---|---|
| **Severidad** | 🟠 Alto |
| **Estado** | **Corregido** |
| **Fecha de detección** | 2026-10-10 |
| **Ubicación** | `app/src/main/java/com/example/earnit/feature/habit/HabitFormScreen.kt:90,97` |

**Descripción**

El botón "Guardar" de `HabitFormScreen` se evaluaba **una sola vez**, en el primer fotograma. Como entonces el campo de nombre está vacío, su `enabled` valía `false` y **no se volvía a evaluar nunca**: escribir el nombre no lo habilitaba y pulsarlo no hacía nada. El usuario no podía crear hábitos, y por tanto tampoco podía verlos en la lista ni marcar ninguno.

```kotlin
actions = {
    Button(
        onClick = { if (viewModel.isValid()) { … } },
        enabled = viewModel.isValid(),   // ← nunca se actualiza
    )
}
```

**Causa raíz**

`isValid()` es un **método** que devuelve `Boolean` leyendo `MutableStateFlow.value`. Esa lectura **no es una lectura de estado de Compose**, así que no registra ninguna dependencia de recomposición.

Peor aún: se evalúa dentro de la **lambda `actions` de la `TopAppBar`**, que es un ámbito de recomposición distinto del cuerpo de `HabitFormScreen`. Ahí es donde se recolecta `viewModel.name` con `collectAsStateWithLifecycle()`. Al escribir el nombre solo se invalidaba ese ámbito, no el de la lambda, que por tanto conservaba el `false` inicial indefinidamente.

El mismo botón en **modo edición** sí nacía habilitado, porque `initialize()` carga el hábito antes de componer la pantalla y la primera evaluación ya daba `true`. Por eso el defecto solo era visible al crear.

**Pasos para reproducir**

1. Lanzar la app con la base de datos vacía.
2. Pulsar "Crear hábito" en el estado vacío.
3. Escribir un nombre en "Nombre del hábito".
4. Mirar el botón "Guardar" y pulsarlo.

**Esperado** — el botón se habilita al escribir un nombre y guarda el hábito.

**Observado** — el botón permanece deshabilitado. Con `adb shell uiautomator dump`, el nodo del botón se mantuvo en `enabled="false"` tras escribir `Leer libro`, y el toque no cambió de pantalla.

**Impacto**

Bloquea por completo la creación de hábitos: la app es inusable en su función principal. Ningún otro bug del proyecto tiene ese alcance.

**Causa relacionada**

`EstadoYViewModels.md` ya describía que `HabitFormViewModel` expone siete `StateFlow` sueltos, pero no hitcho ningún `isValid` derivado: la validación existía solo como método. Ese hueco es exactamente lo que秆permitió el defecto.

**Solución aplicada**

Convertir `isValid()` en estado observable, derivado de `_name`:

```kotlin
val isValid: StateFlow<Boolean> = _name
    .map { it.trim().isNotBlank() }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

y recolectarlo en la pantalla para usarlo en `enabled` y en el guard de `onClick`. El mismo patrón `stateIn` que ya usa `HomeViewModel`.

**Resolución**

- **Causa final:** lectura de `MutableStateFlow.value` desde un ámbito de recomposición que no la observa.
- **Cambio aplicado:** `HabitFormViewModel.isValid` pasa de método a `StateFlow<Boolean>`; `HabitFormScreen` lo recolecta.
- **Cómo se verifica:** test de regresión `HabitFormScreenTest.saveButton_enablesOnceTheNameIsNotBlank`.
- **Prueba añadida:** además, `saveButton_savesAndNotifiesWhenPressed` comprueba que el guardado persiste y notifica.

---

### BUG-015 — La creación de hábitos no estaba cubierta por ninguna prueba

| Campo | Valor |
|---|---|
| **Severidad** | 🔵 Configuración |
| **Estado** | **Corregido** |
| **Fecha de detección** | 2026-10-10 |
| **Ubicación** | `app/src/androidTest/` y `app/src/test/` |

**Descripción**

Hasta la corrección de BUG-014 la app tenía **1 test unitario** (que comprueba que `2 + 2 == 4`) y **2 instrumentados**, ninguno sobre creación o edición de hábitos. BUG-014 estuvo presente desde el primer commit y ninguna prueba lo detectó: era invisible para la suite.

Del mismo modo, BUG-002 —que pierde la fecha de creación del usuario— no tenía cobertura, pese a que `Testing.md` lo señalaba como el hueco prioritario.

**Solución aplicada**

- `HabitFormScreenTest` (instrumentado): dos tests nuevos de regresión sobre el botón de guardado.
- `HabitDetailScreenTest` (instrumentado, nuevo): la flecha "Volver" invoca `onBack`, y el detalle carga el hábito por id.
- `HabitFormViewModelTest` (JVM, nuevo): editar conserva `createdAt` y `status`; `isValid` ignora espacios.

**Resolución**

Es el primer uso real de `kotlinx-coroutines-test` en el proyecto. Ver [[Testing]].

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

Todos los bugs anteriores están **confirmados y sin corregir**. Se han documentado; no se ha modificado ningún archivo de código.

### Mejoras recomendadas (no decididas)

Estos puntos **no son bugs verificados**, sino riesgos o inconsistencias pendientes de valoración:

- **El botón "Guardar" no muestra confirmación.** Tras guardar, la pantalla se cierra y no hay ningún `Snackbar`. No es un fallo: nunca se implementó la confirmación. `HomeScreen` ya tiene un `SnackbarHostState` listo y sin usar.
- **Sin estado de carga ni de error.** `initialValue = emptyList()` hace que "cargando" y "no tienes hábitos" sean indistinguibles. Cualquier excepción de Room cancelaría el `viewModelScope` sin surfacing al usuario.
- **`getIcon()` falla en silencio** ante una clave desconocida, devolviendo el icono de libro. Un error de tipeo en la base de datos no se manifiesta.
- **`Habit.copyWith()` no puede limpiar `description`.** El patrón `campo ?: this.campo` impide poner `null`.
- **`HabitCompletionEntity.value`** es una columna siempre `null`, sin uso.
- **La clave primaria compuesta depende de la zona horaria.** `toEpochMillis()` usa `ZoneId.systemDefault()`: cambiar de zona puede desplazar la clave de un día. Ver [[Persistencia]].

## Preguntas abiertas

- **¿`assembleRelease` falla hoy?** No se ha ejecutado. Es lo que determina si BUG-008 es latente o activo. También queda por determinar qué clave de firma usaría Gradle sin `signingConfig`.
- **¿La salida con código 1 de `connectedAndroidTest` se reproduce siempre con dos dispositivos?** Es lo que confirmaría la causa de BUG-006.
- **¿BUG-003 se manifiesta de forma visible?** El síntoma depende del orden de emisión de los `Flow` de Room, que es determinista pero no se ha comprobado experimentalmente. Está confirmado a nivel de código, no de observación.
- **¿Archivar debería pedir confirmación?** Hoy la acción es inmediata e irreversible. Con BUG-012, un archivado accidental es pérdida de datos.
- **¿Se sabe si algún usuario ha instalado la app y archivado hábitos?** No hay telemetría ni forma de saberlo desde el repositorio.