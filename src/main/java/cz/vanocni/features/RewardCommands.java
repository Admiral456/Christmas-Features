package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Map;

final class RewardCommands {
    private RewardCommands() {}

    static void dispatch(ChristmasFeaturesPlugin plugin, List<String> commands, Player player,
                         Map<String, String> extraPlaceholders) {
        if (player == null) return;
        dispatch(plugin, commands, player.getName(), player.getUniqueId().toString(), extraPlaceholders);
    }

    static void dispatch(ChristmasFeaturesPlugin plugin, List<String> commands, String playerName,
                         String uuid, Map<String, String> extraPlaceholders) {
        if (commands == null || commands.isEmpty()) return;
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) continue;
            String command = raw.replace("%player%", playerName)
                    .replace("%uuid%", uuid)
                    .replace("%display_name%", playerName);
            if (extraPlaceholders != null) {
                for (Map.Entry<String, String> entry : extraPlaceholders.entrySet()) {
                    command = command.replace("%" + entry.getKey() + "%", entry.getValue());
                }
            }
            try {
                Bukkit.dispatchCommand(console, command);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Nepodařilo se spustit odměnový příkaz: " + command);
                plugin.getLogger().warning(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            }
        }
    }

    static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    static Component component(String input) {
        return LegacyComponentSerializer.legacySection().deserialize(color(input));
    }
}
