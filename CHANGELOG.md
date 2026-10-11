# Changelog

All notable changes to the Palimpsest project will be documented in this file.

## [Unreleased] - M3
### Added
- `TensionTracker`: Per-player tension budget tracker with multi-factor inputs (darkness, underground, isolation, safe signals from villagers/landmarks, and sleep attempts). Includes combat and damage suppression with cooldown tracking.
- `AnomalyDirector`: Subsystem ticking at 1Hz, driving deterministic anomaly dispatch via seeded RNG (`createDeterministicRng(worldSeed, playerUUID, eventIndex)`), respecting configured `maxTier`, quiet windows (120s default), and budget costs.
- 8 Core Initial Anomalies:
  - `DoorAjarAnomaly` (Tier 1): Silently cracks open an untargeted door behind the player.
  - `TorchOutAnomaly` (Tier 1): Extinguishes a torch behind the player into an unlit redstone/placeholder state with smoke particles.
  - `FootstepsBehindAnomaly` (Tier 1): Plays phantom directional step sounds 3-6 blocks directly behind the player.
  - `SoundOfYouAnomaly` (Tier 2): Replays the player's last recorded action sound 10 blocks away.
  - `ItemFrameTurnAnomaly` (Tier 1): Silently increments the rotation of an item in a nearby item frame outside player FOV.
  - `SignChangeAnomaly` (Tier 2): Temporarily corrupts a sign line with an encrypted/garbled message or cryptic note.
  - `WatcherAnomaly` (Tier 2): Spawns a silent, frozen, invulnerable mannequin at the edge of vision that vanishes when looked at or approached.
  - `WrongHourAnomaly` (Tier 1): Sends a transient client-side time packet causing sky or clock desynchronization.
- Config and Accessibility Integration: Wired into `PalimpsestConfig` (`maxTier`, `quietWindowSeconds`, `disableFlashingLights`, `disableJumpscares`).
- Debug Commands: `/palimpsest dev director fire <anomaly>` and `/palimpsest dev director tension <amount>`.
- Automated tests: `DirectorSimulationTest` validating all 8 anomalies, tier 0 suppression, 1-hour session density & quiet window adherence, combat/damage suppression, and deterministic replay.

## M2 - Terminals & Puzzles
### Added
- `TerminalBlock` and `TerminalBlockEntity`: In-game terminal console block with custom node identification and state persistence.
- `TerminalScreen`: Client retro CRT phosphor terminal UI with scrollback buffer, blinking cursor, typing clicks, command history, tab auto-completion, and escape exit.
- `VirtualFs`: Data-driven virtual filesystem with path normalization, direct/ancestral flag permissions gating, and `onRead` flag triggers.
- `TerminalManager`: Server-authoritative command interpreter supporting `help`, `whoami`, `pwd`, `cd`, `ls`, `cat`, `login`, `logout`, `status`, `checksum`, `mount`, `umount`, `diff`, `restore`, `format`, and `clear`. Includes length limits (512 chars) and rate limiting (5 cmds/sec).
- Networking payloads: `OpenTerminalPayload` (S2C), `TerminalOutputPayload` (S2C), `TerminalCommandPayload` (C2S).
- `AnswerVerifier`: Normalization pipeline (`lowercase`, `trim`, `collapse_ws`, `strip_punct`, `digits_only`, `octal_to_decimal`) and salted SHA-256 verification.
- `PuzzleEngine`: Puzzle registration, answer validation, flag granting, and state synchronization with `StoryState`.
- `ChapterPackLoader`: Reload listener loading `chapters/<id>/chapter.json`, `puzzles/*.json`, and `vfs/*.json` with schema validation and fault tolerance.
- Spoiler hygiene Gradle task: `verifyNoSpoilers` integrated into `check` task.
- Automated tests: `PuzzleSelfTest`, `VirtualFsTest`, `TerminalEngineTest`, and `TerminalFuzzTest` (37 tests total passing).

## M1 - Levels
### Added
- `LevelManager`: Fantasy wrapper handling temporary levels (Annex, Echo) and persistent levels (Mirror, Backup).
- Registry persistence: Automatic re-opening of registered persistent levels on server startup via `StoryState`.
- Dimension types: `annex.json`, `echo.json`, `mirror.json`, `backup.json` matching Minecraft 26.3 dimension format.
- `TaskScheduler`: Prioritized queue of resumable tasks bounded by per-tick millisecond budget.
- `StoryState`: `SavedData` with Codec serialization for world flags, player flags, last anchor positions, landmarks, and level registry.
- `Evacuator`: Player evacuation to last known anchor positions before dimension unload/deletion and upon join.
- `DebugCommands`: `/palimpsest dev level list|open|close|tp` for runtime level management.
- Unit and spike tests: `TaskSchedulerTest`, `StoryStateTest`, and `LevelSpikeTest` for V6, V7, and V8.

## M0 - Scaffold and verification spike

