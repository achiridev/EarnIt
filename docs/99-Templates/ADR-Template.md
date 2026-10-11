# ADR-XXXX: Título de la decisión

> **Cómo usar esta plantilla**
> 1. Copia este archivo a `docs/05-Decisiones/ADR-XXXX.md` con el siguiente número libre.
> 2. Renombra `# ADR-XXXX` por `# ADR-XXXX: <título>`.
> 3. Rellena todas las secciones. **La sección "Opciones consideradas" es obligatoria**: una decisión sin alternativas descartadas no es una decisión.
> 4. Pon el estado en `Propuesta` hasta que se acepte. No escribas un ADR "aceptado" para algo que aún no se ha decidido.
> 5. Enlázalo desde [[DecisionesTecnicas]].
>
> **Regla de honestidad:** un ADR documenta lo que se decidió **y por qué**, en el momento en que se decidió. No es un documento de justificación a posteriori. Si la justificación se escribe después del hecho, indícalo explícitamente.

| Campo | Valor |
|---|---|
| **ID** | ADR-XXXX |
| **Título** | <!-- Una frase: qué se decide, no cómo --> |
| **Estado** | <!-- Propuesta / Aceptada / Reemplazada por ADR-XXXX / Obsoleta --> |
| **Fecha** | <!-- AAAA-MM-DD --> |
| **Ámbito** | <!-- Qué parte del proyecto afecta: "Todo el proyecto (`:app`)", "Capa de datos", … --> |
| **Sustituye a** | <!-- ADR-XXXX, o "—" --> |
| **Sustituida por** | <!-- ADR-XXXX, o "—" --> |
| **Relacionada con** | <!-- ADR-XXXX y documentos del vault --> |

## Contexto

<!--
Qué condiciones del proyecto obligan a decidir algo.
Describe el estado del proyecto en el momento de decidir: tamaño, dependencias,
restricciones del toolchain, plazos, restricciones que ya existen en el código.

Regla: cita archivos y líneas reales. "El proyecto tiene 3 ViewModel
(`MainActivity.kt:27-39`) y 6 casos de uso (`domain/usecase/`)".
-->

**Estado verificable en el momento de la decisión:**

- Módulos: <!-- ¿cuántos? -->
- ViewModel: <!-- ¿cuántos? -->
- Dependencias relevantes: <!-- lista -->

## Problema

<!--
La pregunta concreta que esta decisión responde.

Escribe una pregunta, no un tema:
  ✅ "¿Cómo construimos el grafo de dependencias en un proyecto de un solo módulo?"
  ❌ "Inyección de dependencias"

Si la decisión responde a más de una pregunta, crea varios ADRs.
-->

## Opciones consideradas

<!--
OBLIGATORIO. Mínimo dos alternativas reales además de la elegida.

Para cada opción:
  - Qué implica (qué habría que añadir o cambiar).
  - Qué tiene a favor.
  - Qué tiene en contra, ESPECÍFICAMENTE para este proyecto.

No inventes opciones descartadas para quedar bien con la elegida.
Una opción que no se considera de verdad no debe aparecer.
-->

### Opción A — <!-- nombre de la opción -->

- **Implica:**
- **A favor:**
- **En contra:**

### Opción B — <!-- nombre de la opción -->

- **Implica:**
- **A favor:**
- **En contra:**

### Opción C — <!-- nombre de la opción -->

- **Implica:**
- **A favor:**
- **En contra:**

## Decisión

<!--
Qué se decide, de forma inequívoca y en una frase.

Luego el detalle técnico: qué clases, qué archivos, qué configuración.
Cita rutas reales. Si la decisión afecta a archivos concretos, dilo aquí.
-->

## Justificación

<!--
Por qué esta opción y no las otras, en este proyecto concreto.

Este texto es el que un lector futuro necesitará dentro de un año para entender
por qué el código es así. Razona sobre las restricciones REALES del proyecto,
no sobre generalidades.

Prefiere: "porque KSP ya está en uso y AGP 9 lo trata de forma delicada"
a "porque es más moderno".
-->

## Consecuencias

<!--
Divide en positivas, negativas y neutras. Sé honesto con las negativas:
si la decisión no tiene ningún coste, probablemente no es una decisión real.
-->

### Positivas

-

### Negativas

-

### Neutras / a vigilar

-

## Alternativas sobre la mesa

<!--
Qué señales indicarían que esta decisión debe revisarse.

Ejemplo:
  - "Si el proyecto supera los ~8 ViewModel, el coste de mantenimiento
     por pantalla debe reevaluarse."
  - "Si se divide en varios módulos, la inyección manual deja de ser viable."

Esto convierte el ADR en algo útil y no en un documento histórico.
-->

| Señal de que hay que revisar esta decisión | Revisar hacia |
|---|---|
| | |
| | |

## Referencias relacionadas

<!--
Documentos del vault ([[…]]) y archivos del repositorio.
No incluyas secretos, claves, tokens ni URLs internas.
-->

| Documento | Relación |
|---|---|
| | |

## Registro de cambios

| Fecha | Cambio |
|---|---|
| AAAA-MM-DD | Borrador inicial |
| | Cambio de estado a «Aceptada» |
| | Sustituida por ADR-XXXX |