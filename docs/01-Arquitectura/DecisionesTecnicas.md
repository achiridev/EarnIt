# Decisiones Técnicas

## Propósito

Dar una **vista rápida y navegable** de las decisiones arquitectónicas del proyecto y de su justificación, sin repetirlas.

> [!important] Este documento es un índice, no la fuente
> Cada decisión se detalla en su propio ADR dentro de `05-Decisiones/`. Si este documento y el ADR discrepan, **manda el ADR**. Si el código y el ADR discrepan, **manda el código** (y hay que revisar el ADR).

## Estado actual

| Decisión registrada | ADR | Estado |
|---|---|---|
| Inyección de dependencias manual, sin Hilt | [[ADR-0001]] | Aceptada y aplicada |

**No hay más ADRs.** El repositorio no contiene ningún registro de decisiones previo: `AGENTS.md` documenta reglas de build y arquitectura en prosa, pero no como ADR formal.

## Información encontrada

### Decisiones con ADR

| Decisión | Resumen | Dónde se aplica |
|---|---|---|
| **DI manual, sin Hilt** | `AppContainer` construye el grafo a mano; `ViewModelFactories` inyecta en los ViewModel. Las anotaciones `@javax.inject.Inject` se conservan pero **no generan nada**. | `core/di/AppContainer.kt`, `core/di/ViewModelFactories.kt`, `MainActivity.kt` |

Justificación completa, opciones descartadas y consecuencias en [[ADR-0001]].

### Decisiones documentadas en prosa dentro del repositorio

Estas decisiones **no son ADRs**, pero están razonadas en `AGENTS.md` y son verificables en el código. Se registran aquí para que no se pierdan, yTDBConvertirlas en ADR si se desea:

| Decisión | Evidencia en `AGENTS.md` | Evidencia en el código | ADR sugerido |
|---|---|---|---|
| **Kotlin lo aporta AGP 9**; no añadir `org.jetbrains.kotlin.android` | "AGP 9 owns Kotlin. Keep `com.android.application`, `org.jetbrains.kotlin.plugin.compose` and `com.google.devtools.ksp` only." | `build.gradle.kts` raíz solo declara `android.application` y `kotlin.compose`; `app/build.gradle.kts` añade `ksp` | Sí, merecería ADR propio |
| **Room fijado en 2.7.2** | "Older Room (2.6.x) crashes with KSP2 (`unexpected jvm signature V`)" | `libs.versions.toml:9` | Sí |
| **Turbine fijada en 1.0.0** | "the originally pinned `1.0.1` does not exist on Maven Central and fails resolution" | `libs.versions.toml:17` | No, es una nota de build, no arquitectura |
| **Daimon en JDK 25 vía `gradle-daemon-jvm.properties`** | "Never delete it, and never add `org.gradle.java.home`" | `gradle/gradle-daemon-jvm.properties` existe y Gradle lo respeta | No, es infraestructura |
| **`android.disallowKotlinSourceSets=false`** | "the KSP plugin still registers its generated sources through the `kotlin.sourceSets` DSL" | Comentado en `gradle.properties:10-12` | No |
| **Room usa `fallbackToDestructiveMigration()`** | Mencionado como advertencia, **sin justificación** | `AppDatabase.kt:33` | **Candidato a ADR**: la decisión de aceptar pérdida de datos nunca se razonó |

### Contradicciones detectadas entre `AGENTS.md` y el código

Registradas aquí y detalladas en [[Bugs]]:

| Afirmación en `AGENTS.md` | Realidad en el código | ID |
|---|---|---|
| "`./gradlew assembleRelease` — Build the release APK (**requires sign config**)" | **No existe ningún bloque `signingConfig`** en `app/build.gradle.kts` | BUG-007 |
| "Mockito es usado para mocking; Robolectric para Android context; Turbine para Flow testing" | Las tres están **declaradas y sin usar**: el único test unitario es `ExampleUnitTest`, que solo usa JUnit | Ver [[Testing]] |
| "AppContainer ... provides `DateProvider`" | Correcto | — |
| "`AppDatabase.getInstance()` uses `fallbackToDestructiveMigration()`" | Correcto | — |

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- `AGENTS.md` contiene al menos dos afirmaciones desactualizadas (BUG-007 y la de herramientas de testing). **No se han corregido** porque la tarea actual es exclusivamente documental.

### Mejoras recomendadas (no decididas)

- **Convertir en ADR las decisiones con justificación real:** el stack AGP 9 / Kotlin integrado y el pin de Room 2.7.2 por KSP2. Ambas tienen un motivo técnico verificable y evitarían que alguien "arregle" el build devolviendo el plugin de Kotlin a su sitio.
- **Redactar un ADR sobre la migración destructiva de Room.** Hoy `fallbackToDestructiveMigration()` + `exportSchema = false` significa que cualquier cambio de esquema borra los datos del usuario sin aviso. Merece una decisión explícita, aunque sea para aceptarla conscientemente.
- **Normalizar la referencia a ADRs.** `AGENTS.md` habla de reglas pero no enlaza a `docs/`; conviene que ambos documentos se citen mutuamente.
- **Documentar el patrón "ViewModel compartido entre destinos"** como ADR cuando se estabilice, porque condiciona cómo se añade cualquier pantalla nueva.

## Preguntas abiertas

- ¿Se quiere mantener la convención de ADRs numerados (`ADR-0001`) o se prefieres nombres temáticos?
- ¿Quién decide sobre la migración destructiva de Room? Es la decisión de mayor riesgo de pérdida de datos del proyecto.
- ¿`AGENTS.md` debe pasar a ser un índice que apunte a `docs/`, o se mantendrán como dos fuentes paralelas? Mantener ambas garantiza que vuelva a desincronizarse.
- ¿Se considera `AGENTS.md` o `README.md` como documentación oficial para personas no familiarizadas con agentes? Hoy el repositorio **no tiene `README.md`**.