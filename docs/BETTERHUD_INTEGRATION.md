# BetterHud integration

ChristmasFeatures has an optional direct integration with BetterHud. It registers live placeholders through BetterHud's Java API:

- `[christmas_event_active]`, `[christmas_event_name]`, `[christmas_event_score]`, `[christmas_event_time]`
- `[christmas_chatgame_active]`, `[christmas_chatgame_question]`, `[christmas_chatgame_time]`
- `[christmas_key_count]` (the player's own valid Christmas keys in their inventory)

## Safe upgrade behavior

The integration installs versioned files (`christmas-font-v2.yml`, `christmas-layout-v2.yml`, `christmas-hud-v2.yml`) so later plugin updates do not overwrite existing custom files. On startup it removes only the previous ChristmasFeatures HUD ID (`christmas_features_hud`) from BetterHud's `default-hud` list and adds `christmas_features_hud_v2`. Other HUD entries and BetterHud settings are retained.

## First startup after installing the updated ChristmasFeatures JAR

1. Confirm BetterHud is enabled and the server is running Paper/Purpur 1.21.10.
2. Replace the old ChristmasFeatures JAR with the new build and perform a full server restart.
3. For an existing installation, make sure `hud.mode: BETTERHUD` is set in `plugins/ChristmasFeatures/config.yml`. The plugin does not overwrite an existing config.
4. Check that the three versioned files were created in `plugins/BetterHud/texts`, `plugins/BetterHud/layouts` and `plugins/BetterHud/huds`.
5. Check that `christmas_features_hud_v2` appears under `default-hud` in BetterHud's config. The old ChristmasFeatures HUD entry is migrated automatically; unrelated HUDs stay enabled.
6. ChristmasFeatures reloads BetterHud after creating its files and before registering its custom placeholders.
7. Rejoin and accept BetterHud's resource pack. The HUD is server-side, but its display still depends on the generated BetterHud resource pack being accepted by each client.

BetterHud 1.14.0 is the dependency/API version pinned by this project and explicitly lists Minecraft 1.21.9/1.21.10 support. Keep a release compatible with 1.21.10. If the BetterHud API reload or registration fails, ChristmasFeatures uses `hud.fallback-mode` (BOSSBAR by default).

## Configuration

```yaml
hud:
  mode: BETTERHUD
  fallback-mode: BOSSBAR
```

Set `fallback-mode: SIDEBAR` only if no other plugin owns the scoreboard sidebar. You can set `mode: SIDEBAR` or `mode: BOSSBAR` to use the built-in HUD instead of BetterHud.
