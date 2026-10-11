# Build

## Propósito

Documentar los **comandos exactos** de compilación, análisis estático y pruebas del proyecto, con el orden correcto de ejecución y el estado verificado de cada uno.

Para los requisitos de la máquina ver [[Entorno]]. Para la instalación en dispositivo ver [[Instalacion]]. Para las pruebas en detalle ver [[Testing]].

## Estado actual

| Tarea | Estado verificado |
|---|---|
| `lintDebug` | ✅ **Ejecutada con éxito** — 24 avisos, **0 errores** |
| `test` / `testDebugUnitTest` | ✅ **Ejecutada con éxito** — 1 test, 1 pasa |
| `assembleDebug` | ✅ **Ejecutada con éxito** — `app-debug.apk` generado |
| `connectedAndroidTest` | ⚠️ Tests pasan pero la tarea **sale con código 1** (BUG-006) |
| `assembleRelease` | ❓ **No ejecutada**. Estado desconocido (ver BUG-007 y BUG-008) |

## Información encontrada

### Orden recomendado

```bash
./gradlew lintDebug      # 1. Análisis estático
./gradlew test           # 2. Pruebas unitarias
./gradlew assembleDebug  # 3. Compilación
```

El orden importa. `AGENTS.md` lo justifica así:

> Order matters: Run `lint -> test -> assembleDebug` in that order (running tests alone may miss lint errors).

**Por qué es necesario `lintDebug` antes que los tests:** el Lint de Android puede detectar errores que solo aparecen en la fase de lint y que además **bloquean la build** en este proyecto. Concretamente, el error `NonObservableLocale` —producido por llamar a `Locale.getDefault()` dentro de un `@Composable`— está documentado en [[UI]] como un fallo que ya rompió la compilación.

### Verificación rápida (todo de una vez)

```bash
./gradlew lintDebug test assembleDebug
```

### Comandos completos

#### Análisis estático

```bash
./gradlew lintDebug              # Lint de la variante debug
./gradlew lint                   # Lint de todas las variantes
```

Informes generados:

| Ruta | Formato |
|---|---|
| `app/build/reports/lint-results-debug.txt` | Texto plano, ideal para CI |
| `app/build/reports/lint-results-debug.html` | Navegable en navegador |
| `app/build/reports/lint-results-debug.xml` | XML para análisis automatizado |
| `app/build/reports/lint-results-debug.sarif` | Formato SARIF |

#### Pruebas unitarias

```bash
./gradlew test                   # Todas las variantes
./gradlew testDebugUnitTest      # Solo debug
./gradlew testReleaseUnitTest    # Solo release
```

Informe: `app/build/reports/tests/testDebugUnitTest/index.html`
Resultados: `app/build/test-results/testDebugUnitTest/`

#### Pruebas instrumentadas

```bash
./gradlew connectedAndroidTest   # REQUIERE dispositivo o emulador conectado
./gradlew connectedDebugAndroidTest   # Variante debug, más rápida
```

Informes:

| Ruta | Contenido |
|---|---|
| `app/build/reports/androidTests/connected/debug/index.html` | Informe HTML |
| `app/build/outputs/androidTest-results/connected/debug/` | XML + `device-info.pb` + logs de logcat |
| `…/test-result-exit-code.txt` | **Código de salida de la tarea** |

> [!warning] `connectedAndroidTest` y varios dispositivos
> Si hay más de un dispositivo conectado, la tarea puede salir con código 1 aunque todos los tests pasen. Es BUG-006 en [[Bugs]]. Antes de culpar a los tests, comprobar siempre `test-result-exit-code.txt`.

#### Compilación

```bash
./gradlew assembleDebug      # APK de depuración
./gradlew assembleRelease    # APK de release — ver advertencias
./gradlew build              # debug + release + lint + tests
./gradlew clean              # Limpia app/build/
```

APK generado en: `app/build/outputs/apk/debug/app-debug.apk`

#### Utilidades

```bash
./gradlew tasks              # Lista todas las tareas disponibles
./gradlew projects           # Lista los módulos (solo :app)
./gradlew dependencies       # Árbol de dependencias de todas las configuraciones
./gradlew :app:dependencies  # Solo el módulo app
./gradlew --version          # Versión de Gradle, JVM lanzador y JVM del daemon
./gradlew --stop             # Detiene los daemons
./gradlew help               # Tarea de ayuda
```

### Estado verificado del Lint

Ejecutado sobre el árbol de trabajo actual. **0 errores, 24 avisos** — el proyecto **no falla** por Lint.

| ID | Cantidad | Dónde |
|---|---|---|
| `UnusedResources` | 7 | `res/values/colors.xml` líneas 3-9 |
| `GradleDependency` | 7 | `libs.versions.toml` — versiones con actualizaciones disponibles |
| `NewerVersionAvailable` | 6 | `libs.versions.toml` |
| `ModifierParameter` | 2 | `HabitItem.kt:39`, `HomeScreen.kt:58` |
| `RedundantLabel` | 1 | `AndroidManifest.xml:18` |
| `AndroidGradlePluginVersion` | 1 | `gradle-wrapper.properties:5` — Gradle 9.8.0 disponible |

