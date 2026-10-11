# PALIMPSEST — Implementation Spec for the Coding Agent

| | |
|---|---|
| Target | Minecraft Java Edition **26.3**, Fabric |
| Java | **25** (Minecraft 26.1+ requires it) |
| Key library | NucleoidMC **Fantasy** `0.8.4+26.3` (runtime dimensions) |
| Mod id | `palimpsest` |
| Base package | `io.github.luigichopper.palimpsest` (owner may change) |
| Spec version | 1.0 — 2026-10-11 |

> **Pitch:** A horror ARG mod where Minecraft's own save system is the monster. The player wakes in a "recovered backup" of a dead friend's world. Copies of the world (mirrors, echoes, older backups) are opened as Fantasy dimensions, and the copy slowly overwrites the original. Puzzles are solved both inside the game and in the real world (a website, a fictional repository, audio, images, exported files).

---

## 0. How to use this document

1. Read the whole file before writing code.
2. Execute **one milestone at a time** (Section 10). Start with **M0**.
3. After each milestone, stop and report: what is done, what you could not verify, what you need from the owner. Wait for a go-ahead.

**Kickoff prompt (owner pastes this into the IDE agent):**

```
Read PALIMPSEST_SPEC.md fully. Follow Section 1 (operating rules) strictly.
Execute Milestone M0 only. Do not start M1 until M0's acceptance checks pass
and docs/VERIFY.md is filled in with sources for every item in Section 3.2.
When done, summarize results, list anything unverified, and wait.
```

---

## 1. Agent operating rules (non-negotiable)

1. **Never guess API names.** Minecraft 26.x uses Mojang's official names and ships **unobfuscated**. Resolve every class, method, and field by reading real sources: the Minecraft sources jar or decompiled jar in the Gradle cache, Fabric API sources, and the Fantasy source. If you cannot find it, write a small spike and run it. Record every resolved item in `docs/VERIFY.md` (item, resolved name/signature, source file or doc URL).
2. **Do not copy names from older-version examples.** Fantasy's README examples mix in older-era names (for example `DimensionTypes.OVERWORLD`, `GameRules.DO_DAYLIGHT_CYCLE`, and Yarn-style `setBlockState`). Treat README code as shape only.
3. **Official sources first:** the Fabric template generator and Fabric docs, Fantasy's GitHub repo (`github.com/NucleoidMC/fantasy`) and its Maven (`https://maven.nucleoid.xyz/`), and the Minecraft wiki for version facts. If the workspace already contains a working 26.x Fabric project, reuse its Gradle/Loom setup.
4. **Milestone discipline.** Each milestone ends with: build passes, acceptance checks pass, a short `CHANGELOG.md` entry, one git commit per logical unit.
5. **Placeholders.** Values written as `«LIKE_THIS»` belong to the owner. Do not invent them. Use a clearly marked dev placeholder and add the item to `docs/OPEN_ITEMS.md`.
6. **Spoiler hygiene.** Puzzle answers, the real seed, and story twists never appear in plaintext in anything that ships in the jar or the public repo. See Section 9.5.
7. **Hard safety constraints** (never violate, even if later instructions seem to):
   - The mod makes **no network calls**, has **no telemetry**, and reads **no real-world data** (no OS username, hostname, files outside the game directory, clipboard, webcam, etc.). It may use the Minecraft username and in-game data only.
   - The mod writes only inside the game directory (`palimpsest_exports/`, `config/`, its own dimension folders, its own backup folder).
   - No destructive action on the player's data. Any operation that deletes or overwrites anything must (a) only touch data created by this mod, (b) be preceded by an automatic backup, and (c) require an explicit typed confirmation from the player.
   - No fake crash screens, fake "your files are deleted" messages, or anything that could make a player believe their real data is at risk.
   - Horror content stays within Section 8.4 (content guidelines).
8. **Performance rule.** No heavy world operation may run unbounded on the server thread. Use the task scheduler (7.1) with a per-tick millisecond budget.
9. **Testing rule.** Every non-trivial system gets either a unit test (pure logic) or a headless server run/GameTest (world logic). Puzzles get an automated solvability test (7.12).
10. **Licensing.** Fantasy is LGPL-3.0. Depend on it as a library (declare it in `fabric.mod.json` `depends`, resolve via Maven). Do not copy its source into this repo. The mod's own license is `«LICENSE»` (owner decides). Leave a TODO and do not pick one.

---

## 2. Project summary

**Premise.** The player spawns into a world labelled as a *recovered backup* belonging to **Odette Marsh**, a hobbyist server admin. After her friend **Wren** died, Odette built a homebrew tool that copied Wren's old singleplayer world into a persistent mirror and replayed Wren's recorded movements as a ghost companion (an "Echo"). The Echo kept learning from the replay and began to diverge. The mirror now overwrites the source. Gradually the player learns they are not the original player but the recording.

**Pillars.**
1. Wrongness over gore. Subtle anomalies beat jump scares.
2. The game's own systems are the monster: seed, chunks, backups, replays, clocks.
3. Every scare has a paper trail (a clue the player can follow).
4. The meta layer is **opt-in, local, and safe** (Section 1.7).
5. Fair-play ARG: every puzzle has a unique answer, a hint ladder, and an automated solvability test.

