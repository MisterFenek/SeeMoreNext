# SeeMoreNext

> A maintained fork of [SeeMore](https://github.com/froobynooby/SeeMore) for modern Paper (1.19 → 26.2) and Folia.

SeeMoreNext adjusts a player's **server-side view distance** to match their
**client-side render distance**, optionally clamped per world. This reduces
bandwidth and tick cost for players who don't need a far render distance, while
keeping the experience identical for those who do.

This fork exists because the original SeeMore (`v1.0.2`) targets Bukkit API 1.17
and emits deprecation warnings on Paper 1.21.7+. SeeMoreNext:

- Targets Paper API 1.19 → 26.2 (Mojang new versioning included)
- Supports Folia (regionised scheduler) out of the box
- Has **no bStats / no metrics** — drop-in for privacy-conscious servers
- Ships pre-built JARs for every supported Paper version

---

## Compatibility

| Server type | Supported |
|---|---|
| Paper 1.19.4 | ✅ |
| Paper 1.20.4 / 1.20.6 | ✅ |
| Paper 1.21.1 / 1.21.4 / 1.21.8 | ✅ |
| Paper 26.1 / 26.2 (new Mojang versioning) | ✅ |
| Folia (regionised) | ✅ |
| Spigot / CraftBukkit | ❌ (uses Paper API) |

Pick the JAR from the [Releases](../../releases) page that matches your
server's Minecraft/Paper version.

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
# Build for Paper 1.20.4 (default)
./gradlew shadowJar

# Build for a specific Paper version
./gradlew shadowJar -Dpaper.api.version=26.2-R0.1-SNAPSHOT
```

Output: `build/libs/SeeMoreNext-<version>.jar`

> ℹ️ For Paper 26.x the deprecation warning on `PlayerClientOptionsChangeEvent`
> is avoided by reading `Player.getClientViewDistance()` directly. Behaviour
> is unchanged.

---

## Why a fork?

The original SeeMore is great, but as of 2026 it had not been updated to compile
cleanly against Paper 1.21.7+ and the new Mojang `26.x` versioning. This fork
preserves the original design verbatim and only changes what's needed to keep
it compiling and running on modern servers.

## Credits

- Original plugin: [froobynooby / SeeMore](https://github.com/froobynooby/SeeMore) — MIT
- Fork maintainer: [MisterFenek](https://github.com/MisterFenek)
- Powered by: Paper API, Folia, NabConfiguration, Gradle

## License

MIT — see [LICENSE](LICENSE).
