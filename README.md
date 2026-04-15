# 🧹 ChunkCleaner

> A lightweight Minecraft plugin to trim bloated worlds.

---

## Overview

**ChunkCleaner** is a simple yet powerful plugin designed to reduce world file size by removing chunks that were never meaningfully used. Whether it's leftover exploration chunks or areas a player passed through for two seconds, ChunkCleaner takes care of the clutter automatically or on demand.

---

## Features

- 🗑️ **Automatic world trimming** — deletes uninhabited or empty chunks on server stop
- 📦 **Content-aware scanning** — preserves chunks containing Chests and other meaningful blocks
- ⏱️ **Inhabitation threshold** — configurable minimum time a chunk must be inhabited to be kept
- 🔁 **File rewriting** — properly rewrites region files and rebuilds headers after deletion
- 🔍 **Manual chunk inspection** — check any chunk's eligibility on the fly
- ⚡ **Auto Mode** — lightweight auto-trim triggered on server stop with optional extra event hooks

---

## Commands

### `/deletechunks <world> <new or resume>`
Scans the specified world and deletes chunks that meet **all** of the following criteria:
- Contain no significant blocks (e.g. no Chests)
- Were not inhabited long enough (configurable threshold)

After deletion, the plugin rewrites the affected region files and rebuilds their headers cleanly.

> 💡 This is great for clearing out mass-exploration chunks players generated but never built in.

---

### `/chunkinfo`
Manually inspect the chunk you're currently standing in. Displays whether the chunk would be eligible for deletion, including:
- Inhabitation time
- Detected significant blocks

> ⚠️ **Note:** Inhabitation time is retrieved via an API call and may occasionally be unreliable or slightly inaccurate.

---

## Auto Mode

**Auto Mode** is enabled by default and provides a passive, event-driven alternative to manually running `/deletechunks`.

Instead of scanning the entire world, it:
1. Listens for **new chunk generation events** during the session
2. Queues newly generated chunks for review
3. **Deletes eligible chunks on server stop**

This keeps your world trim in the background without any manual intervention.

### Extra Events (Optional)

Auto Mode can optionally enable **extra event hooks** via the config. This provides a safer, more thorough version of auto-trim by catching additional scenarios where junk chunks may be created.

> ✅ Recommended for servers where players explore frequently.

---

## Configuration

```yaml
# config.yml

# Minimum inhabitation time (in seconds) for a chunk to be kept
inhibitedTime: 60

# Enable Auto Mode (auto-trim on server stop)
chunkLoadDeletion: true

# Enable extra event hooks in Auto Mode for safer coverage
extraEvents: true

# Sets the prefix that gets displayed via Chat
prefix: §5CC §8| §f
```

---

## How It Works

1. Chunks are scanned for **significant content** (Chests, etc.)
2. Chunks below the **inhabitation threshold** are flagged
3. Flagged chunks are removed from the region file
4. Region files are **rewritten** with rebuilt headers to keep them valid

---

## Notes

- Always **back up your worlds** before running `/deletechunks` on production servers
- The inhabitation time check is API-based and may be inconsistent in edge cases
- Slim Mode is the safest way to use ChunkCleaner on active servers

---

## License

MIT — free to use, modify, and distribute.
