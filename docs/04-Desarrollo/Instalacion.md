# Instalación

## Propósito

Documentar **cómo instalar y ejecutar EarnIt** en un emulador o en un dispositivo Android físico, y cómo depurar desde el código.

Para los requisitos previos de la máquina ver [[Entorno]]. Para los comandos de compilación ver [[Build]].

## Estado actual

| Vía | Estado |
|---|---|
| Emulador Android Studio / AVD | **Verificada en la última ejecución** |
| Dispositivo físico por USB | Configuración estándar de Android, **no verificada en este análisis** |
| Dispositivo físico por ADB inalámbrico | **Se observó en uso** (ver nota), pero **no hay procedimiento documentado en el repositorio** |
| `adb install` manual | Procedimiento estándar, no verificado aquí |

## Información encontrada

### Opción 1: desde Gradle (la más simple)

```bash
./gradlew installDebug
```

Compila el APK de debug y lo instala en **todos los dispositivos conectados** (o en el emulador que Gradle tenga asignado). Es el comando de la tabla de `AGENTS.md` y el más usado en el día a día.

### Opción 2: compilar e instalar por separado

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

| Flag | Significado |
|---|---|
| `-r` | Reinstala, conservando datos de la aplicación |
| `-d` | Permite downgrade de `versionCode` |
| `-s` | Desinstalar antes de instalar (equivale a borrar los datos) |

**`-r` es el que se quiere casi siempre.** Sin él, una instalación limpia puede fallar si ya hay una versión instalada con firma distinta.

### Opción 3: emulador

```bash
# Listar emuladores disponibles
~/Android/Sdk/emulator/emulator -list-avds

# Arrancar uno
~/Android/Sdk/emulator/emulator -avd <NOMBRE_AVD> &

# Comprobar que está listo
adb wait-for-device
adb devices
```

> [!note] Emuladores usados en la última ejecución verificada
> Los artefactos de `connectedAndroidTest` registran el emulador `Medium_Phone(AVD) - 17` con los 2 tests pasando. También existe un AVD llamado `Medium_Phone_API_37.0`. Estos nombres son del entorno local, **no del repositorio**.

### Opción 4: dispositivo físico

1. Activar **Opciones de desarrollador** (tocando 7 veces "Número de compilación").
2. Activar **Depuración USB**.
3. Conectar por USB y aceptar la huella RSA en el teléfono.
4. Verificar:

```bash
adb devices -l
```

El dispositivo debe aparecer como `device`, **no** como `unauthorized`.

### Opción 5: dispositivo físico por ADB inalámbrico

> [!important] Procedimiento genérico, no verificado en el repositorio
> Los artefactos de la última ejecución de `connectedAndroidTest` muestran que **se usó un dispositivo físico conectado por ADB inalámbrico** (la carpeta de resultados incluye un identificador de red junto a `M2101K7BL - 13`). **Pero el repositorio no contiene ningún procedimiento documentado**: ni script, ni tarea de Gradle, ni sección en `AGENTS.md`. Lo que sigue es el procedimiento estándar de `adb`.

```bash
# 1. Emparejar (la primera vez). Desde Android 11+ ambosiden deben estar en la misma red.
adb pair <IP_DEL_DISPOSITIVO>:<PUERTO_DE_EMparejamiento>
#    → introduce el código de 6 dígitos que muestra el teléfono

# 2. Conectar
adb connect <IP_DEL_DISPOSITIVO>:<PUERTO_DE_DEBUG>

# 3. Verificar
adb devices -l
```

En el teléfono, la opción de emparejamiento wireless está en **Ajustes → Opciones de desarrollador → Emparejamiento inalámbrico con ADB**. Android 10 y anteriores no lo soportan.

