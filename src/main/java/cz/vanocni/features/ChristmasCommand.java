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
        if (!sender.hasPermission("christmas.admin")) {
            sender.sendMessage("§cNa tento příkaz nemáš oprávnění.");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.events().scheduleAutoStart();
                plugin.chatGames().reloadTasks();
                plugin.chatGames().scheduleAutoStart();
                plugin.winter().refresh();
                plugin.getLogger().info("Konfigurace ChristmasFeatures byla znovu načtena.");
                sender.sendMessage("§aChristmasFeatures: konfigurace byla znovu načtena.");
            }
            case "event" -> handleEvent(sender, args);
            case "chatgame" -> handleChatGame(sender, args);
            case "key" -> handleKey(sender, args);
            case "chest" -> handleChest(sender, args);
            case "winter" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("apply")) {
                    plugin.winter().refresh();
                    sender.sendMessage("§aZimní biom se znovu aplikuje na načtené chunky. Nové chunky se upraví automaticky.");
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

    private void handleKey(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage("§ePoužití: /christmas key give <hráč> [počet]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§cHráč není online.");
            return;
        }
        int amount = 1;
        if (args.length > 3) {
            try { amount = Integer.parseInt(args[3]); }
            catch (NumberFormatException exception) {
                sender.sendMessage("§cPočet musí být celé číslo.");
                return;
            }
        }
        amount = Math.max(1, Math.min(64, amount));
        plugin.keyChest().giveKey(target, amount);
        sender.sendMessage("§aPředal jsi " + amount + "× vánoční klíč hráči " + target.getName() + ".");
    }

    private void handleChest(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("set")) {
            sender.sendMessage("§ePoužití: /christmas chest set (dívej se na truhlu nebo sud na spawnu)");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTento příkaz může použít pouze hráč ve hře.");
            return;
        }
        plugin.keyChest().setChest(player);
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§b§lChristmasFeatures §7— příkazy");
        sender.sendMessage("§f/christmas event start gifts|ornaments §7— spustí event");
        sender.sendMessage("§f/christmas event stop §7— ukončí event");
        sender.sendMessage("§f/christmas chatgame start|stop §7— správa ChatGames");
        sender.sendMessage("§f/christmas key give <hráč> [počet] §7— vydá klíč");
        sender.sendMessage("§f/christmas chest set §7— nastaví zamčenou bednu na cílovém bloku");
        sender.sendMessage("§f/christmas winter apply §7— znovu aplikuje zimu na načtené chunky");
        sender.sendMessage("§f/christmas reload §7— načte konfiguraci znovu");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) return filter(List.of("help", "event", "chatgame", "key", "chest", "winter", "reload"), args[0]);
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "event" -> filter(List.of("start", "stop"), args[1]);
                case "chatgame" -> filter(List.of("start", "stop"), args[1]);
                case "key" -> filter(List.of("give"), args[1]);
                case "chest" -> filter(List.of("set"), args[1]);
                case "winter" -> filter(List.of("apply"), args[1]);
                default -> suggestions;
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("start")) {
            return filter(List.of("gifts", "ornaments"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("key") && args[1].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        return suggestions;
    }

    private List<String> filter(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
