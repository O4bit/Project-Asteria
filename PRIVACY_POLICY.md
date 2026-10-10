# Privacy Policy for Project Asteria

**Last Updated:** October 10, 2026

Project Asteria ("we", "our", or "the app"), developed by O4bit, is committed to respecting and protecting your privacy. This Privacy Policy explains our practices regarding user information.

## 1. Information Collection and Use

**Project Asteria does not collect, transmit, store, or sell any personal data.**

- **No Account Required:** You do not need to create an account or provide any personal details to use Project Asteria.
- **No Analytics or Trackers:** The app contains no third-party tracking libraries, behavioral analytics, advertising SDKs, or telemetry.
- **No Device Identifiers:** The app does not access or collect your advertising ID, device serial number, or other unique hardware identifiers.

## 2. Network Requests and Third-Party APIs

To provide space imagery and mission telemetry, Project Asteria communicates over secure HTTPS connections with public space data providers:

- **NASA APOD API** (and our open-source caching proxy at `asteria.o4bit.dev`): Used to retrieve the daily Astronomy Picture of the Day, images, and explanations.
- **Where The ISS At? API**: Used to retrieve real-time coordinates of the International Space Station.
- **The Space Devs (Launch Library 2)**: Used to display upcoming space launches.

These requests only ask for public astronomical information. No personal details, location coordinates from your phone, or user identifiers are ever sent to these services.

## 3. Device Permissions

Project Asteria requests only the minimum Android permissions necessary to provide its core features:

- **Internet & Network State (`INTERNET`, `ACCESS_NETWORK_STATE`):** Required to download astronomy images, launch schedules, and ISS coordinates from public space APIs.
- **Notifications (`POST_NOTIFICATIONS`):** Optional. Allows the app to schedule local reminders for new daily pictures or upcoming rocket launches. All notification scheduling runs locally on your device.
- **Set Wallpaper (`SET_WALLPAPER`):** Optional. Allows you to set the daily astronomy picture as your home screen or lock screen wallpaper when you explicitly tap the wallpaper button.
- **Run at Startup (`RECEIVE_BOOT_COMPLETED`):** Used solely to reschedule your existing home screen widget updates and notification alarms after your device restarts.

## 4. Diagnostics and Crash Logs

The app maintains a local, file-based diagnostic log stored securely inside the app's private sandbox storage on your device. 
- These logs never leave your phone automatically.
- Logs are only shared if you manually and deliberately tap the "Export Logs" button to report a bug.

## 5. Children's Privacy

Project Asteria is a general-audience educational application. Because we do not collect any personal data from any user, we do not knowingly collect personal information from children under the age of 13 (or applicable age in your jurisdiction).

## 6. Changes to This Policy

If we ever make changes to this policy, the updated version will be published in this repository with a revised "Last Updated" date.

## 7. Contact Us

If you have any questions or feedback regarding this Privacy Policy, please open an issue on our official GitHub repository:
https://github.com/O4bit/Project-Asteria
