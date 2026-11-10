# Android

## Propósito

Describir la **estructura de la aplicación Android**: el manifiesto, el ciclo de vida del proceso, el `Activity`, los valores de SDK, los recursos y las reglas de copia de seguridad. Es la vista "de plataforma" del proyecto.

Para el contenido visual ver [[UI]]. Para la base de datos ver [[Persistencia]].

## Estado actual

| Elemento | Estado |
|---|---|
| `Application` personalizada | Implementado (`EarnItApplication`) |
| `Activity` única | Implementado (`MainActivity`) |
| `enableEdgeToEdge()` | Implementado |
| Soporte de rotación / cambios de configuración | **No hay nada específico**: no se guarda estado, ni `SavedStateHandle`, ni `rememberSaveable` |
| Permisos declarados | **Ninguno** (ver [[Permisos]]) |
| WorkManager / AlarmManager / Services | **Ninguno** |
| Deep links / App Links | **Ninguno** |

## Información encontrada

### Manifiesto

`app/src/main/AndroidManifest.xml` completo y **muy corto**. No declara ni un solo `<uses-permission>`.

| Atributo | Valor |
|---|---|
| `android:name` de `<application>` | `.EarnItApplication` |
| `android:allowBackup` | `true` |
| `android:dataExtractionRules` | `@xml/data_extraction_rules` |
| `android:fullBackupContent` | `@xml/backup_rules` |
| `android:supportsRtl` | `true` |
| `android:theme` | `@style/Theme.EarnIt` |
| `<activity>` | `.MainActivity`, `exported="true"`, `windowSoftInputMode="adjustResize"` |
| Intent filter | `MAIN` + `LAUNCHER` (único punto de entrada) |

Detalles:

- El tema XML es `android:Theme.Material.Light.NoActionBar` (`res/values/themes.xml`). Es un tema **de plataforma**, no de AppCompat ni Material Components: la app es 100 % Compose y el tema solo evita la barra de acción nativa durante el arranque.
- `windowSoftInputMode="adjustResize"` es la opción correcta para que el teclado redimensione la pantalla del formulario.
- `android:label="@string/app_name"` está **duplicado** en `<application>` y `<activity>`; Lint lo señala como `RedundantLabel` (aviso, no error).
- El namespace `xmlns:tools` está declarado pero **no se usa** en el manifiesto.

### Ciclo de vida del proceso

```kotlin
// EarnItApplication.kt
class EarnItApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
```

El grafo de dependencias se construye **una sola vez**, al arrancar el proceso. `AppContainer` recibe `this` y lo pasa a `AppDatabase.getInstance(context)`, que a su vez usa `context.applicationContext` internamente.

### `MainActivity`

`MainActivity` es un `ComponentActivity` sin ninguna otra responsabilidad:

1. Obtiene el `AppContainer` de la `Application` mediante `by lazy { (application as EarnItApplication).appContainer }`.
2. Declara tres ViewModel con `by viewModels { … }` y sus factories.
3. En `onCreate`: `enableEdgeToEdge()` y `setContent { … }`.

Estructura del contenido:

```text
EarnItTheme                                  ← tema Material 3
└── Scaffold(Modifier.fillMaxSize())          ← el andamiaje global
    └── AppNavHost(Modifier.padding(innerPadding).fillMaxSize())
```

Hay **dos `Scaffold` anidados**: el global de `MainActivity` y uno propio en cada pantalla. El `Scaffold` externo solo aporta el `innerPadding` que respeta las barras del sistema tras `enableEdgeToEdge()`.

> [!warning] Edge-to-edge con `targetSdk 37`
> `enableEdgeToEdge()` es explícito y correcto, pero conviene revisar el resultado visual en el sistema: con un `targetSdk` tan alto, el comportamiento por defecto del sistema ya es edge-to-edge.

### Valores de SDK y de compilación

| Propiedad | Valor | Implicación |
|---|---|---|
| `minSdk` | **26** (Android 8.0 Oreo) | `java.time` está disponible **de forma nativa**: por eso el proyecto puede usar `LocalDate`, `Instant` y `ZoneId` sin desugaring ni librería externa |
| `targetSdk` | **37** | Las notificaciones exigirían permiso en tiempo de ejecución; el edge-to-edge es el comportamiento por defecto |
| `compileSdk` | **37** | Coincide con `targetSdk` |
| `source`/`targetCompatibility` | `VERSION_11` | Coherentes con `jvmTarget = JVM_11` |
| `buildFeatures.compose` | `true` | Sin `composeOptions`: el BOM fija la versión |
| `isMinifyEnabled` | `false` | R8 desactivado en release |

