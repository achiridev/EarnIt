# Entorno

## Propósito

Describir los **requisitos para configurar una máquina** desde cero de forma que el proyecto compile y las pruebas se ejecuten correctamente. Todo lo que se afirma aquí está verificado contra la configuración del repositorio.

Para los comandos concretos ver [[Build]]. Para la instalación en dispositivo ver [[Instalacion]].

## Estado actual

| Componente | Versión / valor | Fuente |
|---|---|---|
| Sistema operativo | Linux (verificado en el entorno de análisis) | — |
| Gradle | **9.6.0** | `gradle/wrapper/gradle-wrapper.properties` |
| **JVM del daemon** | **25** (Adoptium 25.0.3), **auto-aprovisionado** | `gradle/gradle-daemon-jvm.properties` |
| JVM lanzador | 21 (verificado con `./gradlew --version`) | Variable local `JAVA_HOME` |
| AGP | **9.4.1** | `gradle/libs.versions.toml` |
| Kotlin | **2.2.10** | `gradle/libs.versions.toml` |
| Android SDK Platform | **android-37** (`compileSdk`/`targetSdk = 37`) | `app/build.gradle.kts` |
| Build Tools | 36.0.0 (instalado) | Entorno local |
| `minSdk` | 26 | `app/build.gradle.kts` |

## Información encontrada

### Requisito 1: el SDK de Android

El proyecto necesita **un solo platform SDK**, el 37. Está definido en `app/build.gradle.kts`:

```kotlin
android {
    namespace = "com.example.earnit"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        targetSdk = 37
    }
}
```

El SDK se localiza por cualquiera de estos dos mecanismos:

| Mecanismo | Dónde se configura |
|---|---|
| `local.properties` con `sdk.dir=/ruta/al/sdk` | Generado por Android Studio, **ignorado por Git** |
| Variable de entorno `ANDROID_HOME` o `ANDROID_SDK_ROOT` | Configuración del sistema |

> [!important] `local.properties` no se versiona
> Está en `.gitignore` (línea 15 del `.gitignore` raíz). Contiene rutas locales y **no debe subirse al repositorio**. Si falta, Gradle fallará con un error de "SDK location not found".

### Requisito 2: el JDK 25 del daemon — el punto más importante

Este proyecto tiene una configuración poco habitual que conviene entender bien:

```properties
# gradle/gradle-daemon-jvm.properties  (generado por updateDaemonJvm)
toolchainVersion=25
toolchainUrl.LINUX.X86_64=https\://api.foojay.io/disco/v3.0/ids/…/redirect
# … una URL por plataforma
```

