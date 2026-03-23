## 1. Project Setup

- [x] 1.1 Create new Android project with Kotlin, targeting API 29+ (minSdk 29, compileSdk 34)
- [x] 1.2 Add dependencies: Jetpack Compose, Room, Kotlin coroutines, Material 3, Compose lifecycle
- [x] 1.3 Configure AndroidManifest with SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE permissions and intent filters for ACTION_PROCESS_TEXT and ACTION_SEND

## 2. Data Layer

- [x] 2.1 Define Room entities: Session (id, name, createdAt, status) and Annotation (id, sessionId, screenshotPath, selectedText, comment, sourceApp, timestamp)
- [x] 2.2 Create Room DAOs for Session and Annotation with queries for CRUD, list-by-session, and active session lookup
- [x] 2.3 Create Room database class and provide singleton instance
- [x] 2.4 Create repository classes wrapping DAOs with Flow-based data access

## 3. Overlay Service & Floating UI

- [x] 3.1 Implement OverlayService as a foreground service with persistent notification and stop action
- [x] 3.2 Create collapsed bubble Compose UI (small circular FAB) attached to WindowManager with TYPE_APPLICATION_OVERLAY
- [x] 3.3 Implement drag-to-reposition with snap-to-edge behavior on the collapsed bubble
- [x] 3.4 Create expanded toolbar Compose UI with action buttons: Screenshot, Add Text, View All, Copy All, Collapse, Close
- [x] 3.5 Implement toggle between collapsed and expanded states on tap
- [x] 3.6 Implement overlay hide/show during screenshot capture

## 4. Permission Handling

- [x] 4.1 Create permission check flow for SYSTEM_ALERT_WINDOW in main activity with navigation to system settings
- [x] 4.2 Create MediaProjection consent flow triggered on first screenshot capture per session
- [x] 4.3 Cache MediaProjection token for session duration to avoid repeated consent dialogs

## 5. Annotation Capture

- [x] 5.1 Implement screenshot capture using MediaProjection virtual display, saving as JPEG (80% quality) to app-private storage
- [x] 5.2 Implement comment input overlay that appears after screenshot capture with Save/Cancel actions
- [x] 5.3 Register and handle ACTION_PROCESS_TEXT intent to receive selected text from other apps
- [x] 5.4 Register and handle ACTION_SEND intent to receive shared text via share sheet
- [x] 5.5 Implement manual text entry from the expanded overlay's "Add Text" button
- [x] 5.6 Auto-populate metadata (timestamp, foreground app package name) on every annotation creation

## 6. Review Session Management

- [x] 6.1 Auto-create a new session (or resume active session) when overlay service starts
- [x] 6.2 Implement annotation list panel accessible from expanded overlay (scrollable, showing thumbnail/text preview/comment/timestamp)
- [x] 6.3 Implement edit comment functionality on annotation list items
- [x] 6.4 Implement swipe-to-delete on annotations with screenshot file cleanup
- [x] 6.5 Build main activity UI: session list with rename, close, delete actions

## 7. Export & Compile

- [x] 7.1 Implement markdown compilation of all annotations in a session (numbered sections with metadata, blockquoted text, comments, screenshot indicators)
- [x] 7.2 Implement "Copy All" to clipboard from both the overlay and main activity with confirmation toast
- [x] 7.3 Implement "Share" via share sheet with compiled text and optional screenshot attachments (ACTION_SEND_MULTIPLE)
- [x] 7.4 Handle empty session export gracefully with user message

## 8. Polish & Testing

- [x] 8.1 Add edge-case handling: overlay removed on low memory, service restart after process death
- [ ] 8.2 Test overlay behavior across app switches, screen rotation, and split-screen mode
- [ ] 8.3 Test full workflow: start session, capture screenshots, add text, add comments, export, paste
- [ ] 8.4 Verify storage cleanup when deleting annotations and sessions
