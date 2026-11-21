# BetterHud integration

ChristmasFeatures has an optional direct integration with BetterHud. It registers live placeholders through BetterHud's Java API:

- `[christmas_event_active]`, `[christmas_event_name]`, `[christmas_event_score]`, `[christmas_event_time]`
- `[christmas_chatgame_active]`, `[christmas_chatgame_question]`, `[christmas_chatgame_time]`
- `[christmas_key_count]` (the player's own valid Christmas keys in their inventory)

The plugin copies its example font, layouts and HUD definition into the BetterHud data folder only when those files do not already exist. It also appends `christmas_features_hud` to BetterHud's `default-hud` list without replacing other listed HUDs.

## First startup after installing the updated ChristmasFeatures JAR

1. Confirm BetterHud is enabled and the server is running Paper/Purpur 1.21.10.
2. Replace the old ChristmasFeatures JAR with the new build and restart the server.
3. For an existing installation, set `hud.mode: BETTERHUD` in `plugins/ChristmasFeatures/config.yml`. The plugin does not overwrite an existing config.
4. Check that the integration files exist in `plugins/BetterHud/texts`, `plugins/BetterHud/layouts` and `plugins/BetterHud/huds`.
5. Check that `christmas_features_hud` appears under `default-hud` in BetterHud's config.
6. ChristmasFeatures now explicitly reloads BetterHud after installing the files, then registers its custom placeholders against the reloaded API. A manual `/hud reload` should normally not be required.
7. Rejoin and accept BetterHud's resource pack. The HUD is drawn server-side by BetterHud; players still need to accept its resource pack for the visual elements to appear.

BetterHud 1.14.0 is the dependency/API version pinned by this project and explicitly lists support for Minecraft 1.21.9/1.21.10. Keep that version for this 1.21.10 server unless a newer release explicitly supports your server version. If the API reload or registration fails, ChristmasFeatures uses `hud.fallback-mode` (BOSSBAR by default).

## Configuration

```yaml
hud:
  mode: BETTERHUD
  fallback-mode: BOSSBAR
```

Set `fallback-mode: SIDEBAR` only if no other plugin owns the scoreboard sidebar. You can set `mode: SIDEBAR` or `mode: BOSSBAR` to use the built-in HUD instead of BetterHud.
