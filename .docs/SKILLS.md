# SKILLS

Use the current agent's available-skills catalog to resolve skill locations. Claude may use `~/.claude/skills/`. Read an applicable installed SKILL.md before using it, and name applied skills in the summary. User instructions take precedence.

Groups describe project relevance, not installation status. Keep all template rows. Not-applicable rows become relevant only if the scope changes. A documentation-only setup does not itself require implementation skills.

If a required skill is missing, report the missing capability and inspect the official Android skills catalog when relevant. Do not claim a listed skill is installed, and do not install outside the project without user authorization. Follow explicit skill triggers.

| Group | Skill | Use it when… |
|---|---|---|
| **Primary** | `android-cli` | Creating projects or modules, building, running on devices or emulators, taking screenshots, managing the SDK, looking up official docs |
| **Primary** | `testing-setup` | Adding unit, UI, screenshot or end-to-end tests, or test infrastructure |
| **Primary** | `android-intent-security` | Editing manifest components, `exported` flags, or code that handles incoming intents |
| **Primary** | `play-policy-insights` | Adding permissions or data collection, and before any distribution |
| Situational | `android-xr-specialist` | Android XR implementation or review; verify installed API versions and fallbacks |
| Situational | `display-glasses-with-jetpack-compose-glimmer` | Android XR display-glasses apps (Glimmer UI) |
| Situational | `projected-hardware` | Camera, microphone or connection lifecycle on projected or XR devices |
| Situational | `android-smartwatch-bluetooth` | Wear OS apps, or a phone acting as a Bluetooth HID device |
| Situational | `compose-focus` | Compose or Glimmer focus navigation or input traversal |
| Situational | `dotlottie-compottie-android` | Android Lottie or dotLottie rendering, playback or integration |
| Situational | `agp-9-upgrade` | Changing the Android Gradle Plugin, Gradle or Kotlin plugin setup |
| Situational | `android-profiler` | Investigating CPU, memory, jank, startup or battery |
| Situational | `r8-analyzer` | Enabling or tuning R8 and keep rules |
| Situational | `navigation-3` | Adopting or extending Jetpack Navigation 3 |
| Situational | `styles` | Compose Styles API, shared design-system components |
| Situational | `camerax` | Camera features |
| Situational | `edge-to-edge` | Compose UI overlapping system bars; IME insets |
| Situational | `adaptive` | Layouts for tablets, foldables, desktop, TV, Auto or XR; window size handling |
| Situational | `navigation-event` | Custom back handling or predictive back |
| Situational | `wear-compose-m3` | Wear OS UI |
| Situational | `compottie` | Explicit Compottie or KMP animation work |
| Not applicable | `ml-kit-genai-prompt-api` | On-device Gemini Nano (ML Kit Prompt API) |
| Not applicable | `appfunctions` | Exposing app actions to system agents |
| Not applicable | `verified-email` | Verified email retrieval with Credential Manager |
| Not applicable | `engage-sdk-integration` | Integrating the Play Engage SDK |
| Not applicable | `leanback-to-compose-tv-migration` | Migrating Android TV UI from Leanback to Compose for TV |
| Not applicable | `media3-cast-integration` | Adding Google Cast with Media3 |
| Not applicable | `restore-credentials` | Silent sign-in after a device restore |
| Not applicable | `migrate-xml-views-to-jetpack-compose` | Migrating XML View UI to Compose |
| Not applicable | `play-billing-library-version-upgrade` | Upgrading the Play Billing Library |

Move a skill from "Not applicable" to "Situational" only when the project's scope changes, and record that change in HISTORY.

## Local setup tools

The repository contains .agents/skills/harness-router, project-onboarding and documentation-and-adrs.
For harness maintenance, follow root AGENTS.md and the [Project contract](../.agents/gates/project-contract.md).
The broader application adoption workflow requires scripts currently absent from the working tree; no full-onboarding success is implied.
The Primary Android skill names above are guidance, not verified installations in this session.
No skills were installed or removed.
