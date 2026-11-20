package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/** Applies a cold 3-D biome to loaded and newly loaded chunks without regenerating terrain. */
public final class WinterManager implements Listener {
    private final ChristmasFeaturesPlugin plugin;
    private final Queue<ChunkRef> queue = new ArrayDeque<>();
    private final Set<String> queued = new HashSet<>();
    private BukkitTask workerTask;
    private BukkitTask weatherTask;

    public WinterManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (isManagedExternally()) {
            plugin.getLogger().info("ChristmasSeason je aktivní; správu zimních biomů a počasí přebírá ChristmasSeason.");
            return;
        }
        if (!plugin.getConfig().getBoolean("winter.enabled", true)) return;
        applyWeather();
        queueLoadedChunks(false);
        workerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processQueue, 1L, 1L);
        weatherTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyWeather, 1200L, 1200L);
    }

    public void refresh() {
        if (isManagedExternally()) return;
        queueLoadedChunks(true);
        applyWeather();
    }

    /** True when the dedicated ChristmasSeason plugin owns winter biomes and weather. */
    public boolean isManagedExternally() {
        return Bukkit.getPluginManager().isPluginEnabled("christmas-season");
    }

    public void shutdown() {
        if (workerTask != null) workerTask.cancel();
        if (weatherTask != null) weatherTask.cancel();
        queue.clear();
        queued.clear();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!isWinterWorld(event.getWorld())) return;
        enqueue(event.getChunk().getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        if (!isWinterWorld(event.getBlock().getWorld())) return;
        // Ice formation is intentionally allowed; the biome conversion makes the vanilla freeze checks work.
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (!plugin.getConfig().getBoolean("winter.enabled", true)) return;
        if (!isWinterWorld(event.getBlock().getWorld())) return;
        Block block = event.getBlock();
        switch (block.getType()) {
            case ICE, PACKED_ICE, BLUE_ICE, FROSTED_ICE, SNOW, SNOW_BLOCK, POWDER_SNOW -> event.setCancelled(true);
            default -> { }
        }
    }

    public boolean isWinterWorld(World world) {
        if (isManagedExternally()) return false;
        if (world == null || !plugin.getConfig().getBoolean("winter.enabled", true)) return false;
        List<String> worlds = plugin.getConfig().getStringList("winter.worlds");
        if (worlds.isEmpty()) return world.getEnvironment() == World.Environment.NORMAL;
        return worlds.stream().anyMatch(name -> name.equalsIgnoreCase(world.getName()));
    }

    private void queueLoadedChunks(boolean force) {
        for (World world : Bukkit.getWorlds()) {
            if (!isWinterWorld(world)) continue;
            for (Chunk chunk : world.getLoadedChunks()) {
                if (force) chunk.getPersistentDataContainer().remove(plugin.winterAppliedToken());
                else if (chunk.getPersistentDataContainer().has(plugin.winterAppliedToken(), PersistentDataType.BYTE)) continue;
                enqueue(world.getUID(), chunk.getX(), chunk.getZ());
            }
        }
    }

    private void enqueue(UUID worldId, int chunkX, int chunkZ) {
        String id = worldId + ":" + chunkX + ":" + chunkZ;
        if (queued.add(id)) queue.add(new ChunkRef(worldId, chunkX, chunkZ, id));
    }

    private void processQueue() {
        if (!plugin.getConfig().getBoolean("winter.enabled", true)) return;
        int perTick = Math.max(1, plugin.getConfig().getInt("winter.chunks-per-tick", 1));
        for (int i = 0; i < perTick && !queue.isEmpty(); i++) {
            ChunkRef ref = queue.poll();
            queued.remove(ref.id());
            World world = Bukkit.getWorld(ref.worldId());
            if (world == null || !isWinterWorld(world) || !world.isChunkLoaded(ref.x(), ref.z())) continue;

            Chunk chunk = world.getChunkAt(ref.x(), ref.z());
            if (chunk.getPersistentDataContainer().has(plugin.winterAppliedToken(), PersistentDataType.BYTE)) continue;
            applyBiome(chunk);
            chunk.getPersistentDataContainer().set(plugin.winterAppliedToken(), PersistentDataType.BYTE, (byte) 1);
        }
    }

    private void applyBiome(Chunk chunk) {
        String configured = plugin.getConfig().getString("winter.biome", "SNOWY_PLAINS");
        Biome winterBiome;
        try {
            winterBiome = Biome.valueOf(configured.toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Neznámý biome " + configured + ", používám SNOWY_PLAINS.");
            winterBiome = Biome.SNOWY_PLAINS;
        }

        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        // Biomes are stored as 4x4x4 quart cells. One write per quart is enough.
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 4) {
            for (int x = 0; x < 16; x += 4) {
                for (int z = 0; z < 16; z += 4) {
                    world.setBiome(baseX + x, y, baseZ + z, winterBiome);
                }
            }
        }
    }

    private void applyWeather() {
        if (!plugin.getConfig().getBoolean("winter.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("winter.force-snow-weather", true)) return;
        for (World world : Bukkit.getWorlds()) {
            if (!isWinterWorld(world)) continue;
            if (!world.hasStorm()) world.setStorm(true);
            if (world.isThundering()) world.setThundering(false);
            world.setWeatherDuration(1_000_000);
            world.setThunderDuration(1_000_000);
        }
    }

    private record ChunkRef(UUID worldId, int x, int z, String id) { }
}
