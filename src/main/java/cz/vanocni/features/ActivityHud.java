package cz.vanocni.features;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Right-side event panel, with a BossBar fallback for servers that already use a sidebar scoreboard. */
public final class ActivityHud implements Listener {
    private final ChristmasFeaturesPlugin plugin;
    private final HolidayEventManager events;
    private final ChatGameManager chatGames;
    private final Map<UUID, ScoreboardSession> sessions = new HashMap<>();
    private final BossBar bossBar = Bukkit.createBossBar("❄ Vánoční event", BarColor.BLUE, BarStyle.SEGMENTED_10);
    private BukkitTask updateTask;
    private boolean wasActive;
    private String displayMode = "SIDEBAR";

    public ActivityHud(ChristmasFeaturesPlugin plugin, HolidayEventManager events, ChatGameManager chatGames) {
        this.plugin = plugin;
        this.events = events;
        this.chatGames = chatGames;
    }

    public void start() {
        String requestedMode = plugin.getConfig().getString("hud.mode", "SIDEBAR");
        if ("BETTERHUD".equalsIgnoreCase(requestedMode)) {
            if (plugin.betterHud() != null && plugin.betterHud().isAvailable()) {
                displayMode = "BETTERHUD";
                plugin.getLogger().info("Vánoční event HUD vykresluje BetterHud.");
                return;
            }
            displayMode = plugin.getConfig().getString("hud.fallback-mode", "SIDEBAR");
            plugin.getLogger().warning("BetterHud není dostupný; použit náhradní HUD režim: " + displayMode);
        } else {
            displayMode = requestedMode;
        }

        if ("SIDEBAR".equalsIgnoreCase(displayMode)) {
            plugin.getLogger().warning("HUD používá postranní scoreboard. Pokud ho už zobrazuje jiný plugin, nastav hud.fallback-mode: BOSSBAR.");
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 1L, 20L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!"BETTERHUD".equalsIgnoreCase(displayMode)) {
            Bukkit.getScheduler().runTaskLater(plugin, this::update, 2L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
        bossBar.removePlayer(event.getPlayer());
    }

    public void update() {
        if ("BETTERHUD".equalsIgnoreCase(displayMode)) return;
        boolean active = events.isActive() || chatGames.isActive();
        if (!active) {
            if (wasActive) restoreAll();
            wasActive = false;
            return;
        }
        wasActive = true;
        if ("BOSSBAR".equalsIgnoreCase(displayMode)) {
            updateBossBar();
        } else {
            bossBar.removeAll();
            updateSidebars();
        }
    }

    private void updateSidebars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ScoreboardSession session = sessions.get(player.getUniqueId());
            if (session == null) {
                ScoreboardManager manager = Bukkit.getScoreboardManager();
                if (manager == null) continue;
                Scoreboard previous = player.getScoreboard();
                Scoreboard board = manager.getNewScoreboard();
                Objective objective = board.registerNewObjective("christmas_event", Criteria.DUMMY,
                        RewardCommands.component("§b§l❄ VÁNOČNÍ UDÁLOSTI ❄"));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                session = new ScoreboardSession(previous, board, objective, new ArrayList<>());
                sessions.put(player.getUniqueId(), session);
                player.setScoreboard(board);
            }

            List<String> lines = createLines(player);
            for (String old : session.lines()) session.board().resetScores(old);
            session.lines().clear();
            int score = lines.size();
            for (String line : lines) {
                String unique = line;
                while (session.lines().contains(unique)) unique = unique + "§r";
                session.objective().getScore(unique).setScore(score--);
                session.lines().add(unique);
            }
            if (player.getScoreboard() != session.board()) player.setScoreboard(session.board());
        }
    }

    private List<String> createLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("§8                    ");
        if (events.isActive()) {
            lines.add("§b✦ §f" + events.hudType());
            lines.add("§7Nasbíráno: §f" + events.hudScore(player));
            lines.add("§7Zbývá: §b" + TimeFormat.mmss(events.remainingSeconds()));
        }
        if (chatGames.isActive()) {
            lines.add("§d✧ §fVánoční ChatGame");
            lines.add("§7Úkol: §fOdpověz do chatu");
            lines.add("§7Zbývá: §d" + TimeFormat.mmss(chatGames.remainingSeconds()));
        }
        lines.add("§8                    ");
        lines.add("§b❄ §7Vánoční sezóna");
        return lines;
    }

    private void updateBossBar() {
        String title;
        double progress;
        if (events.isActive()) {
            title = "❄ " + events.hudType() + "  |  " + TimeFormat.mmss(events.remainingSeconds());
            progress = Math.min(1.0, events.remainingSeconds() / (double) Math.max(1,
                    plugin.getConfig().getInt("events." + (events.hudType().contains("dárků") ? "gifts" : "ornaments") + ".duration-seconds", 300)));
        } else {
            title = "✦ Vánoční ChatGame  |  " + TimeFormat.mmss(chatGames.remainingSeconds());
            progress = Math.min(1.0, chatGames.remainingSeconds() / (double) Math.max(1,
                    plugin.getConfig().getInt("chatgames.duration-seconds", 60)));
        }
        bossBar.setTitle(title);
        bossBar.setProgress(Math.max(0.0, progress));
        for (Player player : Bukkit.getOnlinePlayers()) bossBar.addPlayer(player);
        bossBar.setVisible(true);
    }

    private void restoreAll() {
        for (Map.Entry<UUID, ScoreboardSession> entry : sessions.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) player.setScoreboard(entry.getValue().previous());
        }
        sessions.clear();
        bossBar.removeAll();
        bossBar.setVisible(false);
    }

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        restoreAll();
    }

    private record ScoreboardSession(Scoreboard previous, Scoreboard board, Objective objective, List<String> lines) { }
}
