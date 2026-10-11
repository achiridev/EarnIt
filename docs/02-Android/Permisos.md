# Permisos

## Propósito

Registrar el **estado verificable de los permisos y capacidades de plataforma** de EarnIt, y dejar por escrito qué haría falta si en el futuro se implementan notificaciones, tareas en segundo plano o cualquier otra capacidad que exija permiso.

Para el resto de la superficie de plataforma ver [[Android]].

## Estado actual

| Concepto | Estado verificado |
|---|---|
| Permisos declarados en `AndroidManifest.xml` | **Ninguno. Cero.** |
| `android.permission.INTERNET` | No declarado |
| `android.permission.POST_NOTIFICATIONS` | No declarado |
| Permisos de almacenamiento | No declarados |
| Permisos de calendario / contactos / ubicación | No declarados |
| Notificaciones | **No implementadas** |
| WorkManager / `AlarmManager` / `JobScheduler` | **Ninguno** |
| `Service` en primer plano | **Ninguno** |
| `BroadcastReceiver` | **Ninguno** |
| Deep links / App Links | **Ningunos** |
| Red / cliente HTTP | **Ninguno** |

**Conclusión:** EarnIt es hoy una app completamente offline. No solicita nada al usuario ni al sistema por encima de lo implícito.

## Información encontrada

### Evidencia directa

`app/src/main/AndroidManifest.xml` **no contiene ninguna etiqueta `<uses-permission>`**. El manifiesto completo tiene 29 líneas y su única fuente de capacidad es el `intent-filter` `MAIN`/`LAUNCHER`.

Corroboración cruzada — no hay ninguna otra fuente de capacidad en el proyecto:

| Buscar… | Resultado |
|---|---|
| `NotificationManager`, `NotificationCompat` | 0 resultados en `app/src` |
| `NotificationChannel` | 0 resultados |
| `WorkManager`, `CoroutineWorker`, `Worker` | 0 resultados |
| `AlarmManager`, `JobScheduler` | 0 resultados |
| `Service`, `BroadcastReceiver` | 0 resultados |
| `HttpURLConnection`, `OkHttp`, `Retrofit`, `Socket` | 0 resultados |
| `INTERNET` | 0 resultados |

> [!note] La ausencia de red es coherente
> Sin permiso `INTERNET`, cualquier intento de red fallaría con `SecurityException` en el sistema. Es decir que la app no es solo "sin backend": es **técnicamente incapaz** de hablar con la red. Ver [[Objetivos]].

### Lo que sí hace la app sin permiso

Estas operaciones **no requieren permiso** y son las que la app realiza:

| Operación | Por qué no necesita permiso |
|---|---|
| Base de datos Room en almacenamiento interno | El sandbox de la app |
| Notificaciones del sistema del launcher | Controladas por el usuario, no por la app |
| `Scaffold` / edge-to-edge | Sin permisos |
| Leer `LocalConfiguration.current.locales[0]` | Configuración pública del dispositivo |

### Copia de seguridad y transferencia entre dispositivos

Aunque no es un permiso, sí es una capacidad de la plataforma que **afecta a los datos del usuario** y conviene decidir de forma explícita:

| Atributo | Valor | Efecto |
|---|---|---|
| `android:allowBackup` | `true` | La copia de seguridad de Android está habilitada |
| `android:dataExtractionRules` | `@xml/data_extraction_rules` | Aplica en Android 12+ |
| `android:fullBackupContent` | `@xml/backup_rules` | Aplica en Android 11 y anteriores |

Ambas reglas están **sin personalizar** (contenido de plantilla de Android Studio, todos los `<include>` y `<exclude>` comentados). Consecuencia práctica: **la base de datos de hábitos entra en la copia de seguridad sin restricciones**, tanto a la nube como en transferencias de dispositivo a dispositivo.

### Qué haría falta si se implementara cada capacidad

Este apartado es **prospección, no descripción**. Se incluye porque es el motivo de existir el documento, y está redactado en términos de requisitos de plataforma, no de sugerencias de implementación.

| Si se quisiera… | Requisito de plataforma |
|---|---|
| **Notificar al usuario** | Declarar `android.permission.POST_NOTIFICATIONS` (obligatorio desde API 33) **y** solicitarlo en tiempo de ejecución con `requestPermissions`. Con `targetSdk 37`, declarar el permiso en el manifiesto **no basta**: sin la aceptación explícita no se muestra ninguna notificación |
| **Programar recordatorios** | Canal de notificaciones + `AlarmManager`, `WorkManager` o `JobScheduler`. `WorkManager` **no requiere permiso**, pero las notificaciones que dispare sí |
| **Acceso a la red** | `android.permission.INTERNET` |
| **Descargar o guardar un archivo** | Según el destino: almacenamiento propio sin permiso; `READ_MEDIA_IMAGES` para medios en Android 13+ |
| **Sincronizar con el calendario** | `READ_CALENDAR` / `WRITE_CALENDAR` |
| **Ejecutar en segundo plano de forma continua** | `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_*` según el tipo, y notificación obligatoria asociada |
| **Recibir el arranque del dispositivo** | `RECEIVE_BOOT_COMPLETED` + un `BroadcastReceiver` |
| **Abrir enlaces externos hacia la app** | `App Links` (`autoVerify="true"`), sin permiso |

> [!important] Ninguna de estas capacidades está implementada
> La tabla anterior **no describe functionality existente**. Describe lo que la plataforma exigiría. Si alguien lee este documento buscando qué hace la app hoy, la respuesta es: **nada de esto**.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

No hay `TODO` ni código parcialmente implementado en materia de permisos o capacidades de plataforma. La ausencia es total y coherente con el diseño offline.

Los puntos abiertos que sí existen en esta área:

- `res/xml/backup_rules.xml` y `res/xml/data_extraction_rules.xml` están sin personalizar. `data_extraction_rules.xml:8` contiene un `TODO` de plantilla de Android Studio.

### Mejoras recomendadas (no decididas)

- **Personalizar `data_extraction_rules.xml`** para decidir explícitamente si la base de datos de hábitos debe incluirse en la copia de seguridad. Hoy se incluye por herencia, no por decisión.
- **Documentar la política de permisos antes de la primera capacidad que los necesite.** Una vez que la app notifique, el usuario verá un diálogo de permiso, y conviene saber de antemano qué se le va a pedir y por qué.
- **Añadir un `README.md`** que declare explícitamente que la app no requiere ningún permiso, como punto de partida para futuras funcionalidades.

## Preguntas abiertas

- ¿Se prevé alguna vez notificaciones o recordatorios de hábitos? Es la capacidad más natural para esta app y la que más permisos exigiría.
- ¿Debe la copia de seguridad incluir la base de datos? Si en el futuro se añade información más sensible, la respuesta podría cambiar.
- ¿Se exportará alguna vez información del usuario (CSV, por ejemplo)? Requeriría decidir permisos de almacenamiento según el destino.
- ¿Se conectará con servicios externos (calendario, wearables)? Hoy no hay ningún indicio en el repositorio.