Detalle relevante:

- Los 12 avisos de dependencias (**7 + 6** de actualización) confirman que **las versiones fijadas no son las últimas**, pero ninguna es un error. Subirlas sin verificar breaking changes esExactly el riesgo que `AGENTS.md` advierte con Room.
- `UnusedResources` corresponde a `colors.xml`, un archivo entero de residuo de plantilla. Ver [[Android]].
- **`NonObservableLocale` no aparece**, lo que confirma que el patrón `LocalConfiguration.current.locales[0]` se está aplicando correctamente en `HabitDetailScreen`.

### Estado verificado de la compilación

| Dato | Valor |
|---|---|
| APK de debug | `app/build/outputs/apk/debug/app-debug.apk`, **~18 MB** |
| Tamaño de `app-debug.apk` | Alto: incluye `material-icons-extended`, que empaqueta toda la biblioteca de iconos de Material |
| APK de androidTest | `app/build/outputs/apk/androidTest/debug/` |
| Sello de tiempo | Los artefactos son **posteriores** a todos los archivos fuente → reflejan el árbol de trabajo actual |

### Variantes de build

Solo existen las variantes implícitas:

| Variante | `isMinifyEnabled` | Firma | Estado |
|---|---|---|---|
| `debug` | `false` | Clave de depuración automática | ✅ Funciona |
| `release` | `false` | **Sin `signingConfig`** | ❓ No verificada |

Configuración real (`app/build.gradle.kts:21-26`):

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

> [!warning] `assembleRelease` tiene dos problemas conocidos
> 1. **No hay bloque `signingConfig`** en ninguna parte del proyecto, pese a que `AGENTS.md` afirma lo contrario (BUG-007).
> 2. **El archivo `proguard-rules.pro` que se referencia no existe** (BUG-008). Hoy es inofensivo porque `isMinifyEnabled = false` evita que R8 se ejecute, pero no se ha confirmado ejecutando la tarea.
>
> **No se ha ejecutado `assembleRelease`**, por lo que su comportamiento real está sin verificar.

### Aumentar el detalle de la salida

```bash
./gradlew assembleDebug --info      # Information
./gradlew assembleDebug --debug     # Debug (muy verboso)
./gradlew assembleDebug --stacktrace   # Traza completa de excepciones
./gradlew assembleDebug --scan          # Analyses de dependencias
```

### Compilación de una sola tarea

```bash
./gradlew :app:assembleDebug       # Con el prefijo del módulo
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
```

### Limpieza

```bash
./gradlew clean                    # Limpia app/build/
```

Si Gradle queda en un estado inconsistente tras cambiar versiones de AGP o del wrapper:

```bash
./gradlew --stop
rm -rf .gradle app/build
./gradlew --refresh-dependencies assembleDebug
```

> [!caution] El caché de configuración está activo
> `gradle.properties` tiene `org.gradle.configuration-cache=true`. Si un cambio en un script no surte efecto, el primer paso es `./gradlew --stop`.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- `assembleRelease` no se ha verificado nunca (BUG-007, BUG-008).
- 24 avisos de Lint sin resolver.
- 12 avisos de dependencias desactualizadas pendientes de valoración.
- No hay ninguna configuración de integración continua que ejecute estos comandos automáticamente. Ver [[Testing]].

### Mejoras recomendadas (no decididas)

- **Corregir los 24 avisos de Lint**, empezando por eliminar `res/values/colors.xml` (7 avisos de golpe).
- **Añadir un `./gradlew check`** como punto de entrada único. Ya existe como tarea agregada, pero `AGENTS.md` no lo menciona.
- **Evaluar `material-icons-extended`**: pesa en el APK (18 MB de debug) y solo se usan 20 iconos. Los iconos del núcleo (`material-icons-core`) ya vienen en `material3`.
- **Fijar o subir versiones** de Gradle (9.8.0 disponible), KSP (2.2.21-0.0.5), Room (2.8.5), Navigation (2.10.2) y Compose BOM (2026.09.00). **Con caution**: `AGENTS.md` documenta que subir Room a 2.6.x rompió la build con KSP2, así que cada subida necesita verificación.
- **Documentar el procedimiento de release** cuando se decida publicar.

## Preguntas abiertas

- ¿Funciona `assembleRelease` hoy? Es lo único que queda sin verificar de la matriz de build.
- ¿Se firmará con una clave propia o seguirá sin firmarse? Determina si hay que añadir `signingConfig`.
- ¿Se/subirán las dependencias? Cada salto de versión en un proyecto con AGP 9 + KSP2 es un riesgo conocido.
- ¿Habrá más de una variante o *flavors* (por ejemplo, gratis y de pago)? Hoy no hay ninguno.
- ¿Debe añadirse `jacoco` o `kover` para medir cobertura? Ver [[Testing]].