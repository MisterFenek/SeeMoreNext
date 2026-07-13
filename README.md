# SeeMoreNext

> A maintained fork of [SeeMore](https://github.com/froobynooby/SeeMore) for modern Paper (1.19 → 26.2) and Folia.

SeeMoreNext adjusts a player's **server-side view distance** to match their
**client-side render distance**, optionally clamped per world. This reduces
bandwidth and tick cost for players who don't need a far render distance, while
keeping the experience identical for those who do.

This fork exists because the original SeeMore (`v1.0.2`) targets Bukkit API 1.17
and emits deprecation warnings on Paper 1.21.7+. SeeMoreNext:

- Targets Paper 1.19 → 26.2 with a **single universal JAR** (Folia included)
- Has **no bStats / no metrics** — drop-in for privacy-conscious servers
- Ships a single artifact per release, regardless of Paper version

---

## Compatibility

| Server type | Supported |
|---|---|
| Paper 1.19.4 | ✅ |
| Paper 1.20.x | ✅ |
| Paper 1.21.x | ✅ |
| Paper 26.1 / 26.2 (new Mojang versioning) | ✅ |
| Folia (regionised) | ✅ |
| Spigot / CraftBukkit | ❌ (uses Paper API) |

**One universal JAR** covers the entire supported range. Paper guarantees
binary compatibility for plugins compiled against an older API version, so a
plugin built against Paper API 1.19.4 runs on every newer release without
needing to be rebuilt.

---

## Install

1. Download the latest `SeeMoreNext-*.jar` from the
   [Releases](../../releases) page.
2. Drop it into your server's `plugins/` folder.
3. Restart the server. A `config.yml` is generated under `plugins/SeeMoreNext/`.
4. Tweak `config.yml` and run `/seemorenext reload` (or restart again).

---

## Configuration

```yaml
# Configuration for SeeMoreNext.
# Please don't change this!
version: 1

# Delay (in ticks) before a player's view distance is lowered after their
# client settings change. Prevents the server from being flooded by clients
# that spam the view-distance slider.
update-delay: 600

# Whether the plugin should log to the console when it changes a player's
# view distance.
log-changes: true

# Per-world settings. Worlds not listed here fall back to 'default'.
world-settings:
  default:
    # Maximum view distance a player in this world can have.
    # -1 = use the server's configured view distance for this world.
    maximum-view-distance: -1
  # Example: cap a creative world to 16 chunks
  # creative:
  #   maximum-view-distance: 16
```

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/seemorenext` (alias: `/smn`) | `seemorenext.command.seemore` | Show help |
| `/seemorenext reload` | `seemorenext.command.reload` | Reload config |
| `/seemorenext average` | `seemorenext.command.average` | Show effective average view distance per world |
| `/seemorenext players` | `seemorenext.command.players` | List players grouped by view distance |

The wildcard `seemorenext.command.*` grants every subcommand (default: op).

---

## Building

Requires JDK 21.

```bash
# Build the universal JAR
./gradlew shadowJar
```

Output: `build/libs/SeeMoreNext-2.0.0.jar`

> ℹ️ The build always compiles against Paper API 1.19.4 (the minimum we
> support). The single resulting artifact runs on Paper 1.19 → 26.x and Folia.

---

## Why a fork?

The original SeeMore is great, but as of 2026 it had not been updated to compile
cleanly against Paper 1.21.7+ and the new Mojang `26.x` versioning. This fork
preserves the original design verbatim and only changes what's needed to keep
it compiling and running on modern servers — in one universal artifact.

## Credits

- Original plugin: [froobynooby / SeeMore](https://github.com/froobynooby/SeeMore) — MIT
- Fork maintainer: [MisterFenek](https://github.com/MisterFenek)
- Powered by: Paper API, Folia, NabConfiguration (vendored), Gradle

## License

MIT — see [LICENSE](LICENSE).
