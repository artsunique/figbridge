# Figma Bridge for JetBrains

**Version:** 1.1 SPEC  
**Codename:** FigBridge  
**Author:** Andreas / arts-unique  
**Date:** 2026-03-21  
**Target IDEs:** PhpStorm, WebStorm, IntelliJ IDEA  
**License:** Proprietary (14-day free trial, then paid)

---

## 1. Problem Statement

### 1.1 The Gap

The Figma-to-code handoff for JetBrains IDE users is fundamentally broken:

- **VS Code** has Figma's official extension, but it's widely criticized for login issues, clunky UX, and a focus on inspection rather than code generation.
- **PhpStorm/WebStorm** have zero viable Figma integration — only a basic "Design Preview" plugin (inspect-only) and experimental MCP setups that lack the critical "Get Code" capability.
- The open-source "Figma to Code" Figma plugin (2.2M users) generates decent Tailwind/HTML, but the output lands in a Figma panel — copy-pasting into the IDE is manual and context-free.

### 1.2 Who Feels This Pain

- Frontend developers on WebStorm/PhpStorm working with Tailwind CSS or custom CSS
- Any developer building websites/apps who receives Figma designs
- Freelancers and agencies bridging design → code daily
- Laravel, static-site, or any HTML/CSS developers using JetBrains IDEs

### 1.3 Core Insight

Developers don't want to *inspect* designs in their IDE — they want to **pull code from designs** with minimal friction. The interface should feel like Git pull, not like browsing a museum.

---

## 2. Product Vision

**One-sentence pitch:**  
Select a Figma frame, get production-ready Tailwind or Custom CSS/HTML code in your JetBrains IDE — one click, right file, right folder.

**Design philosophy:**  
- Minimal UI — three tabs maximum (Preview, Code, Assets)
- No configuration wizards — sensible defaults, override when needed
- **Figma login must be dead simple** — one click, no friction, no loops
- Keyboard-first — every action has a shortcut
- Swiss Design aesthetic — clean, typographic, zero clutter

---

## 3. Feature Specification

### 3.1 Core Features (v1.0 — MVP)

#### 3.1.1 Figma Connection — Dead Simple Login

**CRITICAL: Login must be the easiest part of the entire plugin.**

The #1 complaint about Figma for VS Code is the broken, looping login flow. FigBridge treats auth as a first-class UX problem.

**Tool Window** in the right sidebar (like Database or Git).

| Element | Behavior |
|---------|----------|
| One-Click Login | Single button "Connect Figma" → browser opens → done |
| Project Picker | Dropdown showing recent Figma files (via REST API) |
| Frame Tree | Collapsible tree of pages → frames → components |
| Live Thumbnail | Selected frame renders as preview (cached, refreshable) |
| Search | Fuzzy search across all frames/components by name |

**Auth flow — optimized for zero friction:**

1. User clicks **"Connect Figma"** — single prominent button, impossible to miss
2. System browser opens Figma OAuth consent screen (NOT embedded webview — avoids cookie/session issues)
3. User clicks "Allow" in Figma
4. Callback to `localhost:{random-port}` captures token automatically
5. Token stored via JetBrains `PasswordSafe` API (encrypted, persistent)
6. Refresh token handles expiry silently — user never sees login again
7. Status indicator in tool window: green dot = connected, click to disconnect

**Auth UX requirements:**
- No manual token copy-paste — ever
- No "Accept Terms" loops — handle TOS acceptance in OAuth scope
- Connection survives IDE restarts — token persists
- If token expires: silent refresh, no user action needed
- If refresh fails: gentle notification with one-click re-auth
- Works behind corporate proxies (configurable proxy settings)
- Timeout after 60s with clear error message, not infinite spinner

**Fallback: Personal Access Token**
For users who can't use OAuth (corporate firewalls, etc.):
- Settings → FigBridge → "Use Personal Access Token"
- Link to Figma token generation page
- Paste token once → stored encrypted → done

#### 3.1.2 Code Generator

The heart of the plugin. Select a frame or component → get code.

**Two output modes (v1.0):**

