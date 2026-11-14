# Standard theme and token reference

Values retained from the existing guide; [Standard](standard.md) owns their mode-specific use.
These are historical palette references, not permission to bypass current semantic theme roles.

## Themes

| Theme | Variant | Primary | Secondary | Error | Background | Surface |
|---|---|---|---|---|---|---|
| Ciano industrial | Dark | #30C0B7 | #5690DA | #EE227D | #040D1B | #191B1C |
| Verde terminal | Dark | #70FF9A | #B7FFCA | #FF7A9E | #030D07 | #0D1C12 |
| Rubro arcano | Dark | #FF866E | #FFC06B | #FF75AA | #150605 | #2A1210 |
| Violeta Rúnico | Dark | #BCA8FF | #69E6DC | #FF8DBD | #0D0719 | #21172F |
| Edgerunners | Dark | #FCEE09 | #00F0FF | #FF003C | #050A18 | #111827 |
| Magenta Onírico | Dark | #FF2AA1 | #8F7CFF | #FF6B82 | #110713 | #261126 |
| Aperture White | Light | #FF9A00 | #27A7D8 | #D45500 | #FFFFFF | #FFFFFF |
| Alto contraste | Dark | #63FFF1 | #A9CBFF | #FF70AE | #000000 | #101214 |
| Tema do sistema | System | Dark cyan / #006A64 | Blue / #3D6374 | Magenta / #BA1A1A | System | System |

Portuguese names above are existing in-app labels, not translated theme identifiers.

## Historical palette

| Token | Hex |
|---|---|
| `Void` | `#040D1B` |
| `VoidDeep` | `#061424` |
| `Panel` | `#191B1C` |
| `ContainmentPanel` | `#1A060F` |
| `ArcanePanel` | `#261E3C` |
| `MysticPanel` | `#3B0855` |
| `Carbon` | `#132B49` |
| `CarbonAlt` | `#183451` |
| `Grid` | `#383B3D` |
| `GridGuide` | `#595F61` |
| `WireframeNeutral` | `#7B8285` |
| `WireframeLight` | `#9BA3A8` |
| `TechCutDark` | `#274D7D` |
| `TechCut` | `#2C5784` |
| `TechCutCyan` | `#498099` |
| `Ice` / `TextGlow` | `#FCFCFD` |
| `TextPrimary` | `#EDEFF0` |
| `LabelFunctional` | `#CADCF2` |
| `LabelLight` | `#D8E6F6` |
| `Muted` | `#C2C9CC` |
| `MetaStamp` | `#D5D1E5` |
| `MetalType` | `#B0A8CE` |
| `MetalDeep` | `#8F82BA` |
| `AcidCyan` (`Acid`) | `#30C0B7` |
| `EnergyBlue` | `#5690DA` |
| `EnergyLight` | `#5E9CDE` |
| `AuraBlue` | `#91B6E6` |
| `AuraLight` | `#9CC1EA` |
| `StatHeader` | `#3B6FB0` |
| `StatHeaderLight` | `#407AB7` |
| `ArcanePassive` | `#6E5BA2` |
| `ArcaneLatent` | `#483B6D` |
| `AcidMagenta` (`Signal`) | `#EE227D` |
| `NeonCoral` | `#FD8083` |
| `PenaltyPink` | `#D85E99` |
| `StressPink` | `#E59BBA` |
| `HostileHeader` | `#852467` |
| `InsanityPink` | `#A84876` |
| `DamageTrack` | `#773153` |
| `DamageTrackDeep` | `#46192F` |
| `HazardText` | `#F2D1DD` |

## Resource mapping

| Resource | Active | Track |
|---|---|---|
| Life | StatHeaderLight | DamageTrack |
| Sanity | StatHeader | DamageTrack |
| Arcane | AuraBlue | ArcaneLatent |
| Energy | EnergyBlue | TechCutDark |
| Destiny | AcidCyan | TechCutDark |
| Exhaustion | NeonCoral | DamageTrack |
| Corruption | AcidMagenta | DamageTrackDeep |

Use Ice text when resource accents do not provide sufficient foreground contrast.
Do not use StatHeader, StatHeaderLight or EnergyBlue as small text on Carbon; reserve them for borders, icons and bars.
