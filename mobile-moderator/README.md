# Moderator App (Android / Kotlin)

> Updated for the 29 September final website export. See
> [integration checks, backups and run instructions](../FINAL_PROJECT_INTEGRATION.md).

Native Android app for the Second-Hand Goods Verification Marketplace, used by
moderators to review listing verifications and resolve disputes on the go.

Built with:
- Kotlin + Jetpack Compose (Material 3)
- Retrofit + OkHttp (networking, with automatic JWT refresh)
- Jetpack DataStore (secure token storage)
- Navigation Compose

It talks to the same Django REST API as the web app — no separate backend needed.

See `MOBILE_SETUP_GUIDE.md` (in the project root, Sinhala) for full setup instructions.

## Quick notes

- The emulator API base URL is `http://10.0.2.2:8000/api/` — this is the Android emulator's
  alias for your computer's `localhost`, so it talks directly to your local Django server.
- Choose **Server settings** on the login screen to select the backend address.
  For a **physical phone** on the same Wi-Fi, use your computer's LAN IP, e.g.
  `http://192.168.1.10:8000/api/`. Debug builds accept development HTTP addresses;
  release builds require HTTPS.
- Only accounts with `role = moderator` can log in to this app (enforced by the app itself).

## Compose compiler and Android Studio

The app uses Kotlin **2.0.21** and `org.jetbrains.kotlin.plugin.compose`
**2.0.21**. Keep these versions identical. The old
`composeOptions.kotlinCompilerExtensionVersion = "1.5.14"` setting has been
replaced by the explicit Compose compiler plugin, following the
[Kotlin migration guide](https://kotlinlang.org/docs/compose-compiler-migration-guide.html).

Open the `mobile-moderator` folder as the Gradle project in Android Studio and
use **Sync Project with Gradle Files** after changing compiler plugins. The
repository root contains separate backend, website and Android projects.
Studio must import the app module's Compose plugin to analyze its UI code;
without it, one configuration issue can produce many `ComposableFunction1`,
lambda type and `weight` errors in the editor.

Verify the build from this folder with:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```
