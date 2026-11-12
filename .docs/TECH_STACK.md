# TECH_STACK — SDO Companion

Versions below are declared values, not independently resolved artifacts. Tests: [VALIDATION](VALIDATION.md).

## Toolchain

| Item | Version / value | Source |
|---|---|---|
| Kotlin / Compose / serialization plugins | 2.4.20 | root build.gradle.kts |
| Gradle | 9.7.1 | gradle/wrapper/gradle-wrapper.properties |
| AGP | 9.4.0 | root build.gradle.kts |
| Daemon JDK | JetBrains 25 | gradle/gradle-daemon-jvm.properties |
| Java source / target | 25 / 25 | app/build.gradle.kts |
| UI | Compose, Material 3 | app/build.gradle.kts and version catalog |
| Android CI launcher | 25, matching daemon | .github/workflows/ Android jobs |
| Firebase-rules CI launcher | 21; separate rules job | .github/workflows/firebase-rules.yml |

## Modules

| Module | Type | Namespace / applicationId | minSdk | targetSdk | compileSdk |
|---|---|---|---|---|---|
| :app | Application | com.kinderman.sdo / com.kinderman.sdo | 30 | 37 | 37 |

versionCode defaults to 1 (releaseCode override); versionName defaults to 0.1.0 (releaseVersion override).

## Build configuration

| Item | Value |
|---|---|
| Types / flavors | debug, release; no product flavors |
| Signing | Optional release config; debug uses it when available, otherwise default debug certificate |
| Signing inputs | SDO_KEYSTORE_FILE, SDO_KEYSTORE_PASSWORD, SDO_KEY_ALIAS, SDO_KEY_PASSWORD; KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD aliases |
| Keystore fallback | firebase/sdo-companion-release.jks when present; no credentials recorded |
| Release shrinking | optimization.enable = true; debug has no explicit optimization configuration |
| ABIs | No explicit ABI filters or splits |
| APK naming | No output-name customization in module Gradle |
| Properties | -Xmx3g, UTF-8, caching/parallel enabled, official Kotlin style, AndroidX |
| Repositories | google, mavenCentral; plugin portal for plugin resolution |
| Extra resources | root catalogs/ included as main JVM resources |
| local.properties | Absent; values not inspected |
| Local libs | No AAR/JAR found under root/module libs |
| Google services | Plugin applied only when root/app google-services.json exists |

## Commands

Windows uses .\gradlew.bat; POSIX uses ./gradlew.

| Task | Windows command |
|---|---|
| Debug APK | .\gradlew.bat :app:assembleDebug |
| Release APK | .\gradlew.bat :app:assembleRelease |
| Install debug | .\gradlew.bat :app:installDebug |

APK directories: app/build/outputs/apk/debug/ and app/build/outputs/apk/release/.
No .github/init.gradle exists.

## Plugins

Versions are pinned in root build.gradle.kts unless noted.

| ID | Version | Use |
|---|---|---|
| com.android.application | 9.4.0 | app |
| org.jetbrains.kotlin.android | 2.4.20 | Declared apply false; not applied by app |
| org.jetbrains.kotlin.plugin.compose | 2.4.20 | app |
| org.jetbrains.kotlin.plugin.serialization | 2.4.20 | app |
| com.google.devtools.ksp | 2.3.12 | app |
| com.google.gms.google-services | 4.5.0 | Conditional app |
| org.jlleitschuh.gradle.ktlint | 14.2.0 | app |
| dev.detekt | 2.0.0-alpha.6 | app |
| org.jetbrains.kotlinx.kover | 0.9.11 | app |
| org.gradle.toolchains.foojay-resolver-convention | 1.0.0 | settings.gradle.kts |

## Dependencies

Source: gradle/libs.versions.toml, except core-ktx explicitly pinned in app/build.gradle.kts.
BOM entries identify the declared platform; child artifact versions are not individually declared.

| Coordinates | Declared version / source | Used in |
|---|---|---|
| androidx.compose:compose-bom | 2026.09.00, catalog | app + androidTest platform |
| androidx.compose.material3:material3 | Compose BOM | app |
| androidx.compose.foundation:foundation | Compose BOM | app |
| androidx.compose.material:material-icons-extended | Compose BOM | app |
| androidx.compose.ui:ui-test-junit4 | Compose BOM | androidTest |
| androidx.compose.ui:ui-test-manifest | Compose BOM | debug only |
| androidx.activity:activity-compose | 1.13.0, catalog | app |
| androidx.lifecycle:lifecycle-runtime-compose | 2.11.0, catalog | app |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.11.0, catalog | app |
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.11.0, catalog | app |
| androidx.navigation:navigation-compose | 2.10.1, catalog | app dependency; custom navigation actually used |
| androidx.credentials:credentials | 1.6.0, catalog | app |
| androidx.credentials:credentials-play-services-auth | 1.6.0, catalog | app |
| com.google.android.libraries.identity.googleid:googleid | 1.2.0, catalog | app |
| androidx.room:room-runtime | 2.8.5, catalog | app |
| androidx.room:room-ktx | 2.8.5, catalog | app |
| androidx.room:room-compiler | 2.8.5, catalog | app KSP |
| com.google.firebase:firebase-bom | 34.19.0, catalog | app platform |
| com.google.firebase:firebase-auth | Firebase BOM | app |
| com.google.firebase:firebase-firestore | Firebase BOM | app |
| org.jetbrains.kotlinx:kotlinx-coroutines-play-services | 1.11.0, catalog | app |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.11.0, catalog | app |
| junit:junit | 4.13.2, catalog | test |
| androidx.test.ext:junit | 1.3.0, catalog literal | androidTest |
| androidx.test:core-ktx | 1.7.0, module literal | androidTest |

## Platform APIs

ComponentActivity/Compose lifecycle, SharedPreferences, Credential Manager, Android back handling and Room SQLite are present. Firebase provides auth/cloud persistence. No CameraX, Bluetooth, TTS, Hilt or Koin dependency appears in the inspected build.
