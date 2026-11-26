# BunkSafe — Glassmorphic Attendance Tracker

A native Android attendance tracking application built with **Kotlin**, **Jetpack Compose**, and **Room**, designed with a clean frosted-glass aesthetic, adaptive theme presets, and real-time safe-bunk calculations.

---

## Features

- **Frosted Apple-Glass UI/UX**
  - Custom `GlassCard` surfaces with ambient vertical lighting gradients, subtle rim highlights, and a floating magnifier navigation dock (`FloatingGlassMagnifierDock`).
  - Full Light and Dark mode support paired with 5 adaptive glass accent themes (**Frost**, **Sage**, **Nordic**, **Amber**, and **Lavender**).
  - Custom tuned haptic feedback (`triggerGlassHaptic`) across navigation tabs, date pills, and attendance actions.

- **Fast Daily Attendance Logging**
  - **7-Day Horizontal Date Strip:** Jump between days of the active week with live "Today" indicators and a full Material 3 Date Picker.
  - **One-Tap Batch Actions:** Mark an entire day's schedule in a single tap with **All Present**, **All Absent**, **All Cancel**, and **Clear All** pills, or log individual subjects as `PRESENT`, `ABSENT`, or `CANCELLED`.

- **Subject & Timetable Management**
  - Configure subjects with custom target attendance thresholds (e.g., 75%) and initial baseline counts (`initialPresent` / `initialTotal`) for mid-semester onboarding.
  - Assign weekly recurring slots across Monday–Sunday with instant filtering on the daily calendar.
  - Real-time safe-bunk and recovery calculations showing exactly how many classes you can safely skip or must attend to stay above target.

- **Data Persistence & Daily Reminders**
  - **JSON Backup & Restore:** Export and restore your subjects, weekly timetable, and attendance history via Android's Storage Access Framework (`CreateDocument` / `OpenDocument`) with zero storage permissions required.
  - **Smart End-of-Classes Notification:** Background `WorkManager` worker (`AttendanceReminderWorker`) that checks at your chosen time and notifies you only when scheduled classes for the day remain unlogged.

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM (`AndroidViewModel` + `StateFlow` / Kotlin Coroutines) |
| **Local Database** | Room (`subjects`, `timetable`, `attendance_records`) |
| **Background Tasks** | AndroidX WorkManager |
| **Splash & System UI** | AndroidX Core SplashScreen + Edge-to-Edge |

---

## Project Structure

```text
app/src/main/java/com/shovan/attendancetracker/
├── MainActivity.kt               # Navigation scaffold, floating glass dock, screens & theme presets
├── AttendanceViewModel.kt        # StateFlows, attendance math, batch actions & JSON backup/restore
├── AttendanceDao.kt              # Room queries and one-shot backup/worker helpers
├── AttendanceDatabase.kt         # Room database configuration
├── Entities.kt                   # Subject, TimetableSlot, and AttendanceRecord entities
└── AttendanceReminderWorker.kt   # Periodic WorkManager notification scheduler
