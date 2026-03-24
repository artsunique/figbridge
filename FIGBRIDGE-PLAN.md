# PLAN.md — FigBridge Implementation Plan for Claude Code

## Quick Reference

- **What:** JetBrains IDE plugin that generates Tailwind or Custom CSS code from Figma designs
- **Language:** Kotlin 2.0+
- **Build:** Gradle + IntelliJ Platform Gradle Plugin 2.x
- **Target IDE:** PhpStorm / WebStorm / IntelliJ IDEA (2025.1+)
- **Spec:** See SPEC.md for full product specification

---

## Project Setup

```bash
# Project lives in:
figbridge/

# Run locally:
./gradlew runIde          # Opens sandbox IDE with plugin installed
./gradlew test            # Unit tests
./gradlew buildPlugin     # Build ZIP for distribution
./gradlew verifyPlugin    # Check compatibility
```

### build.gradle.kts (starter)

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.8.0"
}

group = "com.artsunique.figbridge"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.1")
        // For PhpStorm testing: create("PS", "2025.1")
    }
    
    // HTTP client for Figma API
    implementation("io.ktor:ktor-client-core:3.1.1")
    implementation("io.ktor:ktor-client-cio:3.1.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")
    
    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.ktor:ktor-client-mock:3.1.1")
}
```

---

## Project Structure

```
figbridge/
├── build.gradle.kts
├── settings.gradle.kts
├── SPEC.md
├── PLAN.md                              ← this file
├── src/
│   └── main/
│       ├── kotlin/com/artsunique/figbridge/
│       │   │
│       │   ├── FigBridgePlugin.kt       # Plugin lifecycle
│       │   │
│       │   ├── api/                     # Figma API layer
│       │   │   ├── FigmaClient.kt       # HTTP client, caching, rate limiting
│       │   │   ├── FigmaAuth.kt         # OAuth2 + PAT fallback
│       │   │   ├── FigmaModels.kt       # API response data classes
│       │   │   └── FigmaCache.kt        # In-memory cache with TTL
│       │   │
│       │   ├── generator/               # Code generation engine
│       │   │   ├── CodeGenerator.kt     # Main orchestrator
│       │   │   ├── NodeParser.kt        # Figma node tree → internal model
│       │   │   ├── SemanticDetector.kt  # Heuristics for HTML semantics
│       │   │   ├── TailwindGenerator.kt # Tailwind mode output
│       │   │   ├── CustomCssGenerator.kt# Custom CSS mode output (BEM)
│       │   │   └── CodeFormatter.kt     # Indentation, cleanup
│       │   │
│       │   ├── tokens/                  # Design token sync
│       │   │   ├── TokenSync.kt         # Pull/diff logic
│       │   │   ├── TailwindTokenFormatter.kt   # → @theme output
│       │   │   └── CssTokenFormatter.kt        # → :root output
│       │   │
│       │   ├── assets/                  # Asset export
│       │   │   └── AssetExporter.kt     # Image/SVG download + optimize
│       │   │
│       │   ├── ui/                      # Plugin UI
│       │   │   ├── FigBridgeToolWindowFactory.kt  # Tool window registration
│       │   │   ├── ConnectPanel.kt      # First-run / login screen
│       │   │   ├── MainPanel.kt         # Frame tree + preview + actions
│       │   │   ├── CodePanel.kt         # Generated code display
│       │   │   └── SettingsConfigurable.kt # Preferences page
│       │   │
│       │   └── config/                  # Configuration
│       │       ├── FigBridgeSettings.kt # IDE-level persistent settings
│       │       └── ProjectConfig.kt     # .figbridge.json per project
│       │
│       └── resources/
│           ├── META-INF/
│           │   └── plugin.xml           # Plugin descriptor
│           └── messages/
│               └── FigBridgeBundle.properties  # i18n strings
│
└── src/test/
    └── kotlin/com/artsunique/figbridge/
        ├── api/
        │   └── FigmaClientTest.kt       # Mocked API responses
        ├── generator/
        │   ├── TailwindGeneratorTest.kt # Tailwind output tests
        │   ├── CustomCssGeneratorTest.kt# Custom CSS output tests
        │   └── SemanticDetectorTest.kt  # HTML heuristic tests
        └── tokens/
            └── TokenSyncTest.kt         # Token formatting tests
