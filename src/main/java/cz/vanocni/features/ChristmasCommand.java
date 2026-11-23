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
                && args.length == 2 && args[1].equalsIgnoreCase("open");
        if (!publicGrinchOpen && !sender.hasPermission("christmas.admin")) {
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
                if (plugin.winter().isManagedExternally()) {
                    sender.sendMessage("§eZimní svět spravuje ChristmasSeason. Jeho nastavení načti příkazem /xmas reload.");
                }
            }
            case "event" -> handleEvent(sender, args);
            case "chatgame" -> handleChatGame(sender, args);
            case "key" -> handleKey(sender, args);
            case "chest" -> handleChest(sender, args);
            case "trail" -> handleTrail(sender, args);
            case "grinch" -> handleGrinch(sender, args);
            case "mountains" -> handleMountains(sender);
            case "diagnostics" -> new ChristmasDiagnostics(plugin).print(sender);
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

    private void handleMountains(CommandSender sender) {
        sender.sendMessage("§b§lChristmas Mountains §7— FAWE instalace");
        sender.sendMessage("§71. Nahraj .schem do plugins/WorldEdit/schematics/.");
        sender.sendMessage("§72. V cílovém světě world se postav na místo, kde má být počátek hor.");
        sender.sendMessage("§73. Načti schéma: §f//schem load Christmas_Alpine_Range_128x92x128");
        sender.sendMessage("§74. Vlož terén a biomy: §f//paste -ab");
        sender.sendMessage("§7Rozměry: 128 × 92 × 128 bloků. -a přeskočí vzduch, -b vloží biomy.");
        sender.sendMessage("§cPřed vložením zálohuj svět a ověř prázdný prostor. Nevzduchové bloky uvnitř oblasti mohou přepsat stávající terén.");
        sender.sendMessage("§7Při okamžité chybě můžeš zkusit §f//undo§7. Pro jistotu měj zálohu.");
        if (sender instanceof Player player && !player.getWorld().getName().equalsIgnoreCase("world")) {
            sender.sendMessage("§ePrávě jsi ve světě " + player.getWorld().getName() + "; pro tuto schematiku je nastaven svět world.");
        }
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
        sender.sendMessage("§f/christmas trail give <hráč> <effect-id> §7— odemkne TrailStudio kosmetiku");
        sender.sendMessage("§f/christmas grinch open [hráč] §7— otevře Grinchovo menu");
        sender.sendMessage("§f/christmas mountains §7— zobrazí postup vložení horské schematiky přes FAWE");
        sender.sendMessage("§f/christmas diagnostics §7— zkontroluje vánoční integrace a konfigurace serveru");
        sender.sendMessage("§f/christmas chest set §7— nastaví zamčenou bednu na cílovém bloku");
        sender.sendMessage("§f/christmas winter apply §7— znovu aplikuje zimu na načtené chunky");
        sender.sendMessage("§f/christmas reload §7— načte konfiguraci znovu");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) return filter(List.of("help", "event", "chatgame", "key", "chest", "trail", "grinch", "mountains", "diagnostics", "winter", "reload"), args[0]);
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "event" -> filter(List.of("start", "stop"), args[1]);
                case "chatgame" -> filter(List.of("start", "stop"), args[1]);
                case "key" -> filter(List.of("give"), args[1]);
                case "chest" -> filter(List.of("set"), args[1]);
                case "trail" -> filter(List.of("give"), args[1]);
                case "grinch" -> filter(List.of("open"), args[1]);
                case "mountains" -> suggestions;
                case "diagnostics" -> suggestions;
                case "winter" -> filter(List.of("apply"), args[1]);
                default -> suggestions;
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("start")) {
            return filter(List.of("gifts", "ornaments"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("trail") && args[1].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("grinch") && args[1].equalsIgnoreCase("open")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
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
