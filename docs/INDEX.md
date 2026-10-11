# EarnIt — Índice de documentación

## Propósito

Este es el **punto de entrada único** a la documentación técnica de EarnIt. Sirve como mapa del Vault de Obsidian (carpeta `docs/`) y como índice de búsqueda para quien retome el desarrollo del proyecto, tanto una persona como un agente de IA.

La documentación se escribió **inspeccionando el código y la configuración**, y se contrastó con los artefactos de build existentes (`app/build/reports/`, `app/build/test-results/`, `app/build/outputs/`).

> [!note] Regla de oro de este Vault
> El código y la configuración actuales son la única fuente de verdad sobre el comportamiento implementado. Si un documento y el código se contradicen, el documento está desactualizado: corrígelo o actualiza el código.

## Cómo usar este Vault

**Lee solo lo que tu tarea necesite.** No es un manual que se lea de principio a fin: úsalo como enrutador.

1. **¿No sabes qué hace la app ni hasta dónde llega?** → [[Contexto]]
2. **¿Vas a tocar código?** → [[Arquitectura]] primero, luego la sección de Android que corresponda.
3. **¿Vas a añadir una pantalla o un ViewModel?** → [[Arquitectura]] (incluye *Cómo añadir una funcionalidad*) → [[Navegacion]] → [[EstadoYViewModels]]
4. **¿Vas a tocar la base de datos?** → [[Persistencia]]
5. **¿Vas a añadir un icono o componente visual?** → [[Componentes]]
6. **¿Algo falla y no sabes por qué?** → [[Bugs]]
7. **¿No puedes compilar o instalar?** → [[Entorno]] → [[Build]] → [[Instalacion]]
8. **¿Quieres saber por qué el proyecto es como es?** → [[DecisionesTecnicas]] → [[ADR-0001]]

**Esfuerzo proporcional.** Si el cambio **no** afecta a comportamiento, arquitectura ni documentación —un texto de UI, un color, un nombre interno— ve directo al código y no abras ningún documento. Los pasos 1–8 son para tareas que sí tocan lo documentado.

> [!tip] Reglas de comportamiento, no conocimiento
> `AGENTS.md` (raíz del repositorio) contiene las reglas que gobiernan **cómo** se trabaja aquí: carga de contexto, invariantes, verificación y límites de actuación. Si estás leyendo documentación técnica, necesitas `docs/`, no `AGENTS.md`.

## Mapa de carpetas

| Carpeta | Contenido |
|---|---|
| `00-Overview/` | Propósito del producto, objetivos y estado verificable del roadmap |
| `01-Arquitectura/` | Capas, flujo de datos e índice de decisiones técnicas |
| `02-Android/` | Estructura de la app Android, Compose, Material 3, estado, navegación y persistencia |
| `03-Calidad/` | Pruebas existentes y registro de bugs |
| `04-Desarrollo/` | Entorno, comandos de compilación e instalación en dispositivo |
| `05-Decisiones/` | ADR (Architecture Decision Records) |
| `99-Templates/` | Plantillas reutilizables para añadir ADR y bugs |

## Cuándo consultar cada documento

### Punto de partida

| Documento | Consultarlo cuando… |
|---|---|
| [[Contexto]] | Necesitas saber qué es EarnIt, a qué público apunta y qué functionalities existen realmente hoy. **Léelo primero si acabas de llegar al repo.** |
| [[Objetivos]] | Quieres entender qué busca el producto y, sobre todo, **qué está explícitamente fuera de alcance**. |
| [[Roadmap]] | Quieres saber qué está implementado, qué está a medias y qué está planificado, sin inventarios de deseos. |

### Arquitectura

| Documento | Consultarlo cuando… |
|---|---|
| [[Arquitectura]] | Necesitas entender cómo se relacionan UI, ViewModels, casos de uso, repositorios y base de datos. **Documento central del proyecto.** |
| [[DecisionesTecnicas]] | Quieres el resumen de las decisiones arquitectónicas y por qué existen. Delega en los ADR, no los repite. |
| [[Navegacion]] | Vas a añadir, mover o eliminar una pantalla o una ruta. |

### Android

| Documento | Consultarlo cuando… |
|---|---|
| [[Android]] | Quieres el mapa general de la aplicación: `Manifest`, `Application`, `Activity`, SDK, recursos, backup. |
| [[UI]] | Trabajas con Material 3, el tema, la tipografía, el color o la internacionalización. |
| [[Componentes]] | Reutilizas o extiendes los componentes de `core/ui/components/`. |
| [[EstadoYViewModels]] | Añades o modificas un ViewModel, un `StateFlow` o el ciclo de vida del estado. |
| [[Persistencia]] | Modificas entidades, DAOs, conversores, fechas o el esquema de Room. |
| [[Permisos]] | Necesitas saber qué permisos existen hoy (ninguno) y qué haría falta para añadir notificaciones o tareas en segundo plano. |

