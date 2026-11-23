# Christmas mountain range – FAWE installation

The companion asset bundle contains `Christmas_Alpine_Range_128x92x128.schem` in Sponge Schematic v3 format. It is designed for FastAsyncWorldEdit 2.14.x and Minecraft Java 1.21.10.

## Terrain design

- Size: 128 × 92 × 128 blocks.
- Three large mountain peaks with snowy slopes and steep exposed rock faces.
- Frozen lake in the central basin.
- Approximately 140 spruce trees arranged in foothill forest pockets.
- 3D biome data: `minecraft:snowy_slopes`, `minecraft:frozen_peaks`, `minecraft:jagged_peaks`, `minecraft:grove`.
- Yeti spawn filters target snowy slopes, frozen peaks and groves.
- Frost Emperor spawn filters target frozen and jagged peaks.

## Paste instructions

The helper command `/christmas mountains` prints these instructions in-game; it does not paste automatically. This keeps the final paste under the operator's control.

1. Back up the destination world.
2. Upload the `.schem` file to the WorldEdit schematic directory. Default Bukkit/Paper path: `plugins/WorldEdit/schematics/`; FAWE uses the WorldEdit schematic manager unless its path has been customised.
3. Choose an open/flat area or a separate mountain world. Allow at least 128 × 128 horizontal blocks and 92 blocks of vertical clearance.
4. Stand at the southwest corner at ground level and load `//schem load Christmas_Alpine_Range_128x92x128`.
5. Preview the paste location and run `//paste -ab`. `-a` skips air. FAWE's current source describes `-b` as pasting biome data, but one published FAWE command-reference page describes the opposite; check `/help //paste` in the installed 2.14.4 build before relying on it. Blocks already present inside the mountain volume may overlap it.
6. Only use `//paste` without `-a` when you explicitly want to clear/replace the entire 128 × 92 × 128 volume and have confirmed that it contains nothing you need.
7. Use `//undo` immediately if pasted at the wrong location.\n8. Verify the biome at several points on the slopes and peaks with `//biomeinfo -t`; check for the intended `snowy_slopes`, `frozen_peaks`, `jagged_peaks` and `grove` biomes before enabling natural MythicMobs spawns.

The schematic includes biome data. Do not subsequently convert the mountain area to `SNOWY_PLAINS`, or the MythicMobs biome filters will stop matching the mountain biomes.

## MythicMobs configuration

Set the exact target world in `plugins/MythicMobs/RandomSpawns/ChristmasMountains.yml`. The RandomSpawns file is configured for the requested world name `world`. Enable `GenerateSpawnPoints: true` in `plugins/MythicMobs/config/config-spawning.yml` without replacing the rest of the config.

