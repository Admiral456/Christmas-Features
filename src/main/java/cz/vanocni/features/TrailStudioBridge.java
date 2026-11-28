package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Optional integration with TrailStudio. Unlocks are stored locally and exposed through
 * Bukkit PermissionAttachments, so LuckPerms is not required.
 */
public final class TrailStudioBridge implements Listener {
    private static final Pattern EFFECT_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
    private final ChristmasFeaturesPlugin plugin;
    private final File dataFile;
    private final YamlConfiguration data;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private boolean dataLoadFailed;

    public TrailStudioBridge(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "trail-unlocks.yml");
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Nelze vytvořit složku pluginu pro TrailStudio unlock data.");
        }
        this.data = loadDataSafely();
    }

    private YamlConfiguration loadDataSafely() {
        YamlConfiguration loaded = new YamlConfiguration();
        if (!dataFile.isFile()) return loaded;
        try {
            loaded.load(dataFile);
        } catch (IOException | InvalidConfigurationException exception) {
            dataLoadFailed = true;
            File backup = plugin.backupInvalidYaml(dataFile);
            String backupInfo = backup == null
                    ? "Původní soubor zůstal nedotčen; zálohu se nepodařilo vytvořit."
                    : "Vytvořena záloha " + backup.getName() + ".";
            plugin.getLogger().severe("Soubor trail-unlocks.yml obsahuje neplatný YAML: "
                    + exception.getMessage() + ". Nová odemknutí se nebudou zapisovat. " + backupInfo);
        }
        return loaded;
    }

    public boolean isAvailable() {
        PluginManager manager = Bukkit.getPluginManager();
        Plugin trailStudio = manager.getPlugin("TrailStudio");
        return trailStudio != null && trailStudio.isEnabled();
    }

    public boolean grant(Player player, String requestedEffectId) {
        if (player == null) return false;
        return grant(player.getUniqueId(), requestedEffectId);
    }

    public boolean grant(UUID playerId, String requestedEffectId) {
        if (playerId == null || requestedEffectId == null || requestedEffectId.isBlank()) return false;
        String effectId = requestedEffectId.trim().toLowerCase(Locale.ROOT);
        if (!EFFECT_ID.matcher(effectId).matches()) {
            plugin.getLogger().warning("Neplatné TrailStudio effect ID v konfiguraci: " + requestedEffectId);
            return false;
        }

        Player onlinePlayer = Bukkit.getPlayer(playerId);
        if (dataLoadFailed) {
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage("§cTrailStudio odemknutí nelze bezpečně uložit. trail-unlocks.yml je poškozený a musí být opraven ze zálohy.");
            }
            return false;
        }
        if (isAvailable() && !isEffectConfigured(effectId)) {
            plugin.getLogger().warning("TrailStudio neobsahuje efekt '" + effectId
                    + "'. Přidej jej do plugins/TrailStudio/effects.yml nebo oprav cosmetics konfiguraci.");
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage("§cKosmetický efekt není v TrailStudio nakonfigurovaný: §f" + effectId);
            }
            return false;
        }

        String path = "unlocks." + playerId;
        List<String> unlocked = new ArrayList<>(data.getStringList(path));
        if (unlocked.contains(effectId)) {
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage("§7Tento TrailStudio efekt už máš odemčený: §b" + effectId);
            }
            return true;
        }

        unlocked.add(effectId);
        data.set(path, unlocked);
        save();

        if (onlinePlayer != null) {
            if (isAvailable()) {
                apply(onlinePlayer);
                onlinePlayer.sendMessage("§aOdemkl sis vánoční kosmetiku: §b" + effectId
                        + "§a. Vybavíš ji přes §f/trails§a.");
            } else {
                onlinePlayer.sendMessage("§aOdemknutí kosmetiky §b" + effectId
                        + " §aje uloženo. Efekt bude dostupný po instalaci TrailStudio a restartu serveru.");
            }
        } else {
            plugin.getLogger().info("TrailStudio unlock '" + effectId + "' uložen pro offline hráče " + playerId + ".");
        }
        return true;
    }

    public void apply(Player player) {
        if (player == null || !isAvailable()) return;
        List<String> unlocked = data.getStringList("unlocks." + player.getUniqueId());
        if (unlocked.isEmpty()) return;

        PermissionAttachment attachment = attachments.computeIfAbsent(
                player.getUniqueId(), ignored -> player.addAttachment(plugin));
        for (String effectId : unlocked) {
            if (effectId == null || !EFFECT_ID.matcher(effectId).matches()) continue;
            if (!isEffectConfigured(effectId)) continue;
            attachment.setPermission("trailstudio.effect." + effectId, true);
        }
    }

    public List<String> unlocked(UUID playerId) {
        return List.copyOf(data.getStringList("unlocks." + playerId));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PermissionAttachment attachment = attachments.remove(event.getPlayer().getUniqueId());
        if (attachment != null) attachment.remove();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!event.getPlugin().getName().equalsIgnoreCase("TrailStudio")) return;
        for (Player player : Bukkit.getOnlinePlayers()) apply(player);
        plugin.getLogger().info("TrailStudio bylo nalezeno; uložená vánoční kosmetická odemknutí jsou aktivní.");
    }

    public void shutdown() {
        for (PermissionAttachment attachment : attachments.values()) {
            try {
                attachment.remove();
            } catch (IllegalStateException ignored) {
                // Bukkit may already be removing attachments during plugin shutdown.
            }
        }
        attachments.clear();
        save();
    }

    private boolean isEffectConfigured(String effectId) {
        Plugin trailStudio = Bukkit.getPluginManager().getPlugin("TrailStudio");
        if (trailStudio == null || !trailStudio.isEnabled()) return false;
        File effectsFile = new File(trailStudio.getDataFolder(), "effects.yml");
        if (!effectsFile.isFile()) return false;
        YamlConfiguration effects = YamlConfiguration.loadConfiguration(effectsFile);
        return effects.isConfigurationSection("effects." + effectId)
                && effects.getBoolean("effects." + effectId + ".enabled", true);
    }

    private void save() {
        if (dataLoadFailed) return;
        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Nepodařilo se uložit TrailStudio unlock data: " + exception.getMessage());
        }
    }
}
