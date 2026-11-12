# Architecture decisions — SDO Companion

Append-only record of changes to [`../ARCHITECTURE.md`](../ARCHITECTURE.md), oldest first. The entry format and the rules are in `ARCHITECTURE.md` § Decision record.

## Define initial architecture
**Date:** 2026-11-12 · **Time:** 19:05 · America/La_Paz

**Modification:** Established the documentation baseline for the existing Compose/ViewModel and repository pattern, the module and package boundaries, the lifecycle and resource ownership, and Firebase Auth and Firestore contracts. Documented them in ARCHITECTURE.md. No application architecture was changed.

**Reason:** The architecture was documented from the code as it is, so future agents start from a verified baseline and not from assumptions. Templates and older guides were not treated as current.

## Record the observed SDO architecture
**Date:** 2026-11-12 · **Time:** 19:05 · America/La_Paz

**Modification:** Documented single-module manual dependency wiring, custom saveable navigation, multiple state flows, Room, preferences and Firebase collection boundaries. No application architecture changed.

**Reason:** The observed code differs from generic Hilt, NavHost and single-state guidance; an accurate baseline preserves those differences for explicit future decisions.
