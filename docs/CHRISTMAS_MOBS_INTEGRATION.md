# Christmas NPC models – ModelEngine + MythicMobs

This integration uses the supplied Grinch and Yeti Blockbench models with ModelEngine 4 and MythicMobs. It does not use FancyNpcs.

## Models

| Model ID | Model | Animations |
|---|---|---|
| grinch | Grinch | idle (loop), greet (one-shot) |
| yeti | Yeti | idle (loop), roar (one-shot) |

## Install

1. Back up plugins/ModelEngine/ and plugins/MythicMobs/.
2. Copy the Grinch and Yeti blueprints into plugins/ModelEngine/blueprints/.
3. Copy the Grinch and Yeti definitions into plugins/MythicMobs/Mobs/Christmas/.
4. Copy the Yeti random-spawn configuration into plugins/MythicMobs/RandomSpawns/ only if you want the Yeti to spawn naturally.
5. In plugins/MythicMobs/config/config-spawning.yml, enable GenerateSpawnPoints: true. Do not replace the rest of that file.
6. Run /meg reload, then /mm reload. Check the console for model import errors.
7. Players must receive and accept ModelEngine's generated resource pack. ModelEngine generates the pack but does not distribute it automatically.

## Grinch – spawn NPC

Stand exactly where you want the Grinch and run:

~~~text
/christmas npc spawn
~~~

When ModelEngine and MythicMobs are enabled, ChristmasFeatures calls MythicMobs' /mm m spawn ChristmasGrinch at the invoking player's position, using the imported Blockbench model. Right-clicking that model opens the ChristmasFeatures GUI when the supplied MythicMobs interaction definition is installed.

If either model plugin is unavailable, ChristmasFeatures creates its own persistent, clickable in-plugin Grinch NPC at the player's position. Right-click opens the same menu.

The GUI contains:
- Hledání vánočních dárků: starts the global gift hunt, shows its configuration and whether the Aurora reward is unlocked.
- Vánoční ChatGames: starts a question round and displays normal/VIP reward information.
- Vánoční tagy and Trail effects: opens the built-in cosmetic selectors.
- Grinchovy úkoly: optional command handoff to an external quest plugin.

The default hooks are:

~~~yaml
grinch:
  menu:
    tags-command: "christmas cosmetics %player% tag"
    quests-command: ""
    gift-start-cooldown-seconds: 1800
    chatgame-start-cooldown-seconds: 900
~~~

## Yeti – mountains

The supported optional random-spawn configuration is for the Yeti only:
- snowy slopes, frozen/jagged peaks and groves;
- a low spawn chance per generated spawn point;
- a cooldown and radius cap to avoid repeated nearby spawns.

To test manually:

~~~text
/mm m spawn ChristmasYeti
~~~

The random-spawn configuration uses the world name world; change it if your overworld is named differently. MythicMobs ADD random spawns are additive to vanilla spawns and require GenerateSpawnPoints: true.

## Frost Emperor removed

The Frost Emperor winter boss is no longer part of the ChristmasFeatures setup. If an earlier model bundle was already installed on the server, remove its ChristmasFrostEmperor mob definition from plugins/MythicMobs/Mobs/Christmas/ and its random-spawn entry from plugins/MythicMobs/RandomSpawns/ChristmasMountains.yml, then run /mm reload. If one is currently spawned, remove the live entity through MythicMobs before or during maintenance. This external MythicMobs data cannot be deleted by ChristmasFeatures itself.

## Preserve actual mountains

If ChristmasSeason is used for winter biome conversion, do not convert the mountain biome IDs used by the Yeti random-spawn configuration to SNOWY_PLAINS. Keep the mountain range excluded from biome conversion or update the Yeti's biome filters after checking them in-game.
