package cz.vanocni.features;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChristmasFeaturesPlugin extends JavaPlugin {
    private NamespacedKey keyToken;
    private NamespacedKey eventItemToken;
    private NamespacedKey winterAppliedToken;

    private WinterManager winterManager;
    private HolidayEventManager eventManager;
    private ChatGameManager chatGameManager;
    private KeyChestManager keyChestManager;
    private ActivityHud activityHud;
    private TrailStudioBridge trailStudioBridge;
    private BetterHudBridge betterHudBridge;
    private GrinchMenuManager grinchMenuManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        keyToken = new NamespacedKey(this, "christmas_key");
        eventItemToken = new NamespacedKey(this, "christmas_event_item");
        winterAppliedToken = new NamespacedKey(this, "winter_biome_applied");

        winterManager = new WinterManager(this);
        eventManager = new HolidayEventManager(this);
        chatGameManager = new ChatGameManager(this);
        keyChestManager = new KeyChestManager(this);
        activityHud = new ActivityHud(this, eventManager, chatGameManager);
        trailStudioBridge = new TrailStudioBridge(this);
        betterHudBridge = new BetterHudBridge(this);
        grinchMenuManager = new GrinchMenuManager(this);

        getServer().getPluginManager().registerEvents(winterManager, this);
        getServer().getPluginManager().registerEvents(eventManager, this);
        getServer().getPluginManager().registerEvents(chatGameManager, this);
        getServer().getPluginManager().registerEvents(keyChestManager, this);
        getServer().getPluginManager().registerEvents(activityHud, this);
        getServer().getPluginManager().registerEvents(trailStudioBridge, this);
        getServer().getPluginManager().registerEvents(grinchMenuManager, this);

        ChristmasCommand command = new ChristmasCommand(this);
        if (getCommand("christmas") != null) {
            getCommand("christmas").setExecutor(command);
            getCommand("christmas").setTabCompleter(command);
        }

        winterManager.start();
        eventManager.startScheduler();
        chatGameManager.startScheduler();
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
    }

    public NamespacedKey keyToken() {
        return keyToken;
    }

    public NamespacedKey eventItemToken() {
        return eventItemToken;
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

    public KeyChestManager keyChest() {
        return keyChestManager;
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
}
