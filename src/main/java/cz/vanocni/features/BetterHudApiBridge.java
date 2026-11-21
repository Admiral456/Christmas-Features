package cz.vanocni.features;

import kr.toxicity.hud.api.BetterHud;
import kr.toxicity.hud.api.plugin.ReloadState;
import kr.toxicity.hud.api.placeholder.HudPlaceholder;
import kr.toxicity.hud.api.placeholder.PlaceholderContainer;
import kr.toxicity.hud.api.player.HudPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * API-dependent side of the optional BetterHud integration.
 * It is loaded reflectively so ChristmasFeatures can still run without BetterHud.
 */
public final class BetterHudApiBridge {
    private static volatile Map<UUID, HudSnapshot> snapshots = Map.of();
    private static boolean registered;

    private BetterHudApiBridge() {
    }

    public static synchronized void register(ChristmasFeaturesPlugin plugin) {
        if (registered) return;

        var manager = BetterHud.getInstance().getPlaceholderManager();
        PlaceholderContainer<String> strings = manager.getStringContainer();

        addString(strings, "christmas_event_active", HudSnapshot::eventActive);
        addString(strings, "christmas_event_name", HudSnapshot::eventName);
        addString(strings, "christmas_event_score", HudSnapshot::eventScore);
        addString(strings, "christmas_event_time", HudSnapshot::eventTime);
        addString(strings, "christmas_chatgame_active", HudSnapshot::chatGameActive);
        addString(strings, "christmas_chatgame_question", HudSnapshot::chatGameQuestion);
        addString(strings, "christmas_chatgame_time", HudSnapshot::chatGameTime);
        addString(strings, "christmas_key_count", HudSnapshot::keyCount);

        registered = true;
        refresh(plugin);
    }

    private static void addString(
            PlaceholderContainer<String> container,
            String name,
            Function<HudSnapshot, String> value
    ) {
        HudPlaceholder<String> placeholder = HudPlaceholder.<String>builder()
                .function((args, reason) -> (HudPlayer player) -> {
                    HudSnapshot snapshot = snapshots.get(player.uuid());
                    return snapshot == null ? "" : value.apply(snapshot);
                })
                .build();
        container.addPlaceholder(name, placeholder);
    }

    /** Called from the Bukkit main thread. Placeholder readers only see immutable snapshots. */
    public static void refresh(ChristmasFeaturesPlugin plugin) {
        Map<UUID, HudSnapshot> next = new HashMap<>();
        boolean eventActive = plugin.events().isActive();
        boolean chatGameActive = plugin.chatGames().isActive();

        BukkitAccess.onlinePlayers().forEach(player -> {
            String eventName = eventActive ? plugin.events().hudType() : "";
            String eventScore = eventActive ? plugin.events().hudScore(player) : "";
            String eventTime = eventActive ? TimeFormat.mmss(plugin.events().remainingSeconds()) : "";
            String chatQuestion = chatGameActive ? plugin.chatGames().hudQuestionText() : "";
            String chatTime = chatGameActive ? TimeFormat.mmss(plugin.chatGames().remainingSeconds()) : "";
            String keyCount = Integer.toString(plugin.keyChest().countKeys(player));

            next.put(player.getUniqueId(), new HudSnapshot(
                    Boolean.toString(eventActive),
                    eventName,
                    eventScore,
                    eventTime,
                    Boolean.toString(chatGameActive),
                    chatQuestion,
                    chatTime,
                    keyCount
            ));
        });
        snapshots = Map.copyOf(next);
    }

    /** Reloads BetterHud after the integration files and default-hud entry are installed. */
    public static boolean reload() {
        ReloadState state = BetterHud.getInstance().reload();
        if (state instanceof ReloadState.Failure failure) {
            throw new IllegalStateException("BetterHud reload failed", failure.throwable());
        }
        return state instanceof ReloadState.Success;
    }

    public static void clear() {
        snapshots = Map.of();
    }

    private record HudSnapshot(
            String eventActive,
            String eventName,
            String eventScore,
            String eventTime,
            String chatGameActive,
            String chatGameQuestion,
            String chatGameTime,
            String keyCount
    ) {
    }
}
