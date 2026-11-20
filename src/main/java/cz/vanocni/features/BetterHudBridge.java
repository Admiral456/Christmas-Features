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
 * Optional BetterHud integration. BetterHud API classes are only loaded reflectively
 * after the BetterHud plugin has been detected, so BetterHud remains optional.
 */
public final class BetterHudBridge {
    private static final String HUD_ID = "christmas_features_hud";

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
            installDefaultFiles(betterHud.getDataFolder().toPath());
            ensureDefaultHud(betterHud.getDataFolder().toPath().resolve("config.yml"));

            Class<?> apiBridge = Class.forName(
                    "cz.vanocni.features.BetterHudApiBridge", true, plugin.getClass().getClassLoader());
            Method registerMethod = apiBridge.getMethod("register", ChristmasFeaturesPlugin.class);
            refreshMethod = apiBridge.getMethod("refresh", ChristmasFeaturesPlugin.class);
            clearMethod = apiBridge.getMethod("clear");

            registerMethod.invoke(null, plugin);
            available = true;
            refreshNow();
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshNow, 1L, 20L);
            plugin.getLogger().info("BetterHud integrace byla aktivována. Vánoční HUD je přidán mezi výchozí HUDy.");
        } catch (Throwable error) {
            available = false;
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "BetterHud integrace se nepodařila inicializovat; použije se náhradní HUD.", error);
        }
    }

    private void installDefaultFiles(Path dataFolder) throws IOException {
        copyIfMissing("betterhud/texts/christmas-font.yml", dataFolder.resolve("texts/christmas-font.yml"));
        copyIfMissing("betterhud/layouts/christmas-event-layout.yml", dataFolder.resolve("layouts/christmas-event-layout.yml"));
        copyIfMissing("betterhud/huds/christmas-event-hud.yml", dataFolder.resolve("huds/christmas-event-hud.yml"));
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
            plugin.getLogger().warning("BetterHud config.yml nebyl nalezen; HUD soubory byly vytvořeny, ale je nutné přidat "
                    + HUD_ID + " do default-hud.");
            return;
        }

        String original = Files.readString(configPath, StandardCharsets.UTF_8);
        Pattern item = Pattern.compile("(?m)^\\s*-\\s*" + Pattern.quote(HUD_ID) + "\\s*$");
        if (item.matcher(original).find()) return;

        String updated = appendDefaultHud(original);
        if (!updated.equals(original)) {
            Files.writeString(configPath, updated, StandardCharsets.UTF_8);
        }
    }

    private String appendDefaultHud(String yaml) {
        Pattern inlineList = Pattern.compile("(?m)^([ \\t]*)default-hud\\s*:\\s*\\[(.*?)\\](\\s*(?:#.*)?)$");
        Matcher inline = inlineList.matcher(yaml);
        if (inline.find()) {
            String key = inline.group(1) + "default-hud:";
            String indent = inline.group(1) + "  ";
            String contents = inline.group(2).trim();
            StringBuilder replacement = new StringBuilder(key).append('\\n');
            if (!contents.isEmpty()) {
                for (String entry : contents.split(",")) {
                    String value = entry.trim();
                    if (!value.isEmpty()) replacement.append(indent).append("- ").append(value).append('\\n');
                }
            }
            replacement.append(indent).append("- ").append(HUD_ID).append(inline.group(3));
            return inline.replaceFirst(Matcher.quoteReplacement(replacement.toString()));
        }

        Pattern key = Pattern.compile("(?m)^([ \\t]*)default-hud\\s*:(?:[ \\t]*(?:#.*)?)$");
        Matcher matcher = key.matcher(yaml);
        if (matcher.find()) {
            String keyLine = matcher.group();
            String tail = yaml.substring(matcher.end());
            Matcher existingEntry = Pattern.compile("(?m)^([ \\t]*)-\\s+").matcher(tail);
            String indent = existingEntry.find() ? existingEntry.group(1) : matcher.group(1) + "  ";
            String insertion = keyLine + "\\n" + indent + "- " + HUD_ID;
            return yaml.substring(0, matcher.start()) + insertion + tail;
        }

        String separator = yaml.endsWith("\\n") ? "" : "\\n";
        return yaml + separator + "\\ndefault-hud:\\n  - " + HUD_ID + "\\n";
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