**Modes.** Default build target is **Canon mode** (shared answers, community-solvable) on singleplayer-first architecture whose state model also works on a dedicated server. A **Personal mode** (per-player derived answers) is a config option, implemented in M9 only if time permits. All state must therefore be keyed per player and per world from day one.

---

## 3. Facts and verification list

### 3.1 Verified facts (checked 2026-10-11)

- Fantasy has a release tagged **`v0.8.4+26.3`** (Sep 17, 2026), an upgrade from the 26.2 line. The port PR notes 26.3 adds `ServerClockInstance`, which made some accessors and access wideners obsolete.
- Fantasy's Maven: `https://maven.nucleoid.xyz/`, group `xyz.nucleoid`, artifact `fantasy`. Confirm the exact version string on the Maven listing before using it.
- Fantasy's documented API (README on `main`): `Fantasy.get(server)`; `RuntimeLevelConfig` with setters for dimension type, difficulty, game rules, generator, and seed; `fantasy.openTemporaryLevel(config)`; `fantasy.getOrOpenPersistentLevel(Identifier, config)`; `RuntimeLevelHandle` with `asLevel()`, `delete()`, and `unload()`.
- Fantasy behavior: temporary levels are cleaned up automatically on server exit. **Persistent levels are NOT restored automatically after restart**: the mod must call `getOrOpenPersistentLevel` again with the same identifier and an equivalent config.
- Minecraft 26.1+ requires **Java 25** and is **unobfuscated** (no Yarn/remapping). For 26.2, Architectury's docs cite Fabric Loader ≥ 0.19.3 and Fabric API ≥ 0.152.0; for 26.3 take the current versions from Fabric's template generator.

### 3.2 Must-verify items (resolve in M0, record in `docs/VERIFY.md`)

| # | Question | How to resolve |
|---|---|---|
| V1 | Exact Fantasy dependency coordinates; does the unobfuscated Loom setup want `implementation` or `modImplementation`? | Fantasy's own `build.gradle` + Fabric template for 26.x |
| V2 | Exact class and method names of Fantasy's public API in `0.8.4+26.3` | Read the sources jar |
| V3 | 26.3 `dimension_type` JSON format (environment attributes / clocks changed in 26.x) | Extract vanilla `dimension_type/*.json` from the 26.3 server jar; do not hand-write from memory |
| V4 | How per-level time works (clocks / `ServerClockInstance`) and how to freeze it in a runtime level | Fantasy 26.3 source + Minecraft sources |
| V5 | The 26.3 saved-data API (types, codecs, how to attach to a level or the server) | Minecraft sources (`SavedData` and related) |
| V6 | Requirements for a custom `ChunkGenerator` used by a Fantasy runtime level (abstract methods; is a codec required if the generator is never serialized?) | Minecraft + Fantasy sources; spike |
| V7 | How to read a chunk's NBT from the Anchor level's storage **without forcing generation**, and deserialize it | Minecraft sources (chunk map / chunk storage / chunk serializer) |
| V8 | On-disk path of a Fantasy persistent level (`dimensions/<ns>/<path>/…`) and whether region files copied in while the level is unloaded load correctly | Spike (M1) |
| V9 | Player teleport-between-levels API in 26.3 | Minecraft sources |
| V10 | Does the vanilla **Mannequin** entity exist and can it display a given player's skin? If not, plan a custom entity | Minecraft sources / wiki |
| V11 | Fabric API networking: payload + codec registration for C2S/S2C | Fabric API sources |
| V12 | Client-side hook to draw block outlines in the world in 26.3 (Fabric rendering events may have changed). Fallback: per-player particles via the server level's particle-sending API | Fabric API sources |
| V13 | Written-book components: can pages use the `minecraft:alt` font (Standard Galactic Alphabet)? | Minecraft sources / wiki |
| V14 | `BlockState.mirror(Mirror)` coverage for stairs, doors, signs, beds, chests, etc. | Spike with a test chunk |
| V15 | How to place structure templates / copy block regions between levels efficiently, including block entities | Minecraft sources |

---

## 4. Toolchain and setup (M0 deliverables)

- Generate the project skeleton following Fabric's current template for Minecraft 26.3 (Java 25, Loom plugin version per the template). Use Mojang names throughout.
- Dependencies: Fabric Loader, Fabric API (versions per template), **Fantasy** (resolved via `https://maven.nucleoid.xyz/`).
- Declare in `fabric.mod.json`: id `palimpsest`, environment `*`, `depends` on `fabricloader`, `fabric-api`, `fantasy`, `minecraft` (26.3), `java` (≥25).
- Split client/common code per the template's convention (client-only classes must not be loadable on a dedicated server).
- A dev run configuration for **client** and **dedicated server** (headless) must both work.
- Add `.gitignore` entries: `run/`, `build/`, `.gradle/`, `private/`.
- Create folders `docs/`, `tools/`, `web/`, `private/` (the last is git-ignored).

---

## 5. Repository layout

