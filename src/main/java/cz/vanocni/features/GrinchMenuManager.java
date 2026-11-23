package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Grinch interaction menu and bridge to the installed player-facing tag/quest commands.
 * MythicMobs adds a scoreboard tag to the Grinch; this listener intentionally uses
 * Bukkit's entity interaction event so MythicCrucible/MythicRPG are not required.
 */
public final class GrinchMenuManager implements Listener {
    private static final String GRINCH_SCOREBOARD_TAG = "christmas_grinch";
    private static final String MODEL_ENGINE_API = "com.ticxo.modelengine.api.ModelEngineAPI";
    private static final String MODELED_ENTITY_API = "com.ticxo.modelengine.api.model.ModeledEntity";
    private static final String ACTIVE_MODEL_API = "com.ticxo.modelengine.api.model.ActiveModel";
    private static final String ANIMATION_HANDLER_API = "com.ticxo.modelengine.api.animation.handler.AnimationHandler";

    private final ChristmasFeaturesPlugin plugin;
    private boolean modelEngineApiWarningLogged;

    public GrinchMenuManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles right-clicks on the MythicMobs Grinch. The MythicMobs mob definition
     * adds the scoreboard tag on spawn, which makes the NPC easy to identify safely.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGrinchInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getRightClicked().getScoreboardTags().contains(GRINCH_SCOREBOARD_TAG)) return;

        event.setCancelled(true);
        playGreetingAnimation(event.getRightClicked().getUniqueId());
        open(event.getPlayer());
    }

    /**
     * ModelEngine remains an optional dependency. Reflection keeps ChristmasFeatures
     * loadable when ModelEngine is absent, while using its documented R4.1 API when present.
     */
    private void playGreetingAnimation(UUID entityId) {
        Plugin modelEngine = Bukkit.getPluginManager().getPlugin("ModelEngine");
        if (modelEngine == null || !modelEngine.isEnabled()) return;

        try {
            ClassLoader loader = modelEngine.getClass().getClassLoader();
            Class<?> api = Class.forName(MODEL_ENGINE_API, true, loader);
            Class<?> modeledEntityType = Class.forName(MODELED_ENTITY_API, true, loader);
            Class<?> activeModelType = Class.forName(ACTIVE_MODEL_API, true, loader);
            Class<?> animationHandlerType = Class.forName(ANIMATION_HANDLER_API, true, loader);

            Object modeledEntity = api.getMethod("getModeledEntity", UUID.class).invoke(null, entityId);
            if (modeledEntity == null) return;

            @SuppressWarnings("unchecked")
            Map<String, Object> models = (Map<String, Object>) modeledEntityType
                    .getMethod("getModels").invoke(modeledEntity);
            Object grinchModel = models.get("grinch");
            if (grinchModel == null && models.size() == 1) {
                grinchModel = models.values().iterator().next();
            }
            if (grinchModel == null) return;

            Object handler = activeModelType.getMethod("getAnimationHandler").invoke(grinchModel);
            Method playAnimation = animationHandlerType.getMethod("playAnimation",
                    String.class, double.class, double.class, double.class, boolean.class);
            Object played = playAnimation.invoke(handler, "greet", 0.12d, 0.18d, 1.0d, true);
            if (played == null) {
                plugin.getLogger().fine("ModelEngine Grinch model was found, but its 'greet' animation is missing.");
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!modelEngineApiWarningLogged) {
                modelEngineApiWarningLogged = true;
                plugin.getLogger().warning("Nepodařilo se spustit Grinchovu animaci greet přes ModelEngine API: "
                        + (exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
            }
        }
    }

    public void open(Player player) {
        GrinchMenuHolder holder = new GrinchMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27,
                RewardCommands.component("§2§lGrinch §8| §fVánoční průvodce"));
        holder.setInventory(inventory);

        ItemStack filler = named(Material.GREEN_STAINED_GLASS_PANE, "§r");
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);

        inventory.setItem(11, item(Material.WRITABLE_BOOK,
                "§a§lGrinchovy úkoly",
                List.of("§7Prohlédni si dostupné vánoční úkoly.",
                        "§eKlikni pro otevření týdenních úkolů.")));
        inventory.setItem(15, item(Material.NAME_TAG,
                "§d§lVánoční tagy",
                List.of("§7Vyber si svůj vánoční chat tag.",
                        "§eKlikni pro otevření nabídky tagů.")));
        inventory.setItem(13, item(Material.GREEN_DYE,
                "§2Grinchův vzkaz",
                List.of("§7Dárky? Ty si nechám všechny!",
                        "§8Ale možná ti nějaký úkol nabídnu.")));
        inventory.setItem(22, item(Material.BARRIER, "§cZavřít", List.of("§7Zavře toto menu.")));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GrinchMenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        switch (event.getRawSlot()) {
            case 11 -> dispatchConfiguredPage(player, "grinch.menu.quests-command",
                    "§eÚkoly zatím nejsou napojené. Správce musí nastavit grinch.menu.quests-command.");
            case 15 -> dispatchConfiguredPage(player, "grinch.menu.tags-command",
                    "§eTagy zatím nejsou napojené. Správce musí nastavit grinch.menu.tags-command.");
            case 22 -> {
                player.closeInventory();
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            }
            default -> {
                // Decorative slot.
            }
        }
    }

    private void dispatchConfiguredPage(Player player, String configPath, String missingMessage) {
        String configured = plugin.getConfig().getString(configPath, "").trim();
        if (configured.isEmpty()) {
            player.sendMessage(missingMessage);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            return;
        }

        // Player-prefixed commands open GUI commands in the clicking player's context.
        // Close the Grinch menu first so it cannot close the new SupremeTags/BattlePass GUI.
        player.closeInventory();

        if (configured.regionMatches(true, 0, "player:", 0, "player:".length())) {
            String command = configured.substring("player:".length()).trim()
                    .replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .replace("%display_name%", player.getName());
            if (command.startsWith("/")) command = command.substring(1);
            if (command.isEmpty() || !Bukkit.dispatchCommand(player, command)) {
                player.sendMessage("§cPříkaz pro tuto nabídku není dostupný. Zkontroluj konfiguraci a oprávnění.");
            }
            return;
        }

        // Commands without the player: prefix retain the console-dispatch integration.
        String command = configured.startsWith("/") ? configured.substring(1) : configured;
        RewardCommands.dispatch(plugin, List.of(command), player, Map.of());
    }

    private ItemStack named(Material material, String name) {
        return item(material, name, List.of());
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(RewardCommands.component(name));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(RewardCommands::component).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private static final class GrinchMenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }
}
