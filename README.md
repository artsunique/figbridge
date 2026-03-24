# FigBridge — Figma to Code for JetBrains IDEs

Generate production-ready **Tailwind CSS v4** or **Custom CSS (BEM)** code directly from your Figma designs, inside PhpStorm, WebStorm, IntelliJ IDEA, and all IntelliJ Platform IDEs.

## Features

- **Tailwind CSS v4** — utility classes with `@theme` token support
- **Custom CSS (BEM)** — HTML + separate CSS with `block__element` naming
- **Semantic HTML** — detects `section`, `nav`, `header`, `footer`, `button`, `h1`–`h6`, `img`, `input`, `a`
- **Auto Layout to Flexbox** — converts Figma layouts to `flex`, `gap`, `padding`, `alignment`
- **Flex Wrap** — supports wrapped Auto Layouts with row/column gap
- **Absolute Positioning** — non-Auto-Layout frames use `top`/`left`
- **Design Tokens** — import Figma Variables as CSS custom properties
- **Asset Export** — SVG icons, PNG/WebP images, deduplicated by content
- **Google Fonts** — auto-detects custom fonts and generates `<link>` tag
- **Tailwind Snap** — convert arbitrary values to standard Tailwind classes
- **Frame Preview** — browse pages and frames with thumbnail preview
- **Figma OAuth + PAT** — login via browser or Personal Access Token

## Getting Started

1. Install FigBridge from the [JetBrains Marketplace](https://plugins.jetbrains.com)
2. Open the **FigBridge** tool window (right sidebar)
3. Click **Login with Token** and paste your [Figma Personal Access Token](https://www.figma.com/developers/api#access-tokens)
4. Paste a Figma file URL or select a recent file
5. Select a frame and click **Generate Code**

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Open FigBridge | `Ctrl+Alt+F` |
| Refresh file | `Ctrl+Alt+R` |

## Requirements

- JetBrains IDE 2025.1 or later
- Figma account (free tier works for code generation)
- Figma Professional/Enterprise plan for design token export via API

## Build from Source

```bash
# Requires Java 21
export JAVA_HOME="/path/to/java21"

./gradlew runIde          # Launch sandbox IDE with plugin
./gradlew test            # Run tests
./gradlew buildPlugin     # Build distributable ZIP
```

## License

Proprietary. 14-day free trial included.

## Author

[arts-unique](https://arts-unique.com) — Andreas Burget