```

---

## Implementation Steps — Ordered by Priority

### Step 1: Plugin Scaffold + Build System
**Goal:** Empty plugin that loads in a sandbox IDE.

- [ ] Create project with IntelliJ Platform Gradle Plugin 2.x
- [ ] Configure `plugin.xml` with ID `com.artsunique.figbridge`
- [ ] Register an empty Tool Window (right sidebar)
- [ ] Verify `./gradlew runIde` launches IDE with tool window visible
- [ ] Set minimum IDE version to 2025.1

**Files:** `build.gradle.kts`, `settings.gradle.kts`, `plugin.xml`, `FigBridgeToolWindowFactory.kt`

**Test:** `./gradlew runIde` → IDE opens → "FigBridge" tab visible in right sidebar.

---

### Step 2: Figma OAuth Login (THE critical feature)
**Goal:** One-click Figma connection that just works.

- [ ] Implement `FigmaAuth.kt`:
  - Start local HTTP server on random port (Ktor or simple `HttpServer`)
  - Open system browser with Figma OAuth URL + redirect to `localhost:{port}`
  - Capture callback with authorization code
  - Exchange code for access + refresh token
  - Store tokens via `PasswordSafe`
- [ ] Implement PAT fallback (manual token entry in settings)
- [ ] Build `ConnectPanel.kt`:
  - Single "Connect Figma" button (centered, prominent)
  - "or use Personal Access Token" link below
  - After connection: green status dot + "Connected as {user}"
- [ ] Token refresh logic: silent refresh on 401, re-auth prompt only if refresh fails
- [ ] Persist connection across IDE restarts

**Figma OAuth details:**
- Auth URL: `https://www.figma.com/oauth?client_id={ID}&redirect_uri=http://localhost:{PORT}/callback&scope=files:read&response_type=code`
- Token URL: `https://api.figma.com/v1/oauth/token` (POST with code)
- Refresh URL: same endpoint with `grant_type=refresh_token`

**Files:** `FigmaAuth.kt`, `ConnectPanel.kt`, `FigBridgeSettings.kt`

**Test:** Click "Connect Figma" → browser opens → allow → tool window shows "Connected". Restart IDE → still connected.

---

### Step 3: Figma API Client + Caching
**Goal:** Fetch file structure, nodes, and images from Figma.

- [ ] Implement `FigmaClient.kt` with Ktor:
  - `getFile(fileKey)` → file structure with pages/frames
  - `getNodes(fileKey, nodeIds)` → detailed node data
  - `getImage(fileKey, nodeIds, format)` → rendered PNG/SVG URLs
  - `getVariables(fileKey)` → design tokens
- [ ] Implement `FigmaCache.kt`:
  - In-memory cache with configurable TTL (default 5 min)
  - Cache key = endpoint + params
  - Manual invalidation via refresh button
- [ ] Rate limiting: max 30 req/min, queue excess requests
- [ ] Error handling: network errors, 403, 404, rate limit (429)
- [ ] Define `FigmaModels.kt` data classes for API responses

**Files:** `FigmaClient.kt`, `FigmaCache.kt`, `FigmaModels.kt`

**Test:** Unit tests with mocked HTTP responses (use `ktor-client-mock`). Verify caching, rate limiting, error handling.

---

### Step 4: Frame Tree Browser + Preview
**Goal:** Navigate Figma file structure and see frame previews.

- [ ] Build `MainPanel.kt`:
  - File picker dropdown (recent files from API)
  - Tree view: pages → frames → components (JTree or similar)
  - Click on frame → show thumbnail preview below tree
  - Frame info: name, dimensions, layer count
- [ ] Thumbnail loading:
  - Use `getImage()` to fetch PNG thumbnail
  - Cache thumbnails locally
  - Show loading spinner while fetching
- [ ] Fuzzy search: filter tree by typing (like "Go to File" in IDE)

**Files:** `MainPanel.kt`, update `FigBridgeToolWindowFactory.kt`

**Test:** Connect Figma → select file → tree shows pages/frames → click frame → thumbnail appears.

---

### Step 5: Code Generator — Tailwind Mode
**Goal:** Select a frame → get Tailwind HTML.

- [ ] Implement `NodeParser.kt`:
  - Parse Figma node tree recursively
  - Build internal representation: `ParsedNode` with type, styles, children, text
  - Extract: layout mode, padding, gap, fills, strokes, typography, effects