| Mode | Output | Use Case |
|------|--------|----------|
| **Tailwind CSS** | HTML with Tailwind utility classes | Tailwind-based projects |
| **Custom CSS** | HTML + separate CSS with custom classes | Any project, vanilla CSS, BEM |

User picks the mode once in settings. Both modes generate clean, semantic HTML.

**Tailwind mode output:**
```html
<section class="flex flex-col gap-8 px-6 py-16 max-w-5xl mx-auto">
  <h1 class="text-4xl font-bold text-gray-900 leading-tight">
    Hero Headline
  </h1>
  <p class="text-lg text-gray-600 max-w-2xl">
    Subline text from the Figma frame.
  </p>
</section>
```

**Custom CSS mode output:**
```html
<section class="hero">
  <h1 class="hero__title">Hero Headline</h1>
  <p class="hero__subtitle">Subline text from the Figma frame.</p>
</section>
```
```css
.hero {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  padding: 4rem 1.5rem;
  max-width: 64rem;
  margin: 0 auto;
}
.hero__title {
  font-size: 2.25rem;
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}
.hero__subtitle {
  font-size: 1.125rem;
  color: #4b5563;
  max-width: 42rem;
}
```

**Generation pipeline:**

```
Figma Frame (REST API)
  → Parse node tree (recursive)
    → Map Auto Layout → Flexbox/Grid
    → Map fills → colors (Tailwind classes OR CSS properties)
    → Map typography → text styles
    → Map spacing → spacing values
    → Map constraints → responsive utilities
  → Assemble semantic HTML structure
    → Detect landmarks (header, nav, main, footer, section)
    → Detect repeating patterns → components
    → Detect text nodes → appropriate HTML elements (h1-h6, p, span)
  → Apply output mode (Tailwind / Custom CSS)
  → Format code
  → Output to editor or file
```

**Code quality targets:**
- Semantic HTML5 elements (not nested `<div>` soup)
- Tailwind: classes follow logical order (layout → spacing → typography → color)
- Custom CSS: BEM-style naming derived from Figma layer names
- No inline styles in either mode
- Responsive breakpoints inferred from Figma frame variants or constraints

**UI for code generation:**

```
┌──────────────────────────────────────────┐
│ ▼ Code                           [⚙][📋] │
│──────────────────────────────────────────│
│ Mode: [Tailwind ▼]  Target: [src/  ▼]   │
│──────────────────────────────────────────│
│  1 │ <section class="flex flex-col ...   │
│  2 │   <h1 class="text-4xl font-bold...  │
│  3 │   <p class="text-lg text-gray...    │
│  4 │   ...                               │
│──────────────────────────────────────────│
│ [Insert at Cursor] [Save to File] [Copy] │
└──────────────────────────────────────────┘
```

**Actions:**
- `Insert at Cursor` — pastes code at current editor position
- `Save to File` — creates file in selected project directory
- `Copy` — clipboard
- `⚙` — settings (mode, class naming, responsive behavior)
- `📋` — history of generated snippets

#### 3.1.3 Asset Export

Automatic export of images, icons, and SVGs from the selected frame.

| Asset Type | Export Format | Target Directory |
|------------|-------------|-----------------|
| Photos / Raster | WebP + fallback PNG | `images/` (configurable) |
| Icons | SVG (optimized) | `icons/` (configurable) |
| Logos | SVG | `images/` (configurable) |
| Illustrations | SVG or PNG@2x | `images/` (configurable) |

**Behavior:**
- Assets auto-detected from frame's image fills and component instances
- Naming: kebab-case derived from Figma layer name
- Duplicates detected by content hash — skipped on re-export
- Asset references in generated code use correct relative paths
- Target directories fully configurable per project

#### 3.1.4 Design Token Sync

Sync Figma Variables → project configuration files.

**Output follows selected code mode:**

| Source (Figma) | Tailwind Mode | Custom CSS Mode |
|---------------|---------------|-----------------|
| Color Variables | `@theme { --color-* }` | `:root { --color-* }` |
| Typography | `@theme { --font-*, --text-* }` | `:root { --font-* }` |
| Spacing | `@theme { --spacing-* }` | `:root { --spacing-* }` |
| Border Radius | `@theme { --radius-* }` | `:root { --radius-* }` |
| Shadows | `@theme { --shadow-* }` | `:root { --shadow-* }` |

