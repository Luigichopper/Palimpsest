# Open Items & Placeholders

The following items represent owner-provided assets, secrets, and configurations:

| Key | Status / Default | Description |
|---|---|---|
| `«SEED»` | Dev placeholder in `private/seed.txt` | Target Minecraft world seed. Salt and SHA-256 hash stored in `src/main/resources/palimpsest_seed.json`. Plaintext seed must never ship. |
| `«LICENSE»` | Dev placeholder: `TODO` | Final license for the Palimpsest mod (Fantasy is LGPL-3.0). |
| `«SITE_URL»` | Dev placeholder: `https://palimpsest.example.com` | Target URL for the static ARG website. |
| `«DISPLAY_NAME»` | Default: `Palimpsest` | Player-facing mod name. |
| `«ART»` | 16x16 placeholder textures | Generated via `tools/gen_placeholder_textures.py`. |
| `«AUDIO»` | Vanilla sound references | Final custom ambient/horror audio tracks (`.ogg`). |
| `«LANDMARKS»` | Placeholder coordinates in `landmarks.json` | Final coordinates scouted on the canonical seed. |
