# Christmas NPC models – ModelEngine + MythicMobs

This integration targets the exact plugin versions reported on the warriorland server:

| Plugin | Server version |
|---|---:|
| ModelEngine | 4.1.1 |
| MythicMobs Premium | 5.12.1-SNAPSHOT 5882 |
| FastAsyncWorldEdit | 2.14.4-SNAPSHOT-1257 |
| SupremeTags | 2.1.16 |
| BattlePass | 5.0.12 |

The Blockbench source models are converted only by ModelEngine when its blueprint loader runs; nothing is procedurally generated during gameplay.

## Models

| Model ID | Model | Animations |
|---|---|---|
| `grinch` | Grinch, 229 cubes | `idle` (loop), `greet` (one-shot) |
| `yeti` | Yeti, 231 cubes | `idle` (loop), `roar` (one-shot) |
| `frost_emperor` | Frost Emperor, 238 cubes | `idle` (loop), `raise_staff` (one-shot) |

The prepared blueprints preserve the original cube and animation UUIDs. Each model's head bone is tagged `h[yaw=75;pitch=30;speed=8]_head`, so the model can turn its head separately from its body. Each model also has an empty `tag_name` bone as a nametag anchor.

## Install model and mob files

1. Stop the server and back up `plugins/ModelEngine/` and `plugins/MythicMobs/`.
2. Copy the package's `plugins/ModelEngine/blueprints/Christmas/` directory into the matching server directory.
3. Copy `plugins/MythicMobs/Mobs/Christmas/` into the server's `plugins/MythicMobs/Mobs/Christmas/`.
4. Copy `plugins/MythicMobs/RandomSpawns/ChristmasMountains.yml` into `plugins/MythicMobs/RandomSpawns/`.
5. In `plugins/MythicMobs/config/config-spawning.yml`, enable `GenerateSpawnPoints: true`. Change only that setting; do not replace the whole file.
6. Set the `Worlds:` value in `ChristmasMountains.yml` to the exact world name where the mountain range will be pasted. The console excerpt showed a player in `spawnmapa`, but that does not prove that it is the intended mountain world.
7. Run `/meg reload models`, then `/mm reload`. Check the console for import or YAML errors.
8. ModelEngine generates a client resource pack. The pack must then be distributed to players through the server's resource-pack setup; the blueprint ZIP does not configure a public pack URL automatically.

## Grinch – spawn NPC

The Grinch is a stationary, non-hostile MythicMobs zombie with a ModelEngine model. Its MythicMobs definition adds the scoreboard tag `christmas_grinch` on spawn. The updated ChristmasFeatures plugin listens for Bukkit's `PlayerInteractEntityEvent`, identifies this tag and opens the Grinch menu on a main-hand right-click.

This interaction does not depend on MythicCrucible or MythicRPG `~onInteract` triggers. It requires the updated ChristmasFeatures JAR, not only the MythicMobs YAML. The user's current Hostify `plugins/` listing did not show a ChristmasFeatures folder/JAR, so that plugin still has to be installed.

Test the menu independently with:

```
/christmas grinch open
```

Spawn the NPC at the player's current position:

```
/mm m spawn ChristmasGrinch
```

The model head tracks the nearest player within 24 blocks on a 10-tick interval. The Grinch itself remains non-collidable and invulnerable.

## Grinch menu – installed tag and quest plugins

The server already has SupremeTags 2.1.16 and BattlePass 5.0.12. The default config in the updated ChristmasFeatures version is:

```yaml
grinch:
  menu:
    tags-command: "player:tags"
    quests-command: "player:bp open quests"
```

The `player:` prefix means the clicking player executes the command, so permission checks and player GUI context are retained. The previous implementation dispatched all commands as console, which is not suitable for these player menus.

SupremeTags opens its player tag menu through `/tags`. BattlePass supports `/bp open <menu name>`; `quests` is the weekly quests menu. To show daily quests instead, use `player:bp open daily-quests`. Check that the default group has the relevant plugin permissions; the SupremeTags player command permission is `supremetags.player`.

## Yeti and Frost Emperor – mountain spawns

The RandomSpawns configuration is additive to vanilla spawns:

| Mob | Biomes | Chance per generated spawn point | Cooldown |
|---|---|---:|---:|
| Yeti | `SNOWY_SLOPES`, `FROZEN_PEAKS`, `GROVE` | 1% | 180 seconds |
| Frost Emperor | `FROZEN_PEAKS`, `JAGGED_PEAKS` | 0.2% | 900 seconds |

There are no fixed Y-level conditions. This keeps the spawns working if the mountain schematic is pasted at a different elevation. MythicMobs `ADD` random spawns require generated spawn points and a suitable Survival/Adventure player nearby.

Manual tests:

```
/mm m spawn ChristmasYeti
/mm m spawn ChristmasFrostEmperor
```

The current RandomSpawns file uses `Worlds: world` as a placeholder. Replace it before enabling natural spawns.

## Mountain terrain – FAWE schematic

The separate `Christmas-Mountain-NPC-Integration.zip` package includes `Christmas_Alpine_Range_128x92x128.schem`, a Sponge Schematic v3 region with three snowy peaks, steep stone faces, a frozen lake, spruce forest pockets and 3D biome data (`snowy_slopes`, `frozen_peaks`, `jagged_peaks`, `grove`). The structure is 128 × 92 × 128 blocks and includes approximately 140 spruce trees.

WorldEdit/FAWE uses `//schem load <filename>` for loading a schematic. On Bukkit/Paper the default schematic folder is `plugins/WorldEdit/schematics/` (unless the path was changed in configuration).

1. Prefer a separate mountain world or a verified empty area, at least 128 × 128 blocks wide with 92 blocks of free vertical space.
2. Back up the target world. Place the paste origin at the ground-level southwest corner of the chosen footprint.
3. Load it with `//schem load Christmas_Alpine_Range_128x92x128`.
4. Use `//paste -ab` to skip air blocks and preserve existing buildings outside the mountain mass. Existing terrain blocks inside the footprint can still overlap the mountain, so a flat/open target or a prepared mountain world is strongly preferred.
5. If you deliberately want to replace every block inside the 128 × 92 × 128 box, use `//paste` without `-a` only after confirming the area is empty or safely backed up. This removes existing blocks in that full box.
6. Check biome data on the slopes and peaks. Do not let a global biome converter overwrite these four mountain biome IDs with `SNOWY_PLAINS`; that would invalidate MythicMobs' biome filters.

Use `//undo` immediately if the paste is in the wrong place.

## Verification status and safety

- The model JSON files, original animation UUIDs, schematic palette/data lengths and MythicMobs YAML structure were locally checked.
- The live ModelEngine import, actual mob spawning and resource-pack delivery still require an in-game server test.
- The mountain schematic does not place mob entities; MythicMobs handles their spawns separately.
- The server's storage was at 93.3% when the plugin list was supplied. Before pasting new terrain or making server-side copies of large packs/world backups, free storage or upgrade the SSD package. Prefer downloading backups off-server so the backup itself does not consume the remaining server disk.
