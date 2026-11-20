package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;

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

    public HolidayEventManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    public void startScheduler() {
        scheduleAutoStart();
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

        int count = Math.max(1, plugin.getConfig().getInt(path + ".spawn-count", 12));
        int radius = Math.max(5, plugin.getConfig().getInt(path + ".radius", 28));
        int duration = Math.max(30, plugin.getConfig().getInt(path + ".duration-seconds", 300));
        int target = Math.max(1, plugin.getConfig().getInt(path + ".target-score", 5));
        Location center = resolveCenter(world, path);

        scores.clear();
        playerNames.clear();
        eventItems.clear();
        String eventId = UUID.randomUUID().toString();
        active = new EventState(type, eventId, System.currentTimeMillis() + duration * 1000L, target, world);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Location location = findSafeLocation(world, center, radius);
            if (location == null) continue;
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
            return false;
        }

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

    private Location findSafeLocation(World world, Location center, int radius) {
        for (int attempt = 0; attempt < 32; attempt++) {
            int x = center.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = center.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            int y = world.getHighestBlockYAt(x, z);
            if (y < world.getMinHeight() || y + 1 >= world.getMaxHeight()) continue;
            if (!world.getBlockAt(x, y, z).getType().isSolid()) continue;
            if (!world.getBlockAt(x, y + 1, z).isEmpty()) continue;
            if (!world.getBlockAt(x, y + 1, z).isPassable()) continue;
            return new Location(world, x + 0.5, y + 1.2, z + 0.5);
        }
        return null;
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
        item.remove();
        eventItems.remove(item.getUniqueId());
        int score = scores.merge(player.getUniqueId(), 1, Integer::sum);
        playerNames.put(player.getUniqueId(), player.getName());
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.65f, 1.5f);
        Bukkit.broadcastMessage("§b[ Vánoční event ] §f" + player.getName() + " §7našel předmět! (§e" + score
                + "§7/" + active.targetScore() + ")");

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
        Player winner = Bukkit.getPlayer(uuid);
        if (winner != null) {
            String trailEffect = plugin.getConfig().getString("cosmetics.events." + type + ".trailstudio-effect", "");
            if (!trailEffect.isBlank()) plugin.trailStudio().grant(winner, trailEffect);
            List<String> commands = plugin.getConfig().getStringList("cosmetics.events." + type + ".first-place-commands");
            RewardCommands.dispatch(plugin, commands, winner, Map.of(
                    "event", type,
                    "score", Integer.toString(score),
                    "rank", "1"));
        }
        finish("§a§l" + name + " vyhrál vánoční event! §7Získal " + score + " bodů.", uuid, true);
    }

    public void stop(String reason, boolean announce) {
        if (active == null) return;
        finish(reason, null, announce);
    }

    private void finish(String message, UUID winner, boolean announce) {
        if (active == null) return;
        if (endTask != null) endTask.cancel();
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