| Consideración | Detalle |
|---|---|
| Ambos dispositivos deben estar en la **misma red Wi-Fi** | La dirección IP debe ser alcanzable |
| La dirección IP del teléfono puede cambiar | Al cambiar de red hay que rehacer `adb connect` |
| El puerto de depuración puede variar | Android muestra ambos puertos en la pantalla de emparejamiento |
| Modo ahorro de energía | Puede desconectar el dispositivo; conviene desactivarlo durante la depuración |
| Android 11+ usa **RP** con verificación de red | Puede pedir confirmación en el teléfono |

> [!warning] No se documentan direcciones IP concretas
> A propósito **no** se incluyen aquí las IP y puertos concretos que aparecen en los artefactos locales de `app/build/`. Pertenecen al entorno de desarrollo de una máquina concreta, **no al repositorio**, y quedaría obsoletos de inmediato. Sustituir `<IP>` y `<PUERTO>` por los valores que muestre el propio teléfono.

### Instalar solo en un dispositivo concreto

Si hay varios conectados, se puede fijar con la variable de entorno `ANDROID_SERIAL`:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew installDebug
```

```bash
adb -s <SERIAL> install -r app/build/outputs/apk/debug/app-debug.apk
```

Esto es especialmente relevante dado **BUG-006**, donde `connectedAndroidTest` falla con varios dispositivos conectados.

### Lanzar la aplicación

```bash
# Conocer el paquete y la actividad principal
adb shell cmd package resolve-activity --brief com.example.earnit

# Lanzar
adb shell am start -n com.example.earnit/.MainActivity

# Detener
adb shell am force-stop com.example.earnit
```

### Depuración

```bash
adb logcat                                   # Ver todos los logs
adb logcat --pid=$(adb shell pidof com.example.earnit)   # Solo de la app
adb logcat | grep -i "earnit"

adb shell dumpsys activity activities | grep earnit   # Estado de las actividades
adb shell run-as com.example.earnit        # Acceso a los datos privados en debug

# Inspeccionar la base de datos Room
adb shell "run-as com.example.earnit cat databases/earnit_database" | sqlite3 -   # si sqlite3 está disponible
```

### Limpiar datos de la aplicación

```bash
adb shell pm clear com.example.earnit
```

**Borra la base de datos Room completa.** Es la forma más rápida de reproducir el estado de "app recién instalada", y también la única forma de deshacer un archivo accidental (dado **BUG-012**).

### Limpiar la app del dispositivo

```bash
adb uninstall com.example.earnit
```

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- **No existe ningún procedimiento de instalación documentado** en el repositorio más allá de la tabla de una línea de `AGENTS.md`.
- **No hay script de ayuda** (por ejemplo, un `./scripts/install.sh`) para automatizar compilación + instalación.
- `assembleRelease` no funciona de forma verificable (BUG-007, BUG-008), así que **no hay procedimiento de publicación**.

### Mejoras recomendadas (no decididas)

- **Documentar el procedimiento de publicación** cuando se decida distribuir la app: firma, `versionCode`/`versionName`, y subida a Google Play.
- **Añadir un apartado de depuración** al repositorio con los comandos de `logcat` más útiles para este proyecto.
- **Considerar un script de instalación** que acepte un `--device` para seleccionar emulador o teléfono.
- **Fijar `ANDROID_SERIAL`** en la configuración local o documentar cómo evitar el fallo de `connectedAndroidTest` con varios dispositivos (BUG-006).

## Preguntas abiertas

- ¿El dispositivo de pruebas habitual es el emulador o un teléfono físico? Condiciona si conviene documentar ADB inalámbrico en detalle.
- ¿Se publique en Google Play? De ello depende la necesidad de firmar y de un procedimiento de release.
- ¿Se necesita probar en un rango de versiones de Android distintas (26, 33, 37)? Hoy los artefactos muestran ejecución en API 33 y 37.
- ¿Se debe documentar el uso con `scrcpy` u otras herramientas de espejo de pantalla para depurar la UI? No hay nada de eso hoy.
- ¿Debe documentarse la configuración de firma de forma explícita en algún momento, o se mantiene fuera del repositorio por ser material sensible? ** Nunca debe incluirse ni una clave ni una contraseña en esta documentación. **