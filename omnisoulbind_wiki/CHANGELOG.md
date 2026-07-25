# Changelog

All notable changes to OmniSoulbind are documented here.

## [1.0.0] - 2026-07-25

### Added
- Soulbinding stored in item NBT (PersistentDataContainer) — owner UUID + name, no external database.
- Manual binding via `/soulbind` (binds the held item to the sender).
- `/soulbind info` to show the owner of the held item.
- `/soulbind remove` (admin) to strip binding + lore from the held item.
- `/soulbind reload` (admin) for hot config reload. Alias `/sb`.
- Keep-on-death: a dying player's own soulbound items are removed from the drop list and returned on respawn (or immediately, via `return-on-respawn`). Overflow drops at the player's feet.
- Anti-pickup: other players and non-player entities cannot pick up soulbound items they don't own (`restrict-pickup`).
- Use restriction: other players cannot click or interact with items bound to someone else (`restrict-use`).
- MMOItems-aware auto-bind on pickup by tier or vanilla material list (`auto-bind`).
- Configurable, non-destructive owner lore line (`lore-line`) — appended without wiping existing lore and de-duplicated on re-scan.
- Optional PlaceholderAPI expansion: `%omnisoulbind_owner%`, `%omnisoulbind_bound%`.
- Full Folia support via FoliaLib (respawn returns scheduled on the owner's entity scheduler).