**Sync modes:**
- **Pull** — Figma → Code (overwrites local tokens)
- **Diff** — shows changes before applying (like Git diff)
- **Watch** (v1.5) — polls Figma every N minutes, notifies on changes

---

### 3.2 Advanced Features (v1.5)

#### 3.2.1 Component Mapping

Link Figma components to existing code files in the project. Stored in `.figbridge.json`.

#### 3.2.2 Design Diff View

Compare Figma screenshot vs rendered HTML. Pixel diff with annotated report.

#### 3.2.3 AI-Assisted Code Generation (v2.0)

Claude API integration (user's own key). Context-aware code that matches project conventions.

---

### 3.3 Non-Features (Explicit Exclusions)

| Excluded | Reason |
|----------|--------|
| Full Figma editor in IDE | Out of scope |
| Bidirectional sync (code → Figma) | Complex, low ROI for v1 |
| Design system builder | Tokens Studio exists |
| Prototyping preview | Figma handles this natively |
| Sketch/Adobe XD support | Figma-only focus |
| Framework output (React, Vue) in v1 | HTML first — frameworks in v2 |

---

## 4. Technical Architecture

### 4.1 Plugin Stack

```
┌─────────────────────────────────────────────────┐
│                 JetBrains IDE                    │
│  ┌───────────────────────────────────────────┐  │
│  │           FigBridge Plugin (Kotlin)        │  │
│  │  ┌─────────┐ ┌──────────┐ ┌───────────┐  │  │
│  │  │ UI Layer │ │ Services │ │ Generator │  │  │
│  │  │ (Swing/  │ │ (Figma   │ │ (Code     │  │  │
│  │  │  JCEF)   │ │  REST +  │ │  Output)  │  │  │
│  │  │          │ │  OAuth)  │ │           │  │  │
│  │  └─────────┘ └──────────┘ └───────────┘  │  │
│  │       │            │             │        │  │
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │        Storage / Config Layer       │  │  │
│  │  │  .figbridge.json | PasswordSafe    │  │  │
│  │  └─────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 4.2 Key Dependencies

| Dependency | Purpose | Version |
|-----------|---------|---------|
| IntelliJ Platform SDK | Plugin framework | 2025.1+ |
| Kotlin | Plugin language | 2.0+ |
| IntelliJ Platform Gradle Plugin | Build system | 2.x |
| Ktor Client | HTTP client for Figma API | 3.x |
| kotlinx.serialization | JSON parsing | 1.7+ |
| JCEF | Preview rendering | Bundled with IDE |

### 4.3 Figma API Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /v1/files/:key` | File structure, pages, frames |
| `GET /v1/files/:key/nodes?ids=` | Node data with geometry |
| `GET /v1/images/:key?ids=` | Render frames as PNG/SVG |
| `GET /v1/files/:key/variables/local` | Design tokens |
| `GET /v1/files/:key/styles` | Published styles |

### 4.4 Node-to-HTML Mapping

```
FRAME (Auto Layout: horizontal) → <div class="flex ...">
FRAME (Auto Layout: vertical)   → <div class="flex flex-col ...">
FRAME (no Auto Layout)          → <div> with absolute positioning
TEXT                             → <p|h1-h6|span>
RECTANGLE (image fill)          → <img>
RECTANGLE (solid fill)          → <div> with background
INSTANCE                        → mapped component or generated
GROUP                           → <div>
VECTOR                          → <svg> or <img>
```

### 4.5 Semantic HTML Heuristics

| Figma Pattern | HTML Output |
|---------|--------|
| Frame "Header" / "Nav" | `<header>` / `<nav>` |
| Frame "Footer" | `<footer>` |
| Frame "Hero" / "Section" | `<section>` |
| Frame "Card" / repeated pattern | `<article>` |
| Frame "Sidebar" / "Aside" | `<aside>` |
| Largest text in frame | `<h1>` (hierarchy by size) |

---

## 5. User Interface

### 5.1 First-Run (most important screen)

```
┌─ FigBridge ─────────────────────────────────────────┐
│                                                      │
│                    ◆ FigBridge                        │
│                                                      │
│         Connect your Figma account                   │
│         to start generating code.                    │
│                                                      │
│              [ Connect Figma ]                        │
│                                                      │
│         ─── or ───                                   │
│                                                      │
│         Use Personal Access Token                    │
│                                                      │
│         14-day free trial · No card required          │
└──────────────────────────────────────────────────────┘
```

### 5.2 Main Tool Window

```
┌─ FigBridge ──────────────────────────── [⟳] [⚙] ─┐
│  ● Connected                                        │
│  [Preview]  [Code]  [Assets]  [Tokens]              │
│ ─────────────────────────────────────────────────── │
│  ┌─ Files ──────────────────────────────────────┐  │
│  │ ▼ Homepage Redesign                          │  │
│  │   ▼ Desktop                                  │  │
│  │     ▶ Hero Section                           │  │
│  │     ▶ Features Grid                          │  │
│  │     ▶ Footer                                 │  │
│  │   ▶ Mobile                                   │  │
│  └──────────────────────────────────────────────┘  │
│  ┌─ Preview ────────────────────────────────────┐  │
│  │          [frame thumbnail]                    │  │
│  │  Hero Section · 1440×680 · 12 layers         │  │
│  └──────────────────────────────────────────────┘  │
│  [ Generate Code ▼ ]  [ Export Assets ]             │
└─────────────────────────────────────────────────────┘
```

### 5.3 Keyboard Shortcuts

| Action | Mac | Win |
|--------|-----|-----|
| Open panel | `⌘⇧F` | `Ctrl+Shift+F` |
| Generate code | `⌘⇧G` | `Ctrl+Shift+G` |
| Insert at cursor | `⌘⇧I` | `Ctrl+Shift+I` |
| Sync tokens | `⌘⇧T` | `Ctrl+Shift+T` |
| Export assets | `⌘⇧E` | `Ctrl+Shift+E` |
| Refresh data | `⌘⇧R` | `Ctrl+Shift+R` |
| Search frames | `/` | `/` |

### 5.4 Settings

**General:** Figma connection, code mode (Tailwind/Custom CSS), asset format, cache duration.

**Tailwind Mode:** TW version (v3/v4), class order, responsive strategy, color format.

**Custom CSS Mode:** Naming (BEM/flat/camelCase), units (rem/px), separate file toggle, color format.

**Paths:** Asset dir, icon dir, token output file — all configurable per project.

---

## 6. Business Model

| Tier | Price | Features |
|------|-------|----------|
| **Free Trial** | $0 / 14 days | All features, no limits, no card |
| **Pro Individual** | $49/year | Everything unlimited |
| **Pro Organization** | $89/year/seat | Pro + shared config + priority support |

Distribution via JetBrains Marketplace (15% commission). After 12 months: perpetual fallback license.

---

## 7. Roadmap

| Phase | Duration | Scope |
|-------|----------|-------|
| **v1.0 MVP** | 8 weeks | Auth, frame browser, preview, Tailwind + Custom CSS codegen, asset export, settings, trial logic |
| **v1.5** | 4 weeks | Token sync, component mapping, keyboard shortcuts, frame search |
| **v2.0** | 6 weeks | Design diff, AI codegen (Claude), framework output (JSX, Vue, Blade) |
| **v2.5+** | ongoing | Team features, custom templates, Figma webhooks |

---

## 8. Competitive Landscape

| Tool | Platform | Price | Code Gen | JetBrains |
|------|----------|-------|----------|-----------|
| Figma for VS Code | VS Code | Free | No | No |
| Figma to Code | Figma | Free | Tailwind/HTML | No |
| Anima | Web | $19/mo | React/HTML | No |
| Locofy.ai | Web | $39/mo | Multi-framework | No |
| Builder.io | Web | $19/mo | AI codegen | No |
| **FigBridge** | **JetBrains** | **$49/yr** | **Tailwind + Custom CSS** | **Yes** |
