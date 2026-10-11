# Objetivos

## Propósito

Fijar **qué busca EarnIt** como producto y, tan importante como eso, **qué NO busca**. Este documento separa las ambiciones del producto de las ambiciones técnicas, que están en [[Arquitectura]].

Para el estado concreto de implementación ver [[Roadmap]]. Para el contexto del producto ver [[Contexto]].

## Estado actual

El producto cumple su bucle principal (crear → marcar → revisar historial). **No tiene backend, ni cuentas de usuario, ni sincronización, ni notificaciones, ni exportación de datos.** Ninguna de esas capacidades está planeada en el repositorio; este documento refleja lo que el código actual sostiene, no un wish list.

## Información encontrada

### Objetivos de producto verificables

Estos objetivos se deducen del comportamiento que el código ya ofrece, no de documentación previa:

1. **Permitir definir hábitos con una identidad visual reconocible.** Cada hábito tiene nombre, descripción opcional, icono y color propios, elegibles desde el formulario. Es lo que hace `HabitFormScreen` con `IconPicker` y `ColorPicker`.
2. **Ofrecer una acción de marcado inmediato y de baja fricción.** Desde la pantalla principal, un toque sobre el indicador circular del hábito lo marca o desmarca sin abrir la pantalla de detalle. Es el `onToggle` de `HabitItem`.
3. **Conservar un historial por hábito.** `HabitDetailScreen` lista los registros ordenados del más reciente al más antiguo.
4. **Mantener el foco en el día actual.** La pantalla principal opera sobre `LocalDate.now()` y muestra únicamente hábitos `ACTIVE`.

### Objetivos técnicos verificables

1. **Cero infraestructura externa.** La app funciona completamente offline. No hay permisos de red en el `AndroidManifest.xml`, no hay cliente HTTP, no hay capas de backend.
2. **Android moderno.** `minSdk 26`, Jetpack Compose con Material 3, `enableEdgeToEdge()` en `MainActivity`, y **color dinámico** activo por defecto (`dynamicColor = true` en `ui/theme/Theme.kt:40`).
3. **Trazabilidad de las conversiones de datos.** Toda conversión `Entity` ↔ modelo de dominio está explícita en los repositorios, nunca implícita.

### Explícitamente fuera del alcance actual

Esta sección es tan importante como los objetivos: define los límites y evita que se asuman capacidades inexistentes.

| Fuera de alcance | Estado verificado |
|---|---|
| Backend / API REST | No existe ninguna interfaz de red en el código |
| Autenticación y cuentas de usuario | No hay ningún tipo, pantalla ni flujo de identidad |
| Multitenancy | No aplica: no hay usuarios |
| Sincronización / servicios en la nube | No hay ningún cliente de red |
| Redis, Docker, CI/CD | No hay configuración alguna en el repositorio |
| Notificaciones push o recordatorios | No hay `POST_NOTIFICATIONS`, ni `WorkManager`, ni `AlarmManager` |
| Tareas en segundo plano | No hay ningún `Service`, `BroadcastReceiver` ni worker |
| Sincronización con Wear OS, autos o web | No hay ningún módulo ni Activity adicional |
| Internationalización | No hay `values-xx`; todo el texto está hardcodeado |
| Persistencia en la nube | No hay DataStore, ni `CredentialManager`, ni nada remoto |

### Principios de diseño que se desprenden del código

No están escritos como tales en ningún archivo, pero se ven aplicados de forma consistente:

- **Una entidad de dominio por concepto, sin lógica de persistencia.** Los repositorios son el único punto donde existe conocimiento de Room.
- **Las fechas se convierten en un solo sitio.** `core/util/DateConverter.kt` centraliza `LocalDate` ↔ epoch ms, con un comentario que advierte explícitamente de no usar `LocalDate.ofEpochDay(millis / 86_400_000)` porque desplaza un día en zonas no UTC. Ese error existió y fue corregido (ver [[Persistencia]]).
- **Las pantallas no mutan datos directamente.** Las escrituras pasan por casos de uso desde los callbacks de `AppNavHost`. **Excepción notable:** `HomeScreen` y `HabitFormScreen` reciben *repositorios* directamente, lo cual rompe la capa y se detalla en [[Arquitectura]] y [[EstadoYViewModels]].
- **Nada de secretos ni de configuración sensible en el repositorio.** No hay claves, tokens ni credenciales; `local.properties` está correctamente ignorado por Git.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

No hay objetivos de producto declarados como tareas pendientes en forma de `TODO` en el código. El único `TODO` de negocio es el de frecuencia (`TODO(frequency)`), tratado en [[Contexto]] y [[Roadmap]].

### Mejoras recomendadas (no decididas)

- **Escribir los objetivos formalmente.** Este documento los reconstruye desde el comportamiento observable. Fijarlos por escrito evitaría ambigüedad sobre la dirección del producto.
- **Decidir qué pasa con los hábitos archivados.** Hoy archivarlos los hace desaparecer de la app sin posibilidad de recuperarlos desde la UI, aunque `getAllHabits()` ya exista en el repositorio. Es una decisión de producto, no solo técnica.
- **Considerar objetivo de datos del usuario** (exportar/importar). `allowBackup="true"` está activo con las reglas por defecto de Android Studio, pero esas reglas están sin personalizar (ver [[Permisos]]).

## Preguntas abiertas

- ¿Archivar debería ser reversible desde la UI? Hoy el dato se conserva en la base de datos, pero es inalcanzable.
- ¿Existe la intención de añadir hábitos con recurrencia semanal o mensual? El enum `Frequency` solo tiene `DAILY` y el filtrado ni siquiera está implementado.
- ¿La app debe soportarWidget de pantalla de inicio, notificaciones o Recordatorios? Nada en el repositorio lo sugiere, pero `targetSdk 37` implica que las notificaciones requerirían permiso en tiempo de ejecución.
- ¿Cuál es la prioridad entre localización y nuevas funcionalidades? Afecta directamente a si conviene externalizar cadenas ahora o después.