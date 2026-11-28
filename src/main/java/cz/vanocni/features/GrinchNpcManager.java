package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

public final class GrinchNpcManager implements Listener {
    private final ChristmasFeaturesPlugin plugin;

    public GrinchNpcManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Uses the imported Blockbench model if both optional model plugins are enabled.
     * Otherwise it creates a persistent, clickable in-plugin Grinch NPC.
     */
    public void spawn(Player player) {
        boolean modelPluginsAvailable =
                Bukkit.getPluginManager().isPluginEnabled("MythicMobs")
                        && Bukkit.getPluginManager().isPluginEnabled("ModelEngine");
        if (modelPluginsAvailable) {
            boolean dispatched = Bukkit.dispatchCommand(player, "mm m spawn ChristmasGrinch");
            if (dispatched) {
                player.sendMessage("§aPříkaz pro 3D Grinche byl předán MythicMobs.");
                player.sendMessage("§7Model musí být importovaný podle docs/CHRISTMAS_MOBS_INTEGRATION.md.");
                return;
            }
        }
        spawnNative(player);
    }

    private void spawnNative(Player player) {
        ZombieVillager grinch = player.getWorld().spawn(player.getLocation(), ZombieVillager.class);
        grinch.setAdult();
        grinch.setAI(false);
        grinch.setSilent(true);
        grinch.setInvulnerable(true);
        grinch.setPersistent(true);
        grinch.setRemoveWhenFarAway(false);
        grinch.setCanPickupItems(false);
        grinch.setCollidable(false);
        grinch.setCustomName("§2§lGRINCH §8• §fVánoční průvodce");
        grinch.setCustomNameVisible(true);
        grinch.getPersistentDataContainer().set(plugin.grinchNpcToken(), PersistentDataType.BYTE, (byte) 1);

        EntityEquipment equipment = grinch.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(leather(Material.LEATHER_HELMET, Color.fromRGB(28, 92, 38), "§2Grinchova kapuce"));
            equipment.setChestplate(leather(Material.LEATHER_CHESTPLATE, Color.fromRGB(37, 132, 52), "§aGrinchův kabátek"));
            equipment.setLeggings(leather(Material.LEATHER_LEGGINGS, Color.fromRGB(24, 78, 35), "§2Zelené kalhoty"));
            equipment.setBoots(leather(Material.LEATHER_BOOTS, Color.fromRGB(91, 48, 34), "§6Kožené boty"));
            equipment.setItemInMainHand(named(Material.CHEST, "§cGrinchův dárek"));
            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
            equipment.setItemInMainHandDropChance(0.0f);
        }

        grinch.getWorld().spawnParticle(Particle.SNOWFLAKE,
                grinch.getLocation().add(0, 1.0, 0), 24, 0.45, 0.65, 0.45, 0.02);
        grinch.getWorld().playSound(grinch.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.55f, 1.5f);
        player.sendMessage("§aGrinch byl umístěn přesně na tvou pozici.");
        player.sendMessage("§7Pravým kliknutím otevřeš jeho vánoční menu.");
        player.sendMessage("§8Pro importovaný 3D model zapni ModelEngine + MythicMobs a nahraj modelový balíček.");
    }

    private ItemStack leather(Material material, Color color, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof LeatherArmorMeta leather) leather.setColor(color);
        meta.displayName(RewardCommands.component(name));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack named(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(RewardCommands.component(name));
        stack.setItemMeta(meta);
        return stack;
    }

    private boolean isNativeGrinch(Entity entity) {
        return entity.getPersistentDataContainer().has(plugin.grinchNpcToken(), PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!isNativeGrinch(event.getRightClicked())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.getWorld().playSound(event.getRightClicked().getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.25f);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE,
                event.getRightClicked().getLocation().add(0, 1.5, 0), 8, 0.25, 0.25, 0.25, 0.01);
        plugin.grinchMenu().open(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (isNativeGrinch(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (isNativeGrinch(event.getEntity())) event.setCancelled(true);
    }
}
