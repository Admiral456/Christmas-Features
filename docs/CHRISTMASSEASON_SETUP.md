# ChristmasSeason setup for Paper/Purpur 1.21.10

The uploaded ChristmasSeason-2.5.0.jar identifies itself as plugin christmas-season and declares api-version 1.21.3. It is therefore intended for 1.21.3+ servers, including 1.21.10. It owns the /xmas command and provides safer biome snapshot/restore behaviour than the small fallback winter manager in ChristmasFeatures.

When ChristmasSeason is installed, ChristmasFeatures detects it and does not run its own WinterManager. Do not run two biome converters at once.

## Recommended configuration in plugins/ChristmasSeason/config.yml

Keep the existing file and edit these settings rather than replacing the entire file:

~~~~yaml
active: false

snowWorld: "world"
snowWorlds: []

biome:
  enabled: true
  target: "SNOWY_PLAINS"
  changeMinY: 50
  changeMaxY: 200
  verticalStep: 4
  enableSnapshot: true
  playerBubble:
    enabled: true
    radiusChunks: 2
    refreshClient: true
    tickIntervalTicks: 10
    perTickBudget: 8
  instantSnow:
    enabled: true
    coverage: 0.8
    freezeWater: true
  convertAll:
    radiusChunks: 64
    perTick: 2
  restore:
    perTick: 4
    removeSnowLayers: true
    removeIce: true

snowstorm:
  enabled: true
  mode: manual
  auto:
    onSeconds: 150
    offSeconds: 45

gifts:
  enabled: false

decoration:
  enabled: true
~~~~

Use the actual name of your overworld if it is not world.

### Why these settings

- enableSnapshot: true keeps the original biome data in plugins/ChristmasSeason/biome-snapshot.db, allowing restoration.
- playerBubble keeps extending winter biomes around active players as they explore.
- instantSnow.enabled with freezeWater: true places initial snow and freezes still water when chunks are converted.
- snowstorm.mode: manual avoids the default auto snowstorm pauses.
- gifts.enabled controls ChristmasSeason's own random gift chests; leave it false unless you explicitly want to use that separate gift system.
- decoration.enabled: true keeps the separate decorative drops available.

coverage: 0.8 puts snow on 80% of eligible surface columns when converted. Test this on a backup first: it can add snow to exposed player-built surfaces too. Exclude protected spawn/build areas through biome.exclude.areas or WorldGuard region exclusions if needed.

## Activation and map conversion

1. Back up the world using the host's backup system before changing biomes.
2. Start/restart the server with ChristmasSeason and ChristmasFeatures installed.
3. Run /xmas on. ChristmasSeason creates its safety snapshot as part of activation.
4. To convert generated chunks around spawn, run /xmas biome convert-all 64 world confirm. The command only works while ChristmasSeason is active and processes chunks gradually. The default 64 is a radius in chunks, not a promise to convert an infinite world.
5. Explore farther areas; the player bubble converts chunks around players as they visit them.
6. Use /xmas status to inspect the event and biome progress.

Use /xmas off to stop the event and start restoring saved biomes. Do not delete biome-snapshot.db while you may need restoration.

## Command ownership

- ChristmasSeason owns /xmas.
- ChristmasGift owns /christmasgift and /cgift, and also declares /xmasgift as an alias.
- ChristmasSeason also declares /xmasgift, so avoid that shared alias. Use /christmasgift or /cgift for the ChristmasGift admin menu and use /xmas for ChristmasSeason.
- ChristmasFeatures uses /christmas, /vanocni and /xfeatures.

References:
- ChristmasSeason project and compatibility: https://modrinth.com/project/christmas-season
- ChristmasGift commands and compatibility: https://modrinth.com/project/christmasgift
