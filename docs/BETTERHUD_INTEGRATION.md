# BetterHud integration

ChristmasFeatures has an optional direct integration with BetterHud. It registers the following live placeholders through BetterHud's Java API:

- `[christmas_event_active]`, `[christmas_event_name]`, `[christmas_event_score]`, `[christmas_event_time]`
- `[christmas_chatgame_active]`, `[christmas_chatgame_question]`, `[christmas_chatgame_time]`

The plugin copies its example font, layouts and HUD definition into the BetterHud data folder only when those files do not already exist. It also appends `christmas_features_hud` to BetterHud's `default-hud` list without replacing other listed HUDs. Your other BetterHud settings and custom files are not overwritten.

## First startup after installing the updated ChristmasFeatures JAR

1. Confirm BetterHud is enabled and the server is running Paper/Purpur 1.21.10.
2. Replace the old ChristmasFeatures JAR with the new build and restart the server.
3. If this is an existing ChristmasFeatures installation, open `plugins/ChristmasFeatures/config.yml` and set `hud.mode: BETTERHUD`. `saveDefaultConfig()` does not overwrite an existing config, so the new default is applied automatically only on a fresh installation.
4. Check that these files were created:
   - `plugins/BetterHud/texts/christmas-font.yml`
   - `plugins/BetterHud/layouts/christmas-event-layout.yml`
   - `plugins/BetterHud/huds/christmas-event-hud.yml`
5. Check that `christmas_features_hud` appears in `default-hud` in `plugins/BetterHud/config.yml`.
6. Run `/hud reload`, then rejoin and accept the resource pack if BetterHud asks for it.

BetterHud must be installed and enabled before ChristmasFeatures starts (the plugin declares it as a soft dependency). If the BetterHud API cannot be loaded, ChristmasFeatures falls back to the configured `hud.fallback-mode` (SIDEBAR by default).

## Configuration

The ChristmasFeatures default is:

```yaml
hud:
  mode: BETTERHUD
  fallback-mode: SIDEBAR
```

Set `fallback-mode: BOSSBAR` if another plugin already uses the scoreboard sidebar. Set `mode: SIDEBAR` or `mode: BOSSBAR` to use the built-in HUD instead of BetterHud.

The example HUD uses BetterHud's built-in Minecraft font. It does not add custom image files or require a client-side mod. BetterHud still needs its own resource pack accepted by each player for its HUD rendering.