- [ ] Implement `SemanticDetector.kt`:
  - Detect HTML landmarks from frame names (header, nav, footer, section, etc.)
  - Detect heading hierarchy by font-size ranking
  - Detect repeated patterns (potential components)
- [ ] Implement `TailwindGenerator.kt`:
  - Map Figma Auto Layout → `flex`, `flex-col`, `gap-*`
  - Map fills → `bg-{color}` (nearest Tailwind color or arbitrary value)
  - Map typography → `text-{size}`, `font-{weight}`, `leading-{lh}`
  - Map spacing → `p-*`, `px-*`, `py-*`, `m-*`
  - Map border radius → `rounded-*`
  - Map effects → `shadow-*`, `opacity-*`
  - Generate class string in logical order
- [ ] Implement `CodeFormatter.kt`:
  - Indent HTML properly
  - Wrap long class lists
- [ ] Wire into UI: "Generate Code" button → fetches nodes → generates → shows in CodePanel

**Files:** `NodeParser.kt`, `SemanticDetector.kt`, `TailwindGenerator.kt`, `CodeFormatter.kt`, `CodePanel.kt`

**Test:** Feed fixture Figma JSON → verify generated HTML. Test Auto Layout horizontal, vertical, nested. Test text nodes, images, vectors.

---

### Step 6: Code Generator — Custom CSS Mode
**Goal:** Same frame → HTML + separate CSS file with BEM naming.

- [ ] Implement `CustomCssGenerator.kt`:
  - Derive BEM class names from Figma layer names
  - Block = top-level frame name (kebab-case)
  - Element = child layer name (block__element)
  - Generate HTML with class references
  - Generate CSS file with all properties
- [ ] CSS property mapping:
  - Same Figma properties as Tailwind mode
  - Output as standard CSS properties (`display: flex`, `gap: 2rem`, etc.)
  - Configurable units: rem / px
  - Configurable color format: hex / rgb / oklch
- [ ] Mode toggle in settings + UI dropdown

**Files:** `CustomCssGenerator.kt`, update `CodePanel.kt`, `FigBridgeSettings.kt`

**Test:** Same fixture Figma JSON → verify BEM-named HTML + CSS output. Compare with Tailwind output for same input.

---

### Step 7: Asset Export
**Goal:** Export images and icons from a frame.

- [ ] Implement `AssetExporter.kt`:
  - Scan frame for image fills and vector nodes
  - Download via Figma image export API
  - Raster images: export as WebP (with PNG fallback option)
  - Vectors/icons: export as SVG
  - File naming: kebab-case from layer name
  - Deduplicate by content hash
- [ ] Save to configurable project directory
- [ ] Update generated code with correct asset paths

**Files:** `AssetExporter.kt`

**Test:** Frame with images → export → files saved to target dir → paths in generated code are correct.

---

### Step 8: Design Token Sync
**Goal:** Pull Figma Variables into CSS/Tailwind token files.

- [ ] Implement `TokenSync.kt`:
  - Fetch variables via `getVariables()`
  - Group by collection (colors, typography, spacing, etc.)
  - Diff mode: compare with existing file, show changes
  - Pull mode: write/overwrite target file
- [ ] Implement `TailwindTokenFormatter.kt`:
  - Output as `@theme { --color-*: ...; }` block
- [ ] Implement `CssTokenFormatter.kt`:
  - Output as `:root { --color-*: ...; }` block
- [ ] UI: Tokens tab with pull/diff buttons

**Files:** `TokenSync.kt`, `TailwindTokenFormatter.kt`, `CssTokenFormatter.kt`

**Test:** Figma file with variables → pull → verify output file matches expected format.

---

### Step 9: Settings + Configuration
**Goal:** Clean settings page, per-project config.

- [ ] Implement `SettingsConfigurable.kt` (IDE Preferences → Tools → FigBridge):
  - Figma connection status + re-auth
  - Code mode toggle (Tailwind / Custom CSS)
  - Tailwind settings: version, class order, responsive strategy
  - Custom CSS settings: naming, units, color format
  - Path settings: asset dir, icon dir, token file
- [ ] Implement `ProjectConfig.kt`:
  - Read/write `.figbridge.json` in project root
  - Per-project overrides for paths and mode
- [ ] Settings persist via `PersistentStateComponent`

