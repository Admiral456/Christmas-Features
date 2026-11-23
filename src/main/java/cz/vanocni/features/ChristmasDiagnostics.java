package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Read-only deployment audit for the live Hostify server.
 * It never edits plugin configs or starts/reloads external plugins.
 */
public final class ChristmasDiagnostics {
    private static final String[] REQUIRED_MOUNTAIN_BIOMES = {
            "SNOWY_SLOPES", "FROZEN_PEAKS", "JAGGED_PEAKS", "GROVE"
    };

    private final ChristmasFeaturesPlugin plugin;

    public ChristmasDiagnostics(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    public void print(CommandSender sender) {
        sender.sendMessage("§b§lChristmasFeatures Diagnostics §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Kontrola je pouze čtecí; nic nemění ani nereloaduje.");

        checkPlugin(sender, "ModelEngine");
        checkPlugin(sender, "MythicMobs");
        checkPlugin(sender, "ItemsAdder");
        checkPlugin(sender, "SupremeTags");
        checkPlugin(sender, "BattlePass");

        World world = Bukkit.getWorld("world");
        report(sender, world != null,
                world == null ? "Svět world není načten." : "Svět world je načten.");

        Path modelFolder = pluginFolder("ModelEngine", "blueprints/Christmas");
        checkFile(sender, modelFolder.resolve("grinch.bbmodel"), "ModelEngine Grinch blueprint");
        checkFile(sender, modelFolder.resolve("yeti.bbmodel"), "ModelEngine Yeti blueprint");
        checkFile(sender, modelFolder.resolve("frost_emperor.bbmodel"), "ModelEngine Frost Emperor blueprint");
        Path modelPack = pluginFolder("ModelEngine", "resource pack");
        report(sender, Files.isDirectory(modelPack),
                "ModelEngine vygenerovaný resource pack: " + (Files.isDirectory(modelPack) ? "složka nalezena" : "zatím chybí; vznikne po importu modelů"));

        Path mythicMobs = pluginFolder("MythicMobs", "Mobs/Christmas");
        checkFile(sender, mythicMobs.resolve("ChristmasGrinch.yml"), "MythicMobs Grinch YAML");
        checkFile(sender, mythicMobs.resolve("ChristmasYeti.yml"), "MythicMobs Yeti YAML");
        checkFile(sender, mythicMobs.resolve("ChristmasFrostEmperor.yml"), "MythicMobs Frost Emperor YAML");

        Path randomSpawns = pluginFolder("MythicMobs", "RandomSpawns/ChristmasMountains.yml");
        report(sender, Files.isRegularFile(randomSpawns),
                "Horské RandomSpawns: " + (Files.isRegularFile(randomSpawns) ? "soubor nalezen" : "soubor chybí"));

        checkGenerateSpawnPoints(sender);
        checkItemsAdder(sender);
        checkMountainBiomeProtection(sender);
        checkGrinchCommands(sender);

        sender.sendMessage("§7Po instalaci vždy ověř také konzoli po /meg reload models a /mm reload.");
    }

    private void checkPlugin(CommandSender sender, String name) {
        Plugin dependency = Bukkit.getPluginManager().getPlugin(name);
        boolean enabled = dependency != null && dependency.isEnabled();
        String version = dependency == null ? "" : " v" + dependency.getDescription().getVersion();
        report(sender, enabled, name + (enabled ? version + " je aktivní." : " není načtený nebo není aktivní."));
    }

    private void checkGenerateSpawnPoints(CommandSender sender) {
        Plugin mythic = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mythic == null) {
            report(sender, false, "GenerateSpawnPoints nelze ověřit: MythicMobs není načtený.");
            return;
        }

        File file = mythic.getDataFolder().toPath()
                .resolve("config")
                .resolve("config-spawning.yml")
                .toFile();
        if (!file.isFile()) {
            report(sender, false, "Chybí MythicMobs/config/config-spawning.yml.");
            return;
        }
        boolean enabled = YamlConfiguration.loadConfiguration(file).getBoolean("GenerateSpawnPoints", false);
        report(sender, enabled, "MythicMobs GenerateSpawnPoints: " + (enabled ? "true" : "false – nastav true."));
    }

    private void checkItemsAdder(CommandSender sender) {
        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null) {
            report(sender, false, "ItemsAdder není načtený; společný resource pack nelze ověřit.");
            return;
        }

        Path iaFolder = itemsAdder.getDataFolder().toPath();
        Path content = iaFolder.resolve("contents/christmasfeatures/resourcepack/pack.mcmeta");
        report(sender, Files.isRegularFile(content), "ChristmasFeatures ItemsAdder obsah: "
                + (Files.isRegularFile(content) ? "nalezen" : "chybí pack.mcmeta"));

        File configFile = iaFolder.resolve("config.yml").toFile();
        if (!configFile.isFile()) {
            report(sender, false, "ItemsAdder config.yml nebyl nalezen.");
            return;
        }

        List<String> merged = YamlConfiguration.loadConfiguration(configFile)
                .getStringList("merge_other_plugins_resourcepacks_folders");
        boolean modelEngineMerged = merged.stream()
                .anyMatch(value -> value.equalsIgnoreCase("ModelEngine/resource pack"));
        report(sender, modelEngineMerged, "ItemsAdder merge ModelEngine resource pack: "
                + (modelEngineMerged ? "nastaveno" : "přidej ModelEngine/resource pack do merge_other_plugins_resourcepacks_folders"));
    }

    private void checkMountainBiomeProtection(CommandSender sender) {
        List<String> configured = plugin.getConfig().getStringList("winter.preserve-biomes");
        boolean complete = true;
        for (String biome : REQUIRED_MOUNTAIN_BIOMES) {
            if (!configured.stream().anyMatch(value -> value.equalsIgnoreCase(biome))) {
                complete = false;
                break;
            }
        }
        // 1.0.6 also protects these four biomes in code, so the feature remains safe
        // even if an older user config has not yet been migrated.
        report(sender, true, "Ochrana horských biomů: aktivní (vestavěná ochrana); "
                + (complete ? "všechna 4 jsou také v config.yml." : "starší config.yml bude doplněn jen ručně, ochrana je i tak aktivní."));
    }

    private void checkGrinchCommands(CommandSender sender) {
        String tags = plugin.getConfig().getString("grinch.menu.tags-command", "");
        String quests = plugin.getConfig().getString("grinch.menu.quests-command", "");
        boolean ok = "player:tags".equalsIgnoreCase(tags.trim())
                && "player:bp open quests".equalsIgnoreCase(quests.trim());
        report(sender, ok, "Grinch menu: " + (ok ? "SupremeTags a BattlePass příkazy nastaveny." : "zkontroluj grinch.menu v config.yml."));
    }

    private Path pluginFolder(String pluginName, String child) {
        Plugin dependency = Bukkit.getPluginManager().getPlugin(pluginName);
        Path root = dependency == null ? Path.of("plugins", pluginName)
                : dependency.getDataFolder().toPath();
        return root.resolve(child);
    }

    private void checkFile(CommandSender sender, Path path, String label) {
        report(sender, Files.isRegularFile(path), label + ": " + (Files.isRegularFile(path) ? "nalezen" : "chybí"));
    }

    private void report(CommandSender sender, boolean ok, String message) {
        sender.sendMessage((ok ? "§a✔ " : "§c✖ ") + message);
    }
}