### Calidad

| Documento | Consultarlo cuando… |
|---|---|
| [[Testing]] | Vas a ejecutar pruebas o necesitas saber qué está cubierto y qué no. |
| [[Bugs]] | Detectas un fallo o quieres saber qué problemas conocidos tiene la app. |

### Desarrollo

| Documento | Consultarlo cuando… |
|---|---|
| [[Entorno]] | Configuras una máquina nueva o algo no compila por versiones de JDK/SDK. |
| [[Build]] | Necesitas los comandos exactos de compilación, lint y pruebas. |
| [[Instalacion]] | Quieres instalar la app en un emulador o en un dispositivo Android físico. |

### Decisiones y plantillas

| Documento | Consultarlo cuando… |
|---|---|
| [[ADR-0001]] | Quieres saber por qué el proyecto usa inyección de dependencias manual y no Hilt. |
| [[ADR-Template]] | Vas a registrar una decisión arquitectónica nueva. |
| [[Bug-Template]] | Vas a registrar un bug nuevo. |

## Estado rápido del proyecto

Solo orientación. El detalle está en [[Contexto]], [[Roadmap]] y [[Bugs]].

App Android **100 % local** (Kotlin + Compose + Material 3), un módulo, un `Activity`, sin red ni permisos. Funcionalidad: crear, listar, marcar, ver historial, editar y archivar hábitos. Pruebas: 2 unitarios + 3 instrumentados; la cobertura de la lógica de dominio sigue siendo baja. **15 bugs** documentados en [[Bugs]], cinco de ellos ya corregidos.

## Criterio para añadir documentos y mantener este índice

**Añade un documento solo si** aporta conocimiento que **no cabe** en uno existente. Antes de crearlo, revisa la tabla de arriba: casi todo cabe en un documento existente.

| Tipo de conocimiento | Dónde va |
|---|---|
| Qué hace la app, objetivos, alcance | `00-Overview/` |
| Capas, flujo de datos, decisiones | `01-Arquitectura/` |
| Plataforma Android, UI, estado, navegación, persistencia, permisos | `02-Android/` |
| Pruebas y fallos | `03-Calidad/` |
| Entorno, build, instalación | `04-Desarrollo/` |
| Decisiones con consecuencias duraderas | `05-Decisiones/` (ADR) |
| Formularios reutilizables | `99-Templates/` |

**No añadas documentos para:** una sola clase, Una pantalla sin nada singular que explicar, o el historial de commits.

**Al añadir o mover un documento**, actualiza en el mismo cambio:

1. La tabla de *Cuándo consultar cada documento* — con enlace `[[…]]` y el criterio de cuándo leerlo.
2. El *Mapa de carpetas*, si cambió la organización.

**Al resolver un bug**, márcalo como corregido en [[Bugs]] en lugar de borrarlo: la causa suele ser el conocimiento más valioso del Vault.

**Regla de nombres**: los nombres de archivo son únicos en todo el Vault, porque los enlaces `[[…]]` de Obsidian los resuelven por nombre. Dos documentos con el mismo nombre romperían los enlaces de forma silenciosa.

## Convenciones de este Vault

- Los enlaces `[[Documento]]` son enlaces internos de Obsidian y funcionan porque los nombres de archivo son únicos en todo el Vault.
- Las rutas de código se escriben relativas a la raíz del repositorio, por ejemplo `app/src/main/java/com/example/earnit/feature/home/HomeScreen.kt`.
- Los identificadores de código (`HabitEntity`, `toEpochMillis`, `AppContainer`…) se citan literalmente, sin traducir.
- Cuando una sección no aplica a un documento, se indica explícitamente en lugar de dejarla vacía.
- No se documenta el estado puntual de una sesión de desarrollo ni datos del entorno local de ninguna máquina (rutas de SDK, IPs, puertos).

## Documentos de referencia fuera de `docs/`

| Archivo | Naturaleza |
|---|---|
| `AGENTS.md` (raíz del repo) | **Reglas de comportamiento para agentes**, no documentación técnica: carga de contexto bajo demanda, invariantes que evitan romper la build, orden de verificación y límites de actuación. No duplica el detalle técnico de `docs/`; lo enlaza. Una versión anterior contenía tablas de versiones y comandos, y dos afirmaciones inexactas (`assembleRelease` "requires sign config" —no existe bloque `signingConfig`— y el uso de Mockito/Robolectric/Turbine, que están declaradas y sin usar). Ambas contradicciones siguen registradas con evidencia en [[Bugs]] (BUG-007 y BUG-011). |
| `gradle/libs.versions.toml` | Fuente de verdad de todas las versiones de dependencias. |