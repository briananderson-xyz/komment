## Context

This is a greenfield Android app. There is no existing codebase. The app provides a floating overlay for annotating content while reading documents or browsing on Android. It targets Android 10+ (API 29+) due to media projection and overlay API requirements.

The user's workflow: open a long document (PDF, web page, etc.), tap the floating bubble to expand the annotation toolbar, capture a screenshot or paste selected text, add a comment, and continue reading. When done, compile all annotations and copy them to clipboard.

## Goals / Non-Goals

**Goals:**
- Minimal-friction annotation capture from any app via floating overlay
- Structured annotations: screenshot + selected text + comment + metadata
- Session-based grouping so annotations from one review are kept together
- One-tap export of all session annotations as formatted text (markdown)
- Fully offline, local-only storage

**Non-Goals:**
- Cloud sync or multi-device support (future consideration)
- OCR or text extraction from screenshots
- PDF annotation or in-document markup
- Collaboration or sharing features beyond clipboard/share sheet
- Widget or notification-based capture (overlay only for v1)

## Decisions

### 1. Foreground Service with SYSTEM_ALERT_WINDOW for overlay
**Choice**: Use a foreground service to host the overlay window via WindowManager.

**Rationale**: This is the standard Android pattern for draw-over-other-apps. A foreground service keeps the process alive and the overlay responsive. Alternatives like accessibility services would work but require accessibility permission which has stricter Play Store review requirements and is not the intended use case.

**Alternatives considered**:
- Accessibility Service: More powerful but requires justification for Play Store, overkill for this use case
- Bubble API (Android 11+): Limited to notification-based bubbles, not flexible enough for custom UI

### 2. Jetpack Compose for overlay UI
**Choice**: Use Compose with ComposeView inside the WindowManager overlay.

**Rationale**: Compose provides a modern, declarative UI framework. ComposeView can be attached to WindowManager-managed views. This keeps the codebase modern and avoids XML layout overhead. The overlay UI is simple enough that Compose performance in an overlay context is not a concern.

**Alternatives considered**:
- XML Views: More battle-tested in overlays but more verbose and harder to iterate on
- Flutter: Would add significant complexity for a native Android feature

### 3. Room database for local storage
**Choice**: Room with SQLite for sessions and annotations.

**Rationale**: Room is the standard Android persistence library. Annotations are relational (session has many annotations). Screenshots are stored as files on disk with paths in the database. Room provides compile-time query verification and LiveData/Flow integration.

**Alternatives considered**:
- DataStore: Better for key-value but not for relational data with queries
- Raw SQLite: No compile-time safety, more boilerplate

### 4. MediaProjection API for screenshots
**Choice**: Use MediaProjection with a virtual display to capture screenshots.

**Rationale**: This is the only sanctioned way to capture screen content on Android 10+. It requires user consent via a system dialog each time (or once per session on some OEMs). The permission prompt is unavoidable but acceptable since screenshots are an explicit user action.

**Alternatives considered**:
- Root-based screen capture: Not viable for general users
- Accessibility service screen capture: Requires accessibility permission

### 5. Share sheet / text selection integration
**Choice**: Register as a share target and process `ACTION_PROCESS_TEXT` intents.

**Rationale**: This lets users select text in any app and send it directly to the overlay via the text selection menu or share sheet. Combined with the overlay, this provides two capture paths: screenshot and text selection.

## Risks / Trade-offs

- **[MediaProjection consent UX]** Users must grant screen capture permission each session. On some devices this is per-capture. → Mitigation: Cache the MediaProjection token for the session duration; guide users through the permission flow on first use.
- **[Overlay battery impact]** A foreground service with overlay consumes battery. → Mitigation: The overlay is lightweight (no continuous rendering); use a persistent notification so users can stop the service easily.
- **[Android 14+ overlay restrictions]** Newer Android versions may further restrict overlay behavior. → Mitigation: Target API 29-34 and test on latest; the SYSTEM_ALERT_WINDOW permission is still supported.
- **[Screenshot storage size]** Screenshots can consume significant storage over time. → Mitigation: Compress to JPEG at 80% quality; provide session cleanup/delete functionality.
