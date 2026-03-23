## Why

Reading long spec documents or technical content on Android requires switching between the document and a note-taking app to capture feedback. This context-switching is disruptive and makes it hard to maintain a structured review. We need an always-on floating overlay that lets users annotate content in-place - capturing screenshots, selected text, and comments - then export all annotations at once when the review is complete.

## What Changes

- New Android application with a floating overlay (draw-over-other-apps) that persists above all other apps
- Capture mechanism for screenshots (partial or full), selected text, and user-written comments
- Each annotation is a structured entry with: screenshot (optional), selected text (optional), metadata (timestamp, source app), and a user comment
- Review session management: start a session, collect annotations, end session and compile all comments
- Export/copy-all functionality that formats all annotations into a pasteable block (e.g., markdown or plain text)
- Minimal, non-intrusive floating UI (collapsible bubble or small toolbar) that expands on tap

## Capabilities

### New Capabilities
- `floating-overlay`: System overlay service that draws above other apps, handles lifecycle, collapse/expand states, and drag-to-reposition
- `annotation-capture`: Capture screenshots (via media projection), receive shared/selected text, and attach metadata to create structured annotation entries
- `review-session`: Manage review sessions - create, add annotations to, browse, edit, and close sessions with persistent local storage
- `export-compile`: Compile all annotations from a session into formatted output (markdown/plain text) and copy to clipboard or share

### Modified Capabilities

## Impact

- New Android project (Kotlin, Jetpack Compose for overlay UI)
- Requires `SYSTEM_ALERT_WINDOW` permission for draw-over-other-apps
- Requires `MEDIA_PROJECTION` for screenshot capture
- Local storage via Room database for sessions and annotations
- No backend dependencies - fully offline/local
- Targets Android 10+ (API 29+) for media projection and overlay compatibility
