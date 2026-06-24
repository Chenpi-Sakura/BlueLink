# BlueLink 图谱主页设计 QA

- source visual truth path: `D:\competition\AIGC\BlueLink\picture\图谱\图谱主页面.png`
- implementation screenshot path: `D:\competition\AIGC\BlueLink\tmp\device\bluelink-graph-probe.png`
- side-by-side comparison path: `D:\competition\AIGC\BlueLink\tmp\device\graph-comparison-small.jpg`
- viewport: source 842 × 1874 px; implementation device viewport unavailable
- state: intended success state with graph data

## Full-view comparison evidence

The source and running implementation were captured and combined side by side at matching aspect ratios. The implementation is 1080 × 2408 px and reports 6 cached nodes, 6 edges, `webReady: true`, and `script: ok`.

## Focused region comparison evidence

The top parchment bar and bottom glass controls were compared in the combined image. The control text is now explicitly light and readable, and the bar width is closer to the source. Chrome DevTools found `#main` at `360 × 0` and its canvas at `1080 × 0`, identifying the direct cause of invisible nodes.

## Findings

- [P1] Final graph rendering fix is not yet device-verified
  - Location: `graph.html` main canvas.
  - Evidence: the installed build reports a zero-height ECharts container; the source has a dense visible graph while the implementation screenshot has an empty canvas.
  - Impact: graph nodes, edges, and node-click verification remain blocked in the installed build.
  - Fix: `#main` is now pinned to `100vw × 100vh`, with immediate and deferred `chart.resize()` calls. Rebuild, install, and capture the fixed build.

## Required fidelity surfaces

- Fonts and typography: title is 24 sp and controls are 15 sp; the device screenshot confirms readable controls.
- Spacing and layout rhythm: top bar is 68 dp with 16 dp margins; the control bar is 84% width and 68 dp high.
- Colors and visual tokens: parchment, Klein blue, deep navy, and explicit warm-white control text are visible on-device.
- Image quality and asset fidelity: the project-local starfield renders sharply and matches the restrained navy direction.
- Copy and content: all graph-page controls are Chinese and match the requested actions.

## Patches made

- Added a project-local low-noise navy starfield background.
- Added the parchment title bar with Chinese search and refresh actions.
- Added the three-part Chinese glass control bar.
- Added real refresh, reset-view, and force/circular layout actions.
- Added safe Snackbar placeholders for search and filtering.
- Preserved selected-node feedback and loading/empty/error states.
- Added Debug demo data, source/error/render counts, and a tappable demo entry.
- Added `onGraphRendered` and `onGraphError` bridge callbacks.
- Diagnosed the invisible graph as a zero-height HTML container and patched viewport sizing plus deferred resize.

## Implementation checklist

- Rebuild and install the viewport-height fix when execution quota is available.
- Capture cache and demo graph states on the connected 1080 × 2408 device.
- Click DOCUMENT, INSPIRATION, and CONCEPT nodes and verify the selected-node card.
- Re-run side-by-side QA and pass only after nodes and all four edge types are visible.

## Follow-up polish

- Tune graph force density and node label wrapping using real production-sized graph data after the first device capture.

final result: blocked
