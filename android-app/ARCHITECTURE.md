# Athar (أثر) Architecture & Modularization Guidelines

## 1. Core Principle: High Cohesion, Small Units
To maintain absolute stability, maintainability, and zero code bloat, every feature in the application must strictly adhere to the **Small Units Pattern**. No single UI file or component should exceed ~200-250 lines. Large monolithic files are prohibited.

---

## 2. Directory & Module Boundaries

```
app.xrtm8.athar/
├── core/                  # Pure algorithms & engine logic (Image processing, Kashida engine, Color grading)
├── export/                # High-resolution rendering, MediaStore storage, and sharing pipelines
├── model/                 # Immutable domain and UI state models
│   ├── CanvasModels.kt        # Canvas aspect ratios, scrims, blend modes
│   ├── TypographyModels.kt    # Fonts, text alignments, Kashida modes
│   ├── VisualEffectModels.kt  # Gradients, swatches, materials, glass presets
│   ├── TextLayer.kt           # Immutable layer data model
│   └── CanvasState.kt         # Master project state & multi-layer composition
├── project/               # Serialization (.athar), thumbnail caching, project metadata
├── typography/            # Text layout calculations, font caching, curved text, line justification
├── ui/
│   ├── canvas/            # Canvas rendering sub-units (Text renderer, guides, interaction hit-testing)
│   ├── home/              # Decomposed home screen units (Header, Hero card, Action cards, Projects grid)
│   ├── panels/
│   │   ├── background/    # Decomposed canvas background sub-panels (Framing, Adjustments, Scrim)
│   │   ├── effects/       # Decomposed effects sub-panels (Materials, Stroke/Shadow, Glass Plaque)
│   │   ├── typography/    # Decomposed typography sub-panels (Font, Metrics, Formatting, Kashida, Spacing)
│   │   └── ...
│   ├── components/        # Reusable design system widgets (StudioSlider, Dock, Drawer, Strip)
│   ├── dialogs/           # Modal dialogs & bottom sheets
│   └── theme/             # Color tokens, AMOLED white/black system, Typography styles
└── updater/               # In-app update check, download, and APK installation pipeline
```

---

## 3. Strict Rules for Adding or Modifying Features

1. **State Isolation**:
   - State data classes must be immutable (`data class` with `val` fields).
   - Business mutations happen via pure copy operations (`state.copy(...)`).

2. **UI Decompilation into Sub-Panels**:
   - Any panel with sub-tabs or multiple logical controls MUST be split into dedicated composables inside its feature package (e.g. `ui/panels/<feature>/<SubSection>.kt`).
   - Do NOT inline complex dialogs, sheets, or extensive control groups inside a single file.

3. **Separation of Canvas Rendering**:
   - Drawing operations on `android.graphics.Canvas` must reside in dedicated renderer objects (e.g., `CanvasLayerTextRenderer`, `CanvasBackgroundRenderer`, `TextGlassBackdropRenderer`).
   - `CanvasView` only handles Compose Canvas setup, gesture inputs, and calls out to specialized renderers.

4. **Zero AI Slop & Direct Tool Terminology**:
   - All UI copy, labels, and descriptions must be functional, concise, and professional tool Arabic.
   - Avoid marketing adjectives ("خارق", "ملكي", "أسطوري", "ثوري").

5. **Verification & Testing**:
   - Every model serialization change must have corresponding unit test coverage in `src/test/java/app/xrtm8/athar/`.
   - Running `./gradlew testReleaseUnitTest` and `./gradlew assembleRelease` is mandatory before every release.
