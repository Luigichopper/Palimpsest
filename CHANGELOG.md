# Changelog

All notable changes to the Palimpsest project will be documented in this file.

## [Unreleased] - M5
### Added
- Echo Dimension & Base Duplication System:
  - `EchoBuilder`: Choice of base center via player respawn data or world spawn.
  - Sliced level duplication via `TaskScheduler` (`EchoCopyTask` with 2ms per-tick slice budget) ensuring radius-32 region copies never exceed server tick budgets.
  - Asynchronous and synchronous (`copyDirect`) region duplication routines including block entity data (chests, signs, lecterns, item frames).
  - Chapter-specific mutations: doors removed, torches extinguished, item frames rotated +1, chests populated with journal pages, and the "Double" entity (custom Mannequin) placed at the player's most-frequented station.
  - Station interaction tracking in `StoryState` via `UseBlockCallback` (recording player bed, crafting table, and furnace interactions).
  - Runtime Echo level configuration: mob spawning disabled, per-level time frozen.
  - `LevelManager.openEcho`: Creates and opens runtime Echo levels, triggers `EchoBuilder`, and teleports player to base center.
- Diff Lens & LevelDiffService:
  - `LevelDiffService`: Server-authoritative comparison of two levels or block provider functions over a specified bounding region.
  - Support for `IDENTITY` (Anchor<->Echo, Anchor<->Backup) and `REFLECT` (Anchor<->Mirror) coordinate transforms.
  - Diffs categorized into `ADDED`, `REMOVED`, and `CHANGED` states, sorted by distance from center, and capped to configured max results (default 2048).
  - `DiffLensItem`: Scanner item triggering diff requests and sending S2C `DiffOverlayPayload`.
  - Client-side `DiffOverlayHandler`: Multi-colored particle visualization (Green for ADDED, Red for REMOVED, Cyan for CHANGED) rendered in client tick events.
- Chapter 1 Progression & Content:
  - Chapter 1 definition in `chapters/ch1/chapter.json` (requires `ch0.complete`).
  - Puzzle `ch1.spot_diff` (`diff_pattern`) and `ch1.terminal_diff` (`terminal_command`).
  - VFS files `/home/odette/echo_notes.txt` and `/echo/base_manifest.txt`.
  - Solutions recorded in `private/answers.md` and mechanism descriptions in `docs/PUZZLES.md`.
  - Automated tests: `LevelDiffServiceTest` (identical regions, planted diffs, distance sorting, result capping, reflect transform) and expanded `PuzzleSelfTest`.
  - Spoiler hygiene check (`verifyNoSpoilers`) passing.

## M4 - Chapter 0 Content
### Added
- Landmarks L0 (Cabin) & L1 (Annex Entrance Bunker):
  - `LandmarkDef` and `landmarks.json` schema.
  - `ProceduralDsl`: DSL utilities for boxes, fills, doors, lecterns, terminals, chests, and pots.
  - `CabinBuilder`: Generates L0 Cabin with Terminal node `term0_cabin` and README written book with shuffled SGA acrostic pages (`EPITAPH`).
  - `BunkerBuilder`: Generates L1 Subterranean Annex bunker embedded in terrain, iron blast door, 4 decorated ceramic pots, and Terminal node `term1_bunker`.
  - `LandmarkPlacer`: Automated, idempotent worldgen placement using temporary forced chunk tickets on first activation in eligible worlds.
  - Spawn-to-Cabin teleport on first player activation in eligible worlds.
  - Commands: `/palimpsest dev scout` (surface Y, biome ID, slope) and `/palimpsest dev landmark place <id>`.
- Annex Temporary Puzzle Rooms & Progression:
  - `AnnexTemplate` and `AnnexBuilder` for generating temporary per-player Annex chambers (Waiting Room + Frame Room).
  - P0.4 octal item-frame calibration code (Compass, Clock, Echo Shard).
  - `AnnexManager`: Solves P0.4, sets `ch0.complete`, emits narrative collapse beat, evacuates player safely to Anchor, and cleans up temporary dimension.
- Complete Chapter 0 Puzzle Chain:
  - P0.0: Seed steganography tool `tools/make_seed_stego.py` embedding seed in red-channel LSB of `web/assets/recovery_notice.png`.
  - P0.1: L0 Cabin lectern acrostic puzzle (`ch0.lectern_order.json`).
  - P0.2: L0 Terminal login puzzle (`ch0.terminal_login.json`).
  - P0.3: L1 Bunker door ceramic sherd cipher (`ch0.sherd_code.json`).
  - P0.4: Annex Frame Room octal code (`ch0.frame_code.json`).
  - VFS files: `/home/odette/notes.txt`, `/home/odette/sherd_legend.txt`, `/var/log/recovery.log`.
  - `PuzzleSelfTest` dynamically verifies all Chapter 0 puzzle definitions against `private/answers.md`.
- Website v0 (`web/`):
  - Retro phosphor CRT terminal styling (`style.css`), dark mode, and high-contrast toggle.
  - Landing page (`index.html`) with recovery notice and downloadable carrier graphic.
  - Remote Terminal console web emulator (`terminal.html`).
  - Research archive forum threads and incident logs (`archive.html`).
  - Three-rung hint ladders for P0.0–P0.4 and WebCrypto in-browser stage decryption (`hints.html`, `decrypt.js`).
  - Tool `tools/encrypt_stage.js` using PBKDF2 (SHA-256) and AES-256-GCM.
- Field Journal (Basic):
  - `JournalEntry`, `JournalManager`, and network payloads (`SyncJournalPayload`, `OpenJournalPayload`).
  - Client-side `JournalScreen` CRT UI with entry navigation, metadata tags, and note viewing.
  - Terminal integration: `journal` and `unlock <code>` commands.
  - In-game command: `/palimpsest journal`.
- Documentation:
  - Created `docs/PUZZLES.md` (spoiler-free mechanism descriptions).
  - Created `tools/landmark_check.py` regression check tool.

## M3 - Anomaly Director v1
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