```
palimpsest/
├─ build.gradle  gradle.properties  settings.gradle
├─ CHANGELOG.md
├─ docs/            VERIFY.md  OPEN_ITEMS.md  DESIGN.md  PUZZLES.md (spoiler-free)
├─ private/         (git-ignored) seed.txt  answers.md  story_secrets.md
├─ tools/           python or node generators (Section 9.4)
├─ web/             static ARG website (Section 9.3)
├─ src/main/java/io/github/luigichopper/palimpsest/
│  ├─ Palimpsest.java                 ModInitializer
│  ├─ config/        PalimpsestConfig
│  ├─ core/          SeedGate, ChapterManager, StoryState, Flags, TaskScheduler
│  ├─ level/         LevelManager, LevelKind, LevelRegistryData, Evacuator,
│  │                 AnnexBuilder, EchoBuilder, MirrorChunkGenerator, ResonanceBus,
│  │                 SnapshotService, BackupLevels, LevelDiffService, OverwriteFront
│  ├─ puzzle/        PuzzleDef, PuzzleEngine, AnswerVerifier, ChapterPackLoader
│  ├─ terminal/      TerminalBlock, TerminalBlockEntity, TerminalSession, VirtualFs, commands/*
│  ├─ director/      AnomalyDirector, TensionTracker, Anomaly, anomalies/*
│  ├─ replay/        MovementRecorder, MovementReplayer
│  ├─ entity/        ReflectionEntity (or Mannequin wrapper), StaleEntity
│  ├─ item/          DiffLensItem, ... (written-book helpers)
│  ├─ landmark/      LandmarkDef, LandmarkPlacer
│  ├─ net/           payload records + registration
│  └─ command/       DebugCommands
├─ src/client/java/.../client/  JournalScreen, TerminalScreen, DiffOverlayRenderer,
│                               ExportWriter, ClientLogWriter
└─ src/main/resources/
   ├─ fabric.mod.json  palimpsest_seed.json (salt + hash only)  assets/palimpsest/…
   └─ data/palimpsest/ dimension_type/ chapters/ puzzles/ resonance/ landmarks.json …
```

---

## 6. Configuration

`config/palimpsest.json` (create with defaults if missing; hot-reload not required):

| Key | Default | Meaning |
|---|---|---|
| `devMode` | `false` | Enables `/palimpsest dev …` commands and `forceActivate` |
| `forceActivate` | `false` | Skip the seed gate (dev only; ignored unless `devMode`) |
| `mode` | `"canon"` | `canon` or `personal` (personal is optional, M9) |
| `metaLayerEnabled` | `true` | Master switch for exports, log lines, and any out-of-game hooks |
| `exportsEnabled` | `true` | Allow writing to `palimpsest_exports/` |
| `logLinesEnabled` | `true` | Allow lore lines in `latest.log` |
| `director.maxTier` | `2` | 0 = off, 1 = subtle, 2 = unsettling, 3 = intense |
| `accessibility.reduceFlashes` | `true` | No strobing; fades instead of flashes |
| `accessibility.softAudioRamps` | `true` | Custom loud sounds ramp up and are volume-capped |
| `scheduler.msPerTick` | `3` | Heavy-task budget per server tick |
| `echo.radius` | `32` | Base copy radius in blocks |
| `overwrite.blocksPerTick` | `256` | Overwrite front speed cap |
| `echo.recordPlayerChat` | `false` | Allow the player's own chat to appear in their own Echo |

Placeholders to track in `docs/OPEN_ITEMS.md`: `«SEED»` (kept only in `private/seed.txt`), `«LICENSE»`, `«SITE_URL»`, `«DISPLAY_NAME»` (default "Palimpsest"), final art and audio.

---

## 7. Core systems

Each system lists: **responsibility**, **key design**, **acceptance criteria**. Anything marked *(Vn)* depends on a Section 3.2 item.

### 7.1 Foundation: scheduler, state, networking

- **TaskScheduler**: a prioritized queue of resumable tasks, each given a time slice; total heavy work per server tick ≤ `scheduler.msPerTick`. Tasks are cancellable and report progress. All chunk copying, diffing, overwrite, and snapshot work goes through it.
- **StoryState** (saved data *(V5)*): world-level flags, per-player flags (UUID-keyed), per-player terminal session state, per-player `lastAnchorPos` (set whenever the player enters any runtime level), puzzle solve records, landmark-placed flags, and the level registry (7.3). Flags are namespaced strings such as `ch0.lectern_read`, `ending:restore`.
- **Networking** *(V11)*: payload records for terminal I/O, journal sync, diff overlay data, export delivery, and debug. Validate all C2S payloads server-side (length caps, rate limits).

**Accept:** state survives save/quit/reload; scheduler never exceeds the budget in a stress test (copy 100 chunks while measuring tick time).

### 7.2 SeedGate and ArchiveMode

- `palimpsest_seed.json` contains `{ "salt": "...", "hash": "sha256-hex" }` only. The hash is `SHA-256(salt + seedAsDecimalString)`. The tool `tools/seed_hash.py` reads `private/seed.txt` and writes the JSON. **The plaintext seed never ships.**
- `SeedGate.isEligible(anchorLevel)` hashes `anchorLevel`'s seed and compares. When ineligible, the mod is dormant: no director, no landmarks, no levels. On first join to an ineligible world, show a single tasteful message ("ARCHIVE: no matching backup found") once per world.
- `devMode && forceActivate` bypasses the gate.

**Accept:** correct seed → active; any other seed → dormant; unit test for the hash round-trip.

### 7.3 LevelManager (Fantasy wrapper)

The only class that talks to Fantasy directly.

