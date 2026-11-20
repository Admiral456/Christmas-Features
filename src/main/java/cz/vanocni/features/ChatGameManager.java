package cz.vanocni.features;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatGameManager implements Listener {
    private final ChristmasFeaturesPlugin plugin;
    private final Random random = new Random();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private List<ChatTask> tasks = new ArrayList<>();
    private volatile GameState active;
    private BukkitTask autoTask;
    private BukkitTask endTask;

    public ChatGameManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
        reloadTasks();
    }

    public void startScheduler() {
        scheduleAutoStart();
    }

    public void reloadTasks() {
        List<ChatTask> loaded = new ArrayList<>();
        for (Map<?, ?> section : plugin.getConfig().getMapList("chatgames.tasks")) {
            Object questionValue = section.get("question");
            Object answerValue = section.get("answer");
            if (questionValue == null || answerValue == null) continue;
            String question = String.valueOf(questionValue).trim();
            String answer = String.valueOf(answerValue).trim();
            if (question.isEmpty() || answer.isEmpty()) continue;
            List<String> aliases = new ArrayList<>();
            Object aliasesValue = section.get("aliases");
            if (aliasesValue instanceof List<?> list) {
                for (Object alias : list) aliases.add(String.valueOf(alias));
            }
            loaded.add(new ChatTask(question, answer, aliases));
        }
        tasks = loaded;
    }

    public void scheduleAutoStart() {
        if (autoTask != null) autoTask.cancel();
        if (!plugin.getConfig().getBoolean("chatgames.auto-start.enabled", true)) return;
        long minutes = Math.max(1L, plugin.getConfig().getLong("chatgames.auto-start.interval-minutes", 20L));
        autoTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (active == null && !Bukkit.getOnlinePlayers().isEmpty()) start();
        }, minutes * 60L * 20L, minutes * 60L * 20L);
    }

    public boolean start() {
        if (active != null || tasks.isEmpty()) return false;
        ChatTask selected = tasks.get(random.nextInt(tasks.size()));
        int duration = Math.max(10, plugin.getConfig().getInt("chatgames.duration-seconds", 60));
        GameState state = new GameState(selected, System.currentTimeMillis() + duration * 1000L);
        active = state;
        Bukkit.broadcastMessage("§d§l✦ VÁNOČNÍ CHATGAME ✦");
        Bukkit.broadcastMessage("§f" + selected.question());
        Bukkit.broadcastMessage("§7První správná odpověď vyhrává! Máte §d" + TimeFormat.mmss(duration) + "§7.");
        endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> expire(state), duration * 20L);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        GameState state = active;
        if (state == null || state.claimed().get()) return;
        String message = plainText.serialize(event.message()).trim();
        if (!isCorrect(state.task(), message)) return;
        if (!state.claimed().compareAndSet(false, true)) return;

        event.setCancelled(true);
        UUID winnerId = event.getPlayer().getUniqueId();
        String winnerName = event.getPlayer().getName();
        Bukkit.getScheduler().runTask(plugin, () -> finishWinner(state, winnerId, winnerName));
    }

    private boolean isCorrect(ChatTask task, String input) {
        String normalized = normalize(input);
        if (normalize(task.answer()).equals(normalized)) return true;
        return task.aliases().stream().anyMatch(alias -> normalize(alias).equals(normalized));
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private void finishWinner(GameState state, UUID winnerId, String winnerName) {
        if (active != state) return;
        if (endTask != null) endTask.cancel();
        List<String> commands = plugin.getConfig().getStringList("cosmetics.chatgames.winner-commands");
        RewardCommands.dispatch(plugin, commands, winnerName, winnerId.toString(), Map.of("answer", state.task().answer()));
        Bukkit.broadcastMessage("§a§l✔ " + winnerName + " úspěšně splnil vánoční úkol jako první a získává odměnu!");
        active = null;
    }

    private void expire(GameState state) {
        if (active != state || state.claimed().get()) return;
        Bukkit.broadcastMessage("§7Čas na vánoční úkol vypršel. Nikdo neodpověděl správně.");
        active = null;
    }

    public void stop(String reason, boolean announce) {
        GameState state = active;
        if (state == null) return;
        if (endTask != null) endTask.cancel();
        active = null;
        if (announce) Bukkit.broadcastMessage("§d[ ChatGame ] §7" + reason);
    }

    public boolean isActive() {
        return active != null;
    }

    public String hudQuestion() {
        return active == null ? "" : "Odpověz do chatu";
    }

    public long remainingSeconds() {
        if (active == null) return 0L;
        return Math.max(0L, (active.endsAtMillis() - System.currentTimeMillis() + 999L) / 1000L);
    }

    private record ChatTask(String question, String answer, List<String> aliases) { }
    private record GameState(ChatTask task, long endsAtMillis, AtomicBoolean claimed) {
        private GameState(ChatTask task, long endsAtMillis) {
            this(task, endsAtMillis, new AtomicBoolean(false));
        }
    }
}
