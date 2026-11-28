# Christmas NPC models – ModelEngine + MythicMobs

This integration uses the three supplied Blockbench models with ModelEngine 4 and MythicMobs. It does not use FancyNpcs: the FancyNpcs authors currently state that ModelEngine 4 is not supported.

## Models

| Model ID | Model | Animations |
|---|---|---|
| `grinch` | Grinch, 229 cubes | `idle` (loop), `greet` (one-shot) |
| `yeti` | Yeti, 231 cubes | `idle` (loop), `roar` (one-shot) |
| `frost_emperor` | Frost Emperor, 238 cubes | `idle` (loop), `raise_staff` (one-shot) |

The import bundle adds one empty `tag_name` bone to each model for the ModelEngine nametag anchor. The existing `head` bone is preserved for ModelEngine's `headtarget` mechanic.

## Install

1. Back up `plugins/ModelEngine/` and `plugins/MythicMobs/`.
2. Copy the bundle's `plugins/ModelEngine/blueprints/` into your server's `plugins/ModelEngine/blueprints/`.
3. Copy the bundle's `plugins/MythicMobs/Mobs/Christmas/` into `plugins/MythicMobs/Mobs/Christmas/`.
4. Copy `plugins/MythicMobs/RandomSpawns/ChristmasMountains.yml` into `plugins/MythicMobs/RandomSpawns/`.
5. In `plugins/MythicMobs/config/config-spawning.yml`, enable `GenerateSpawnPoints: true`. Do not replace the rest of that file.
6. Run `/meg reload`, then `/mm reload`. Check the console for model import errors.
7. Players must receive and accept ModelEngine's generated resource pack. ModelEngine generates the pack but does not distribute it automatically.

## Grinch – spawn NPC

Stand at the exact location where the Grinch should stand and run:

```
/mm m spawn ChristmasGrinch
```

The Grinch is a persistent, non-collidable `INTERACTION` NPC. Right-clicking him opens the ChristmasFeatures GUI. His model plays `greet` on interaction. Every 10 ticks, ModelEngine aims the `head` bone at the nearest player within 24 blocks; it follows that player as they move left, right or around him.

The GUI has two destinations: **Grinchovy úkoly** and **Vánoční tagy**. The tag button is connected by default to the built-in ChristmasFeatures cosmetic selector:

```yaml
grinch:
  menu:
    tags-command: "christmas cosmetics %player% tag"
    quests-command: ""
```

ChristmasFeatures migrates an older blank `tags-command` to this default when the plugin starts. The quest page is intentionally left blank until the actual quest plugin and its command are confirmed. If you use another tag plugin instead, you can replace `tags-command` with its console command and keep `%player%` as the player placeholder.

You can test the GUI without the NPC using `/christmas grinch open`.

## Yeti and Frost Emperor – mountains

The package adds MythicMobs RandomSpawns for:
- Yeti: snowy slopes, frozen/jagged peaks and groves; 1% chance per generated spawn point, 3-minute cooldown and a radius cap.
- Frost Emperor: frozen and jagged peaks only; 0.2% chance per generated spawn point, 15-minute cooldown and a radius cap.

To test manually:

```
/mm m spawn ChristmasYeti
/mm m spawn ChristmasFrostEmperor
```

The random-spawn configuration uses the world name `world`; change it if your overworld is named differently. MythicMobs `ADD` random spawns are additive to vanilla spawns and require `GenerateSpawnPoints: true`.

## Preserve actual mountains

This server also has ChristmasSeason available for winter biome conversion. If its `convert-all` operation replaces the mountain biome IDs with `SNOWY_PLAINS`, the MythicMobs biome filters above will no longer match those areas. Keep the mountain range excluded from biome conversion (or change the `Biomes:` lists to the actual biome IDs after checking them in-game). Do not convert the whole map before the mountain exclusion has been configured.

## Verification status

The model JSON files and MythicMobs YAML files in the bundle were structurally checked. The Java-side Grinch menu is included in ChristmasFeatures. A real ModelEngine/MythicMobs spawn test and the actual external tag/quest menu handoff still require the live server and the installed tag/quest plugin versions.