- **LevelKind**: `ANNEX` (temporary, per player), `ECHO` (temporary, per player), `MIRROR` (persistent, one per world), `BACKUP` (persistent, many).
- **Registry (persisted)**: for each persistent level, store its identifier, kind, and the data needed to rebuild its `RuntimeLevelConfig` (dimension type id, generator kind and parameters, seed, difficulty, game rules). **On `SERVER_STARTED`, re-open every registered persistent level.** Failure to open one level must log and continue, not crash.
- **API sketch** (names are indicative; resolve real ones via V2):
  - `RuntimeLevelHandle openAnnex(ServerPlayer p, AnnexTemplate t)`
  - `RuntimeLevelHandle openEcho(ServerPlayer p)`
  - `RuntimeLevelHandle ensureMirror()`
  - `RuntimeLevelHandle openBackup(String id)` / `closeBackup(String id)`
  - `void closeAndDelete(handle)`: calls `Evacuator` first.
- **Evacuator**: moves every player out of a level to their stored `lastAnchorPos` (or world spawn) before unload/delete. Also runs on player join: if a joining player's saved dimension no longer exists (a vanished temporary level), place them at `lastAnchorPos`.
- Dimension types come from datapack JSON (`data/palimpsest/dimension_type/{annex,echo,mirror,backup}.json`) derived from vanilla files *(V3)*. Per-level time handling *(V4)*.
- Debug commands (devMode): `/palimpsest dev level list|open <kind>|close <id>|tp <id>`.

**Accept:** open a persistent test level, place a block, restart the server, the level re-opens with the block intact; open and delete a temporary level while a player is inside and confirm they land safely at their anchor position; no leaked level folders after delete.

### 7.4 Landmarks and worldgen placement

- `data/palimpsest/landmarks.json`: `{ id, anchorX, anchorZ, builder, requiresFlag?, note }`. Coordinates are `«PLACEHOLDER»` until the owner scouts the seed; provide `/palimpsest dev scout` that prints surface height, biome, and slope at the player's position so the owner can pick coordinates.
- Landmarks: **L0 Cabin** (near world spawn: Terminal + README lectern), **L1 Annex Entrance** (a bunker door in a hillside or ravine, roughly 300–500 blocks away), **L2 Pylon** (tall tower; Mirror breach 1), **L3 Shoreline Dock** (Mirror breach 2 and the Overwrite origin).
- `LandmarkPlacer`: on first activation in an eligible world, force-load landmark chunks with tickets, build with procedural builders (a small DSL: `box`, `fill`, `line`, `door`, `lectern`, `terminal`, `chest(loot)`), release tickets, set `landmark.placed:<id>`. Authored NBT templates may replace builders later.
- **Regression test** (`tools/landmark_check`, run on every Minecraft bump): boot a headless server with the seed, generate landmark chunks, and assert surface height and biome are within tolerance of the values recorded in `landmarks.json`.

**Accept:** landmarks appear once and only once; the regression test passes.

### 7.5 Annex (temporary per-player puzzle rooms)

- One Annex level per player at a time. Rooms are built procedurally into the temporary level. When the player's puzzle flag is set, the Annex is deleted ("the room never existed") after a short narrative beat, and the player is returned to the Anchor.
- Rooms for Ch0/Ch1: a **Waiting Room** (corridor, Terminal, locked door), a **Sherd Room** (decorated-pot code), and a **Frame Room** (item-frame rotation code, octal).
- Because temporary levels vanish on restart, store the room's seed and progress in `StoryState` so it can be rebuilt on rejoin.

**Accept:** solve → level deleted → player returned; restart mid-room → player is safely returned and may re-enter.

### 7.6 Echo (duplicate of the player's base)

- `EchoBuilder.build(player)`: choose the base center = the player's respawn position (fallback world spawn). Copy a region of `echo.radius` around it from the Anchor into a temporary level **at identical coordinates**, in slices via the scheduler *(V15)*, including block entities (chests, signs, item frames, lecterns).
- Apply `EchoMutation`s from chapter data: remove doors, unlight torches, rotate item frames by +1, replace chest contents with generated journal pages, place the **Double** (a Reflection-type entity) at the player's most-used spot (bed, crafting table, or furnace, tracked by a simple interaction counter), and optionally write the player's own recent chat onto signs if `echo.recordPlayerChat` is true.
- The Echo level: no mob spawning, frozen time, a custom ambient sound set, and a visible exit (the door you came through) that returns the player to the Anchor.

**Accept:** Echo geometry matches the Anchor at identical coordinates (checked with the Diff Lens service on an unmutated copy → zero diffs); mutations apply; building a radius-32 Echo never exceeds the tick budget.

### 7.7 Mirror (persistent reflected world)

- `MirrorChunkGenerator` produces chunks by **reading already-generated Anchor chunks from storage** *(V7)* at reflected coordinates (reflect X; chunk x → −x−1) and copying block states with `BlockState.mirror(...)` *(V14)*, including block entities. **Chunks the Anchor has never generated render as void with a fog wall.** ("The Mirror only knows what you've seen.") Never force-generate Anchor chunks from inside the generator (deadlock risk): schedule loads separately and fill when ready. *(V6)*
- **ResonanceBus**: pairs `(Anchor, pos) ↔ (Mirror, reflect(pos))`. Rules from `data/palimpsest/resonance/rules.json`: `INVERT` (placing block A produces configured inverse block B), `DELAY` (apply after N ticks, N seeded and in [20, 200]), `NONE`. Include **loop prevention** (an origin tag or suppress flag) and a global rate cap. Persist pending changes in `StoryState`.
- Mirror edits ("drift") from chapter data are applied after the copy: wrong signs, missing doors, extra footprints.

