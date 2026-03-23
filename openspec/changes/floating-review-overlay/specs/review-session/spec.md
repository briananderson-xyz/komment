## ADDED Requirements

### Requirement: Session creation
The system SHALL create a new review session when the user starts the overlay service. Each session SHALL have a name (user-editable, defaults to "Review - {date}"), a creation timestamp, and a status (active or closed).

#### Scenario: Auto-creating a session on overlay start
- **WHEN** user starts the overlay service and no active session exists
- **THEN** the system creates a new session with a default name based on the current date and marks it as active

#### Scenario: Resuming an existing active session
- **WHEN** user starts the overlay service and an active session already exists
- **THEN** the system resumes the existing active session rather than creating a new one

### Requirement: Annotation list within session
The system SHALL display a scrollable list of all annotations in the current session, accessible from the expanded overlay. Each list item SHALL show a thumbnail (for screenshots), text preview, comment preview, and timestamp.

#### Scenario: Viewing annotations in a session
- **WHEN** user taps the "View All" button on the expanded overlay
- **THEN** a panel displays all annotations in the current session, ordered by creation time (newest first)

#### Scenario: Empty session
- **WHEN** user views annotations and the session has no entries
- **THEN** the system displays a message indicating no annotations have been captured yet

### Requirement: Edit and delete annotations
The user SHALL be able to edit the comment on any annotation and delete any annotation from a session.

#### Scenario: Editing a comment
- **WHEN** user taps an annotation in the list and modifies the comment text
- **THEN** the updated comment is saved to the database

#### Scenario: Deleting an annotation
- **WHEN** user swipes or taps delete on an annotation
- **THEN** the annotation (and its screenshot file, if any) is permanently removed from the session

### Requirement: Session persistence
All sessions and their annotations SHALL be persisted in a local Room database. Data SHALL survive app restarts and device reboots.

#### Scenario: Data survives app restart
- **WHEN** the app process is killed and restarted
- **THEN** all previously saved sessions and annotations are available in the main activity

### Requirement: Session management from main activity
The main activity SHALL display a list of all sessions (active and closed). Users SHALL be able to rename, delete, or close sessions from this list.

#### Scenario: Closing a session
- **WHEN** user taps "Close" on an active session
- **THEN** the session status changes to closed; it remains viewable and exportable but no new annotations can be added

#### Scenario: Deleting a session
- **WHEN** user taps "Delete" on a session and confirms
- **THEN** the session and all its annotations (including screenshot files) are permanently removed
