package cz.vanocni.features;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.Random;
import java.util.Locale;

/** Applies a cold 3-D biome to loaded and newly loaded chunks without regenerating terrain. */
public final class WinterManager implements Listener {
    private final ChristmasFeaturesPlugin plugin;
    private final Queue<ChunkRef> queue = new ArrayDeque<>();
    private final Set<String> queued = new HashSet<>();
    private final Map<UUID, WeatherState> originalWeather = new HashMap<>();
    private final Random random = new Random();
    private List<String> cachedPreservedBiomeNames = List.of();
    private Set<Biome> cachedPreservedBiomes = Set.of();
    private BukkitTask workerTask;
    private BukkitTask weatherTask;
    private BukkitTask ambienceTask;

    public WinterManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (isManagedExternally()) {
            stopTasks();
            restoreWeather();
            startAmbienceTask();
            plugin.getLogger().info("ChristmasSeason je aktivní; správu zimních biomů a počasí přebírá ChristmasSeason. Vánoční třpytky zůstávají aktivní.");
            return;
        }
        if (!plugin.getConfig().getBoolean("winter.enabled", true)) {
            stopTasks();
            restoreWeather();
            return;
        }
        applyWeather();
        queueLoadedChunks(false);
        ensureTasks();
    }

    public void refresh() {
        if (isManagedExternally() || !plugin.getConfig().getBoolean("winter.enabled", true)) {
            stopTasks();
            restoreWeather();
            queue.clear();
            queued.clear();
            if (isManagedExternally() && plugin.getConfig().getBoolean("winter.enabled", true)) {
                startAmbienceTask();
            }
            return;
        }
        queue.clear();
        queued.clear();
        queueLoadedChunks(true);
        applyWeather();
        ensureTasks();
    }

    private void ensureTasks() {
        if (workerTask == null || workerTask.isCancelled()) {
            workerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processQueue, 1L, 1L);
        }
        if (weatherTask == null || weatherTask.isCancelled()) {
            weatherTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyWeather, 1200L, 1200L);
        }
        startAmbienceTask();
    }

    private void startAmbienceTask() {
        if (ambienceTask != null) {
            ambienceTask.cancel();
            ambienceTask = null;
        }
        if (!plugin.getConfig().getBoolean("winter.enabled", true)
                || !plugin.getConfig().getBoolean("winter.ambience.enabled", true)) return;
        long interval = Math.max(10L, Math.min(200L,
                plugin.getConfig().getLong("winter.ambience.interval-ticks", 40L)));
        ambienceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnAmbientSparkles,
                interval, interval);
    }

    /** True when the dedicated ChristmasSeason plugin owns winter biomes and weather. */
    public boolean isManagedExternally() {
        return Bukkit.getPluginManager().isPluginEnabled("christmas-season");
    }

    public void shutdown() {
        stopTasks();
        queue.clear();
        queued.clear();
        restoreWeather();
    }

    private void stopTasks() {
        if (workerTask != null) {
            workerTask.cancel();
            workerTask = null;
        }
        if (weatherTask != null) {
            weatherTask.cancel();
            weatherTask = null;
        }
        if (ambienceTask != null) {
            ambienceTask.cancel();
            ambienceTask = null;
        }
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
        return !isManagedExternally() && isConfiguredWinterWorld(world);
    }

    private boolean isConfiguredWinterWorld(World world) {
        if (world == null || !plugin.getConfig().getBoolean("winter.enabled", true)) return false;
        List<String> worlds = plugin.getConfig().getStringList("winter.worlds");
        if (worlds.isEmpty()) return world.getEnvironment() == World.Environment.NORMAL;
        return worlds.stream().anyMatch(name -> name.equalsIgnoreCase(world.getName()));
    }

    /**
     * Adds a small number of cold sparkles around outdoor players. This is purely
     * visual: it never changes blocks, biomes, weather or player state.
     */
    private void spawnAmbientSparkles() {
        if (!plugin.getConfig().getBoolean("winter.enabled", true)
                || !plugin.getConfig().getBoolean("winter.ambience.enabled", true)) return;

        int particleCount = Math.max(0, Math.min(6,
                plugin.getConfig().getInt("winter.ambience.particles-per-player", 2)));
        if (particleCount == 0) return;
        double radius = Math.max(2.0, Math.min(24.0,
                plugin.getConfig().getDouble("winter.ambience.radius", 8.0)));
        Particle.DustOptions frost = new Particle.DustOptions(Color.fromRGB(175, 225, 255), 0.75f);

        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            if (!isConfiguredWinterWorld(world) || player.isDead() || player.isInvisible()
                    || player.getGameMode() == GameMode.SPECTATOR) continue;

            Location base = player.getLocation();
            // Keep the subtle effect outdoors; avoid sparkling through roofs and caves.
            if (world.getHighestBlockYAt(base.getBlockX(), base.getBlockZ()) > base.getBlockY() + 1) continue;

            for (int i = 0; i < particleCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = Math.sqrt(random.nextDouble()) * radius;
                Location point = base.clone().add(Math.cos(angle) * distance,
                        0.35 + random.nextDouble() * 1.8, Math.sin(angle) * distance);
                world.spawnParticle(Particle.SNOWFLAKE, point, 1, 0.025, 0.045, 0.025, 0.001);
                if (random.nextDouble() < 0.35) {
                    world.spawnParticle(Particle.DUST, point, 0, 0, 0, 0, 0, frost);
                }
                if (random.nextDouble() < 0.12) {
                    world.spawnParticle(Particle.END_ROD, point, 0);
                }
            }
        }
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
        Set<Biome> preservedBiomes = getPreservedBiomes();

        // Biomes are stored as 4x4x4 quart cells. One write per quart is enough.
        // Preserve selected mountain biomes so mob spawn filters and regional identity survive.
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 4) {
            for (int x = 0; x < 16; x += 4) {
                for (int z = 0; z < 16; z += 4) {
                    int blockX = baseX + x;
                    int blockZ = baseZ + z;
                    if (preservedBiomes.contains(world.getBiome(blockX, y, blockZ))) continue;
                    world.setBiome(blockX, y, blockZ, winterBiome);
                }
            }
        }
    }

    private Set<Biome> getPreservedBiomes() {
        List<String> configured = plugin.getConfig().getStringList("winter.preserve-biomes")
                .stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        if (configured.equals(cachedPreservedBiomeNames)) return cachedPreservedBiomes;

        Set<Biome> resolved = new HashSet<>();
        var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        for (String configuredName : configured) {
            String normalized = configuredName.toLowerCase(Locale.ROOT);
            NamespacedKey key = normalized.contains(":")
                    ? NamespacedKey.fromString(normalized)
                    : NamespacedKey.minecraft(normalized);
            if (key == null) {
                plugin.getLogger().warning("Neplatný klíč chráněného biomu: " + configuredName);
                continue;
            }

            Biome biome = biomeRegistry.get(key);
            if (biome == null) {
                plugin.getLogger().warning("Chráněný biome nebyl nalezen v registru: " + configuredName);
                continue;
            }
            resolved.add(biome);
        }

        cachedPreservedBiomeNames = List.copyOf(configured);
        cachedPreservedBiomes = Set.copyOf(resolved);
        return cachedPreservedBiomes;
    }

    private void applyWeather() {
        if (!plugin.getConfig().getBoolean("winter.enabled", true)
                || !plugin.getConfig().getBoolean("winter.force-snow-weather", true)
                || isManagedExternally()) {
            restoreWeather();
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            if (!isWinterWorld(world)) continue;
            originalWeather.computeIfAbsent(world.getUID(), ignored -> new WeatherState(
                    world.hasStorm(), world.isThundering(), world.getWeatherDuration(),
                    world.getThunderDuration(), System.currentTimeMillis()));
            if (!world.hasStorm()) world.setStorm(true);
            if (world.isThundering()) world.setThundering(false);
            world.setWeatherDuration(1_000_000);
            world.setThunderDuration(1_000_000);
        }

        // If a world was removed from winter.worlds while the plugin was running,
        // restore its previous weather instead of leaving a permanent snow storm.
        for (UUID worldId : new HashSet<>(originalWeather.keySet())) {
            World world = Bukkit.getWorld(worldId);
            if (world != null && isWinterWorld(world)) continue;
            WeatherState state = originalWeather.remove(worldId);
            if (world != null) restoreWeather(world, state);
        }
    }

    private void restoreWeather() {
        for (Map.Entry<UUID, WeatherState> entry : new HashMap<>(originalWeather).entrySet()) {
            World world = Bukkit.getWorld(entry.getKey());
            if (world != null) restoreWeather(world, entry.getValue());
        }
        originalWeather.clear();
    }

    private void restoreWeather(World world, WeatherState state) {
        long elapsedTicks = Math.max(0L, (System.currentTimeMillis() - state.capturedAtMillis()) / 50L);
        int weatherDuration = (int) Math.max(0L, state.weatherDuration() - elapsedTicks);
        int thunderDuration = (int) Math.max(0L, state.thunderDuration() - elapsedTicks);
        world.setStorm(state.storm());
        world.setThundering(state.storm() && state.thundering());
        world.setWeatherDuration(weatherDuration);
        world.setThunderDuration(thunderDuration);
    }

    private record WeatherState(boolean storm, boolean thundering, int weatherDuration,
                                int thunderDuration, long capturedAtMillis) { }

    private record ChunkRef(UUID worldId, int x, int z, String id) { }
}
