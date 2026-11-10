# Roadmap

## Propósito

Registrar **el estado verificable del proyecto**, separado en cuatro categorías: Implementado, En desarrollo, Planificado y Fuera de alcance. Un roadmap aquí es una **pizarra de estado**, no una lista de deseos: cada entrada corresponde a código que existe o a una limitación que se puede demostrar leyendo el repositorio.

Para los objetivos del producto ver [[Objetivos]].

## Estado actual

| Categoría | Recuento |
|---|---|
| Implementado | 8 funcionalidades |
| En desarrollo | 0 (sin trabajo a medias verificable) |
| Planificado | 0 declarado en el repositorio |
| Fuera de alcance | 9 capacidades ausentes |

## Información encontrada

### Implementado

Cada punto corresponde a código funcionando, con su punto de entrada exacto.

| Funcionalidad | Dónde vive | Notas |
|---|---|---|
| **Crear hábito** | `HabitFormScreen` + `CreateHabitUseCase` | Se abre desde el FAB "Nuevo hábito" de [[Contexto#Información encontrada\|HomeScreen]]. Valida que el nombre no esté vacío. |
| **Listar hábitos activos del día** | `HomeScreen` + `HomeViewModel` + `GetTodaysHabitsUseCase` | `StateFlow` con `SharingStarted.WhileSubscribed(5_000)`. Filtra `status = 'ACTIVE'`, ordenados por `createdAt DESC`, y oculta los hábitos creados después del día consultado. |
| **Calendario y selección de día** | `HabitCalendar` + `CalendarDates` + `HomeViewModel` | Tira semanal siempre visible y mes expandible. Solo se seleccionan días hasta hoy; las flechas cambian semana o mes. |
| **Marcar / desmarcar un día** | `HabitItem.onToggle` → `ToggleCompletionUseCase` | Lógica de alternancia en `ToggleCompletionUseCase.invoke()`: `existing?.completed != true`. Opera sobre el día seleccionado. |
| **Ver detalle e historial** | `HabitDetailScreen` + `HabitDetailViewModel` + `GetHabitHistoryUseCase` | Lista todos los registros del hábito ordenados por fecha descendente. El detalle muestra el día elegido y ofrece "Volver a hoy". |
| **Editar hábito** | `HabitFormScreen` (modo edición) + `UpdateHabitUseCase` | Conserva `createdAt` y `status` al guardar (BUG-002 corregido). |
| **Archivar hábito** | Botón de archivo en `HabitDetailScreen` → `ArchiveHabitUseCase` | Persiste `HabitStatus.ARCHIVED` y saca el hábito de la lista principal. ⚠️ **Irreversible desde la UI**. |
| **Ver la versión de la app** | `AppVersionButton` en la `TopAppBar` de `HomeScreen` | Lee `versionName` del `PackageManager` y lo muestra en un diálogo "Acerca de EarnIt". |

### En desarrollo

**Ninguno.** No hay functionality a medio implementar en el árbol de trabajo: los cuatro únicos `TODO` del proyecto están en `HabitRepositoryImpl.kt:38` (`TODO(frequency)`) y en `data_extraction_rules.xml:8` (comentario de plantilla de Android Studio, sin efecto funcional).

> [!info]
> El árbol de trabajo contiene los cambios sin confirmar de la funcionalidad de calendario (rama `feature/calendario-habitos`). Esos cambios **ya están integrados** en el código que describe este documento; esta nota solo explica por qué el estado del repositorio puede diferir del último commit.

### Planificado

**Nada está planificado en el repositorio.** No hay `TODO` de funcionalidad, ni issues, ni rama de trabajo futuro, ni sección "próximamente" en ningún archivo. La única funcionalidad claramente *iniciada pero no terminada* es la de frecuencias:

| Intención | Evidencia | Estado |
|---|---|---|
| Hábitos con frecuencia distinta de `DAILY` | `Frequency` solo declara `DAILY`; `FrequencySelector` solo dibuja esa opción; `getActiveHabitsForDate(date)` **ignora el parámetro `date`** | **Diseñada pero no implementada.** El filtrado por fecha no existe. |

### Fuera de alcance actual

No implementado **y sin ninguna señal de intención** en el repositorio:

| Capacidad | Verificación |
|---|---|
| Backend / API REST | No hay cliente HTTP ni permiso `INTERNET` |
| Autenticación / usuarios | No hay tipos ni pantallas de identidad |
| Sincronización en la nube | No hay código de red |
| Notificaciones y recordatorios | Sin `POST_NOTIFICATIONS`, sin `WorkManager` |
| Tareas en segundo plano | Sin `Service` ni `BroadcastReceiver` |
| Hábitos archivados consultables | `getAllHabits()` existe pero **ninguna pantalla lo llama** |
| Borrado de hábito | `HabitRepository.delete()` existe pero **ninguna pantalla lo llama** |
| Métricas numéricas por hábito | `HabitCompletion.value` existe pero **siempre es `null`** |
| Localización | Sin `values-xx`, textos hardcodeados |

Ver [[Permisos]] para el detalle de la ausencia de permisos.

## Pendientes

### Trabajo pendiente ya identificado en el proyecto

Este listado recoge **lo que el propio código declara pendiente o lo que la lectura del código demuestra como incompleto**. No son sugerencias.

| # | Pendiente | Evidencia |
|---|---|---|
| 1 | Implementar el filtrado por frecuencia | `TODO(frequency)` en `HabitRepositoryImpl.kt:38` |
| 2 | Decidir si `assembleRelease` debe funcionar | Faltan `proguard-rules.pro` y `signingConfig` (BUG-007, BUG-008) |
| 3 | Eliminar código muerto | 4 métodos de DAO, 3 de repositorio y varios imports sin uso (BUG-009) |
| 4 | Sustituir la salida con código 1 de `connectedAndroidTest` con un segundo dispositivo conectado | BUG-006 en [[Bugs]] |

### Mejoras recomendadas (no decididas)

Estas **no** están en el código ni pendientes de aprobación; son propuestas.

- Añadir una pantalla de hábitos archivados que use el `getAllHabits()` ya existente, y decidir si archivar es reversible.
- Añadir confirmación antes de archivar o borrar.
- Añadir pruebas unitarias de los casos de uso antes de tocar la lógica de fechas o de Archive (ver [[Testing]]).
- Externalizar las cadenas de la UI a `strings.xml`.
- Personalizar `backup_rules.xml` y `data_extraction_rules.xml`, hoy con contenido de plantilla.
- Introducir un `Value` en `HabitCompletion` o eliminar el campo mientras no se use.

## Preguntas abiertas

- ¿Qué prioridades tiene el proyecto ahora mismo? El repositorio no contiene ninguna lista de tareas.
- ¿La app se publica en Google Play? El `applicationId` sigue siendo `com.example.earnit` y no hay firma de release, lo que sugiere que no.
- ¿Debe el historial mostrar rachas (streaks) o estadísticas? No hay ningún indicio en el código.
- ¿Con qué frecuencia se espera migrar el esquema de Room? Hoy `fallbackToDestructiveMigration()` hace que cualquier cambio de esquema **borre los datos del usuario** (ver [[Persistencia]]).