# Palimpsest // Puzzle Mechanism Reference (Spoiler-Free)

This document provides mechanism-level descriptions for all Chapter 0 puzzles without revealing answers or hashes.

---

### P0.0 — Carrier Steganography (World Seed)
- **Carrier Location**: `web/assets/recovery_notice.png` (also downloadable from web portal).
- **Mechanism**: The world seed is encoded into the least significant bit (LSB) of the red color channel.
- **Protocol**:
  - Header: 4-byte ASCII magic identifier `PLMP`.
  - Length: 2-byte unsigned big-endian integer denoting the payload byte count.
  - Payload: ASCII decimal string representing the 64-bit signed world seed.
- **Verification**: The web portal notice displays `sha256(seed)[:16]` allowing players to verify their extraction before world generation.

---

### P0.1 — Cabin Lectern (Acrostic Reordering)
- **Location**: Landmark L0 Cabin (near world spawn).
- **Carrier**: Written book titled `README.txt` resting on the cabin's lectern.
- **Mechanism**:
  - The book contains 7 shuffled pages.
  - Each page is marked with a Roman numeral ([I] through [VII]) and an initial bracketed letter.
  - Ordering the pages by Roman numeral yields an English acrostic keyword.
- **Output**: Terminal master password for user `odette`.

---

### P0.2 — Terminal Node Authentication
- **Location**: Landmark L0 Cabin terminal console (`/dev/terminal0`).
- **Mechanism**: Standard UNIX-style command line shell.
- **Command**: `login odette <password>` where `<password>` is the word derived from P0.1.
- **Output**: Elevates session privileges to user `odette`, unlocking `/home/odette/notes.txt` and `/var/log/recovery.log` which detail coordinates for Landmark L1.

---

### P0.3 — Bunker Reinforced Gate (Ceramic Sherd Cipher)
- **Location**: Landmark L1 Annex Bunker entrance (`[340, -180]`).
- **Carrier**: Four decorated ceramic pots mounted on exterior stone plinths outside the iron blast door, viewed West to East.
- **Mechanism**:
  - Odette's notes in `/home/odette/sherd_legend.txt` provide a 1-to-1 matrix mapping ceramic pottery sherd iconography to numerical values.
  - Players inspect the four pot faces and map them to a 4-digit numerical combination code.
- **Input / Output**: Enter the 4-digit code into the terminal via `unlock <code>`. Valid submission opens the reinforced iron blast door to reveal the subterranean Annex entrance.

---

### P0.4 — Annex Frame Room (Octal Positional Calibration)
- **Location**: Temporary Annex dimension (entered through the bunker threshold).
- **Carrier**: Three item frames holding cardinal instruments (Compass, Clock, Echo Shard) on the back wall.
- **Mechanism**:
  - In Minecraft, item frames possess 8 discrete rotational orientations (indices 0 through 7).
  - The 3 rotational positions represent a 3-digit octal (base-8) number.
  - Converting the octal number into decimal yields the calibration frequency code.
- **Input / Output**: Enter the frequency via terminal command `unlock <code>` (or step onto the calibrated threshold). Solving this puzzle grants the world flag `ch0.complete`, triggers the narrative partition collapse sequence, safely returns the player to the Anchor world, and cleans up the temporary Annex dimension.

---

### P1.0 — Diff Lens Calibration (Spot Difference Pattern)
- **Location**: Echo Dimension (opened upon entering the duplicated player base).
- **Carrier**: Block geometry discrepancies revealed through the Diff Lens tool.
- **Mechanism**:
  - Scanning the Echo dimension against the Anchor world highlights subtle physical mutations (e.g. detached doors, extinguished torches, offset item frames).
  - Isolating the changed blocks in the base's primary observation area forms a visual character glyph / semantic keyword.
- **Input / Output**: Submit the observed word into the terminal console via `unlock <keyword>`. Grants flags `ch1.spot_diff_solved` and `ch1.lens_calibrated`.

---

### P1.1 — Echo Station Divergence (Terminal Node Re-synchronization)
- **Location**: Landmark L0 / Echo Station Console.
- **Carrier**: Odette's `/home/odette/echo_notes.txt` and the Double entity located at the player's most-frequented station.
- **Mechanism**:
  - The Double stands at the coordinate where the player interacted most frequently with functional stations (bed, crafting table, or furnace).
  - The log files note that the copy begins to learn and branch away from the original recording.
  - Correlating the manifest note with the observed station anomaly provides the divergence synchronization keyword.
- **Input / Output**: Enter the terminal command `unlock <keyword>` to complete synchronization. Grants world flag `ch1.complete`, confirming Echo divergence and preparing the runtime level manager for the Mirror phase.

---

### P2.0 — Resonance Inversion (Chamber Material Duality)
- **Location**: Mirror Dimension / Landmark L2 (Reflected Pylon).
- **Carrier**: Block placement feedback between the Anchor world and the Mirror world.
- **Mechanism**:
  - The player experiments with the Resonance Bus by placing light and structural sources in the Anchor world and observing their paired inverse counterparts in the Mirror dimension (e.g. Torch $\leftrightarrow$ Soul Torch, Oak $\leftrightarrow$ Dark Oak, Glass $\leftrightarrow$ Tinted Glass).
  - Cross-referencing the transposition rule described in Odette's notes yields the core resonance keyword.
- **Input / Output**: Enter the terminal command `unlock <keyword>` to calibrate the bidirectional resonance bus. Grants world flags `ch2.mirror_inversion_solved` and `ch2.resonance_calibrated`.

---

### P2.1 — Reflection Divergence Observation (Telemetry Synchronization)
- **Location**: Mirror Dimension / Movement Replay Track.
- **Carrier**: Replay entity and telemetry log at `/var/log/reflection.log`.
- **Mechanism**:
  - The Reflection entity replays the player's past five minutes of movements within exact tolerance before reaching divergence timestamp $t_\text{diverge}$.
  - At the divergence point, the Reflection abandons the recorded track, freezes its translation, and turns to observe the player.
  - Consulting the subsystem telemetry log for the active capture state gives the command to synchronize the replayer.
- **Input / Output**: Enter the terminal command `unlock <keyword>`. Grants world flag `ch2.complete`, completing Chapter 2 and unlocking the Version History subsystem for Chapter 3.


