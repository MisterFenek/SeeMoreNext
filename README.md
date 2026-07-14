# SeeMoreNext

Fork of [SeeMore](https://github.com/froobynooby/SeeMore) for Paper 1.19 → 26.2 and Folia.

Sets each player's **server-side view distance** to match their **client-side render distance**, optionally clamped per world. Players who render fewer chunks save server resources; players who render more still get the full experience.

## Install

1. Drop `SeeMoreNext-*.jar` into `plugins/`.
2. Restart the server.
3. Edit `plugins/SeeMoreNext/config.yml` if needed, then `/smn reload`.

## Configuration

```yaml
# Delay (ticks) before lowering view distance after a client change.
update-delay: 600

# Log level: off | minimal | all
log-changes: minimal

# Apply view distance on join before client sends its settings.
wait-for-client-settings: true

# Check all players' view distance every N ticks. -1 to disable.
polling-interval: 200

# Re-check delays (ticks) after PlayerClientOptionsChangeEvent.
options-change-retries: [20, 60]

world-settings:
  default:
    maximum-view-distance: -1   # -1 = server's world view distance
    # minimum-view-distance: 2  # optional floor
```

### Per-world example

```yaml
world-settings:
  default:
    maximum-view-distance: 12
  creative:
    maximum-view-distance: 6
  survival:
    minimum-view-distance: 4
    maximum-view-distance: 16
```

## Commands

| Command | Permission | Who | Description |
|---|---|---|---|
| `/smn` | `seemorenext.command.seemore` | op | Show help |
| `/smn reload` | `seemorenext.command.reload` | op | Reload config |
| `/smn update` | `seemorenext.command.update` | op | Force update all players |
| `/smn refresh` | `seemorenext.command.refresh` | all | Refresh your own view distance |
| `/smn average` | `seemorenext.command.average` | op | Effective average view distance + histogram |
| `/smn players` | `seemorenext.command.players` | op | Players grouped by view distance |
| `/smn status` | `seemorenext.command.status` | all | Plugin status overview |
| `/smn debug-info` | `seemorenext.command.debug-info` | op | Config values + your view distance |

`seemorenext.bypass` — players with this permission are not clamped (ops, builders, etc).

## Build

Requires JDK 21.

```bash
./gradlew shadowJar
# Output: build/libs/SeeMoreNext-*.jar
```

## Compatibility

- Paper 1.19.4 / 1.20.x / 1.21.x / 26.x
- Folia (regionised scheduler)
- Single universal JAR compiled against Paper API 1.19.4

## Credits

- Original: [froobynooby / SeeMore](https://github.com/froobynooby/SeeMore) — MIT
- Maintainer: [MisterFenek](https://github.com/MisterFenek)

## License

MIT
