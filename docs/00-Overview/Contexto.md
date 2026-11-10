# Contexto

## Propósito

Describir **qué es EarnIt en realidad hoy**: su propósito, su público, su alcance funcional verificable y el stack tecnológico que lo sostiene. Es el documento que debería leer alguien que abre el repositorio por primera vez.

Para los objetivos de producto ver [[Objetivos]]. Para el estado de implementación ver [[Roadmap]].

## Estado actual

| Aspecto | Estado |
|---|---|
| Aplicación Android nativa | **Implementado** |
| Persistencia local | **Implementado** |
| Backend / API / red | **No existe** |
| Autenticación de usuarios | **No existe** |
| Sincronización / nube | **No existe** |
| Notificaciones y tareas en segundo plano | **No existen** (ver [[Permisos]]) |
| Multiple módulos Gradle | **No**: un único módulo `:app` |

## Información encontrada

### Propósito del producto

EarnIt es un **registrador de hábitos**：el usuario crea hábitos, los ve listados cada día y los marca como completados o no. Cada día, un hábito completado genera un registro (`HabitCompletion`) que alimenta un historial por hábito.

La pantalla principal incluye un **calendario**: una tira semanal siempre visible y una vista mensual expandible. Desde ella se puede consultar y marcar cualquier día **hasta hoy**; los futuros no son seleccionables. El detalle también opera sobre el día elegido y ofrece "Volver a hoy".

El ciclo completo está implementado: crear → elegir día → marcar → ver historial → editar → archivar.

### Modelo de dominio

Dos conceptos, definidos en `app/src/main/java/com/example/earnit/domain/model/Habit.kt`:

**`Habit`** — el hábito en sí.

| Campo | Tipo | Nota |
|---|---|---|
| `id` | `Long` | `0` = aún no persistido |
| `name` | `String` | obligatorio, validado con `isNotBlank()` |
| `description` | `String?` | opcional |
| `icon` | `String` | **clave de texto**, no una referencia a recurso (ver [[Componentes]]) |
| `color` | `Int` | color ARGB empaquetado |
| `frequency` | `Frequency` | enum con **un único valor**: `DAILY` |
| `createdAt` | `LocalDate` | en el dominio; `Long` (epoch ms) en la entidad |
| `status` | `HabitStatus` | `ACTIVE` o `ARCHIVED` |

**`HabitCompletion`** — el registro diario.

| Campo | Tipo | Nota |
|---|---|---|
| `habitId` | `Long` | parte de la clave primaria compuesta |
| `date` | `LocalDate` | parte de la clave primaria compuesta |
| `completed` | `Boolean` | `true` = completado ese día |
| `value` | `Double?` | **siempre `null`**: ningún caso de uso lo escribe (ver [[Persistencia]]) |

`Habit` incluye un `copyWith()` propio (`Habit.kt:24-38`). Ojo: `copyWith` usa el patrón `campo ?: this.campo`, por lo que **no permite poner `description = null`**; solo puede dejar el valor anterior o cambiarlo por uno no nulo.

### Stack tecnológico verificado

Todos los datos de esta tabla salen de `gradle/libs.versions.toml` y `app/build.gradle.kts`.

| Pieza | Versión | ¿Se usa de verdad? |
|---|---|---|
| Gradle | 9.6.0 (wrapper) | Sí |
| AGP (Android Gradle Plugin) | 9.4.1 | Sí |
| Kotlin | 2.2.10 | Sí — **lo aporta AGP 9**, no hay plugin `kotlin("android")` |
| KSP | 2.2.10-2.0.2 | Sí — solo para el procesador de Room |
| Compose BOM | 2026.02.01 | Sí |
| Room | 2.7.2 | Sí — base de datos local |
| Navigation Compose | 2.8.4 | Sí — `AppNavHost` |
| Coroutines | 1.8.1 | Sí |
| `javax.inject` | 1 | **Anotación decorativa**: se aplica `@Inject` pero ningún procesador la consume (ver [[ADR-0001]]) |
| JUnit 4 | 4.13.2 | Sí (5 archivos, 20 tests) |
| `kotlinx-coroutines-test` | 1.8.1 | **Sí**, en `HabitFormViewModelTest` y `HomeViewModelTest` |
| Mockito | 5.18.0 | **Declarada, sin usar** |
| Turbine | 1.0.0 | **Declarada, sin usar** |
| Robolectric | 4.11 | **Declarada, sin usar** |
| Espresso / `androidx.test.ext:junit` | 3.7.0 / 1.3.0 | Sí, andamiaje instrumentado |
| `room-testing` | 2.7.2 | **Declarada, sin usar** |