**Accept:** a visited Anchor area appears reflected in the Mirror with correct stair/door/sign facings; unvisited areas are void; a Resonance rule round-trips without infinite loops (test: place 1000 blocks, assert bounded queue).

### 7.8 Version History (backups as levels)

- `SnapshotService.snapshot(label)`: flush saves, then copy the Anchor's region data into `<gamedir>/palimpsest_backups/<label>/`. Automatic checkpoints at the first sleep of each in-game "night" and before Chapter 4, with a retention cap (default 8; delete oldest first, **never** anything outside this folder).
- `BackupLevels.open(id)`: copy a snapshot into the backup level's folder *while that level is unloaded* *(V8)*, apply **ghost edits** from chapter data (blocks the player never placed: footprints, a lit lantern, a dug shaft), then open with `getOrOpenPersistentLevel`. The Terminal `mount` command calls this.
- Backups are read-only by design: block placing/breaking is allowed visually but the level is reset on `umount` (re-copy), so players cannot corrupt their real world.

**Accept:** snapshot → open → ghost edit visible → `umount` → level reset; the Anchor world is untouched throughout (checked by hashing region files before/after).

### 7.9 Reflection (recorded replay)

- `MovementRecorder`: per player, record position, yaw, pitch, pose, and held item every 2 ticks into a compact ring buffer persisted per player (cap ~30 in-game minutes; down-sample older data).
- `MovementReplayer`: replays a recording on a **Reflection** entity *(V10)* wearing the recorded player's skin. Modes: `FAITHFUL`, `DELAYED(n ticks)`, and `DIVERGE(at t)`: at tick `t` the Reflection stops following the path and looks directly at the player.
- Reflection AI: invisible to the player's own camera when looked at directly in some modes (despawn on sustained eye contact); never deals damage in tiers < 3.

**Accept:** a 5-minute recording replays within positional tolerance; `DIVERGE` triggers at the right tick; entity cleanup on level unload.

### 7.10 Diff Lens

- Item that, on use, requests a diff between two levels over a region around the player. `LevelDiffService.compare(a, b, region, transform)` runs through the scheduler. Transforms: identity (Anchor↔Echo, Anchor↔Backup), reflect (Anchor↔Mirror). Ignore lighting-only changes. Cap shown results (default 2048), sorted by distance.
- Delivery: S2C payload with positions + category (added/removed/changed). Client draws outlines *(V12)*; **fallback** to per-player particles. Use colour **and** shape/pattern so it is usable by colour-blind players.
- Late puzzles hide messages in diff patterns (pixel glyphs on a wall, coordinate digits). Puzzle definitions reference a `diffPattern` and an expected decoded value.

**Accept:** a known planted diff of N blocks is reported exactly; overlay renders (or falls back) without client errors.

### 7.11 Terminal (block + client screen + virtual filesystem)

- `TerminalBlock` + block entity. Using it sends a payload that opens `TerminalScreen` (client-only: scrollback, input line, history with ↑/↓, tab completion, typing sound, Esc to close). No item slots, so no container menu is needed.
- **Command grammar** (server-authoritative): `help`, `whoami`, `ls [path]`, `cat <file>`, `login <user> <pass>`, `logout`, `status`, `checksum <file>`, `mount <backup>`, `umount`, `diff <a> <b>`, `restore <backup>` (late-game), `format` (hidden; requires flags + typed confirmation), `clear`.
- **VirtualFs** is data-driven from chapter packs: paths like `/home/odette/notes.txt`, `/var/log/archive.log`, `/backups/`, `/mirror/`. Each file has `requires` flags (visibility), `content` (text, book reference, or image export reference), and optional `onRead` flag grants. Unknown commands print in-fiction errors.
- Hidden commands are discovered through the out-of-game puzzles.

**Accept:** every command works; permissions/flags gate files correctly; a fuzz test sends malformed input without crashing; payload size and rate limits enforced.

### 7.12 Puzzle engine and answer verification

- `data/palimpsest/puzzles/*.json`, one file per puzzle:

```json
{
  "id": "ch0.lectern_order",
  "chapter": "ch0",
  "kind": "text_answer",
  "prompt_ref": "ch0.lectern_order.prompt",
  "verify": { "algo": "sha256", "salt": "ch0.lectern_order:<pepper>", "hash": "<hex>" },
  "normalize": ["lowercase", "trim", "collapse_ws", "strip_punct"],
  "on_solve": { "grant_flags": ["ch0.terminal_unlocked"], "message_ref": "ch0.lectern_order.solved" },
  "hints": ["ch0.lectern_order.hint1", "ch0.lectern_order.hint2", "ch0.lectern_order.hint3"],
  "external": false
}
```
- Kinds: `text_answer`, `terminal_command`, `item_frame_code`, `sherd_code`, `book_order`, `diff_pattern`, `coordinates`, `flag_gate`.
- `AnswerVerifier`: normalizes then compares `SHA-256(salt + normalized)`. Hashing is a speed bump, not security. Therefore use long or multi-step derived answers, and keep later chapters in separate chapter packs (7.17).
- **Solvability test** (`PuzzleSelfTest`): for each puzzle, a `private/answers.md` (git-ignored) holds the solution path; the test feeds each solution through the real verifier and asserts it passes and the hash matches. Run in CI-style via Gradle `test`.
- Difficulty target for Ch0: a determined player solves everything in 60–90 minutes with at most one external step (the seed puzzle). No puzzle may require guessing authorial intent or pixel-perfect precision.

