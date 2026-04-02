# Changelog

All notable changes to Project Asteria will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0] - 2025-10-10

### Changed
- **BREAKING**: Migrated from NASA's official APOD API to custom O4bit Space Mirror API (https://api.o4bit.space)
  - New endpoints: `/apod/latest` and `/apod/{date}`
  - No API key required for the mirror API
  - Improved reliability and performance

### Fixed
- **Notification Icon**: Changed from rectangular placeholder to proper circular app icon for better visual appearance
- **Daily Space Discoveries Toggle**: Fixed non-functional notification toggle in Settings
  - Toggle now properly enables/disables daily notifications
  - State is persisted using DataStore preferences
  - WorkManager properly scheduled/cancelled based on toggle state
- **HTML Rendering**: Fixed explanation text showing raw HTML tags
  - Added HTML stripping utility to display clean text
  - Applies to both explanation card and detail view
- **API Compatibility**: Made `serviceVersion` field optional to support mirror API response structure
  - Added optional `copyright` and `thumbnail` fields from mirror API
- **Image Loading**: Fixed images not loading from NCKU mirror server
  - Added network security configuration to allow HTTP traffic from trusted mirror domain
  - Ensures astronomy pictures load correctly from mirror API

### Added
- `NotificationPreferencesRepository` for managing notification preferences
- `TextUtils` utility class for HTML stripping and text processing
- Proper state management for notification settings
- **Debug Tools Section** in Settings (debug builds only)
  - Manual "Send Test Notification" button for testing notification system
  - Helps developers verify notification functionality without waiting for scheduled notifications

## [2.1] - 2025-09-10

### Fixed
- Minor notification icon updates (attempted fix)

## [2.0] - 2025-08-05

### Added
- Immersive crash reporting integration
- Enhanced crash reporting
- Improved error handling

### Changed
- Updated UI with Material 3 components
- Improved notification system

## [1.0] - 2025-07-01

### Added
- Initial release
- Daily Astronomy Picture of the Day (APOD) display
- Widget with daily image and space facts
- Light/dark mode toggle
- Wallpaper setting functionality
- Daily notifications
- Share functionality

---

## Links
- [Repository](https://github.com/O4bit/Project-Asteria)
- [Issues](https://github.com/O4bit/Project-Asteria/issues)
