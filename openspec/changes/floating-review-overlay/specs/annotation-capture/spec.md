## ADDED Requirements

### Requirement: Screenshot capture via MediaProjection
The system SHALL capture a screenshot of the current screen when the user taps the screenshot button on the expanded overlay. The overlay SHALL temporarily hide itself during capture to avoid appearing in the screenshot. The screenshot SHALL be saved as a JPEG file in app-private storage.

#### Scenario: Capturing a screenshot
- **WHEN** user taps the screenshot button on the expanded overlay
- **THEN** the overlay hides, the system captures the current screen via MediaProjection, saves it as a JPEG file, and the overlay reappears with a confirmation and a prompt to add a comment

#### Scenario: MediaProjection permission not granted
- **WHEN** user taps screenshot for the first time in a session and MediaProjection consent has not been given
- **THEN** the system presents the MediaProjection consent dialog and, upon approval, proceeds with the capture

#### Scenario: MediaProjection permission denied
- **WHEN** user denies the MediaProjection consent dialog
- **THEN** the system displays a message explaining that screenshot capture requires screen recording permission and returns to the expanded overlay

### Requirement: Text capture via share/selection
The system SHALL register as a handler for ACTION_PROCESS_TEXT and ACTION_SEND text intents so users can send selected text from any app directly to the overlay. Received text SHALL be attached to a new annotation entry.

#### Scenario: Receiving selected text via text selection menu
- **WHEN** user selects text in another app and chooses this app from the text selection menu
- **THEN** the system creates a new annotation entry with the selected text pre-filled and prompts the user to add a comment

#### Scenario: Receiving text via share sheet
- **WHEN** user shares text from another app via the system share sheet to this app
- **THEN** the system creates a new annotation entry with the shared text pre-filled and prompts the user to add a comment

### Requirement: Manual text entry
The system SHALL allow the user to manually type or paste text into an annotation entry's selected text field from the expanded overlay.

#### Scenario: Adding text manually
- **WHEN** user taps "Add Text" on the expanded overlay
- **THEN** a text input field appears where the user can type or paste content, which is attached to a new annotation entry

### Requirement: Comment attachment
Every annotation entry SHALL support a user comment field. The system SHALL prompt the user to add a comment after each capture action (screenshot or text). The comment MAY be left empty.

#### Scenario: Adding a comment to a screenshot annotation
- **WHEN** a screenshot is captured successfully
- **THEN** the system presents a comment input field; the user can type a comment and tap "Save" to store the annotation with the comment

#### Scenario: Skipping a comment
- **WHEN** the comment prompt appears after a capture
- **THEN** the user can tap "Save" without entering a comment and the annotation is saved with an empty comment field

### Requirement: Annotation metadata
Each annotation entry SHALL automatically include metadata: timestamp (ISO 8601), and the foreground app package name at time of capture (when available).

#### Scenario: Metadata is recorded on capture
- **WHEN** a new annotation is created via any capture method
- **THEN** the annotation includes the current timestamp and the detected foreground app package name
