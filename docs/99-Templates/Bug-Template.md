# BUG-XXXX: Título corto del fallo

> **Cómo usar esta plantilla**
> 1. Copia este archivo a `docs/03-Calidad/Bugs.md` y pega el bloque siguiente, **o** crea `docs/03-Calidad/BUG-XXXX.md` si el bug tiene suficiente entidad propia.
> 2. Numera correlativamente: BUG-001, BUG-002, …
> 3. **Rellena "Pasos para reproducir" y "Esperado" / "Observado" con datos reales.** Un bug sin pasos no es un bug documentado.
> 4. Distingue lo confirmado de lo supuesto: usa `Estado: Observado` si solo dispones del código, `Estado: Confirmado` si lo has reproducido o verificado en un artefacto.
> 5. **No incluyas secretos** en la sección de evidencias.

---

## BUG-XXXX — <!-- título en una línea, sin punto final -->

| Campo | Valor |
|---|---|
| **ID** | BUG-XXXX |
| **Título** | <!-- una frase: qué falla, no por qué --> |
| **Severidad** | <!-- 🔴 Crítico: pérdida/corrupción de datos · 🟠 Alto: funcionalidad rota visible · 🟡 Medio: degradación o incoherencia · 🔵 Configuración/Documentación --> |
| **Estado** | <!-- Confirmado (reproducido) / Observado (verificado en artefactos) / Sospechado (solo lectura de código) / Corregido --> |
| **Fecha de detección** | <!-- AAAA-MM-DD --> |
| **Ubicación** | `ruta/al/Archivo.kt:línea` |
| **Componente** | <!-- UI / Estado / Persistencia / Build / Documentación --> |
| **Fix en** | <!-- PR, commit o "pendiente" --> |

**Estado actual** (opcional): <!-- Solo si el bug está corregido -->

- **Resuelto por:**
- **Verificado con:**

### Descripción

<!--
Qué ocurre, en prosa. Sin jerga innecesaria.
Incluye el fragmento de código relevante si ayuda (máximo 10-15 líneas).
-->

```kotlin
// fragmento relevante, si aplica
```

### Causa raíz

<!--
Por qué ocurre. Sigue el rastro del dato en el código, no en la conjetura.
Si aún no se sabe, dilo explícitamente: "Causa no confirmada; hipótesis: …"
-->

### Pasos para reproducir

1.
2.
3.

**Datos de partida:** <!-- app recién instalada / con datos de ejemplo / estado concreto -->

### Comportamiento esperado

<!-- Lo que un usuario razonable esperaría. -->

### Comportamiento observado

<!-- Lo que hace realmente. Sé específico y observable. -->

### Impacto

<!--
A quién afecta y cómo. Contexto de datos si los pierde o los corrompe.
Sin impacto real, no es un bug: es una mejora. Muévelo a la sección de pendientes
del documento correspondiente.
-->

### Archivos relacionados

| Archivo | Relación |
|---|---|
| `…/X.kt:línea` | Origen del defecto |
| `…/Y.kt:línea` | Consumidor afectado |

### Evidencias

<!--
Capturas, logs, informes de test, salida de lint.
Referencia rutas de artefactos, sin volcar contenidos enormes.
-->

| Evidencia | Ruta |
|---|---|
| | |

### Posible solución

<!--
Si la causa está clara, describe el arreglo. NO lo apliques en este documento:
este archivo documenta, no modifica código.

Si hay más de una vía, enuméralas con su coste.
-->

### Resolución

<!--
Vacío mientras el bug esté abierto.
Cuando se cierre:

**Causa final:**
**Cambio aplicado:**
**Cómo se verifica:**
**Prueba añadida:** (enlace al test que lo cubre)
-->

### Trabajo pendiente ya identificado

<!--
Acciones concretas que quedan abiertas tras el diagnóstico.
Distínguelas de las mejoras sugeridas: unas son necesarias para cerrar el bug,
otras son comentarios opcionales.
-->

-