**Files:** `SettingsConfigurable.kt`, `ProjectConfig.kt`, `FigBridgeSettings.kt`

---

### Step 10: Trial Logic + Polish
**Goal:** 14-day trial, then lock to inspect-only.

- [ ] Trial tracking:
  - Store first-use date in IDE settings (encrypted)
  - After 14 days: disable "Generate Code" and "Export Assets" buttons
  - Show upgrade prompt with link to JetBrains Marketplace
  - Preview + inspect functionality remains free
- [ ] UI polish:
  - Loading states (spinners, skeleton UI)
  - Error states (clear messages, retry actions)
  - Empty states (no file selected, no frames found)
  - Keyboard shortcuts registration
- [ ] Notification system:
  - Trial expiry reminders (day 7, day 12, day 14)
  - Token sync completed
  - Asset export completed

**Files:** Update multiple UI files, add trial logic to `FigBridgeSettings.kt`

---

## Key Conventions for Claude Code

### Code Style
- Kotlin, no Java (except where IntelliJ API requires it)
- Use `kotlinx.serialization` for all JSON — no Gson, no Jackson
- Coroutines for async work, `Dispatchers.IO` for network calls
- All user-facing strings in `FigBridgeBundle.properties`

### IntelliJ Platform Rules
- **Never block the EDT** (Event Dispatch Thread) — use `ApplicationManager.invokeLater` for UI updates
- All Figma API calls happen on background threads
- Use `ReadAction` / `WriteAction` for file system operations
- Settings via `PersistentStateComponent` (not raw files)
- Tool windows registered in `plugin.xml`

### Figma API Rules
- All API calls go through `FigmaClient` — no direct HTTP elsewhere
- Always check cache before making API calls
- Handle rate limiting (429) with exponential backoff
- Always include `Authorization: Bearer {token}` header

### Testing
- Code generators are **pure functions**: Figma JSON in → String out
- Use fixture JSON files in `src/test/resources/fixtures/`
- Mock HTTP responses for API client tests
- No integration tests that hit real Figma API in CI

### File Naming
- Kotlin files: PascalCase (e.g., `TailwindGenerator.kt`)
- Test files: same name + `Test` suffix
- Resource bundles: `FigBridgeBundle.properties`
- Plugin ID: `com.artsunique.figbridge`

---

## Dependencies Quick Reference

```kotlin
// Ktor HTTP Client
implementation("io.ktor:ktor-client-core:3.1.1")
implementation("io.ktor:ktor-client-cio:3.1.1")
implementation("io.ktor:ktor-client-content-negotiation:3.1.1")
implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.1")

// JSON
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

// Coroutines (bundled with IDE, but explicit for clarity)
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

// Testing
testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
testImplementation("io.ktor:ktor-client-mock:3.1.1")
```

---

## Figma API Quick Reference

```
Base URL: https://api.figma.com

Auth header: Authorization: Bearer {access_token}

GET /v1/files/{file_key}
  → { document: { children: [pages] }, components: {...} }

GET /v1/files/{file_key}/nodes?ids={nodeId1,nodeId2}
  → { nodes: { "nodeId": { document: {...}, components: {...} } } }

GET /v1/images/{file_key}?ids={nodeId}&format=png&scale=2
  → { images: { "nodeId": "https://..." } }

GET /v1/files/{file_key}/variables/local
  → { variables: {...}, variableCollections: {...} }

OAuth:
  GET  https://www.figma.com/oauth?client_id=...&redirect_uri=...&scope=files:read&response_type=code
  POST https://api.figma.com/v1/oauth/token  (code → tokens)
  POST https://api.figma.com/v1/oauth/refresh (refresh_token → new tokens)
```

---

## Checklist Before Marketplace Submission

- [ ] `plugin.xml` complete: name, description, vendor, change notes
- [ ] Plugin icon: 40x40 SVG
- [ ] Screenshots: 3–5 showing key features
- [ ] Description: clear value prop, feature list, trial info
- [ ] `./gradlew verifyPlugin` passes
- [ ] Tested on PhpStorm, WebStorm, IntelliJ IDEA
- [ ] Trial logic works correctly (14 days, then locked)
- [ ] No hardcoded secrets (OAuth client ID via env/config)
- [ ] Error handling: no unhandled exceptions, no crashes
- [ ] Memory: no leaks, tool window disposes properly
