# Changelog

All notable changes to the Palimpsest project will be documented in this file.

## [Unreleased] - M2
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

