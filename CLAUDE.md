# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FigBridge is a JetBrains IDE plugin (PhpStorm, WebStorm, IntelliJ IDEA) that generates Tailwind CSS or Custom CSS/HTML code from Figma designs. It's written in Kotlin and targets IDE version 2025.1+.

- **Plugin ID:** `com.artsunique.figbridge`
- **Package:** `com.artsunique.figbridge`
- **Spec:** `FIGBRIDGE-SPEC.md` — full product specification
- **Plan:** `FIGBRIDGE-PLAN.md` — implementation steps and project structure

## Build & Run Commands

Requires Java 21. Set `JAVA_HOME` before running Gradle:
```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

./gradlew runIde          # Launch sandbox IDE with plugin installed
./gradlew test            # Run all tests
./gradlew build           # Compile + test + package
./gradlew buildPlugin     # Build distributable ZIP
./gradlew verifyPlugin    # Check IDE compatibility
```

Uses Gradle 9.4.1 (wrapper included) and IntelliJ Platform Gradle Plugin 2.13.1.

## Architecture

The plugin has five main layers under `src/main/kotlin/com/artsunique/figbridge/`:

- **`api/`** — Figma REST API client. All HTTP goes through `FigmaClient.kt`. Handles OAuth2 + PAT auth, caching (TTL-based), and rate limiting (30 req/min with exponential backoff on 429).
- **`generator/`** — Code generation engine. `NodeParser` converts Figma node trees into an internal model, `SemanticDetector` applies heuristics (frame names, font sizes) to choose HTML5 elements, then either `TailwindGenerator` or `CustomCssGenerator` produces output. Generators are pure functions: Figma JSON in, String out.
- **`tokens/`** — Design token sync. Pulls Figma Variables and formats them as `@theme { --var }` (Tailwind v4) or `:root { --var }` (Custom CSS).
- **`assets/`** — Asset export. Downloads images as WebP/PNG, icons as SVG, deduplicates by content hash.
- **`ui/`** — Swing-based tool window with Connect, Main (frame tree + preview), Code, and Settings panels.
- **`config/`** — IDE-level settings via `PersistentStateComponent` and per-project `.figbridge.json`.

Plugin descriptor: `src/main/resources/META-INF/plugin.xml`
i18n strings: `src/main/resources/messages/FigBridgeBundle.properties`

## Key Technical Conventions

- **Kotlin only** — no Java unless IntelliJ API requires it
- **kotlinx.serialization** for all JSON — no Gson, no Jackson
- **Coroutines** for async; `Dispatchers.IO` for network calls
- **Never block the EDT** — use `ApplicationManager.invokeLater` for UI updates, `ReadAction`/`WriteAction` for file system ops
- All user-facing strings go in `FigBridgeBundle.properties`
- OAuth tokens stored via JetBrains `PasswordSafe` API

## Testing

- Generators are tested as pure functions with fixture JSON in `src/test/resources/fixtures/`
- API client tests use `ktor-client-mock` — no real Figma API calls in CI
- Test files use same name + `Test` suffix (e.g., `TailwindGenerator.kt` → `TailwindGeneratorTest.kt`)

## Key Dependencies

- IntelliJ Platform Gradle Plugin 2.x (build system)
- Ktor Client 3.x (HTTP for Figma API)
- kotlinx-serialization-json 1.7+ (JSON parsing)
- JUnit Jupiter 5.x (testing)

## Figma API Reference

Base URL: `https://api.figma.com`. Auth via `Authorization: Bearer {token}` header.

- `GET /v1/files/{key}` — file structure (pages, frames)
- `GET /v1/files/{key}/nodes?ids=` — detailed node data
- `GET /v1/images/{key}?ids=&format=png&scale=2` — rendered images
- `GET /v1/files/{key}/variables/local` — design tokens
- OAuth: `https://www.figma.com/oauth` → token exchange at `/v1/oauth/token`

## Node-to-HTML Mapping

Figma Auto Layout horizontal → `flex`, vertical → `flex flex-col`. Text nodes map to `h1-h6`/`p`/`span` based on size hierarchy. Semantic HTML elements (`header`, `nav`, `footer`, `section`, `article`, `aside`) are inferred from Figma frame names. Custom CSS mode uses BEM naming derived from layer names.