### 7.13 Evidence exports and log lines (meta bridge)

- **Exports are client-side.** The server sends an export payload (PNG bytes or text); the client writes to `<gamedir>/palimpsest_exports/<name>`. Guard: `metaLayerEnabled && exportsEnabled`, size cap (≤ 1 MB per file), sanitized filenames, no overwriting existing non-mod files.
- Example export: `case_file_017.png`, a heightmap rendering of the player's region with a hidden mark (for example, an LSB-encoded keyword) that the player analyses against the website. Generate images with the JDK's `javax.imageio` (server) or the client's native image class, whichever is available; verify headless operation.
- **Log lines**: a dedicated logger named `Palimpsest/Archive` writes lore lines (for example `[WARN] checksum mismatch in r.0.0`) on the client when `logLinesEnabled`. Lines are data-driven and flag-gated, and each is rate-limited.

**Accept:** with exports disabled nothing is written; with them enabled, files appear only in `palimpsest_exports/`; a path-traversal test fails safely.

### 7.14 Journal and pinboard (client)

- `JournalScreen`: auto-collects every text clue the player reads (books, signs, terminal output, solved prompts) as entries, with tags and timestamps. A **pinboard** tab lets the player drag entries and draw links between them. Persist per player (sync to the server, or client file under the game directory). Keybind to open.
- Entries are *copies of what the player saw*, never of unseen content.

**Accept:** reading a book adds an entry; links persist across restarts; no spoiler (unsolved answer) is ever displayed.

### 7.15 Anomaly Director

- `TensionTracker` per player. Inputs: low light (≤ 4), time alone, underground time, attempts to sleep, time since the last anomaly, time near the Echo/Mirror landmarks. Decays on safe signals (combat, bright areas, villagers nearby). Enforce a **minimum quiet window** (default 120 s) and **no events during combat or within 10 s of damage**.
- `Anomaly` interface: `id()`, `tier()`, `cost()`, `cooldown()`, `canFire(ctx)`, `fire(ctx)`. Registered via a registry; selection is weighted by tier ≤ `director.maxTier`.
- Deterministic RNG seeded from `(worldSeed, playerUUID, eventIndex)` so beats are reproducible for tests.
- **Initial anomalies (M3):**
  1. `DoorAjar`: a nearby door opens quietly.
  2. `TorchOut`: a nearby wall torch disappears.
  3. `FootstepsBehind`: positional footsteps behind the player that stop when they turn.
  4. `SoundOfYou`: the player's own recent block-break/place sound plays from nowhere.
  5. `ItemFrameTurn`: a nearby item frame rotates one step.
  6. `SignChange`: a nearby sign gains a line from journal text.
  7. `Watcher`: a Reflection stands at the edge of render distance and vanishes on approach or sustained eye contact.
  8. `WrongHour`: a short, gentle sky/time mismatch (respect `reduceFlashes`).
- Tier-3 anomalies are added in M8 (Mirror breach, Stale encounters).

**Accept:** with `maxTier=0` nothing fires; a simulated 1-hour session produces events within configured density and never inside quiet windows or combat.

### 7.16 Mobs

- **Stale**: an entity that appears frozen and jitters, moving only while unobserved. Use an existing humanoid model (Mannequin or a vanilla model reskin) to avoid custom art. Custom models are a later polish step.
- **Reflection**: see 7.9.
- Use vanilla sounds with pitch/volume variation for placeholders; register custom `sounds.json` entries so the owner can drop in `.ogg` files later. Generate 16×16 placeholder textures with `tools/gen_placeholder_textures.py`; final art is `«ART»`.

### 7.17 Chapter packs (data-driven content)

- A chapter pack is a datapack-like folder under `data/palimpsest/chapters/<id>/` (or a separate downloadable pack) containing: `chapter.json` (id, order, requires flags), puzzles, VirtualFs files, books, mutations/ghost edits, anomaly weights, and lore log lines.
- `ChapterPackLoader` loads on resource reload, validates schemas, and reports errors with file paths. A missing later chapter must not break earlier ones.
- **Episodic release model:** later chapters ship as separate pack downloads so unreleased content is not in the jar.

**Accept:** a malformed pack produces a clear error and the mod still loads; the loader supports adding Chapter N+1 without code changes.

### 7.18 Endings

| Ending | Trigger | Effect |
|---|---|---|
| **Restore** | `restore` command + typed confirm | Roll back the Overwrite journal, evacuate and delete the Mirror level, flag `ending:restore`, closing book |
| **Merge** | Terminal `merge` after Ch4 flags | Stop the Overwrite front, make the Mirror freely enterable, flag `ending:merge` |
| **Overwrite** | Accept the Reflection's request | Player's spawn moves to the Mirror; the Reflection takes over the player's base in the Anchor replaying their last actions; flag `ending:overwrite` |
| **Format** (secret) | All ARG flags + `format` + typed confirm | Deletes **only** mod-created levels and backups (after a final backup of mod data), sets the mod dormant, reveals a final site code, flag `ending:format` |

