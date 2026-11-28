package cz.vanocni.features;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public final class ChristmasPlaceholderExpansion extends PlaceholderExpansion {
    private final ChristmasFeaturesPlugin plugin;

    public ChristmasPlaceholderExpansion(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "christmas";
    }

    @Override
    public String getAuthor() {
        return "ChristmasFeatures";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        if (params.equalsIgnoreCase("tag")) {
            return plugin.cosmetics().tagPlaceholder(player.getUniqueId());
        }
        return null;
    }
}