> [!warning] Dependencias declaradas ≠ funcionalidades
> Cuatro dependencias de testing están en el `classpath` pero **ningún test las usa**: Mockito, Turbine, Robolectric y `room-testing`. Estar en `build.gradle.kts` no significa que la capacidad exista.

### Valores de configuración de la app

Extraídos de `app/build.gradle.kts` y `AndroidManifest.xml`:

| Propiedad | Valor | Nota |
|---|---|---|
| `namespace` / `applicationId` | `com.example.earnit` | **Namespace de ejemplo**, no uno de producción |
| `versionCode` / `versionName` | `2` / `"2.0.0"` | SemVer; ver [[Android]] |
| `minSdk` | **26** (Android 8.0) | Permite `java.time` nativo, sin desugaring |
| `targetSdk` / `compileSdk` | **37** | |
| `jvmTarget` | **JVM 11** | Coherente con `sourceCompatibility`/`targetCompatibility` |
| `isMinifyEnabled` | `false` | R8 desactivado (ver BUG-008 en [[Bugs]]) |
| Bloque `signingConfig` | **No existe** | Contradice a `AGENTS.md` (ver [[Build]]) |

### Estructura del código

Un solo módulo Gradle, paquetes bajo `com.example.earnit`:

```text
app/src/main/java/com/example/earnit/
├── MainActivity.kt              único Activity, punto de entrada de la UI
├── EarnItApplication.kt         crea y expone el AppContainer
├── core/
│   ├── di/                      AppContainer + ViewModelFactories (DI manual)
│   ├── navigation/              AppNavHost (grafo de navegación)
│   ├── ui/components/           HabitItem, HabitCalendar, AppVersionButton, IconPicker, ColorPicker, IconHelper
│   └── util/                    DateProvider, RealDateProvider, DateConverter, CalendarDates
├── data/
│   ├── local/{dao,database,entity}   capa Room
│   └── repository/              implementaciones de los repositorios
├── domain/
│   ├── model/                   Habit, HabitCompletion, Frequency, HabitStatus
│   ├── repository/              interfaces
│   └── usecase/                 7 casos de uso
├── feature/
│   ├── home/                    HomeScreen + HomeViewModel
│   └── habit/                   HabitDetail* y HabitForm*
└── ui/theme/                    Color, Theme, Type (Material 3)
```

Detalle de capas y flujo de datos en [[Arquitectura]].

### Idioma de la interfaz

La UI está **en español y hardcodeada en el código Kotlin**. El único recurso de texto es `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">EarnIt</string>
</resources>
```

Cadenas como `"Hábitos"`, `"Nuevo hábito"`, `"Historial"`, `"Guardar"` o `"No hay hábitos hoy"` están escritas directamente en los Composables. **No hay soporte de localización** y añadir un idioma exigiría externalizar todas esas cadenas. Ver [[UI]]. El botón de versión de la `TopAppBar` lee `versionName` del `PackageManager` (`2.0.0`), no de `BuildConfig`.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- `TODO(frequency)` explícito en `app/src/main/java/com/example/earnit/data/repository/HabitRepositoryImpl.kt:38`: el parámetro `date` de `getActiveHabitsForDate()` **se ignora** porque `Frequency` solo tiene `DAILY`. El propio comentario indica que el filtrado por frecuencia aún no existe.
- `data_extraction_rules.xml:8` contiene un `TODO` de plantilla de Android Studio (comentado, sin efecto).

### Mejoras recomendadas (no decididas)

- Externalizar las cadenas de la UI a `strings.xml` para permitir localización.
- Reemplazar el namespace de ejemplo `com.example.earnit` por uno propio antes de publicar.
- Evaluar si `value: Double?` merece mantenerse o eliminarse hasta que exista una funcionalidad que lo use.

## Preguntas abiertas

- ¿Cuál es el idioma objetivo del producto? Hoy solo hay español y no hay infraestructura de localización.
- ¿El namespace `com.example.earnit` es intencionado o es residuo de la plantilla de Android Studio?
- ¿Existe una visión de hábitos con métricas numéricas (por ejemplo "beber 2 L de agua")? Eso explicaría el campo `value`, hoy siempre `null`.
- ¿`ARCHIVED` es un estado terminal? Actualmente archivar es **irreversible desde la UI** (ver [[Roadmap]]).