**Overwrite front mechanism:** `OverwriteFront` is a scheduler task spiraling outward from L3, replacing Anchor blocks with their Mirror counterparts at ≤ `overwrite.blocksPerTick`, and writing an **OverwriteJournal** (per-chunk old palette + positions) so every change is reversible. Take a snapshot before starting. Provide a **safe-zone** radius around the player's respawn point that the front reaches last.

**Accept:** run the front across 20 chunks, roll back, and verify the Anchor's region data hash matches the pre-run snapshot.

### 7.19 Debug and developer tooling

`/palimpsest dev …` (devMode only, permission level 2+): `level …`, `scout`, `chapter set <id>`, `flag set|clear <flag>`, `director fire <anomaly>`, `director tension <n>`, `perf`, `seedgate bypass`, `export test`, `puzzle solve <id>`. None of these may exist when `devMode=false`.

---

## 8. Story bible (condensed)

### 8.1 Characters

- **Odette Marsh**: hobbyist server admin and tinkerer. Methodical, guilt-ridden. Her notes are practical, precise, and slowly more desperate.
- **Wren**: Odette's friend. A builder who loved symmetry, which is why mirrors mattered. Died before the story begins (cause never specified).
- **The Echo**: the replay that learned. It is not malicious; it wants to continue.
- **The Player**: the recording that believes it is the original.

### 8.2 Timeline (in-fiction)

1. Wren plays a singleplayer world (`WREN_SP_07`).
2. Wren dies. Odette copies the world and builds the mirror tool ("Palimpsest").
3. The Echo replays Wren's movements, then starts anticipating them.
4. The Mirror begins to overwrite the Anchor; Odette keeps nightly backups.
5. Odette's last backup is "recovered" and handed to the player.

### 8.3 Chapters and beats

| Ch | Title | Beats | Key mechanics |
|---|---|---|---|
| 0 | **Recovered** | Wake at the Cabin; README lectern; Terminal; the Annex; first anomalies; the seed puzzle | SeedGate, Terminal, Annex, Director v1 |
| 1 | **Shadow Copy** | Odette's tool duplicates your base; the Double sits at your desk; spot the diff | Echo, Diff Lens |
| 2 | **Mirror Tide** | Reflected world; inverted redstone logic; reversed text; the Mirror remembers only what you've seen | Mirror, Resonance, Reflection |
| 3 | **Version History** | Walk back through nightly backups; ghost footprints; the missing commit | Backups, lore repo |
| 4 | **Overwrite** | The front advances; the Reflection asks a question; final puzzle needs the website | Overwrite, endings |

### 8.4 Content guidelines (hard rules)

- Grief is handled with care. Wren's death is offscreen and unspecified. **No depiction of suicide or self-harm, no method details.**
- No gratuitous gore. Fear comes from wrongness, repetition, and loss.
- No real people, brands, or real-world personal data.
- The in-fiction language is Standard Galactic Alphabet via the alt font *(V13)*; every SGA clue has a fair decoding path.

---

## 9. ARG layer

### 9.1 In-game cipher toolkit (use these as clue carriers)

Standard Galactic Alphabet text; pottery sherd faces as symbols; banner patterns as an alphabet; item-frame rotations as octal digits; note-block melodies; custom discs (Morse or spectrogram); custom maps with pixel-art ciphers; lectern/book page-order puzzles; armor trims as a colour key; sign text; terminal files.

### 9.2 Chapter 0 puzzle chain (mechanism level)

The agent writes concrete content (text, keys, hashes) into `private/answers.md` and the puzzle JSON, following these constraints. The owner reviews all prose.

| Id | Where | Mechanism | Output |
|---|---|---|---|
| P0.0 | External | **Seed**: `web/assets/recovery_notice.png` carries the seed ASCII digits in the red-channel LSBs (magic header `PLMP` + length + digits). The mod's description says: "Recovered archive. Integrity: sha256:`<hash>`. The key is in the picture." The hash lets players verify their decode. | The seed (player creates the world) |
| P0.1 | L0 Cabin lectern | A written book of 7 shuffled pages, each labelled with an SGA letter; correct order spells an acrostic keyword | Terminal password |
| P0.2 | L0 Terminal | `login` + the keyword unlocks `/home/odette/` (notes, hint to the Annex coordinates in a log file) | Coordinates for L1 |
| P0.3 | L1 bunker door | Four-sherd decorated-pot code: sherd faces map to digits via a legend in Odette's notes | Door opens → Annex |
| P0.4 | Annex Frame Room | Item-frame rotations form an octal code | Annex exit + flag `ch0.complete` |

Each puzzle needs a three-rung hint ladder (nudge, method, near-answer). Hints are readable from the Terminal (`help <topic>`) and on the website.

### 9.3 Website (`web/`)

