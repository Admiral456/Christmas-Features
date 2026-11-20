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
5. Check that `christmas_features_hud` appears in `default-hud` in `plugins/BetterHud/config.yml`. ChristmasFeatures inserts it without replacing existing HUD entries.
6. ChristmasFeatures automatically calls the BetterHud API reload after installing its files, so a manual `/hud reload` should not be necessary. If startup logs report a BetterHud reload error, run `/hud reload` once and restart the server.
7. Rejoin and accept BetterHud's resource pack when prompted. The HUD is rendered through BetterHud and depends on its resource pack being accepted by the client.

BetterHud 1.14.0 is the API baseline pinned by this project and explicitly supports Minecraft 1.21.9/1.21.10. Do not replace it with BetterHud 2.0.0 on a Java 21 / Minecraft 1.21.10 server: the 2.0.0 release targets Minecraft 26.1.x and Java 25. BetterHud must be installed and enabled before ChristmasFeatures starts (the plugin declares it as a soft dependency). If its API or automatic reload cannot be initialized, ChristmasFeatures falls back to the configured `hud.fallback-mode` (SIDEBAR by default).

## Configuration

The ChristmasFeatures default is:

```yaml
hud:
  mode: BETTERHUD
  fallback-mode: SIDEBAR
```

Set `fallback-mode: BOSSBAR` if another plugin already uses the scoreboard sidebar. Set `mode: SIDEBAR` or `mode: BOSSBAR` to use the built-in HUD instead of BetterHud.

The example HUD uses BetterHud's built-in Minecraft font. It does not add custom image files or require a client-side mod. BetterHud still needs its own resource pack accepted by each player for its HUD rendering.
