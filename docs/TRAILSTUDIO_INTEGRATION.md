# TrailStudio integration

## Reviewed file

The uploaded TrailStudio.jar is **TrailStudio 1.0.3** (PixelRPG). Its plugin.yml declares api-version 1.13, and its class files target Java 8 bytecode. The JAR contains its own config.yml and effects.yml; no hard plugin dependencies are declared.

The supplied catalogue contains **84 presets in seven collections**: trail, aura, wings, rain, footsteps, projectile and burst.

It is a particle-only cosmetic plugin. It does not create real blocks, items, mobs or client-side 3D wings, and its effects do not require a resource pack.

The uploaded 1.0.3 JAR is not the latest public listing we found: the Spigot resource currently lists **Trail Studio 1.1** and states that 1.21 is among its tested major versions. For production, prefer the author's latest build after checking its download page and changelog. This repository does not redistribute the third-party JAR.

## Winter effects already included in the uploaded JAR

| Effect ID | Category | Appearance | Default access |
|---|---|---|---|
| trail-frost-stream | trail | Snowflake wave | Free |
| rain-snow-globe | rain | Falling snowflakes | Free |
| footsteps-snow-steps | footsteps | Snowflake footsteps | Free |
| projectile-snowball-spark | projectile | Snowball comet | Free |
| trail-aurora-braid | trail | Coloured aurora braid | Permission |
| wings-ice-dragon | wings | Ice-dragon wing outline | Permission |
| burst-frost-flower | burst | Frost-flower celebration | Permission |
| aura-astral-star | aura | Star aura | Permission |

The first four are already available to players who have trailstudio.use. The remaining four are used as permanent Christmas reward unlocks by ChristmasFeatures.

## How the integration works

ChristmasFeatures declares TrailStudio as a soft dependency. When TrailStudio is installed, ChristmasFeatures checks that the effect ID actually exists and is enabled in plugins/TrailStudio/effects.yml.

On a reward, ChristmasFeatures:

1. Saves the unlocked effect ID in plugins/ChristmasFeatures/trail-unlocks.yml.
2. Adds the matching Bukkit permission node trailstudio.effect.<effect-id> to that player using a PermissionAttachment.
3. Re-applies saved unlocks on player join and when TrailStudio is enabled dynamically.

This does **not** require LuckPerms. The Bukkit permission attachment works with the Player.hasPermission check used by the supplied TrailStudio build. LuckPerms can still be used separately for ordinary rank permissions.

If TrailStudio is not installed yet, unlock IDs are saved and can be applied after the plugin is installed and the server restarted. Unknown or disabled effect IDs are rejected when TrailStudio is present.

## ChristmasFeatures reward mapping

- Winning the gift-hunt event unlocks trail-aurora-braid.
- Winning the ornament-hunt event unlocks burst-frost-flower.
- Winning a Christmas ChatGame unlocks wings-ice-dragon.
- The Christmas key chest has a low-weight cosmetic reward that unlocks aura-astral-star.

The chest's weight 3 is a relative weight, not a 3% fixed chance. With the default reward weights 45, 30, 15 and 3, its chance is 3/93 (about 3.23%).

## Commands

- /trails — open the player's TrailStudio menu.
- /trails list trail — list trail presets.
- /trails equip <effect-id> — equip an unlocked effect.
- /christmas trail give <player> <effect-id> — admin command to unlock an effect for an online player.

The player can equip the cosmetic themselves in TrailStudio. The reward does not forcibly replace the player's currently equipped trail/loadout.

## Performance and compatibility notes

The uploaded TrailStudio config already has particle budgets (max-particles-per-frame, max-particles-per-viewer), a maximum of three wearable layers, viewer limits and a 24-block view distance. Keep those limits enabled. If the server has many players, lower the budgets or view distance in TrailStudio's own config instead of increasing them.

Do not rename the TrailStudio plugin, its command, or the built-in effect IDs. Install it as a separate third-party JAR in plugins/; do not put the JAR inside ChristmasFeatures or its GitHub repository.