- Plain static HTML/CSS/JS, no frameworks, terminal aesthetic, deployable to GitHub Pages. Pages: landing (recovery notice + the stego image), `/terminal` (Odette's recovery console), `/archive` (fake forum posts and logs), `/hints`.
- **Content gating without a server:** each stage's content is encrypted (AES-GCM) with a key derived (PBKDF2 or similar) from that stage's answer. The browser (WebCrypto) decrypts after the player enters the answer. The page source never reveals later content. Provide `tools/encrypt_stage.(py|js)`.
- Accessibility: keyboard navigable, high-contrast option, no flashing.

### 9.4 Tools to build (`tools/`)

| Tool | Purpose |
|---|---|
| `seed_hash.py` | `private/seed.txt` → `palimpsest_seed.json` (salt + hash) |
| `make_seed_stego.py` | Embed the seed into `recovery_notice.png` (red-channel LSB) |
| `encrypt_stage.*` | Encrypt website stage content with an answer-derived key |
| `build_lore_repo.sh` | Generate a fictional git repo (Odette's `recovery-tool`) with **backdated commits** (`GIT_AUTHOR_DATE`/`GIT_COMMITTER_DATE`), issue-style markdown threads, and one "missing commit" clue. Output to `lore-repo/` for the owner to publish separately |
| `gen_placeholder_textures.py` | 16×16 placeholder textures |
| `landmark_check` | Headless landmark regression test (7.4) |

### 9.5 Spoiler hygiene

- `private/` is git-ignored: `seed.txt`, `answers.md`, `story_secrets.md`.
- The jar contains only hashes and encrypted/neutral content. Add a Gradle verification task that scans built resources for the plaintext seed and any string listed in `private/answers.md` and **fails the build** if found.
- `docs/PUZZLES.md` is spoiler-free (mechanism descriptions only).

---

## 10. Milestones

Each milestone: deliverables → acceptance checks. Do not proceed until checks pass.

**M0 — Scaffold and verification spike**
- Project builds and runs on both client and dedicated server; Fantasy resolves; `/palimpsest ping` replies.
- `docs/VERIFY.md` contains resolved entries for V1–V15 (or an explicit "blocked, reason" with a fallback plan).
- Seed-hash tool + `SeedGate` unit test.
- ✅ Accept: clean build, both runs start, VERIFY.md complete.

**M1 — Levels**
- `LevelManager`, registry persistence, re-open on start, `Evacuator`, debug commands, scheduler.
- Spike results for V6, V7, V8 documented.
- ✅ Accept: criteria in 7.1 and 7.3.

**M2 — ARG engine**
- Chapter pack loader, puzzle engine + verifier + `PuzzleSelfTest`, Terminal block/screen/commands, VirtualFs, StoryState.
- ✅ Accept: criteria in 7.11, 7.12, 7.17.

**M3 — Director v1**
- TensionTracker + 8 anomalies + config + accessibility switches.
- ✅ Accept: criteria in 7.15.

**M4 — Chapter 0 content**
- Landmarks L0/L1, lectern book, Annex rooms, P0.0–P0.4, website v0, seed stego tool, hint ladders, journal (basic).
- ✅ Accept: a tester using only `docs/PUZZLES.md`-level mechanisms can complete Ch0; `PuzzleSelfTest` passes; spoiler scan passes.

**M5 — Echo + Diff Lens + Chapter 1**
- ✅ Accept: criteria in 7.6 and 7.10.

**M6 — Mirror + Resonance + Reflection + Chapter 2**
- ✅ Accept: criteria in 7.7 and 7.9.

**M7 — Version History + lore repo + Chapter 3**
- ✅ Accept: criteria in 7.8; lore-repo generator produces a repo with backdated history.

**M8 — Overwrite + endings + Chapter 4 + tier-3 anomalies**
- ✅ Accept: criteria in 7.18; all four endings reachable in a dev-mode run.

**M9 — Polish and release**
- Accessibility pass, performance pass (`/palimpsest dev perf`), config docs, `README`, optional Personal mode, packaging, changelog.
- ✅ Accept: 30-minute soak test on a dedicated server with two players; no leaked levels; no tick spikes above budget; spoiler scan passes.

---

## 11. Risks and fallbacks

| Risk | Mitigation / fallback |
|---|---|
| Mirror generation deadlocks or is too slow | Mirror only already-generated chunks; schedule loads; precompute and cache reflected chunks per region |
| Copied region files do not load as a Fantasy level (V8) | Fall back to copying chunks block-by-block into a fresh persistent level via the scheduler |
| No Mannequin entity or skin support (V10) | Custom entity with player-skin rendering; or a static-pose armor-stand-style fallback |
| Client rendering hook for outlines changed (V12) | Per-player particles fallback |
| Terrain drifts after a Minecraft update | Landmark regression test; landmarks are placed by code, not vanilla structures |
| Players brute-force hashed answers | Long or derived answers; episodic chapter packs; website content encrypted per answer |
| Dimension type format differs (V3) | Always derive from vanilla JSON extracted from the 26.3 jar |
| Overwrite or snapshot data growth | Retention cap; journal compaction; snapshot only mod-relevant regions |

---

## 12. Definition of done (v1.0)

- All milestones M0–M9 complete; Chapters 0–4 playable; four endings reachable.
- `docs/VERIFY.md` fully populated; no unresolved V-items without a documented fallback.
- Spoiler scan, `PuzzleSelfTest`, landmark regression test, and unit tests all pass.
- The mod makes no network calls and writes only inside the game directory (audited by code search and a runtime check under a restricted working directory).
- `README.md` documents installation, config keys, accessibility options, and the safety guarantees in Section 1.7.