> [!note] El target JVM 11 no contradice el daemon JDK 25
> `jvmTarget = JVM_11` controla el **bytecode generado** (major version 55). El JDK 25 es el que **ejecuta** Gradle y compila. Son cosas distintas. Es normal ver un aviso indicando que Kotlin no soporta el target 25 y hace fallback a JVM_24; el resultado sigue siendo bytecode Java 11.

### Recursos

| Recurso | Contenido | Nota |
|---|---|---|
| `res/values/strings.xml` | Solo `app_name` = "EarnIt" | Todo el demás texto está hardcodeado en Kotlin (ver [[UI]]) |
| `res/values/themes.xml` | `Theme.EarnIt` → tema de plataforma | Sin modo noche definido en XML |
| `res/values/colors.xml` | 7 colores (`purple_*`, `teal_*`, `black`, `white`) | **Los 7 están sin usar**: Lint los marca como `UnusedResources` |
| `res/drawable/` | `ic_launcher_background.xml`, `ic_launcher_foreground.xml` | Icono adaptativo estándar de plantilla |
| `res/mipmap-*/` | WebP del icono en 5 densidades | Estándar |
| `res/xml/backup_rules.xml` | `full-backup-content` **vacío** | Contenido de plantilla sin personalizar |
| `res/xml/data_extraction_rules.xml` | `cloud-backup` vacío, `device-transfer` comentado | Contenido de plantilla sin personalizar |
| `src/main/keepRules/rules.keep` | Solo comentarios | Reglas de R8, irrelevantes con `isMinifyEnabled = false` |

Sobre los colores: el tema de Compose define sus propios colores en `ui/theme/Color.kt` (`Purple40`, `Purple80`, etc.), por lo que `res/values/colors.xml` es **residuo de la plantilla** y no participa en la UI.

### Copia de seguridad

`allowBackup="true"` con las reglas por defecto de Android Studio (sin `<include>` ni `<exclude>`). En la práctica:

- En Android 12+ se aplica `data_extraction_rules.xml`: **copia a la nube sin restricciones declaradas**.
- En Android 11 y anteriores se aplica `backup_rules.xml`: también sin restricciones.

Esto significa que **la base de datos Room de hábitos se incluye en la copia de seguridad**. Puede ser lo deseable, pero conviene decidirlo explícitamente en lugar de heredar la plantilla. Ver [[Permisos]].

### Versión y firma

- `versionCode = 2`, `versionName = "2.0.0"`.
- **Versionado SemVer** (`MAJOR.MINOR.PATCH`): `PATCH` para correcciones, `MINOR` para funcionalidad nueva compatible y `MAJOR` para cambios incompatibles. `versionCode` se incrementa en **cada** cambio.
- **No existe bloque `signingConfig`** en `app/build.gradle.kts`. Ver [[Build]] y BUG-007 en [[Bugs]].
- **No existe `app/proguard-rules.pro`**, aunque `app/build.gradle.kts:24` lo referencia. Ver BUG-008 en [[Bugs]].
- **No hay `buildTypes` adicionales** (ni `debug` explícito, ni *flavors*, ni `buildConfigField`). Solo `debug` (implícito) y `release`.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

- `AndroidManifest.xml:18` — `android:label` redundante en la actividad (aviso de Lint `RedundantLabel`).
- `xmlns:tools` declarado y sin usar en el manifiesto.
- `res/values/colors.xml` — 7 recursos sin usar (aviso de Lint `UnusedResources`).
- `backup_rules.xml` y `data_extraction_rules.xml` — contenido de plantilla sin personalizar; `data_extraction_rules.xml:8` contiene un `TODO` de Android Studio.
- Referencia a `proguard-rules.pro` inexistente (BUG-008).

### Mejoras recomendadas (no decididas)

- **Eliminar `res/values/colors.xml`**: no aporta nada y genera 7 avisos de Lint en cada ejecución.
- **Personalizar las reglas de copia de seguridad** para decidir explícitamente si la base de datos de hábitos debe incluirse.
- **Definir `buildConfigField`** para separar entornos si alguna vez se necesita.
- **Añadir `android:localeConfig`** si en el futuro se decide localizar la app (hoy no aplica: solo hay un `strings.xml`).
- **Verificar el comportamiento de edge-to-edge** en el sistema con `targetSdk 37`.

## Preguntas abiertas

- ¿Debe la base de datos incluirse en la copia de seguridad de Google? La configuración actual lo hace implícitamente.
- ¿Se necesita soporte de procesos múltiples? El contenedor asume un único proceso y un `AppContainer` único.
- ¿Debe `MainActivity` seguir siendo el único `Activity`? Hoy no hay ningún segundo punto de entrada, y `AppNavHost` asume que todo el flujo pasa por él.
- ¿La ausencia de firma de release es intencionada porque la app aún no se publica, o es un descuido? Determina si `assembleRelease` debe funcionar.
- ¿Se soportarán pantallas plegables o transiciones de configuración complejas? No hay código que lo contemple.