package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public final class GrinchMenuManager implements Listener {
    private final ChristmasFeaturesPlugin plugin;
    private long nextGiftStartAtMillis;
    private long nextChatGameStartAtMillis;

    public GrinchMenuManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GrinchMenuHolder holder = new GrinchMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 45,
                RewardCommands.component("§2§lGRINCH §8| §fVánoční průvodce"));
        holder.setInventory(inventory);
        decorate(inventory);

        inventory.setItem(11, giftItem(player));
        inventory.setItem(15, chatGameItem());
        inventory.setItem(22, item(Material.NETHER_STAR, "§6§lGrinchův stánek",
                List.of("§7Vítej ve vánočním koutku!", "§7Najdi dárky, vyhraj ChatGame", "§7a odemkni si zimní kosmetiku.")));
        inventory.setItem(29, item(Material.NAME_TAG, "§d§lVánoční chat tagy",
                List.of("§7Vyber si vlastní vánoční označení.", "§eKlikni pro otevření nabídky.")));
        inventory.setItem(31, item(Material.FIREWORK_STAR, "§b§lTrail efekty",
                List.of("§7Světelné stopy, polární záře", "§7a další částicové efekty.", "§eKlikni pro výběr.")));
        inventory.setItem(33, questItem());
        inventory.setItem(40, item(Material.BARRIER, "§cZavřít", List.of("§7Zavře vánoční menu.")));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }

    private void decorate(Inventory inventory) {
        ItemStack center = named(Material.GRAY_STAINED_GLASS_PANE, "§r", List.of());
        ItemStack green = named(Material.GREEN_STAINED_GLASS_PANE, "§r", List.of());
        ItemStack red = named(Material.RED_STAINED_GLASS_PANE, "§r", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            boolean edge = row == 0 || row == 4 || column == 0 || column == 8;
            inventory.setItem(slot, !edge ? center : ((slot + row) % 2 == 0 ? green : red));
        }
    }

    private ItemStack giftItem(Player player) {
        int count = Math.max(1, plugin.getConfig().getInt("events.gifts.spawn-count", 10));
        int duration = Math.max(30, plugin.getConfig().getInt("events.gifts.duration-seconds", 600));
        int radius = Math.max(5, plugin.getConfig().getInt("events.gifts.radius", 45));
        String effect = plugin.getConfig().getString("cosmetics.events.gifts.trailstudio-effect", "trail-aurora-braid");
        boolean unlocked = plugin.trailStudio().unlocked(player.getUniqueId()).contains(effect);

        String eventStatus;
        if (!plugin.events().isActive()) {
            eventStatus = "§aEvent je připravený ke spuštění";
        } else if (plugin.events().hudType().equalsIgnoreCase("Hledání dárků")) {
            eventStatus = "§eHledání právě probíhá • " + TimeFormat.mmss(plugin.events().remainingSeconds());
        } else {
            eventStatus = "§cPrávě probíhá jiný vánoční event";
        }

        return item(Material.CHEST, "§c§lHledání vánočních dárků",
                List.of(
                        "§7Po okolí se ukryje §f" + count + "§7 dárků.",
                        "§7Časový limit: §f" + TimeFormat.mmss(duration),
                        "§7Poloměr oblasti: §f" + radius + " bloků",
                        " ",
                        "§6Výhra: §f10 diamantů",
                        "§bOdemčení: §fPolární stuha",
                        unlocked ? "§a✓ Kosmetika už je odemčená" : "§c○ Kosmetika zatím není odemčená",
                        " ",
                        eventStatus,
                        "§eKlikni pro zahájení hledání."
                ));
    }

    private ItemStack chatGameItem() {
        String status = plugin.chatGames().isActive()
                ? "§eChatGame právě probíhá • " + TimeFormat.mmss(plugin.chatGames().remainingSeconds())
                : "§aDalší úkol může začít";
        return item(Material.ENCHANTED_BOOK, "§d§lVánoční ChatGames",
                List.of(
                        "§7Rychlé otázky, hádanky a počítání.",
                        "§7Odpověz první do veřejného chatu.",
                        " ",
                        "§aBěžný hráč: §f3 smaragdy + 12 XP",
                        "§6VIP bonus: §f2 smaragdy + 2 zlaté ingoty",
                        "§6VIP navíc: §fHvězdná aura",
                        " ",
                        status,
                        "§eKlikni pro spuštění úkolu."
                ));
    }

    private ItemStack questItem() {
        String command = plugin.getConfig().getString("grinch.menu.quests-command", "").trim();
        if (command.isEmpty()) {
            return item(Material.WRITABLE_BOOK, "§a§lGrinchovy úkoly",
                    List.of("§7Tato stránka čeká na napojení", "§7externího quest pluginu.", "§8Zatím není nakonfigurovaná."));
        }
        return item(Material.WRITABLE_BOOK, "§a§lGrinchovy úkoly",
                List.of("§7Otevře nabídku vánočních úkolů.", "§eKlikni pro otevření."));
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GrinchMenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        switch (event.getRawSlot()) {
            case 11 -> startGiftHunt(player);
            case 15 -> startChatGame(player);
            case 29 -> dispatchConfiguredPage(player, "grinch.menu.tags-command",
                    "§eVánoční tagy zatím nejsou nakonfigurované.");
            case 31 -> {
                player.closeInventory();
                plugin.cosmetics().openCategory(player, "trail");
            }
            case 33 -> dispatchConfiguredPage(player, "grinch.menu.quests-command",
                    "§eGrinchovy úkoly zatím nejsou napojené na quest plugin.");
            case 40 -> {
                player.closeInventory();
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            }
            default -> { }
        }
    }

    private void startGiftHunt(Player player) {
        if (plugin.events().isActive()) {
            player.sendMessage("§cTeď už probíhá jiný vánoční event. Zkus to později.");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, plugin.getConfig().getLong(
                "grinch.menu.gift-start-cooldown-seconds", 1800L)) * 1000L;
        if (!player.hasPermission("christmas.admin") && now < nextGiftStartAtMillis) {
            long remaining = (nextGiftStartAtMillis - now + 999L) / 1000L;
            player.sendMessage("§eGrinch si musí připravit další hledání. Zkus to za §f"
                    + TimeFormat.mmss(remaining) + "§e.");
            return;
        }
        if (plugin.events().start("gifts")) {
            nextGiftStartAtMillis = now + cooldown;
            player.closeInventory();
            player.sendMessage("§aGrinch zahájil hledání dárků! Vydej se hledat všech 10 balíčků.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        } else {
            player.sendMessage("§cHledání se nepodařilo spustit. Zkontroluj svět a volná místa kolem spawnu.");
        }
    }

    private void startChatGame(Player player) {
        if (plugin.chatGames().isActive()) {
            player.sendMessage("§eChatGame už právě probíhá. Odpověz na otázku v chatu!");
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, plugin.getConfig().getLong(
                "grinch.menu.chatgame-start-cooldown-seconds", 900L)) * 1000L;
        if (!player.hasPermission("christmas.admin") && now < nextChatGameStartAtMillis) {
            long remaining = (nextChatGameStartAtMillis - now + 999L) / 1000L;
            player.sendMessage("§eDalší úkol může Grinch vyhlásit za §f"
                    + TimeFormat.mmss(remaining) + "§e.");
            return;
        }
        if (plugin.chatGames().start()) {
            nextChatGameStartAtMillis = now + cooldown;
            player.closeInventory();
        } else {
            player.sendMessage("§cChatGame nelze spustit. Správce musí ověřit seznam úkolů.");
        }
    }

    private void dispatchConfiguredPage(Player player, String configPath, String missingMessage) {
        String command = plugin.getConfig().getString(configPath, "").trim();
        if (command.isEmpty()) {
            player.sendMessage(missingMessage);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            return;
        }
        if (command.startsWith("/")) command = command.substring(1);
        RewardCommands.dispatch(plugin, List.of(command), player, Map.of());
        player.closeInventory();
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(RewardCommands.component(name));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(RewardCommands::component).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        return item(material, name, lore);
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
