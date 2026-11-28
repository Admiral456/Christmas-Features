package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChristmasCommand implements CommandExecutor, TabCompleter {
    private final ChristmasFeaturesPlugin plugin;

    public ChristmasCommand(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        boolean publicGrinchOpen = args[0].equalsIgnoreCase("grinch")
                && args.length >= 2 && args[1].equalsIgnoreCase("open");
        boolean publicCosmetics = args[0].equalsIgnoreCase("cosmetics") && args.length == 1;
        boolean publicStatus = args[0].equalsIgnoreCase("status");
        if (!publicGrinchOpen && !publicCosmetics && !publicStatus && !sender.hasPermission("christmas.admin")) {
            sender.sendMessage("§cNa tento příkaz nemáš oprávnění.");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> handleStatus(sender);
            case "reload" -> {
                plugin.reloadConfig();
                plugin.events().scheduleAutoStart();
                plugin.chatGames().reloadTasks();
                plugin.chatGames().scheduleAutoStart();
                plugin.winter().refresh();
                plugin.getLogger().info("Konfigurace ChristmasFeatures byla znovu načtena.");
                sender.sendMessage("§aChristmasFeatures: konfigurace byla znovu načtena.");
                if (plugin.winter().isManagedExternally()) {
                    sender.sendMessage("§eZimní svět spravuje ChristmasSeason. Jeho nastavení načti příkazem /xmas reload.");
                }
            }
            case "event" -> handleEvent(sender, args);
            case "chatgame" -> handleChatGame(sender, args);
            case "trail" -> handleTrail(sender, args);
            case "cosmetics" -> handleCosmetics(sender, args);
            case "grinch" -> handleGrinch(sender, args);
            case "npc" -> handleNpc(sender, args);
            case "winter" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("apply")) {
                    if (plugin.winter().isManagedExternally()) {
                        sender.sendMessage("§eZimní biomy spravuje ChristmasSeason. Použij /xmas biome convert-all <radius> <world> confirm.");
                    } else {
                        plugin.winter().refresh();
                        sender.sendMessage("§aZimní biom se znovu aplikuje na načtené chunky. Nové chunky se upraví automaticky.");
                    }
                } else sender.sendMessage("§ePoužití: /christmas winter apply");
            }
            default -> help(sender);
        }
        return true;
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§ePoužití: /christmas event start gifts|ornaments nebo /christmas event stop");
            return;
        }
        if (args[1].equalsIgnoreCase("stop")) {
            plugin.events().stop("Event byl ukončen správcem.", true);
            sender.sendMessage("§aPožadavek na ukončení eventu byl zpracován.");
            return;
        }
        if (!args[1].equalsIgnoreCase("start") || args.length < 3) {
            sender.sendMessage("§ePoužití: /christmas event start gifts|ornaments");
            return;
        }
        boolean started = plugin.events().start(args[2]);
        sender.sendMessage(started ? "§aVánoční event byl spuštěn." : "§cEvent nešel spustit. Ověř typ, svět a zda už neběží jiný event.");
    }

    private void handleChatGame(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§ePoužití: /christmas chatgame start|stop");
            return;
        }
        if (args[1].equalsIgnoreCase("start")) {
            boolean started = plugin.chatGames().start();
            sender.sendMessage(started ? "§aVánoční ChatGame byl spuštěn." : "§cChatGame nelze spustit. Zkontroluj úkoly nebo zda už neběží.");
        } else if (args[1].equalsIgnoreCase("stop")) {
            plugin.chatGames().stop("ChatGame byl ukončen správcem.", true);
            sender.sendMessage("§aChatGame byl zastaven.");
        } else sender.sendMessage("§ePoužití: /christmas chatgame start|stop");
    }

    private void handleTrail(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§ePoužití: /christmas trail give <hráč> <effect-id>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§cHráč není online.");
            return;
        }
        boolean granted = plugin.trailStudio().grant(target, args[3]);
        if (granted) sender.sendMessage("§aTrailStudio kosmetika byla předána hráči " + target.getName() + ".");
        else sender.sendMessage("§cOdemknutí se nepodařilo. Ověř TrailStudio a effect ID.");
    }

    private void handleCosmetics(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§ePoužití: /christmas cosmetics <hráč> [tag|trail|hat]");
                return;
            }
            plugin.cosmetics().openMain(player);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cHráč není online.");
            return;
        }
        if (args.length >= 3) {
            String category = args[2].toLowerCase(Locale.ROOT);
            if (!List.of("tag", "trail", "hat").contains(category)) {
                sender.sendMessage("§eKategorie: tag, trail, hat");
                return;
            }
            plugin.cosmetics().openCategory(target, category);
        } else {
            plugin.cosmetics().openMain(target);
        }
    }

    private void handleStatus(CommandSender sender) {
        String eventStatus = plugin.events().isActive()
                ? "§aProbíhá: " + plugin.events().hudType()
                : "§7Neprobíhá";
        String chatStatus = plugin.chatGames().isActive() ? "§aProbíhá" : "§7Neprobíhá";
        String winterStatus = plugin.winter().isManagedExternally()
                ? "§aChristmasSeason"
                : plugin.getConfig().getBoolean("winter.enabled", true)
                    ? "§aChristmasFeatures" : "§cVypnuto";
        String hudStatus = plugin.betterHud().isAvailable() ? "§aBetterHud" : "§eFallback";
        String trailStatus = plugin.trailStudio().isAvailable() ? "§aTrailStudio" : "§7Není nainstalováno";
        boolean papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        sender.sendMessage("§b§lChristmasFeatures §7| §fStav systému");
        sender.sendMessage("§7Event: " + eventStatus);
        sender.sendMessage("§7ChatGame: " + chatStatus + " §8| §7Úkolů: §f" + plugin.chatGames().taskCount());
        sender.sendMessage("§7Zimní svět: " + winterStatus);
        sender.sendMessage("§7HUD: " + hudStatus + " §8| §7PlaceholderAPI: " + (papi ? "§aAno" : "§eNe"));
        sender.sendMessage("§7TrailStudio: " + trailStatus);
    }

    private void handleGrinch(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("open")) {
            sender.sendMessage("§ePoužití: /christmas grinch open [hráč]");
            return;
        }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
        } else {
            target = sender instanceof Player player ? player : null;
        }
        if (target == null) {
            sender.sendMessage("§cHráč není online. Použití: /christmas grinch open <hráč>");
            return;
        }
        plugin.grinchMenu().open(target);
    }

    private void handleNpc(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("spawn")) {
            sender.sendMessage("§ePoužití: /christmas npc spawn");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTento příkaz musíš spustit přímo ve hře.");
            return;
        }
        plugin.grinchNpc().spawn(player);
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§b§lChristmasFeatures §7— příkazy");
        sender.sendMessage("§f/christmas event start gifts|ornaments §7— spustí event");
        sender.sendMessage("§f/christmas event stop §7— ukončí event");
        sender.sendMessage("§f/christmas chatgame start|stop §7— správa ChatGames");
        sender.sendMessage("§f/christmas cosmetics §7— otevře tagy, trail efekty a čepice");
        sender.sendMessage("§f/christmas status §7— zobrazí stav hlavních systémů");
        sender.sendMessage("§f/christmas trail give <hráč> <effect-id> §7— odemkne TrailStudio kosmetiku");
        sender.sendMessage("§f/christmas grinch open [hráč] §7— otevře Grinchovo menu");
        sender.sendMessage("§f/christmas npc spawn §7— umístí Grinche na tvou pozici");
        sender.sendMessage("§f/christmas winter apply §7— znovu aplikuje zimu na načtené chunky");
        sender.sendMessage("§f/christmas reload §7— načte konfiguraci");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            return filter(List.of("help", "status", "event", "chatgame", "cosmetics", "trail", "grinch", "npc", "winter", "reload"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "event" -> filter(List.of("start", "stop"), args[1]);
                case "chatgame" -> filter(List.of("start", "stop"), args[1]);
                case "trail" -> filter(List.of("give"), args[1]);
                case "cosmetics" -> Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
                case "grinch" -> filter(List.of("open"), args[1]);
                case "npc" -> filter(List.of("spawn"), args[1]);
                case "winter" -> filter(List.of("apply"), args[1]);
                default -> suggestions;
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("cosmetics")) {
            return filter(List.of("tag", "trail", "hat"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("start")) {
            return filter(List.of("gifts", "ornaments"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("trail") && args[1].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("grinch") && args[1].equalsIgnoreCase("open")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        return suggestions;
    }

    private List<String> filter(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
