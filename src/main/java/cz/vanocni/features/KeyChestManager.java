package cz.vanocni.features;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class KeyChestManager implements Listener {
    private static final int KEY_MODEL_DATA = 12021;
    private final ChristmasFeaturesPlugin plugin;
    private final Random random = new Random();

    public KeyChestManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createKey(int amount) {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = key.getItemMeta();
        meta.displayName(RewardCommands.component("§6✦ Vánoční klíč ✦"));
        meta.lore(List.of(
                RewardCommands.component("§7Otevírá vánoční bednu na spawnu."),
                RewardCommands.component("§ePoužije se při otevření.")));
        meta.setCustomModelData(KEY_MODEL_DATA);
        meta.getPersistentDataContainer().set(plugin.keyToken(), PersistentDataType.BYTE, (byte) 1);
        key.setItemMeta(meta);
        return key;
    }

    public void giveKey(Player player, int amount) {
        ItemStack key = createKey(amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(key);
        for (ItemStack leftover : overflow.values()) player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        player.sendMessage("§aDostal jsi " + amount + "× §6Vánoční klíč§a.");
    }

    public void setChest(Player player) {
        Block target = player.getTargetBlockExact(8);
        if (target == null || !isChestBlock(target.getType())) {
            player.sendMessage("§cPodívej se na blok truhly nebo sudu a zkus příkaz znovu.");
            return;
        }
        saveChestLocation(target.getLocation());
        player.sendMessage("§aVánoční bedna byla nastavena na tomto bloku. Otevře ji pouze vánoční klíč.");
    }

    public void saveChestLocation(Location location) {
        plugin.getConfig().set("chest.enabled", true);
        plugin.getConfig().set("chest.location.world", location.getWorld().getName());
        plugin.getConfig().set("chest.location.x", location.getBlockX());
        plugin.getConfig().set("chest.location.y", location.getBlockY());
        plugin.getConfig().set("chest.location.z", location.getBlockZ());
        plugin.saveConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        // PlayerInteractEvent may fire once for each hand; only the main hand may consume a key.
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!isConfiguredChest(event.getClickedBlock())) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (plugin.getConfig().getMapList("chest.rewards").isEmpty()) {
            player.sendMessage("§cVánoční bedna nemá nastavené odměny. Kontaktuj správce.");
            return;
        }
        if (!removeOneKey(player)) {
            player.sendMessage("§cK otevření vánoční bedny potřebuješ §6Vánoční klíč§c.");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_LOCKED, 0.8f, 0.8f);
            return;
        }
        openChest(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChestGuiClick(InventoryClickEvent event) {
        if (isRewardInventory(event.getView().getTopInventory())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChestGuiDrag(InventoryDragEvent event) {
        if (isRewardInventory(event.getView().getTopInventory())) event.setCancelled(true);
    }

    private boolean isRewardInventory(Inventory inventory) {
        return inventory.getHolder() instanceof RewardInventoryHolder;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isConfiguredChest(event.getBlock())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cVánoční bednu nelze rozbít.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isConfiguredChest);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isConfiguredChest);
    }

    private boolean removeOneKey(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() != Material.TRIPWIRE_HOOK || !item.hasItemMeta()) continue;
            Byte marker = item.getItemMeta().getPersistentDataContainer().get(plugin.keyToken(), PersistentDataType.BYTE);
            if (marker == null || marker != (byte) 1) continue;
            if (item.getAmount() <= 1) player.getInventory().setItem(slot, null);
            else item.setAmount(item.getAmount() - 1);
            return true;
        }
        return false;
    }

    private void openChest(Player player) {
        List<Map<?, ?>> rewards = plugin.getConfig().getMapList("chest.rewards");
        Map<?, ?> reward = chooseReward(rewards);
        if (reward == null) {
            player.sendMessage("§cVánoční bedna nemá nastavené žádné odměny. Kontaktuj správce.");
            return;
        }

        Object displayValue = reward.get("name");
        String displayName = displayValue == null ? "§6Vánoční odměna" : String.valueOf(displayValue);
        ItemStack rewardItem = createRewardItem(reward);
        List<String> commands = readStringList(reward.get("commands"));
        RewardCommands.dispatch(plugin, commands, player, Map.of("reward", displayName));
        if (rewardItem != null) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(rewardItem);
            for (ItemStack leftover : overflow.values()) player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        RewardInventoryHolder holder = new RewardInventoryHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, RewardCommands.component("§b✦ Vánoční bedna ✦"));
        holder.setInventory(inventory);
        ItemStack filler = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        inventory.setItem(13, rewardItem == null ? createCommandRewardIcon(displayName) : rewardItem.clone());
        player.openInventory(inventory);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.9f, 1.1f);
        player.sendMessage("§aOtevřel jsi vánoční bednu a získal: " + RewardCommands.color(displayName) + "§a!");
    }

    private ItemStack createCommandRewardIcon(String name) {
        ItemStack icon = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(RewardCommands.component(name));
        meta.lore(List.of(Component.text("§7Kosmetická odměna byla přidána.")));
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createRewardItem(Map<?, ?> reward) {
        Object materialValue = reward.get("material");
        if (materialValue == null || "NONE".equalsIgnoreCase(String.valueOf(materialValue))) return null;
        Material material = Material.matchMaterial(String.valueOf(materialValue));
        if (material == null || material.isAir()) {
            plugin.getLogger().warning("Neznámý materiál v chest.rewards: " + materialValue);
            return null;
        }
        int amount = Math.max(1, Math.min(material.getMaxStackSize(), parseInt(reward.get("amount"), 1)));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        Object name = reward.get("name");
        if (name != null) meta.displayName(RewardCommands.component(String.valueOf(name)));
        List<String> lore = readStringList(reward.get("lore"));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(RewardCommands::component).toList());
        Object modelData = reward.get("model-data");
        if (modelData != null) meta.setCustomModelData(parseInt(modelData, 0));
        item.setItemMeta(meta);
        return item;
    }

    private Map<?, ?> chooseReward(List<Map<?, ?>> rewards) {
        if (rewards.isEmpty()) return null;
        int total = 0;
        for (Map<?, ?> reward : rewards) total += Math.max(0, parseInt(reward.get("weight"), 1));
        if (total <= 0) return rewards.get(0);
        int pick = random.nextInt(total);
        for (Map<?, ?> reward : rewards) {
            pick -= Math.max(0, parseInt(reward.get("weight"), 1));
            if (pick < 0) return reward;
        }
        return rewards.get(rewards.size() - 1);
    }

    private List<String> readStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) for (Object item : list) result.add(String.valueOf(item));
        return result;
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private boolean isChestBlock(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST || material == Material.BARREL;
    }

    private static final class RewardInventoryHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    private boolean isConfiguredChest(Block block) {
        if (!plugin.getConfig().getBoolean("chest.enabled", true)) return false;
        String world = plugin.getConfig().getString("chest.location.world", "");
        if (world.isBlank() || block.getWorld().getName().equalsIgnoreCase(world) == false) return false;
        return block.getX() == plugin.getConfig().getInt("chest.location.x", Integer.MIN_VALUE)
                && block.getY() == plugin.getConfig().getInt("chest.location.y", Integer.MIN_VALUE)
                && block.getZ() == plugin.getConfig().getInt("chest.location.z", Integer.MIN_VALUE);
    }
}
