# OmniSoulbind

Bind items to a player's soul. A soulbound item is kept on death, cannot be picked up or used by other players, and is therefore effectively un-tradeable. MMOItems-aware, Folia-ready, zero external database — the binding lives in the item's NBT.

Part of the SalyVn **OmniBundle** RPG suite for Paper 1.21.

## Features

- **NBT-backed binding** — owner UUID + name are stored in the item's PersistentDataContainer. No database, no player files. The binding travels with the item.
- **Keep-on-death** — a dying player's own soulbound items are pulled from the drop list and returned on respawn (or immediately). Overflow drops at the player's feet.
- **Anti-pickup** — other players (and non-player entities) can't pick up soulbound items they don't own.
- **Use restriction** — other players can't click or interact with items bound to someone else.
- **MMOItems auto-bind** — automatically bind on pickup by MMOItems tier or vanilla material.
- **Non-destructive lore** — a configurable "Soulbound to X" line is appended without wiping existing lore, and never duplicated on re-scan.
- **PlaceholderAPI** — optional `%omnisoulbind_owner%` and `%omnisoulbind_bound%`.
- **Folia support** — deferred returns run on the owner's entity scheduler via FoliaLib.

## Installation

1. Drop `OmniSoulbind-1.0.0.jar` into your server's `plugins/` folder.
2. Start the server once to generate `plugins/OmniSoulbind/config.yml`.
3. Adjust the config, then run `/soulbind reload`.

### Optional soft dependencies

- **MMOItems** — enables tier-based auto-bind.
- **PlaceholderAPI** — enables the placeholders.

## Commands & permissions

| Command | Description | Permission |
|---|---|---|
| `/soulbind` | Bind the held item to yourself | `omnisoulbind.use` (default: true) |
| `/soulbind info` | Show the owner of the held item | `omnisoulbind.use` |
| `/soulbind remove` | Strip binding from the held item | `omnisoulbind.admin` (default: op) |
| `/soulbind reload` | Reload the config | `omnisoulbind.admin` |

Alias: `/sb`.

## Configuration

```yaml
keep-on-death: true       # keep a player's own soulbound items on death
return-on-respawn: true   # true = give back on respawn, false = re-add immediately
restrict-pickup: true     # others cannot pick up items they don't own
restrict-use: true        # others cannot click/interact with items bound to someone else

lore-line: "&7Soulbound to &e%owner%"

auto-bind:
  enabled: false          # auto-bind items on pickup
  tiers: []               # MMOItems tiers, e.g. [LEGENDARY, UNIQUE]
  materials: []           # vanilla materials, e.g. [NETHERITE_SWORD]
```

Every field falls back to a sensible default. See the generated `config.yml` for the full documented set including messages.

## Building from source

```bash
./gradlew.bat shadowJar --no-daemon
```

The shaded jar (with the Kotlin stdlib bundled) lands in `build/libs/`.

## License

Part of the SalyVn OmniBundle. All rights reserved.
