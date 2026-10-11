# Changelog

All notable changes to the Palimpsest project will be documented in this file.

## [Unreleased] - M1
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
