## ADDED Requirements

### Requirement: Compile session annotations to formatted text
The system SHALL compile all annotations in a session into a single formatted text block. The default format SHALL be markdown. Each annotation entry SHALL include: an index number, timestamp, source app (if available), selected text (if any, as a blockquote), and the user's comment.

#### Scenario: Compiling a session with mixed annotations
- **WHEN** user taps "Export" on a session containing screenshots, text captures, and comments
- **THEN** the system generates a markdown document where each annotation is a numbered section with its metadata, quoted text (if present), comment, and a note indicating if a screenshot is attached

#### Scenario: Compiling an empty session
- **WHEN** user taps "Export" on a session with no annotations
- **THEN** the system displays a message that there are no annotations to export

### Requirement: Copy all to clipboard
The system SHALL provide a "Copy All" action that copies the compiled text to the system clipboard.

#### Scenario: Copying compiled annotations
- **WHEN** user taps "Copy All" after compilation
- **THEN** the compiled markdown text is placed on the system clipboard and a confirmation toast is shown

#### Scenario: Pasting copied annotations
- **WHEN** user pastes after copying compiled annotations
- **THEN** the pasted content matches the compiled markdown format with all annotations included

### Requirement: Share compiled output
The system SHALL provide a "Share" action that sends the compiled text via the Android share sheet. If the session includes screenshots, the share intent SHALL include them as attached images.

#### Scenario: Sharing text-only annotations
- **WHEN** user taps "Share" on a session with only text annotations
- **THEN** the system opens the share sheet with the compiled markdown as text content

#### Scenario: Sharing annotations with screenshots
- **WHEN** user taps "Share" on a session that includes screenshot annotations
- **THEN** the system opens the share sheet with the compiled markdown as text and screenshot images as attachments (ACTION_SEND_MULTIPLE)

### Requirement: Export from overlay
The user SHALL be able to trigger export/copy directly from the floating overlay for the active session, without needing to open the main activity.

#### Scenario: Quick copy from overlay
- **WHEN** user taps "Copy All" on the expanded overlay toolbar
- **THEN** all annotations from the active session are compiled and copied to clipboard with a confirmation toast
