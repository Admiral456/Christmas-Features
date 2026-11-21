package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional BetterHud integration. BetterHud API classes are loaded reflectively
 * only after BetterHud has been detected, so the plugin remains optional.
 */
public final class BetterHudBridge {
    private static final String HUD_ID = "christmas_features_hud_v2";
    private static final String LEGACY_HUD_ID = "christmas_features_hud";

    private final ChristmasFeaturesPlugin plugin;
    private BukkitTask refreshTask;
    private Method refreshMethod;
    private Method clearMethod;
    private boolean available;

    public BetterHudBridge(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Plugin betterHud = Bukkit.getPluginManager().getPlugin("BetterHud");
        if (betterHud == null || !betterHud.isEnabled()) {
            plugin.getLogger().info("BetterHud nebyl nalezen; použije se náhradní vánoční HUD.");
            return;
        }

        try {
            Path dataFolder = betterHud.getDataFolder().toPath();
            installVersionedFiles(dataFolder);
            ensureDefaultHud(dataFolder.resolve("config.yml"));

            Class<?> apiBridge = Class.forName(
                    "cz.vanocni.features.BetterHudApiBridge", true, plugin.getClass().getClassLoader());

            // BetterHud loads configuration during its own enable phase. Our versioned
            // files are added after that, so reload before registering custom placeholders.
            Method reloadMethod = apiBridge.getMethod("reload");
            Object reloadResult = reloadMethod.invoke(null);
            if (!Boolean.TRUE.equals(reloadResult)) {
                throw new IllegalStateException("BetterHud API did not confirm a successful reload.");
            }

            Method registerMethod = apiBridge.getMethod("register", ChristmasFeaturesPlugin.class);
            refreshMethod = apiBridge.getMethod("refresh", ChristmasFeaturesPlugin.class);
            clearMethod = apiBridge.getMethod("clear");

            // Register after reload so placeholders are attached to the live manager.
            registerMethod.invoke(null, plugin);
            available = true;
            refreshNow();
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshNow, 1L, 20L);
            plugin.getLogger().info("BetterHud integrace v2 je aktivní; HUD a vánoční proměnné byly načteny.");
        } catch (Throwable error) {
            available = false;
            refreshMethod = null;
            clearMethod = null;
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "BetterHud integraci se nepodařilo inicializovat; použije se náhradní HUD.", error);
        }
    }

    private void installVersionedFiles(Path dataFolder) throws IOException {
        // Versioned filenames let us upgrade the integration without overwriting
        // administrator customizations or leaving players stuck on an old layout.
        copyIfMissing("betterhud/v2/texts/christmas-font-v2.yml",
                dataFolder.resolve("texts/christmas-font-v2.yml"));
        copyIfMissing("betterhud/v2/layouts/christmas-layout-v2.yml",
                dataFolder.resolve("layouts/christmas-layout-v2.yml"));
        copyIfMissing("betterhud/v2/huds/christmas-hud-v2.yml",
                dataFolder.resolve("huds/christmas-hud-v2.yml"));
    }

    private void copyIfMissing(String resource, Path target) throws IOException {
        if (Files.exists(target)) return;
        Files.createDirectories(target.getParent());
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) throw new IOException("Chybí zabalený resource: " + resource);
            Files.copy(input, target);
        }
    }

    private void ensureDefaultHud(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("BetterHud config.yml nebyl nalezen; nelze bezpečně aktivovat vánoční HUD.");
        }

        String original = Files.readString(configPath, StandardCharsets.UTF_8);
        String updated = migrateDefaultHud(original);
        if (!updated.equals(original)) {
            Files.writeString(configPath, updated, StandardCharsets.UTF_8);
        }
    }

    /**
     * Removes only the previous ChristmasFeatures HUD ID, retains all other
     * default HUD entries and adds the new versioned ID. Supports inline and
     * block YAML lists without reserializing the user's complete config.
     */
    private String migrateDefaultHud(String yaml) {
        Pattern inlineList = Pattern.compile("(?m)^([ \\t]*)default-hud\\s*:\\s*\\[(.*?)\\](\\s*(?:#.*)?)$");
        Matcher inline = inlineList.matcher(yaml);
        if (inline.find()) {
            String key = inline.group(1) + "default-hud:";
            String indent = inline.group(1) + "  ";
            String contents = inline.group(2).trim();
            StringBuilder replacement = new StringBuilder(key).append('\n');
            if (!contents.isEmpty()) {
                for (String entry : contents.split(",")) {
                    String value = entry.trim();
                    String plain = value.replace("\"", "").replace("'", "");
                    if (!value.isEmpty() && !plain.equals(LEGACY_HUD_ID) && !plain.equals(HUD_ID)) {
                        replacement.append(indent).append("- ").append(value).append('\n');
                    }
                }
            }
            replacement.append(indent).append("- ").append(HUD_ID).append(inline.group(3));
            return inline.replaceFirst(Matcher.quoteReplacement(replacement.toString()));
        }

        Pattern legacyEntry = Pattern.compile(
                "(?m)^[ \\t]*-[ \\t]*" + Pattern.quote(LEGACY_HUD_ID) + "[ \\t]*(?:\\r?\\n|$)");
        String updated = legacyEntry.matcher(yaml).replaceAll("");

        Pattern activeEntry = Pattern.compile("(?m)^[ \\t]*-[ \\t]*" + Pattern.quote(HUD_ID) + "[ \\t]*$");
        if (activeEntry.matcher(updated).find()) return updated;

        Pattern key = Pattern.compile("(?m)^([ \\t]*)default-hud\\s*:(?:[ \\t]*(?:#.*)?)$");
        Matcher matcher = key.matcher(updated);
        if (matcher.find()) {
            String keyLine = matcher.group();
            String tail = updated.substring(matcher.end());
            Matcher existingEntry = Pattern.compile("(?m)^([ \\t]*)-[ \\t]+").matcher(tail);
            String indent = existingEntry.find() ? existingEntry.group(1) : matcher.group(1) + "  ";
            String insertion = keyLine + "\n" + indent + "- " + HUD_ID;
            return updated.substring(0, matcher.start()) + insertion + tail;
        }

        String separator = updated.endsWith("\n") ? "" : "\n";
        return updated + separator + "\ndefault-hud:\n  - " + HUD_ID + "\n";
    }

    private void refreshNow() {
        if (!available || refreshMethod == null) return;
        try {
            refreshMethod.invoke(null, plugin);
        } catch (ReflectiveOperationException error) {
            plugin.getLogger().warning("Nepodařilo se aktualizovat vánoční BetterHud proměnné: " + error.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void shutdown() {
        if (refreshTask != null) refreshTask.cancel();
        if (available && clearMethod != null) {
            try {
                clearMethod.invoke(null);
            } catch (ReflectiveOperationException error) {
                plugin.getLogger().fine("BetterHud snapshot cleanup failed: " + error.getMessage());
            }
        }
        available = false;
    }
}