**Qué significa:** Gradle exige que el daemon se ejecute en **JDK 25**. Como el JDK del sistema suele ser otro, Gradle **lo descarga automáticamente** de [Foojay Disco API](https://api.foojay.io) y lo cachea en `~/.gradle/jdks/`.

**Consecuencias verificadas:**

| Aspecto | Comportamiento real |
|---|---|
| ¿Hace falta instalar JDK 25 a mano? | **No.** Se descarga solo la primera vez |
| ¿Hace falta internet la primera vez? | **Sí**, para la descarga del toolchain |
| ¿Dónde queda cacheado? | `~/.gradle/jdks/` |
| ¿Qué JDK usa el launcher? | El del sistema (21 en el entorno verificado) |
| ¿Sirve el JDK 25 para *compilar* el proyecto? | No. `jvmTarget = JVM_11`: el **bytecode** es Java 11 |

Salida real de `./gradlew --version`:

```text
Gradle 9.6.0
Launcher JVM:  21.0.12.1 (Arch Linux 21.0.12.1+1)
Daemon JVM:    Compatible with Java 25, any vendor, nativeImageCapable=false
               (from gradle/gradle-daemon-jvm.properties)
```

> [!warning] Reglas que `AGENTS.md` marca como inviolables
> 1. **No borrar `gradle/gradle-daemon-jvm.properties`.** Es lo que fija el JDK 25.
> 2. **No añadir `org.gradle.java.home` a `gradle.properties`.** Se ignora en silencio y desincroniza el daemon del JDK fijado.
>
> Ambas reglas existen porque su violación ya rompió la build en el pasado.

### Requisito 3: particularidades de los plugins de AGP 9

AGP 9 **incorpora el soporte de Kotlin**. En este proyecto eso se manifiesta en `build.gradle.kts` raíz:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

y en `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}
```

**No existe el plugin `org.jetbrains.kotlin.android`.** Si alguien lo añade por costumbre, la build falla.

Además, `gradle.properties` contiene una opción específica:

```properties
android.disallowKotlinSourceSets=false
```

El comentario del propio archivo explica por qué: *"AGP 9 has built-in Kotlin: the KSP plugin still registers its generated sources through the `kotlin.sourceSets` DSL."*

### Requisito 4: resolución de toolchains

`settings.gradle.kts` declara el resolutor de Foojay:

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

Es lo que permite que Gradle descargue JDKs automáticamente en lugar de fallar si no encuentra el toolchain.

### Configuración de repositorios

`settings.gradle.kts` usa `FAIL_ON_PROJECT_REPOS`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

Implicación práctica: **no se pueden declarar repositorios en `build.gradle.kts`**. Si hace falta un repositorio nuevo, debe añadirse en `settings.gradle.kts`.

`pluginManagement.repositories` usa además filtros de contenido para `google()`, limitándolo a grupos `com.android.*`, `com.google.*` y `androidx.*`.

### Propiedades de Gradle activas

`gradle.properties`:

| Propiedad | Valor | Efecto |
|---|---|---|
| `org.gradle.jvmargs` | `-Xmx2048m -Dfile.encoding=UTF-8` | Memoria del daemon; **2 GB** puede ser justo para builds de Compose |
| `org.gradle.configuration-cache` | `true` | **Caché de configuración activo**. Los cambios en scripts se invalidan automáticamente |
| `kotlin.code.style` | `official` | Formato de código Kotlin oficial |
| `android.disallowKotlinSourceSets` | `false` | Necesario para KSP con AGP 9 |
| `org.gradle.parallel` | Comentado | Desactivado |

### Datos del verificador de herramientas (Turbine)

`AGENTS.md` documenta una restricción que conviene conocer **antes** de añadir la dependencia:

> Turbine must stay on `1.0.0` — the originally pinned `1.0.1` does not exist on Maven Central and fails resolution only for `test*` configurations (main builds still pass).

Consecuencia práctica: un error de resolución de Turbine **solo rompe los tests**, no la build principal. Es un fallo fácil de confundir con otro problema.

### Configuración local no versionada

| Archivo | Estado | Notas |
|---|---|---|
| `local.properties` | Existe localmente, **ignorado por Git** | Contiene `sdk.dir` con una ruta local. **Nunca documentar ni versionar su contenido** |
| `.idea/` | Presente | Configuración del IDE |
| `app/build/` | Generado | Artefactos de compilación |
| `.gradle/` | Generado | Caché de Gradle |

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- `org.gradle.jvmargs = -Xmx2048m` puede resultar insuficiente para builds de Compose con el caché de configuración activo. No se ha medido el consumo real de memoria.
- No hay `gradle.properties` con `kotlin.incremental` ni opciones de daemon afinadas; son valores por defecto.

### Mejoras recomendadas (no decididas)

- **Documentar la versión de JDK del sistema** como requisito, aunque no sea la 25, para evitar confusiones. Actualmente solo se documenta la versión del daemon.
- **Considerar subir la memoria del daemon** a 4 GB si se observan OOM en builds de Compose.
- **Registrar la versión de Android Studio / IntelliJ** que se usa, dato que hoy no está en ninguna parte.
- **Añadir un `README.md`** en la raíz con los comandos básicos, para quien no familiarizado con agentes.

## Preguntas abiertas

- ¿La descarga automática del JDK 25 funciona en entornos sin salida a internet (red corporativa)? El `toolchainUrl` de Foojay lo hace dependiente de la red en la primera ejecución.
- ¿Se soporta trabajo sin conexión? No hay ningún uso de `--offline` documentado ni verificado en el repositorio.
- ¿Cuál es la versión mínima de Android Studio que abre el proyecto sin errores? Con AGP 9.4.1 probablemente se necesite una versión muy reciente.
- ¿Debe fijarse el JDK del sistema, o basta con que exista uno compatible? Gradle usa el launcher JDK del sistema (21 verificado) y el daemon JDK 25.