## ADDED Requirements

### Requirement: Overlay service lifecycle
The system SHALL start a foreground service that displays a floating overlay window above all other apps when the user launches the app or taps "Start Review" from the main activity. The service SHALL continue running until the user explicitly stops it via the overlay's close button or the persistent notification action.

#### Scenario: Starting the overlay service
- **WHEN** user taps "Start Review" in the main activity
- **THEN** the system starts a foreground service with a persistent notification and displays a floating bubble overlay on screen

#### Scenario: Stopping the overlay service
- **WHEN** user taps the close/stop button on the expanded overlay OR taps "Stop" on the persistent notification
- **THEN** the overlay is removed from screen and the foreground service stops

#### Scenario: Service survives app switch
- **WHEN** the overlay service is running and the user switches to another app
- **THEN** the floating overlay remains visible above the other app

### Requirement: Overlay permission handling
The system SHALL check for SYSTEM_ALERT_WINDOW permission before starting the overlay service. If the permission is not granted, the system SHALL navigate the user to the system settings to grant it.

#### Scenario: Permission not granted
- **WHEN** user taps "Start Review" and SYSTEM_ALERT_WINDOW is not granted
- **THEN** the system displays a prompt explaining why the permission is needed and opens the system overlay permission settings for the app

#### Scenario: Permission already granted
- **WHEN** user taps "Start Review" and SYSTEM_ALERT_WINDOW is already granted
- **THEN** the overlay service starts immediately without any permission prompt

### Requirement: Collapsed and expanded overlay states
The overlay SHALL have two states: collapsed (a small floating bubble) and expanded (a toolbar with action buttons). The user SHALL be able to toggle between states by tapping the bubble.

#### Scenario: Expanding the overlay
- **WHEN** user taps the collapsed floating bubble
- **THEN** the overlay expands to show the annotation toolbar with capture actions (screenshot, add text, add comment)

#### Scenario: Collapsing the overlay
- **WHEN** user taps the collapse button on the expanded toolbar
- **THEN** the overlay shrinks back to the floating bubble

### Requirement: Overlay drag repositioning
The user SHALL be able to drag the collapsed bubble to reposition it anywhere on screen. The expanded toolbar SHALL also be draggable via a drag handle.

#### Scenario: Dragging the bubble
- **WHEN** user long-presses and drags the collapsed bubble
- **THEN** the bubble follows the user's finger and stays at the release position

#### Scenario: Bubble snaps to edge
- **WHEN** user releases the bubble after dragging
- **THEN** the bubble snaps to the nearest screen edge (left or right) for a non-intrusive resting position
