package cz.vanocni.features;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ChristmasFeaturesPlugin extends JavaPlugin {
    private static final List<String> DEFAULT_PRESERVED_WINTER_BIOMES = List.of(
            "MEADOW",
            "GROVE",
            "SNOWY_SLOPES",
            "FROZEN_PEAKS",
            "JAGGED_PEAKS",
            "STONY_PEAKS",
            "WINDSWEPT_HILLS",
            "WINDSWEPT_GRAVELLY_HILLS",
            "WINDSWEPT_FOREST",
            "WINDSWEPT_SAVANNA"
    );

    private NamespacedKey eventItemToken;
    private NamespacedKey eventItemUniqueToken;
    private NamespacedKey winterAppliedToken;
    private NamespacedKey grinchNpcToken;

    private WinterManager winterManager;
    private HolidayEventManager eventManager;
    private ChatGameManager chatGameManager;
    private ActivityHud activityHud;
    private TrailStudioBridge trailStudioBridge;
    private BetterHudBridge betterHudBridge;
    private GrinchMenuManager grinchMenuManager;
    private GrinchNpcManager grinchNpcManager;
    private CosmeticsManager cosmeticsManager;
    private ChristmasPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!validateMainConfig()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        migrateConfig();

        eventItemToken = new NamespacedKey(this, "christmas_event_item");
        eventItemUniqueToken = new NamespacedKey(this, "christmas_event_item_id");
        winterAppliedToken = new NamespacedKey(this, "winter_biome_applied");
        grinchNpcToken = new NamespacedKey(this, "grinch_npc");

        winterManager = new WinterManager(this);
        eventManager = new HolidayEventManager(this);
        chatGameManager = new ChatGameManager(this);
        activityHud = new ActivityHud(this, eventManager, chatGameManager);
        trailStudioBridge = new TrailStudioBridge(this);
        betterHudBridge = new BetterHudBridge(this);
        grinchMenuManager = new GrinchMenuManager(this);
        grinchNpcManager = new GrinchNpcManager(this);
        cosmeticsManager = new CosmeticsManager(this);

        getServer().getPluginManager().registerEvents(winterManager, this);
        getServer().getPluginManager().registerEvents(eventManager, this);
        getServer().getPluginManager().registerEvents(chatGameManager, this);
        getServer().getPluginManager().registerEvents(activityHud, this);
        getServer().getPluginManager().registerEvents(trailStudioBridge, this);
        getServer().getPluginManager().registerEvents(grinchMenuManager, this);
        getServer().getPluginManager().registerEvents(grinchNpcManager, this);
        getServer().getPluginManager().registerEvents(cosmeticsManager, this);

        ChristmasCommand command = new ChristmasCommand(this);
        if (getCommand("christmas") != null) {
            getCommand("christmas").setExecutor(command);
            getCommand("christmas").setTabCompleter(command);
        }

        winterManager.start();
        eventManager.startScheduler();
        chatGameManager.startScheduler();
        cosmeticsManager.start();
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            getLogger().info("LuckPerms detected: VIP cosmetics require " + CosmeticsManager.VIP_PERMISSION + ".");
        } else {
            getLogger().info("LuckPerms not detected; VIP cosmetics still use Bukkit permissions and can be managed by another permissions plugin.");
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new ChristmasPlaceholderExpansion(this);
            if (!placeholderExpansion.register()) {
                getLogger().warning("Christmas PlaceholderAPI expansion se nepodařilo zaregistrovat.");
            }
        }
        betterHudBridge.start();
        activityHud.start();
        getLogger().info("ChristmasFeatures has been enabled for Paper 1.21.10.");
    }

    /**
     * Validate the server-owned config before any migration can write to it.
     * A malformed YAML file is backed up and the plugin fails closed.
     */
    private boolean validateMainConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        try {
            new YamlConfiguration().load(configFile);
            return true;
        } catch (IOException | InvalidConfigurationException exception) {
            File backup = backupInvalidYaml(configFile);
            String backupInfo = backup == null
                    ? " Zálohu se nepodařilo vytvořit; původní soubor zůstal nedotčen."
                    : " Záloha: " + backup.getName();
            getLogger().severe("Soubor config.yml je neplatný YAML: " + exception.getMessage()
                    + "." + backupInfo + " ChristmasFeatures se vypíná, aby konfiguraci nepřepsal.");
            return false;
        }
    }

    /**
     * Copies a malformed plugin-owned YAML file without replacing the original.
     * A null result means the file was missing or the backup could not be created.
     */
    public File backupInvalidYaml(File source) {
        if (source == null || !source.isFile()) return null;
        String suffix = System.currentTimeMillis() + "-" + Long.toHexString(System.nanoTime());
        File backup = new File(source.getParentFile(),
                source.getName() + ".corrupt-" + suffix + ".bak");
        try {
            Files.copy(source.toPath(), backup.toPath());
            return backup;
        } catch (IOException exception) {
            getLogger().severe("Nepodařilo se vytvořit zálohu poškozeného souboru "
                    + source.getName() + ": " + exception.getMessage());
            return null;
        }
    }

    /**
     * Upgrade old server configs without discarding custom questions or unrelated settings.
     */
    private void migrateConfig() {
        boolean changed = false;

        if (!getConfig().contains("cosmetics.trails.interval-ticks")) {
            getConfig().set("cosmetics.trails.interval-ticks", 4);
            changed = true;
        }

        if (!getConfig().contains("winter.preserve-biomes")) {
            getConfig().set("winter.preserve-biomes", DEFAULT_PRESERVED_WINTER_BIOMES);
            changed = true;
        }

        if (getConfig().getInt("events.gifts.spawn-count", 18) == 18) {
            getConfig().set("events.gifts.spawn-count", 10);
            changed = true;
        }
        if (getConfig().getInt("events.gifts.duration-seconds", 300) == 300) {
            getConfig().set("events.gifts.duration-seconds", 600);
            changed = true;
        }
        if (getConfig().getInt("events.gifts.radius", 32) == 32) {
            getConfig().set("events.gifts.radius", 45);
            changed = true;
        }
        if (getConfig().getInt("events.gifts.target-score", 5) == 5) {
            getConfig().set("events.gifts.target-score", 10);
            changed = true;
        }

        if (getConfig().getStringList("cosmetics.events.gifts.first-place-commands")
                .equals(List.of("give %player% diamond 3"))) {
            getConfig().set("cosmetics.events.gifts.first-place-commands", List.of("give %player% diamond 10"));
            changed = true;
        }
        if (getConfig().getStringList("cosmetics.chatgames.winner-commands")
                .equals(List.of("give %player% emerald 3"))) {
            getConfig().set("cosmetics.chatgames.winner-commands",
                    List.of("give %player% emerald 3", "experience add %player% 12 points"));
            changed = true;
        }
        if (!getConfig().contains("cosmetics.chatgames.vip-winner-commands")) {
            getConfig().set("cosmetics.chatgames.vip-winner-commands",
                    List.of("give %player% emerald 2", "give %player% gold_ingot 2"));
            changed = true;
        }
        if (!getConfig().contains("cosmetics.chatgames.vip-trailstudio-effect")) {
            getConfig().set("cosmetics.chatgames.vip-trailstudio-effect", "aura-astral-star");
            changed = true;
        }

        if (!getConfig().contains("grinch.menu.gift-start-cooldown-seconds")) {
            getConfig().set("grinch.menu.gift-start-cooldown-seconds", 1800);
            changed = true;
        }
        if (!getConfig().contains("grinch.menu.chatgame-start-cooldown-seconds")) {
            getConfig().set("grinch.menu.chatgame-start-cooldown-seconds", 900);
            changed = true;
        }
        if (getConfig().getString("grinch.menu.tags-command", "").isBlank()) {
            getConfig().set("grinch.menu.tags-command", "christmas cosmetics %player% tag");
            changed = true;
        }

        List<String> retainedLegacyPaths = List.of(
                "bosses.frost_emperor",
                "bosses.winter_boss",
                "bosses.illusioner",
                "chest"
        ).stream().filter(getConfig()::contains).toList();
        if (!retainedLegacyPaths.isEmpty()) {
            getLogger().warning("Nalezeny starší konfigurační sekce, které aktuální verze nepoužívá: "
                    + String.join(", ", retainedLegacyPaths)
                    + ". Hodnoty byly ponechány beze změny; odstraň je pouze po ruční kontrole a záloze.");
        }

        InputStream defaultsStream = getResource("config.yml");
        if (defaultsStream != null) {
            try (Reader reader = new InputStreamReader(defaultsStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                List<Map<String, Object>> merged = new ArrayList<>();
                Set<String> questions = new HashSet<>();

                for (Map<?, ?> task : getConfig().getMapList("chatgames.tasks")) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : task.entrySet()) {
                        copy.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    merged.add(copy);
                    Object question = task.get("question");
                    if (question != null) questions.add(String.valueOf(question).trim().toLowerCase(Locale.ROOT));
                }

                for (Map<?, ?> task : defaults.getMapList("chatgames.tasks")) {
                    Object question = task.get("question");
                    if (question == null) continue;
                    String key = String.valueOf(question).trim().toLowerCase(Locale.ROOT);
                    if (!questions.add(key)) continue;
                    Map<String, Object> copy = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : task.entrySet()) {
                        copy.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    merged.add(copy);
                }

                if (merged.size() != getConfig().getMapList("chatgames.tasks").size()) {
                    getConfig().set("chatgames.tasks", merged);
                    changed = true;
                }
            } catch (Exception exception) {
                getLogger().warning("Nové ChatGames úkoly se nepodařilo sloučit do konfigurace: "
                        + exception.getMessage());
            }
        }

        if (changed) {
            saveConfig();
            getLogger().info("Konfigurace byla aktualizována na nové nastavení ChristmasFeatures.");
        }
    }

    @Override
    public void onDisable() {
        if (eventManager != null) eventManager.stop("Plugin vypnut.", false);
        if (chatGameManager != null) chatGameManager.stop("Plugin vypnut.", false);
        if (activityHud != null) activityHud.shutdown();
        if (betterHudBridge != null) betterHudBridge.shutdown();
        if (winterManager != null) winterManager.shutdown();
        if (trailStudioBridge != null) trailStudioBridge.shutdown();
        if (cosmeticsManager != null) cosmeticsManager.shutdown();
        if (placeholderExpansion != null && placeholderExpansion.isRegistered()) placeholderExpansion.unregister();
    }

    public NamespacedKey eventItemToken() {
        return eventItemToken;
    }

    public NamespacedKey eventItemUniqueToken() {
        return eventItemUniqueToken;
    }

    public NamespacedKey winterAppliedToken() {
        return winterAppliedToken;
    }

    public NamespacedKey grinchNpcToken() {
        return grinchNpcToken;
    }

    public WinterManager winter() {
        return winterManager;
    }

    public HolidayEventManager events() {
        return eventManager;
    }

    public ChatGameManager chatGames() {
        return chatGameManager;
    }

    public TrailStudioBridge trailStudio() {
        return trailStudioBridge;
    }

    public BetterHudBridge betterHud() {
        return betterHudBridge;
    }

    public GrinchMenuManager grinchMenu() {
        return grinchMenuManager;
    }

    public GrinchNpcManager grinchNpc() {
        return grinchNpcManager;
    }

    public CosmeticsManager cosmetics() {
        return cosmeticsManager;
    }
}
