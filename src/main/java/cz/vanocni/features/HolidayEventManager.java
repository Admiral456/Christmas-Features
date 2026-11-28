package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class HolidayEventManager implements Listener {
    private final ChristmasFeaturesPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    private final Set<UUID> eventItems = new HashSet<>();

    private EventState active;
    private BukkitTask autoTask;
    private BukkitTask endTask;
    private BukkitTask visualTask;

    public HolidayEventManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    public void startScheduler() {
        cleanupOrphanedLoadedItems();
        scheduleAutoStart();
    }

    private void cleanupOrphanedLoadedItems() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
                    if (entity instanceof Item item && hasEventToken(item)) {
                        item.remove();
                        removed++;
                    }
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Bylo odstraněno " + removed
                    + " osiřelých vánočních eventových předmětů z načtených chunků.");
        }
    }

    private boolean hasEventToken(Item item) {
        ItemMeta meta = item.getItemStack().getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(plugin.eventItemToken(), PersistentDataType.STRING);
    }

    public void scheduleAutoStart() {
        if (autoTask != null) autoTask.cancel();
        if (!plugin.getConfig().getBoolean("events.auto-start.enabled", true)) return;
        long minutes = Math.max(1L, plugin.getConfig().getLong("events.auto-start.interval-minutes", 90L));
        autoTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (active == null && !Bukkit.getOnlinePlayers().isEmpty()) {
                start(random.nextBoolean() ? "gifts" : "ornaments");
            }
        }, minutes * 60L * 20L, minutes * 60L * 20L);
    }

    public boolean start(String requestedType) {
        if (active != null) return false;
        String type = requestedType == null ? "" : requestedType.toLowerCase(Locale.ROOT);
        if (!type.equals("gifts") && !type.equals("ornaments")) return false;

        String path = "events." + type;
        List<String> winterWorlds = plugin.getConfig().getStringList("winter.worlds");
        String defaultWorld = winterWorlds.isEmpty() ? "world" : winterWorlds.get(0);
        String worldName = plugin.getConfig().getString(path + ".world", defaultWorld);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Vánoční event nelze spustit: svět '" + worldName + "' není načten.");
            return false;
        }

        // Guard against accidental extreme values in server configuration.
        int count = Math.min(128, Math.max(1, plugin.getConfig().getInt(path + ".spawn-count", 12)));
        int radius = Math.min(128, Math.max(5, plugin.getConfig().getInt(path + ".radius", 28)));
        int duration = Math.max(30, plugin.getConfig().getInt(path + ".duration-seconds", 300));
        int target = Math.max(1, plugin.getConfig().getInt(path + ".target-score", 5));
        double minDistance = Math.max(0.0, Math.min(32.0,
                plugin.getConfig().getDouble(path + ".min-distance-between-items",
                        type.equals("gifts") ? 9.0 : 7.0)));
        Location center = resolveCenter(world, path);

        scores.clear();
        playerNames.clear();
        eventItems.clear();
        String eventId = UUID.randomUUID().toString();
        long endsAtMillis = System.currentTimeMillis() + duration * 1000L;
        active = new EventState(type, eventId, endsAtMillis, target, world);

        int spawned = 0;
        Set<Long> usedColumns = new HashSet<>();
        List<Location> placedLocations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Location location = findSafeLocation(world, center, radius, usedColumns, placedLocations, minDistance);
            if (location == null) continue;
            placedLocations.add(location);
            ItemStack stack = createEventItem(type, eventId);
            Item item = world.dropItem(location, stack);
            item.setPickupDelay(0);
            item.setGlowing(true);
            item.setInvulnerable(true);
            item.setUnlimitedLifetime(true);
            item.setCanMobPickup(false);
            eventItems.add(item.getUniqueId());
            spawned++;
        }

        if (spawned == 0) {
            active = null;
            plugin.getLogger().warning("Vánoční event nebyl spuštěn: nepodařilo se najít bezpečné místo pro žádný předmět.");
            return false;
        }
        if (type.equals("gifts") && spawned < count) {
            for (UUID entityId : new HashSet<>(eventItems)) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(entityId);
                if (entity != null) entity.remove();
            }
            eventItems.clear();
            active = null;
            plugin.getLogger().warning("Hledání dárků nebylo spuštěno: podařilo se bezpečně rozmístit pouze "
                    + spawned + " z požadovaných " + count + " dárků. Event vyžaduje všech 10.");
            return false;
        }
        if (spawned < target) {
            plugin.getLogger().warning("Event rozmístil pouze " + spawned + " z " + count
                    + " předmětů. Výherní limit byl upraven z " + target + " na " + spawned + ".");
            target = spawned;
            active = new EventState(type, eventId, endsAtMillis, target, world);
        }

        visualTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animateEventItems, 5L, 10L);

        String label = displayName(type);
        Bukkit.broadcastMessage("§b§l✦ VÁNOČNÍ EVENT ✦ §f" + label + " právě začíná!");
        Bukkit.broadcastMessage("§7Najdi a seber co nejvíce předmětů. První hráč s §e" + target
                + "§7 kusy vyhrává. Zbývá §b" + TimeFormat.mmss(duration) + "§7.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§b❄ " + label, "§fHledej předměty po okolí!", 10, 50, 10);
        }

        endTask = Bukkit.getScheduler().runTaskLater(plugin, this::finishByScore, duration * 20L);
        return true;
    }

    private Location resolveCenter(World world, String path) {
        if (plugin.getConfig().contains(path + ".center.x")) {
            return new Location(world,
                    plugin.getConfig().getDouble(path + ".center.x"),
                    plugin.getConfig().getDouble(path + ".center.y"),
                    plugin.getConfig().getDouble(path + ".center.z"));
        }
        return world.getSpawnLocation();
    }

    private Location findSafeLocation(World world, Location center, int radius, Set<Long> usedColumns,
                                          List<Location> placedLocations, double minDistance) {
        // Uniform sampling inside a circle prevents the search area from having empty square corners.
        for (int attempt = 0; attempt < 240; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            long columnKey = (((long) x) << 32) ^ (z & 0xffffffffL);
            if (usedColumns.contains(columnKey)) continue;

            int y = world.getHighestBlockYAt(x, z);
            if (y < world.getMinHeight() || y + 2 >= world.getMaxHeight()) continue;
            if (!world.getBlockAt(x, y, z).getType().isSolid()) continue;

            // Both the feet and head space must be passable, so gifts never spawn under low ceilings.
            org.bukkit.block.Block feet = world.getBlockAt(x, y + 1, z);
            org.bukkit.block.Block head = world.getBlockAt(x, y + 2, z);
            if (!feet.isPassable() || feet.isLiquid() || !head.isPassable() || head.isLiquid()) continue;

            boolean crowded = placedLocations.stream().anyMatch(existing -> {
                double dx = existing.getX() - (x + 0.5);
                double dz = existing.getZ() - (z + 0.5);
                return dx * dx + dz * dz < minDistance * minDistance;
            });
            if (crowded) continue;

            usedColumns.add(columnKey);
            return new Location(world, x + 0.5, y + 1.2, z + 0.5);
        }
        return null;
    }

    private void animateEventItems() {
        if (active == null) return;
        Color accent = active.type().equals("gifts")
                ? Color.fromRGB(255, 95, 105) : Color.fromRGB(115, 220, 255);
        int animated = 0;
        for (UUID entityId : new HashSet<>(eventItems)) {
            if (animated >= 64) break;
            org.bukkit.entity.Entity entity = Bukkit.getEntity(entityId);
            if (!(entity instanceof Item item) || !item.isValid() || item.isDead()) continue;
            Location point = item.getLocation().add(0, 0.28, 0);
            World world = point.getWorld();
            if (world == null) continue;

            world.spawnParticle(Particle.END_ROD, point, 0);
            world.spawnParticle(Particle.DUST, point.getX(), point.getY(), point.getZ(),
                    0, 0, 0, 0, 0, new Particle.DustOptions(accent, 0.9f));
            world.spawnParticle(Particle.SNOWFLAKE, point, 1, 0.08, 0.10, 0.08, 0.005);
            animated++;
        }
    }

    private ItemStack createEventItem(String type, String eventId) {
        Material base = type.equals("gifts") ? Material.PAPER : Material.AMETHYST_SHARD;
        ItemStack stack = new ItemStack(base);
        ItemMeta meta = stack.getItemMeta();
        if (type.equals("gifts")) {
            meta.displayName(RewardCommands.component("§c✦ §fVánoční dárek §c✦"));
            meta.lore(List.of(RewardCommands.component("§7Seber ho a přidej body do eventu.")));
            meta.setCustomModelData(12022);
        } else {
            meta.displayName(RewardCommands.component("§b✧ §fVánoční ozdoba §b✧"));
            meta.lore(List.of(RewardCommands.component("§7Seber ji a přidej body do eventu.")));
            meta.setCustomModelData(12023);
        }
        meta.getPersistentDataContainer().set(plugin.eventItemToken(), PersistentDataType.STRING, eventId);
        // Distinct metadata prevents nearby collectibles from merging into a single stack.
        meta.getPersistentDataContainer().set(plugin.eventItemUniqueToken(), PersistentDataType.STRING,
                UUID.randomUUID().toString());
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Remove leftovers from an event that ended while its chunk was unloaded or before a restart.
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof Item item)) continue;
            ItemMeta meta = item.getItemStack().getItemMeta();
            if (meta == null) continue;
            String eventId = meta.getPersistentDataContainer().get(plugin.eventItemToken(), PersistentDataType.STRING);
            if (eventId != null && (active == null || !eventId.equals(active.eventId()))) entity.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (hasEventToken(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Item item = event.getItem();
        ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta == null) return;
        String itemEventId = meta.getPersistentDataContainer().get(plugin.eventItemToken(), PersistentDataType.STRING);
        if (itemEventId == null) return;
        if (active == null || !itemEventId.equals(active.eventId())) {
            event.setCancelled(true);
            item.remove();
            eventItems.remove(item.getUniqueId());
            return;
        }

        event.setCancelled(true);
        Location pickupLocation = item.getLocation();
        World pickupWorld = pickupLocation.getWorld();
        if (pickupWorld != null) {
            pickupWorld.spawnParticle(Particle.FIREWORK, pickupLocation, 1, 0.12, 0.18, 0.12, 0.01);
            pickupWorld.spawnParticle(Particle.SNOWFLAKE, pickupLocation, 10, 0.28, 0.28, 0.28, 0.02);
            pickupWorld.spawnParticle(Particle.END_ROD, pickupLocation, 5, 0.20, 0.22, 0.20, 0.01);
        }
        item.remove();
        eventItems.remove(item.getUniqueId());
        int score = scores.merge(player.getUniqueId(), 1, Integer::sum);
        playerNames.put(player.getUniqueId(), player.getName());
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.65f, 1.5f);
        player.sendActionBar(RewardCommands.component("§b✦ " + displayName(active.type()) + " §8| §f"
                + score + "/" + active.targetScore() + " §8| §7Zbývá předmětů: §f" + eventItems.size()));
        Bukkit.broadcastMessage("§b[ Vánoční event ] §f" + player.getName() + " §7našel předmět! (§e" + score
                + "§7/" + active.targetScore() + "§7) • Zbývá: §b" + eventItems.size());

        if (score >= active.targetScore()) {
            finishWithWinner(player.getUniqueId(), player.getName(), score);
        } else if (eventItems.isEmpty()) {
            finishByScore();
        }
    }

    public void finishByScore() {
        if (active == null) return;
        List<Map.Entry<UUID, Integer>> ranking = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder()))
                .toList();
        if (ranking.isEmpty()) {
            finish("§7Event skončil, ale nikdo nic nenašel.", null, false);
            return;
        }
        Map.Entry<UUID, Integer> first = ranking.get(0);
        finishWithWinner(first.getKey(), playerNames.getOrDefault(first.getKey(), "hráč"), first.getValue());
    }

    private void finishWithWinner(UUID uuid, String name, int score) {
        if (active == null) return;
        String type = active.type();
        String trailEffect = plugin.getConfig().getString("cosmetics.events." + type + ".trailstudio-effect", "");
        if (trailEffect != null && !trailEffect.isBlank()) plugin.trailStudio().grant(uuid, trailEffect);
        List<String> commands = plugin.getConfig().getStringList("cosmetics.events." + type + ".first-place-commands");
        RewardCommands.dispatch(plugin, commands, name, uuid.toString(), Map.of(
                "event", type,
                "score", Integer.toString(score),
                "rank", "1"));
        finish("§a§l" + name + " vyhrál vánoční event! §7Získal " + score + " bodů.", uuid, true);
    }

    public void stop(String reason, boolean announce) {
        if (active == null) return;
        finish(reason, null, announce);
    }

    private void finish(String message, UUID winner, boolean announce) {
        if (active == null) return;
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
        if (visualTask != null) {
            visualTask.cancel();
            visualTask = null;
        }
        for (UUID entityId : new HashSet<>(eventItems)) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) entity.remove();
        }
        eventItems.clear();
        scores.clear();
        playerNames.clear();
        active = null;
        if (announce && message != null) Bukkit.broadcastMessage("§b§l✦ VÁNOČNÍ EVENT ✦ §f" + message);
        else if (message != null && !message.isBlank()) Bukkit.broadcastMessage("§7" + message);
    }

    public boolean isActive() {
        return active != null;
    }

    public String hudType() {
        return active == null ? "" : displayName(active.type());
    }

    public String hudScore(Player player) {
        if (active == null) return "";
        return scores.getOrDefault(player.getUniqueId(), 0) + "/" + active.targetScore();
    }

    public long remainingSeconds() {
        if (active == null) return 0L;
        return Math.max(0L, (active.endsAtMillis() - System.currentTimeMillis() + 999L) / 1000L);
    }

    public World activeWorld() {
        return active == null ? null : active.world();
    }

    private String displayName(String type) {
        return type.equals("gifts") ? "Hledání dárků" : "Hledání ozdob";
    }

    private record EventState(String type, String eventId, long endsAtMillis, int targetScore, World world) { }
}
