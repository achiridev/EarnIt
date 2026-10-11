# AGENTS.md

Behavioral guide for agents working on **EarnIt** (Android, Kotlin, Compose).

This file does **not** document the project: it says **how to work** in it.
Technical detail lives in `docs/`. The code is always the source of truth.

## 1. Context loading (on demand)

Do not read `docs/` wholesale. For any task:

1. This file.
2. `docs/INDEX.md` — only to locate what you need.
3. The 1–3 documents the task actually requires.
4. The affected code.

**Proportional effort.** If the change does not affect behavior, architecture or
documentation (a string, a color, an internal name), go straight to the code and do not
open `docs/`. Never skip this file.

If the docs do not cover what you need, **read the code** and record the gap when it
affects documented information.

## 2. Invariants — breaking these costs a full build

Each one has broken the build before. The reasoning lives in
`docs/01-Arquitectura/DecisionesTecnicas.md`.

- **Do not delete `gradle/gradle-daemon-jvm.properties`**, and never add
  `org.gradle.java.home` to `gradle.properties`. The first pins the daemon to JDK 25;
  the second is silently ignored and desyncs the daemon.
- **Do not add the `org.jetbrains.kotlin.android` / `kotlin("android")` plugin.**
  AGP 9 provides Kotlin natively.
- **Do not change versions by hand.** Everything lives in `gradle/libs.versions.toml`
  and is referenced through `libs.*`. Upgrading without verifying has already broken
  the build (Room/KSP2).
- **Change `jvmTarget` and `sourceCompatibility`/`targetCompatibility` together.**
- **The warning `Kotlin does not yet support 25 JDK target… JVM_24` is expected.** Ignore
  it; the bytecode is still Java 11.
- **`AppDatabase` uses `fallbackToDestructiveMigration()`.** Any schema change wipes user
  data. If the task requires touching it, say so before touching anything.

## 3. Conventions you must not break

- **No Hilt.** The graph is wired manually (`AppContainer`). Every new ViewModel means
  editing **three** places: `AppContainer`, `core/di/ViewModelFactories.kt` and
  `MainActivity`. See `docs/05-Decisiones/ADR-0001.md`.
- **`Entity` ↔ model conversions are explicit** (`toEntity()` / `toDomain()` private in
  the repositories). Do not add logic to DAOs or implicit conversions.
- **Keep Turbine at `1.0.0`**: `1.0.1` does not exist on Maven Central.
- **`local.properties` is git-ignored.** Do not edit it or let it reach the repo.

## 4. How to work

**Before.** Understand the goal and how the result is checked. Locate the affected
files and read only the docs you need. **Check whether the functionality already
exists** before creating it. Ask only when information is missing that you cannot safely
infer; do not demand lengthy plans for small changes.

**While.** Make the smallest change that solves the problem. Follow existing conventions
and reuse what is already there. Do **not** introduce refactors, layers, abstractions,
dependencies or architecture the task did not ask for. Do not hide errors behind empty
catches, invented values or silent temporary workarounds.

**After.** Review your own diff. Run the verification. Update only the affected docs.
**Report honestly**: distinguish what passed, what failed, and what you **could not
run**. Never claim you compiled, tested or installed something you did not run.

## 5. Verification

```bash
./gradlew lintDebug test assembleDebug
```

In that order: lint catches errors tests never see. Skipping it produces a false
"all green". Command reference and per-task status: `docs/04-Desarrollo/Build.md`.

## 6. Documentation

**Update `docs/` when** a decision, documented behavior, feature, data model, migration
or build/test procedure changes — or when you find a gap between docs and code.

**Do not update** for internal-only changes, typos or inconsequential local tweaks.

Edit only the affected documents. Add a new document only when it carries knowledge that
does not fit an existing one, and update `docs/INDEX.md` whenever the set changes. The
code wins: if it contradicts `docs/`, fix `docs/`.

## 7. ADRs

Create an ADR from `docs/99-Templates/ADR-Template.md` for **durable decisions with
consequences**: architecture, foundational technology, persistence strategy, or
rejecting an alternative a future developer will need to understand.

Do not create one for fixes, visual changes, local refactors or trivial reversible
decisions. Look for a related ADR first; if an existing decision changes, update its
state instead of silently rewriting history.

## 8. Scope and safety

- **Do not touch files outside the task.** If a fix appears to need another file, say
  so instead of widening scope.
- **Check `git status` before editing.** Never discard, revert or overwrite uncommitted
  changes that are not yours — they are the user's work in progress.
- **Do not change dependencies, versions or build configuration on your own initiative.**
- **Never introduce credentials, keys or secrets** into the repo or the docs.
- **Do not disable tests or checks** to manufacture a passing result.
- **Ask first** before destructive or high-impact operations: deleting files or data,
  Git commands that discard work, `clean`, mass reformatting, migrations that wipe data.
- **Do not report partial work as done.** State what is missing.

## 9. Parallel work and Git isolation

One task, one branch, one worktree. Never edit files while on `main`.

- **Isolate before editing.** Create a task branch (`git switch -c <task>`) and,
  when other agents may run at the same time, a dedicated worktree
  (`git worktree add ../earnit-<task> -b <task>`). Two agents sharing one working
  directory overwrite each other.
- **Reuse before creating.** If the task already has a branch and a worktree,
  use them instead of adding another.
- **Look beyond your own worktree.** `git status` only reports the current one:
  check every worktree you touch (`git worktree list`) and treat changes you did
  not make as read-only (§8).
- **No history operations.** Do not `commit`, `push`, merge, rebase, tag or force
  anything without explicit authorization. Leaving the result in the worktree is
  the expected end state.
- **Close with a diff review** (§4, *After*): only this task's changes, nothing
  else — no stray files, no drive-by reformatting.
- **Cannot isolate? Stop and report.** If the task needs files another agent is
  changing, or a branch/worktree cannot be created safely, say so instead of
  working in the shared tree.