package cz.vanocni.features;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChristmasFeaturesPlugin extends JavaPlugin {
    private NamespacedKey eventItemToken;
    private NamespacedKey eventItemUniqueToken;
    private NamespacedKey winterAppliedToken;

    private WinterManager winterManager;
    private HolidayEventManager eventManager;
    private ChatGameManager chatGameManager;
    private ActivityHud activityHud;
    private TrailStudioBridge trailStudioBridge;
    private BetterHudBridge betterHudBridge;
    private GrinchMenuManager grinchMenuManager;
    private CosmeticsManager cosmeticsManager;
    private ChristmasPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getConfig().contains("chest")) {
            getConfig().set("chest", null);
        }
        if (getConfig().contains("bosses.illusioner")) {
            getConfig().set("bosses.illusioner", null);
        }
        // Migrate old installations whose Grinch tag hook was left blank.
        if (getConfig().getString("grinch.menu.tags-command", "").isBlank()) {
            getConfig().set("grinch.menu.tags-command", "christmas cosmetics %player% tag");
        }
        saveConfig();

        eventItemToken = new NamespacedKey(this, "christmas_event_item");
        eventItemUniqueToken = new NamespacedKey(this, "christmas_event_item_id");
        winterAppliedToken = new NamespacedKey(this, "winter_biome_applied");

        winterManager = new WinterManager(this);
        eventManager = new HolidayEventManager(this);
        chatGameManager = new ChatGameManager(this);
        activityHud = new ActivityHud(this, eventManager, chatGameManager);
        trailStudioBridge = new TrailStudioBridge(this);
        betterHudBridge = new BetterHudBridge(this);
        grinchMenuManager = new GrinchMenuManager(this);
        cosmeticsManager = new CosmeticsManager(this);

        getServer().getPluginManager().registerEvents(winterManager, this);
        getServer().getPluginManager().registerEvents(eventManager, this);
        getServer().getPluginManager().registerEvents(chatGameManager, this);
        getServer().getPluginManager().registerEvents(activityHud, this);
        getServer().getPluginManager().registerEvents(trailStudioBridge, this);
        getServer().getPluginManager().registerEvents(grinchMenuManager, this);
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

    public CosmeticsManager cosmetics() {
        return cosmeticsManager;
    }

}
