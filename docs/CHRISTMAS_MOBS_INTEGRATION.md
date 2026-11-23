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

The prepared blueprints preserve the original cube and animation UUIDs. Each model's head bone is tagged `h[yaw=75;pitch=30;speed=8]_head`, so the model can turn its head separately from its body. Each model also has an empty `tag_name` bone as a nametag anchor. In the ModelEngine mechanic, use the bone ID `name` (the `tag_` prefix is not included in the `nametag=` value).

## Install model and mob files

1. Stop the server and back up `plugins/ModelEngine/` and `plugins/MythicMobs/`.
2. Copy the package's `plugins/ModelEngine/blueprints/Christmas/` directory into the matching server directory.
3. Copy `plugins/MythicMobs/Mobs/Christmas/` into the server's `plugins/MythicMobs/Mobs/Christmas/`.
4. Copy `plugins/MythicMobs/RandomSpawns/ChristmasMountains.yml` into `plugins/MythicMobs/RandomSpawns/`.
5. In `plugins/MythicMobs/config/config-spawning.yml`, enable `GenerateSpawnPoints: true`. Change only that setting; do not replace the whole file.
6. Set the `Worlds:` value in `ChristmasMountains.yml` to the exact world name where the mountain range will be pasted. The deployment target selected for the mountains is `world`.
7. Run `/meg reload models`, then `/mm reload`. Check the console for import or YAML errors.
8. ModelEngine generates a client resource pack under `plugins/ModelEngine/resource pack/`. This server already has ItemsAdder 4.0.16, so use ItemsAdder as the single resource-pack distributor: copy the package's `plugins/ItemsAdder/contents/christmasfeatures/` folder into the existing `plugins/ItemsAdder/contents/` folder; in the existing ItemsAdder `config.yml`, append `ModelEngine/resource pack` to `merge_other_plugins_resourcepacks_folders` (do not erase other entries); run `/meg reload` and then `/iazip`. Continue using the existing ItemsAdder hosting configuration. This combines the ChristmasFeatures item assets and ModelEngine mob models into the one pack players receive.

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

The model head tracks the nearest player within 24 blocks on a 10-tick interval. The Grinch uses cleared AI goal selectors instead of `NoAI: true`; MythicMobs documents that `NoAI: true` prevents skills from being cast. ModelEngine's `save=true` keeps the Grinch model attached when the persistent entity's chunk unloads. The Grinch itself remains non-collidable and invulnerable.

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

## Resource pack – ItemsAdder (installed 4.0.16)

The full-install ZIP includes the ChristmasFeatures assets in `plugins/ItemsAdder/contents/christmasfeatures/resourcepack/assets/`. Merge that folder into the existing ItemsAdder contents tree. Add this path to the existing merge list in `plugins/ItemsAdder/config.yml`:

```yaml
merge_other_plugins_resourcepacks_folders:
  - "ModelEngine/resource pack"
```

If the setting already has other folders, keep them and append only this one. After installing the models, run `/meg reload` and then `/iazip`. ItemsAdder will build one combined pack from the ChristmasFeatures custom item assets and the ModelEngine model assets. Keep the current ItemsAdder hosting method; do not configure a second resource-pack URL.

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

The current RandomSpawns file is already configured for `Worlds: world`, as requested. Keep the exact world name in sync with the name shown by `/mv list` if you rename the world.

## Mountain terrain – FAWE schematic

The in-game helper command `/christmas mountains` prints the install steps; it deliberately does not paste the schematic automatically.

The separate `Christmas-Mountain-NPC-Integration.zip` package includes `Christmas_Alpine_Range_128x92x128.schem`, a Sponge Schematic v3 region with three snowy peaks, steep stone faces, a frozen lake, spruce forest pockets and 3D biome data (`snowy_slopes`, `frozen_peaks`, `jagged_peaks`, `grove`). The structure is 128 × 92 × 128 blocks and includes approximately 140 spruce trees.

WorldEdit/FAWE uses `//schem load <filename>` for loading a schematic. On Bukkit/Paper the default schematic folder is `plugins/WorldEdit/schematics/` (unless the path was changed in configuration).

1. Prefer a separate mountain world or a verified empty area, at least 128 × 128 blocks wide with 92 blocks of free vertical space.
2. Back up the target world. Place the paste origin at the ground-level southwest corner of the chosen footprint.
3. Load it with `//schem load Christmas_Alpine_Range_128x92x128`.
4. Use `//paste -ab` to skip air blocks and preserve existing buildings outside the mountain mass. Existing terrain blocks inside the footprint can still overlap the mountain, so a flat/open target or a prepared mountain world is strongly preferred.
5. If you deliberately want to replace every block inside the 128 × 92 × 128 box, use `//paste` without `-a` only after confirming the area is empty or safely backed up. This removes existing blocks in that full box.
6. Check biome data on the slopes and peaks. ChristmasFeatures 1.0.6 now preserves `SNOWY_SLOPES`, `FROZEN_PEAKS`, `JAGGED_PEAKS` and `GROVE` when its winter conversion is enabled. Keep these entries in `winter.preserve-biomes`; do not remove them, or MythicMobs' biome filters may stop matching.

Use `//undo` immediately if the paste is in the wrong place.

## Verification status and safety

- The model JSON files, original animation UUIDs, schematic palette/data lengths and MythicMobs YAML structure were locally checked.
- The live ModelEngine import, actual mob spawning and resource-pack delivery still require an in-game server test.
- The mountain schematic does not place mob entities; MythicMobs handles their spawns separately.
- The server's storage was at 93.3% when the plugin list was supplied. Before pasting new terrain or making server-side copies of large packs/world backups, free storage or upgrade the SSD package. Prefer downloading backups off-server so the backup itself does not consume the remaining server disk.
