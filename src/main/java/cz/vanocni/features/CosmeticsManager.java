package cz.vanocni.features;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CosmeticsManager implements Listener {
    public static final String VIP_PERMISSION = "christmas.cosmetics.vip";

    private final ChristmasFeaturesPlugin plugin;
    private final File dataFile;
    private final YamlConfiguration data;
    private final org.bukkit.NamespacedKey categoryKey;
    private final org.bukkit.NamespacedKey idKey;
    private final org.bukkit.NamespacedKey hatKey;
    private final Map<UUID, TrailPreview> trailPreviews = new HashMap<>();
    private BukkitTask particleTask;
    private long animationTick;

    private static final List<Cosmetic> TAGS = List.of(
            new Cosmetic("winter_wanderer", "❄ Zimní poutník", false, Material.SNOWBALL,
                    "Vánoční cestovatel"),
            new Cosmetic("gift_hunter", "🎁 Lovec dárků", false, Material.CHEST_MINECART,
                    "Sběratel vánočních překvapení"),
            new Cosmetic("snow_guardian", "☃ Strážce sněhu", false, Material.PACKED_ICE,
                    "Ochránce zimního světa"),
            new Cosmetic("aurora_royal", "✦ Aurora VIP", true, Material.AMETHYST_SHARD,
                    "Královský titul pro VIP"),
            new Cosmetic("ice_sovereign", "♛ Ledový vládce", true, Material.BLUE_ICE,
                    "Ultimátní titul pro VIP")
    );

    private static final List<Cosmetic> TRAILS = List.of(
            // Standard trails – all visible without a client-side mod or resource pack.
            new Cosmetic("snow_orbit", "Sněhová spirála", false, Material.SNOW_BLOCK,
                    "Čtyři vločkové proudy obíhají kolem postavy a kreslí jemnou zimní spirálu"),
            new Cosmetic("aurora_ribbon", "Polární záře", false, Material.PRISMARINE_CRYSTALS,
                    "Dvě barevné stuhy tyrkysové, zelené a ledově modré světelné záře"),
            new Cosmetic("stardust", "Hvězdný prach", false, Material.NETHER_STAR,
                    "Zlaté hvězdy a třpytivá stopa, která se rozsvítí při pohybu"),
            new Cosmetic("comet_wake", "Kometa zimy", false, Material.PRISMARINE_SHARD,
                    "Jasná ledová hlava komety a dlouhý světelný ocas za hráčem"),
            new Cosmetic("winter_fox", "Polární liška", false, Material.FOX_SPAWN_EGG,
                    "Elegantní modrobílý ocas z polární záře a dvě jiskřící ouška"),
            new Cosmetic("crystal_heart", "Srdce z křišťálu", false, Material.HEART_OF_THE_SEA,
                    "Vznášející se krystalické srdce z ledových světelných bodů"),
            new Cosmetic("snow_globe", "Sněžítko", false, Material.SNOWBALL,
                    "Jemná kulová bariéra s poletujícími vločkami uvnitř"),

            // VIP trails – larger silhouettes, multiple animated layers and richer colour.
            new Cosmetic("celestial_wings", "Nebeská křídla", true, Material.PHANTOM_MEMBRANE,
                    "Zlatá rozevírací křídla s vykreslenými pery a jiskřivou hranou"),
            new Cosmetic("void_crown", "Koruna prázdnoty", true, Material.DRAGON_BREATH,
                    "Dvojitá fialová koruna z portálových run a temných jisker"),
            new Cosmetic("frost_phoenix", "Ledový fénix", true, Material.BLUE_ICE,
                    "Velká ledová křídla, plamenná koruna a pravidelné mrazivé výbuchy"),
            new Cosmetic("prismatic_dragon", "Prismatický drak", true, Material.END_CRYSTAL,
                    "Dvě propletené duhové dračí spirály s krystalovými záblesky"),
            new Cosmetic("celestial_tempest", "Nebeská bouře", true, Material.NETHER_STAR,
                    "Zlatá koruna a dvě protínající se orbitální bouře kolem těla"),
            new Cosmetic("eclipse_dragon", "Drak zatmění", true, Material.DRAGON_EGG,
                    "Temná dračí křídla, dvojitá zatměná aura a fialové portálové částice"),
            new Cosmetic("aurora_seraph", "Aurorový seraf", true, Material.ELYTRA,
                    "Rozsáhlá polární křídla a zlatá svatozář nad hlavou"),
            new Cosmetic("astral_guardian", "Astrální strážce", true, Material.END_CRYSTAL,
                    "Tři rotující ochranné prstence a hvězdné body kolem hráče"),
            new Cosmetic("frost_leviathan", "Ledový leviatan", true, Material.PRISMARINE,
                    "Obří ledový had stoupající kolem těla s mrazivými rohy")
    );

    private static final List<Cosmetic> HATS = List.of(
            new Cosmetic("santa_cap", "Santova čepice", false, Material.LEATHER_HELMET,
                    "Červená vánoční čepice"),
            new Cosmetic("elf_cap", "Skřítkova čepice", false, Material.LEATHER_HELMET,
                    "Zelená zimní čepice"),
            new Cosmetic("snow_cap", "Sněhová čepice", false, Material.LEATHER_HELMET,
                    "Světlá čepice pro zimní počasí"),
            new Cosmetic("frost_crown", "Koruna věčného ledu", true, Material.DIAMOND_HELMET,
                    "VIP koruna s ledovým leskem"),
            new Cosmetic("aurora_crown", "Koruna polární záře", true, Material.GOLDEN_HELMET,
                    "Zlatá VIP koruna")
    );

    public CosmeticsManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "cosmetics.yml");
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Nelze vytvořit složku pro vánoční kosmetiku.");
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        this.categoryKey = new org.bukkit.NamespacedKey(plugin, "cosmetic_category");
        this.idKey = new org.bukkit.NamespacedKey(plugin, "cosmetic_id");
        this.hatKey = new org.bukkit.NamespacedKey(plugin, "cosmetic_hat");
    }

    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) applySaved(player);
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickTrails, 2L, 4L);
    }

    public void openMain(Player player) {
        MenuHolder holder = new MenuHolder("main", 0);
        Inventory inventory = Bukkit.createInventory(holder, 45,
                RewardCommands.component("§b§lVánoční kosmetika"));
        holder.setInventory(inventory);
        fill(inventory);

        inventory.setItem(20, menuItem(Material.NAME_TAG, "§d§lChat tagy",
                List.of("§7Vyber si vlastní vánoční označení.", "§eKlikni pro výběr."), "open", "tag"));
        inventory.setItem(22, menuItem(Material.FIREWORK_STAR, "§b§lTrail efekty",
                List.of("§7Částice, světelné stopy a aury.", "§eKlikni pro výběr."), "open", "trail"));
        inventory.setItem(24, menuItem(Material.LEATHER_HELMET, "§6§lČepice a koruny",
                List.of("§7Kosmetické pokrývky hlavy.", "§eKlikni pro výběr."), "open", "hat"));
        inventory.setItem(40, menuItem(Material.BARRIER, "§cZavřít", List.of(), "close", "close"));
        player.openInventory(inventory);
        player.playSound(player.getLocation(), "ui.button.click", 0.65f, 1.15f);
    }

    public void openCategory(Player player, String category) {
        openCategory(player, category, 0);
    }

    private void openCategory(Player player, String category, int requestedPage) {
        List<Cosmetic> options = switch (category) {
            case "tag" -> TAGS;
            case "trail" -> TRAILS;
            case "hat" -> HATS;
            default -> List.of();
        };
        String title = switch (category) {
            case "tag" -> "§d§lVánoční tagy";
            case "trail" -> "§b§lTrail efekty";
            case "hat" -> "§6§lČepice a koruny";
            default -> "§bKosmetika";
        };

        final int pageSize = 14;
        int maxPage = Math.max(0, (options.size() - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        MenuHolder holder = new MenuHolder(category, page);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                RewardCommands.component(title + " §8(" + (page + 1) + "/" + (maxPage + 1) + ")"));
        holder.setInventory(inventory);
        fill(inventory);

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int start = page * pageSize;
        for (int i = start; i < options.size() && i < start + pageSize; i++) {
            Cosmetic cosmetic = options.get(i);
            boolean locked = cosmetic.vipOnly() && !player.hasPermission(VIP_PERMISSION);
            String current = equipped(category, player.getUniqueId());
            String status = cosmetic.id().equals(current) ? "§aPrávě používáš" :
                    locked ? "§cPouze pro VIP" : "§eKlikni pro vybavení";
            List<String> lore = new ArrayList<>();
            lore.add("§7" + cosmetic.description());
            lore.add(" ");
            lore.add(cosmetic.vipOnly() ? "§6✦ VIP kosmetika" : "§aDostupné všem hráčům");
            lore.add(status);
            if (category.equals("trail")) {
                lore.add("§bPravý klik: náhled na "
                        + Math.max(2, Math.min(30, plugin.getConfig().getLong("cosmetics.trails.preview-seconds", 8L)))
                        + " s");
                lore.add("§eLevý klik: vybavit");
            }
            inventory.setItem(slots[i - start], menuItem(cosmetic.icon(), cosmetic.label(), lore, category, cosmetic.id()));
        }

        if (page > 0) {
            inventory.setItem(45, menuItem(Material.ARROW, "§ePředchozí stránka",
                    List.of("§7Zobrazí předchozí kosmetiku."), "prev", category));
        }
        inventory.setItem(48, menuItem(Material.BARRIER, "§cSundat / vypnout",
                List.of("§7Vypne vybraný tag nebo trail.", "§7U čepice obnoví původní helmu."), "clear", category));
        inventory.setItem(49, menuItem(Material.ARROW, "§eZpět",
                List.of("§7Návrat do kosmetického menu."), "back", "main"));
        inventory.setItem(50, menuItem(Material.BARRIER, "§cZavřít", List.of(), "close", "close"));
        if (page < maxPage) {
            inventory.setItem(53, menuItem(Material.ARROW, "§eDalší stránka",
                    List.of("§7Zobrazí další kosmetiku."), "next", category));
        }
        player.openInventory(inventory);
        player.playSound(player.getLocation(), "ui.button.click", 0.65f, 1.15f);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String action = clicked.getItemMeta().getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
        String id = clicked.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (action == null || id == null) return;

        if (action.equals("back")) {
            openMain(player);
            return;
        }
        if (action.equals("prev")) {
            openCategory(player, holder.category(), holder.page() - 1);
            return;
        }
        if (action.equals("next")) {
            openCategory(player, holder.category(), holder.page() + 1);
            return;
        }
        if (action.equals("open")) {
            openCategory(player, id);
            return;
        }
        if (action.equals("close")) {
            player.closeInventory();
            return;
        }
        if (action.equals("clear")) {
            String path = path(player);
            switch (id) {
                case "tag" -> data.set(path + ".tag", null);
                case "trail" -> {
                    data.set(path + ".trail", null);
                    trailPreviews.remove(player.getUniqueId());
                }
                case "hat" -> clearHat(player);
                default -> { return; }
            }
            save();
            player.sendMessage("§aKosmetika byla vypnuta.");
            openCategory(player, id, holder.page());
            return;
        }
        if (!List.of("tag", "trail", "hat").contains(action)) return;

        Cosmetic cosmetic = find(action, id);
        if (cosmetic == null) return;
        if (cosmetic.vipOnly() && !player.hasPermission(VIP_PERMISSION)) {
            player.sendMessage("§cTato kosmetika je dostupná pouze hráčům s VIP oprávněním.");
            player.playSound(player.getLocation(), "block.note_block.bass", 0.6f, 0.7f);
            return;
        }

        if (action.equals("trail") && event.isRightClick()) {
            long previewSeconds = Math.max(2L,
                    Math.min(30L, plugin.getConfig().getLong("cosmetics.trails.preview-seconds", 8L)));
            trailPreviews.put(player.getUniqueId(),
                    new TrailPreview(id, System.currentTimeMillis() + previewSeconds * 1000L));
            player.sendMessage("§bNáhled efektu §f" + cosmetic.label()
                    + " §bje aktivní na " + previewSeconds + " sekund. Tvůj uložený trail se nezměnil.");
            player.playSound(player.getLocation(), "block.amethyst_block.chime", 0.7f, 1.4f);
            return;
        }

        if (action.equals("trail")) trailPreviews.remove(player.getUniqueId());

        switch (action) {
            case "tag" -> {
                data.set(path(player) + ".tag", id);
                save();
                player.sendMessage("§aVybavil sis tag: " + cosmetic.label() + "§a.");
            }
            case "trail" -> {
                data.set(path(player) + ".trail", id);
                save();
                player.sendMessage("§aVybavil sis efekt: §b" + cosmetic.label() + "§a.");
            }
            case "hat" -> equipHat(player, id);
            default -> {
                return;
            }
        }
        player.playSound(player.getLocation(), "entity.experience_orb.pickup", 0.7f, 1.4f);
        openCategory(player, action, holder.page());
    }

    private void equipHat(Player player, String id) {
        String path = path(player);
        if (data.getBoolean(path + ".hat-suspended", false)
                || !data.getBoolean(path + ".hat-captured", false)) {
            captureCurrentHelmet(player);
        }
        data.set(path + ".hat-suspended", false);
        data.set(path + ".hat", id);
        save();
        player.getInventory().setHelmet(createHat(id));
        Cosmetic cosmetic = find("hat", id);
        player.sendMessage("§aVybavil sis pokrývku hlavy: §6" + (cosmetic == null ? id : cosmetic.label()) + "§a.");
    }

    private void captureCurrentHelmet(Player player) {
        String path = path(player);
        ItemStack current = player.getInventory().getHelmet();
        if (!isCosmeticHat(current) && current != null && !current.getType().isAir()) {
            data.set(path + ".original-helmet", current.clone());
            data.set(path + ".had-original-helmet", true);
        } else {
            data.set(path + ".original-helmet", null);
            data.set(path + ".had-original-helmet", false);
        }
        data.set(path + ".hat-captured", true);
        data.set(path + ".hat-suspended", false);
        save();
    }

    public void clearHat(Player player) {
        restoreOriginalHat(player, true);
    }

    /**
     * Temporarily removes a VIP hat while its permission is missing without
     * deleting the player's cosmetic selection. The hat is restored when the
     * LuckPerms permission is granted again.
     */
    private void suspendHat(Player player) {
        if (isCosmeticHat(player.getInventory().getHelmet())) {
            restoreOriginalHat(player, false);
            data.set(path(player) + ".hat-suspended", true);
            save();
        }
    }

    private void restoreOriginalHat(Player player, boolean forgetSelection) {
        String path = path(player);
        boolean suspended = data.getBoolean(path + ".hat-suspended", false);
        if (!suspended && data.getBoolean(path + ".hat-captured", false)) {
            ItemStack original = data.getItemStack(path + ".original-helmet");
            player.getInventory().setHelmet(data.getBoolean(path + ".had-original-helmet", false) ? original : null);
        }
        if (forgetSelection) {
            data.set(path + ".hat", null);
            data.set(path + ".hat-captured", null);
            data.set(path + ".hat-suspended", null);
            data.set(path + ".had-original-helmet", null);
            data.set(path + ".original-helmet", null);
        }
        save();
    }

    public String tagPlaceholder(UUID uuid) {
        if (uuid == null) return "";
        String id = data.getString("players." + uuid + ".tag", "");
        Cosmetic cosmetic = find("tag", id);
        if (cosmetic == null) return "";
        if (cosmetic.vipOnly() && !hasVip(uuid)) return "";
        return "§8[§r" + cosmetic.label() + "§8]§r";
    }

    public String equippedTag(UUID uuid) {
        return data.getString("players." + uuid + ".tag", "");
    }

    private String equipped(String category, UUID uuid) {
        return data.getString("players." + uuid + "." + category, "");
    }

    private boolean hasVip(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.hasPermission(VIP_PERMISSION);
    }

    private void applySaved(Player player) {
        String path = path(player);
        // Trail and tag selections remain stored; their render/placeholder checks hide locked VIP items.

        String hat = data.getString(path + ".hat", "");
        Cosmetic hatCosmetic = find("hat", hat);
        if (hatCosmetic != null && hatCosmetic.vipOnly() && !player.hasPermission(VIP_PERMISSION)) {
            suspendHat(player);
        } else if (hatCosmetic != null) {
            if (data.getBoolean(path + ".hat-suspended", false)
                    || !data.getBoolean(path + ".hat-captured", false)) {
                captureCurrentHelmet(player);
            }
            player.getInventory().setHelmet(createHat(hat));
        }
        save();
    }

    private void tickTrails() {
        animationTick += 4L;
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            TrailPreview preview = trailPreviews.get(uuid);
            String id;
            if (preview != null && preview.expiresAtMillis() > now) {
                id = preview.effectId();
            } else {
                if (preview != null) trailPreviews.remove(uuid);
                id = equipped("trail", uuid);
            }
            if (id.isBlank()) continue;
            Cosmetic cosmetic = find("trail", id);
            if (cosmetic == null) continue;
            if (cosmetic.vipOnly() && !player.hasPermission(VIP_PERMISSION)) {
                // Do not render or erase the choice: it becomes active again
                // automatically when LuckPerms restores the VIP permission.
                continue;
            }
            renderTrail(player, id);
        }
        if (animationTick % 20L == 0L) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String hat = equipped("hat", player.getUniqueId());
                Cosmetic cosmetic = find("hat", hat);
                if (cosmetic == null) continue;
                if (cosmetic.vipOnly() && !player.hasPermission(VIP_PERMISSION)) {
                    suspendHat(player);
                    continue;
                }
                if (!isCosmeticHat(player.getInventory().getHelmet())) {
                    String playerPath = path(player);
                    if (data.getBoolean(playerPath + ".hat-suspended", false)) {
                        // Capture even an empty helmet slot: the player may have changed
                        // equipment while their VIP crown was suspended.
                        captureCurrentHelmet(player);
                    } else {
                        // Preserve any helmet inserted by another plugin before restoring the cosmetic.
                        ItemStack replacement = player.getInventory().getHelmet();
                        if (replacement != null && !replacement.getType().isAir()) {
                            data.set(playerPath + ".original-helmet", replacement.clone());
                            data.set(playerPath + ".had-original-helmet", true);
                            data.set(playerPath + ".hat-captured", true);
                            save();
                        }
                    }
                    player.getInventory().setHelmet(createHat(hat));
                }
            }
        }
    }

    private void renderTrail(Player player, String id) {
        Location base = player.getLocation().add(0, 0.10, 0);
        World world = base.getWorld();
        if (world == null || player.isInvisible()
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;

        Vector direction = base.getDirection().setY(0);
        if (direction.lengthSquared() < 0.001) direction = new Vector(0, 0, 1);
        direction.normalize();
        Vector right = new Vector(direction.getZ(), 0, -direction.getX());
        boolean moving = player.getVelocity().clone().setY(0).lengthSquared() > 0.001
                && player.isOnGround();

        switch (id) {
            case "snow_orbit" -> {
                for (int i = 0; i < 4; i++) {
                    double angle = animationTick * 0.105 + i * Math.PI / 2.0;
                    Location point = base.clone().add(Math.cos(angle) * 0.38,
                            0.18 + (Math.sin(angle * 1.35) + 1.0) * 0.30,
                            Math.sin(angle) * 0.38);
                    world.spawnParticle(Particle.SNOWFLAKE, point, 0);
                    if (i % 2 == 0) dust(world, point.clone().add(0, 0.08, 0),
                            Color.fromRGB(205, 239, 255), 0.9f);
                    if (i == 0) world.spawnParticle(Particle.END_ROD, point, 0);
                }
                if (moving) {
                    Location heel = base.clone().subtract(direction.clone().multiply(0.35));
                    world.spawnParticle(Particle.SNOWFLAKE, heel, 3, 0.18, 0.06, 0.18, 0.01);
                }
            }
            case "aurora_ribbon" -> {
                for (int i = 0; i < 8; i++) {
                    double t = i / 7.0;
                    double angle = animationTick * 0.055 + t * Math.PI * 1.55;
                    double radius = 0.34 + Math.sin(t * Math.PI) * 0.10;
                    Location first = base.clone().add(Math.cos(angle) * radius,
                            0.12 + t * 1.55, Math.sin(angle) * radius);
                    Location second = base.clone().add(Math.cos(angle + 1.25) * radius * 0.82,
                            0.12 + t * 1.55, Math.sin(angle + 1.25) * radius * 0.82);
                    Color primary = t < 0.5 ? Color.fromRGB(50, 245, 180) : Color.fromRGB(75, 190, 255);
                    Color secondary = t < 0.5 ? Color.fromRGB(80, 190, 255) : Color.fromRGB(175, 255, 220);
                    dust(world, first, primary, 1.15f);
                    dust(world, second, secondary, 0.9f);
                    if (i % 3 == 0) world.spawnParticle(Particle.END_ROD, first, 0);
                }
            }
            case "stardust" -> {
                if (moving || animationTick % 8L == 0L) {
                    Location tail = base.clone().subtract(direction.clone().multiply(0.42)).add(0, 0.22, 0);
                    for (int i = 0; i < 6; i++) {
                        double t = i / 5.0;
                        Location point = tail.clone().subtract(direction.clone().multiply(t * 0.95))
                                .add(0, Math.sin(animationTick * 0.09 + i * 0.8) * 0.12, 0);
                        dust(world, point, i % 2 == 0 ? Color.fromRGB(255, 225, 135)
                                : Color.fromRGB(255, 250, 220), 1.0f - (float) t * 0.35f);
                        if (i % 2 == 0) world.spawnParticle(Particle.END_ROD, point, 0);
                    }
                    if (animationTick % 20L == 0L) {
                        world.spawnParticle(Particle.TOTEM_OF_UNDYING, tail, 5, 0.24, 0.28, 0.24, 0.02);
                        world.spawnParticle(Particle.FIREWORK, tail.clone().add(0, 0.25, 0), 0);
                    }
                }
            }
            case "comet_wake" -> {
                Location tail = base.clone().subtract(direction.clone().multiply(0.58)).add(0, 0.30, 0);
                for (int i = 0; i < 7; i++) {
                    double t = i / 6.0;
                    Location point = tail.clone().subtract(direction.clone().multiply(t * 1.2))
                            .add(0, Math.sin(animationTick * 0.11 + i * 0.65) * 0.13, 0);
                    dust(world, point, i < 2 ? Color.fromRGB(210, 248, 255)
                            : Color.fromRGB(75, 190, 255), 1.35f - (float) t * 0.48f);
                    if (i % 2 == 0) world.spawnParticle(Particle.SNOWFLAKE, point, 0);
                }
                world.spawnParticle(Particle.END_ROD, tail.clone().add(0, 0.08, 0), 0);
                if (moving && animationTick % 8L == 0L) {
                    world.spawnParticle(Particle.FIREWORK, tail, 2, 0.08, 0.06, 0.08, 0.01);
                }
            }
            case "winter_fox" -> {
                // A flowing spectral tail, with a small pair of ears above the head.
                for (int i = 0; i < 9; i++) {
                    double t = i / 8.0;
                    double sway = Math.sin(animationTick * 0.105 + t * 2.6) * (0.08 + t * 0.16);
                    Location point = base.clone()
                            .subtract(direction.clone().multiply(0.20 + t * 0.95))
                            .add(right.clone().multiply(sway))
                            .add(0, 0.12 + t * 0.36 + Math.sin(animationTick * 0.12 + t * 2.0) * 0.16, 0);
                    dust(world, point, t > 0.72 ? Color.fromRGB(230, 250, 255)
                            : Color.fromRGB(90, 205, 255), 1.15f - (float) t * 0.3f);
                    if (i % 2 == 0) world.spawnParticle(Particle.SNOWFLAKE, point, 0);
                }
                for (int side : new int[]{-1, 1}) {
                    Location ear = base.clone().add(right.clone().multiply(side * 0.16)).add(0, 1.72, 0);
                    dust(world, ear, Color.fromRGB(185, 245, 255), 1.2f);
                    world.spawnParticle(Particle.END_ROD, ear, 0);
                }
            }
            case "crystal_heart" -> {
                // A heart-shaped crystal emblem floating in front of the player.
                Location center = base.clone().add(direction.clone().multiply(0.55)).add(0, 1.02, 0);
                for (int i = 0; i < 18; i++) {
                    double angle = i * Math.PI * 2.0 / 18.0;
                    double sin = Math.sin(angle);
                    double x = 16.0 * sin * sin * sin;
                    double y = 13.0 * Math.cos(angle) - 5.0 * Math.cos(2.0 * angle)
                            - 2.0 * Math.cos(3.0 * angle) - Math.cos(4.0 * angle);
                    Location point = center.clone().add(right.clone().multiply(x * 0.026))
                            .add(0, y * 0.026, 0);
                    dust(world, point, i % 3 == 0 ? Color.fromRGB(220, 250, 255)
                            : Color.fromRGB(95, 205, 255), 1.15f);
                    if (i % 3 == 0) world.spawnParticle(Particle.SNOWFLAKE, point, 0);
                }
                if (animationTick % 16L == 0L) {
                    world.spawnParticle(Particle.END_ROD, center, 2, 0.14, 0.18, 0.08, 0.01);
                }
            }
            case "snow_globe" -> {
                Location center = base.clone().add(0, 0.95, 0);
                for (int ring = 0; ring < 2; ring++) {
                    double y = ring == 0 ? -0.42 : 0.42;
                    double radius = ring == 0 ? 0.48 : 0.42;
                    for (int i = 0; i < 7; i++) {
                        double angle = animationTick * 0.045 + i * Math.PI * 2.0 / 7.0 + ring * 0.35;
                        Location point = center.clone().add(Math.cos(angle) * radius, y,
                                Math.sin(angle) * radius);
                        dust(world, point, Color.fromRGB(150, 225, 255), 0.9f);
                    }
                }
                if (animationTick % 8L == 0L) {
                    world.spawnParticle(Particle.SNOWFLAKE, center, 4, 0.28, 0.42, 0.28, 0.01);
                }
                world.spawnParticle(Particle.END_ROD, center.clone().add(0, 0.48, 0), 0);
            }
            case "celestial_wings" -> {
                renderWings(player, right, direction, Color.fromRGB(255, 221, 130), false, 1.0);
                if (animationTick % 8L == 0L) {
                    world.spawnParticle(Particle.END_ROD, base.clone().add(0, 1.1, 0), 2, 0.55, 0.55, 0.35, 0.01);
                }
            }
            case "void_crown" -> {
                Location crown = base.clone().add(0, 2.08, 0);
                for (int i = 0; i < 12; i++) {
                    double angle = animationTick * 0.075 + i * Math.PI * 2.0 / 12.0;
                    Location outer = crown.clone().add(Math.cos(angle) * 0.48,
                            Math.sin(angle * 2.0) * 0.08, Math.sin(angle) * 0.48);
                    dust(world, outer, Color.fromRGB(155, 90, 255), 1.15f);
                    if (i % 3 == 0) world.spawnParticle(Particle.REVERSE_PORTAL, outer, 0);
                    Location inner = crown.clone().add(Math.cos(-angle * 1.2) * 0.29,
                            0.12 + Math.sin(angle) * 0.05, Math.sin(-angle * 1.2) * 0.29);
                    world.spawnParticle(Particle.PORTAL, inner, 0);
                }
                if (animationTick % 16L == 0L) {
                    world.spawnParticle(Particle.ENCHANT, crown, 8, 0.38, 0.12, 0.38, 0.12);
                }
            }
            case "frost_phoenix" -> {
                renderWings(player, right, direction, Color.fromRGB(105, 220, 255), true, 1.1);
                Location crest = base.clone().add(0, 2.05, 0);
                for (int i = 0; i < 7; i++) {
                    double t = i / 6.0;
                    Location point = crest.clone().add(right.clone().multiply((t - 0.5) * 0.72))
                            .add(0, Math.sin(t * Math.PI) * 0.20, 0);
                    dust(world, point, Color.fromRGB(215, 250, 255), 1.15f);
                }
                if (animationTick % 20L == 0L) {
                    world.spawnParticle(Particle.SNOWFLAKE, base.clone().add(0, 1.2, 0), 16, 0.55, 0.55, 0.55, 0.02);
                    world.spawnParticle(Particle.SOUL, base.clone().add(0, 1.0, 0), 6, 0.38, 0.45, 0.38, 0.01);
                }
            }
            case "prismatic_dragon" -> {
                for (int strand = 0; strand < 2; strand++) {
                    for (int i = 0; i < 9; i++) {
                        double t = i / 8.0;
                        double angle = animationTick * 0.095 + t * Math.PI * 3.0 + strand * Math.PI;
                        double radius = 0.32 + t * 0.20;
                        Location point = base.clone().add(Math.cos(angle) * radius,
                                0.12 + t * 1.62, Math.sin(angle) * radius);
                        Color color = switch ((i + strand * 2) % 4) {
                            case 0 -> Color.fromRGB(80, 235, 255);
                            case 1 -> Color.fromRGB(170, 105, 255);
                            case 2 -> Color.fromRGB(255, 120, 205);
                            default -> Color.fromRGB(255, 220, 105);
                        };
                        dust(world, point, color, 1.2f);
                        if (i % 4 == 0) world.spawnParticle(Particle.END_ROD, point, 0);
                    }
                }
            }
            case "celestial_tempest" -> {
                Location crown = base.clone().add(0, 2.12, 0);
                for (int i = 0; i < 12; i++) {
                    double angle = animationTick * 0.075 + i * Math.PI * 2.0 / 12.0;
                    Location point = crown.clone().add(Math.cos(angle) * 0.50,
                            Math.sin(angle * 2.0) * 0.09, Math.sin(angle) * 0.50);
                    dust(world, point, i % 2 == 0 ? Color.fromRGB(255, 225, 145)
                            : Color.fromRGB(115, 205, 255), 1.2f);
                    if (i % 3 == 0) world.spawnParticle(Particle.END_ROD, point, 0);
                }
                for (int ring = 0; ring < 2; ring++) {
                    for (int i = 0; i < 7; i++) {
                        double t = i / 6.0;
                        double angle = animationTick * (ring == 0 ? 0.055 : -0.065)
                                + t * Math.PI * 2.0;
                        Location point = base.clone().add(
                                Math.cos(angle) * (0.38 + ring * 0.16),
                                0.35 + t * 1.30,
                                Math.sin(angle) * (0.38 + ring * 0.16));
                        dust(world, point, ring == 0 ? Color.fromRGB(175, 115, 255)
                                : Color.fromRGB(95, 220, 255), 0.95f);
                    }
                }
            }
            case "eclipse_dragon" -> {
                renderWings(player, right, direction, Color.fromRGB(150, 95, 245), true, 1.08);
                Location center = base.clone().add(0, 1.08, 0);
                for (int i = 0; i < 10; i++) {
                    double angle = animationTick * 0.085 + i * Math.PI * 2.0 / 10.0;
                    Location point = center.clone().add(Math.cos(angle) * 0.64,
                            Math.sin(angle * 1.5) * 0.43, Math.sin(angle) * 0.64);
                    dust(world, point, i % 2 == 0 ? Color.fromRGB(100, 65, 205)
                            : Color.fromRGB(85, 220, 255), 1.3f);
                    if (i % 2 == 0) world.spawnParticle(Particle.REVERSE_PORTAL, point, 0);
                }
                if (animationTick % 20L == 0L) {
                    world.spawnParticle(Particle.SOUL, center, 7, 0.40, 0.45, 0.40, 0.015);
                }
            }
            case "aurora_seraph" -> {
                renderWings(player, right, direction, Color.fromRGB(100, 235, 220), false, 1.18);
                Location halo = base.clone().add(0, 2.18, 0);
                for (int i = 0; i < 12; i++) {
                    double angle = animationTick * 0.055 + i * Math.PI * 2.0 / 12.0;
                    Location point = halo.clone().add(Math.cos(angle) * 0.42, 0,
                            Math.sin(angle) * 0.42);
                    dust(world, point, Color.fromRGB(255, 224, 135), 1.2f);
                    if (i % 3 == 0) world.spawnParticle(Particle.END_ROD, point, 0);
                }
                for (int side : new int[]{-1, 1}) {
                    Location feather = base.clone().add(right.clone().multiply(side * 0.92)).add(0, 1.48, 0);
                    dust(world, feather, Color.fromRGB(255, 240, 190), 1.3f);
                }
            }
            case "astral_guardian" -> {
                Location center = base.clone().add(0, 1.0, 0);
                for (int ring = 0; ring < 3; ring++) {
                    for (int i = 0; i < 8; i++) {
                        double angle = animationTick * (ring == 1 ? -0.07 : 0.06)
                                + i * Math.PI * 2.0 / 8.0 + ring * 0.65;
                        Location point;
                        if (ring == 0) {
                            point = center.clone().add(Math.cos(angle) * 0.72, 0,
                                    Math.sin(angle) * 0.72);
                        } else if (ring == 1) {
                            point = center.clone().add(right.clone().multiply(Math.cos(angle) * 0.58))
                                    .add(0, Math.sin(angle) * 0.78, 0)
                                    .add(direction.clone().multiply(0.12));
                        } else {
                            point = center.clone().add(direction.clone().multiply(Math.cos(angle) * 0.58))
                                    .add(0, Math.sin(angle) * 0.78, 0)
                                    .add(right.clone().multiply(-0.12));
                        }
                        Color color = ring == 0 ? Color.fromRGB(95, 215, 255)
                                : ring == 1 ? Color.fromRGB(185, 130, 255) : Color.fromRGB(255, 220, 130);
                        dust(world, point, color, 1.1f);
                        if (i % 4 == 0) world.spawnParticle(Particle.END_ROD, point, 0);
                    }
                }
                if (animationTick % 16L == 0L) {
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 5, 0.38, 0.55, 0.38, 0.01);
                }
            }
            case "frost_leviathan" -> {
                for (int i = 0; i < 12; i++) {
                    double t = i / 11.0;
                    double angle = animationTick * 0.085 + t * Math.PI * 3.15;
                    double radius = 0.25 + Math.sin(t * Math.PI) * 0.42;
                    Location point = base.clone().add(Math.cos(angle) * radius,
                            0.08 + t * 1.75, Math.sin(angle) * radius);
                    dust(world, point, i % 3 == 0 ? Color.fromRGB(225, 250, 255)
                            : Color.fromRGB(75, 190, 255), 1.25f - (float) t * 0.18f);
                    if (i % 3 == 0) world.spawnParticle(Particle.SNOWFLAKE, point, 0);
                }
                Location crown = base.clone().add(0, 1.85, 0);
                for (int side : new int[]{-1, 1}) {
                    Location horn = crown.clone().add(right.clone().multiply(side * 0.20))
                            .add(0, 0.20 + Math.sin(animationTick * 0.10) * 0.05, 0);
                    dust(world, horn, Color.fromRGB(215, 250, 255), 1.35f);
                    world.spawnParticle(Particle.END_ROD, horn, 0);
                }
                if (animationTick % 20L == 0L) {
                    world.spawnParticle(Particle.SOUL, base.clone().add(0, 0.85, 0), 5, 0.28, 0.65, 0.28, 0.01);
                }
            }
            default -> { }
        }
    }

    private void renderWings(Player player, Vector right, Vector direction,
                             Color color, boolean icy, double scale) {
        Location base = player.getLocation();
        World world = base.getWorld();
        if (world == null) return;
        Vector back = direction.clone().multiply(-0.48);
        for (int side : new int[]{-1, 1}) {
            // Outer arch of each wing.
            for (int i = 0; i <= 7; i++) {
                double progress = i / 7.0;
                double spread = (0.18 + progress * 0.92) * scale;
                double height = (0.42 + Math.sin(progress * Math.PI) * 1.02) * scale;
                double wave = Math.sin(animationTick * 0.16 + progress * Math.PI) * 0.08;
                Location point = base.clone().add(back)
                        .add(right.clone().multiply(side * spread))
                        .add(0, height + wave, 0);
                dust(world, point, color, 1.18f);
                if (i % 2 == 0) {
                    world.spawnParticle(icy ? Particle.SNOWFLAKE : Particle.END_ROD, point, 0);
                }
            }
            // Two feather veins give the wing a visible silhouette instead of a bare line.
            for (int feather = 0; feather < 2; feather++) {
                double startSpread = (0.22 + feather * 0.22) * scale;
                double endSpread = (0.70 + feather * 0.22) * scale;
                double startHeight = (0.72 - feather * 0.08) * scale;
                double endHeight = (0.24 + feather * 0.16) * scale;
                for (int step = 0; step <= 2; step++) {
                    double t = step / 2.0;
                    Location point = base.clone().add(back.clone().multiply(0.88))
                            .add(right.clone().multiply(side * (startSpread + (endSpread - startSpread) * t)))
                            .add(0, startHeight + (endHeight - startHeight) * t, 0);
                    dust(world, point, color, 0.92f);
                }
            }
        }
        if (icy && animationTick % 12L == 0L) {
            world.spawnParticle(Particle.SOUL, base.clone().add(0, 1.0, 0), 2, 0.35, 0.3, 0.35, 0.01);
        }
    }

    private void dust(World world, Location location, Color color, float size) {
        world.spawnParticle(Particle.DUST, location.getX(), location.getY(), location.getZ(),
                0, 0, 0, 0, 0, new Particle.DustOptions(color, size));
    }

    private ItemStack createHat(String id) {
        Material material = switch (id) {
            case "frost_crown" -> Material.DIAMOND_HELMET;
            case "aurora_crown" -> Material.GOLDEN_HELMET;
            default -> Material.LEATHER_HELMET;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String label = switch (id) {
            case "santa_cap" -> "§cSantova čepice";
            case "elf_cap" -> "§aSkřítkova čepice";
            case "snow_cap" -> "§fSněhová čepice";
            case "frost_crown" -> "§bKoruna věčného ledu";
            case "aurora_crown" -> "§6Koruna polární záře";
            default -> "§bVánoční pokrývka hlavy";
        };
        meta.displayName(RewardCommands.component(label));
        meta.lore(List.of(RewardCommands.component("§7Kosmetika ChristmasFeatures")));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(hatKey, PersistentDataType.STRING, id);
        if (meta instanceof LeatherArmorMeta leather) {
            Color color = switch (id) {
                case "santa_cap" -> Color.fromRGB(185, 35, 45);
                case "elf_cap" -> Color.fromRGB(35, 135, 65);
                default -> Color.fromRGB(200, 220, 235);
            };
            leather.setColor(color);
        }
        if (id.equals("frost_crown")) meta.addEnchant(Enchantment.PROTECTION, 1, true);
        if (id.equals("aurora_crown")) meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isCosmeticHat(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(hatKey, PersistentDataType.STRING);
    }

    private Cosmetic find(String category, String id) {
        if (id == null || id.isBlank()) return null;
        List<Cosmetic> options = switch (category) {
            case "tag" -> TAGS;
            case "trail" -> TRAILS;
            case "hat" -> HATS;
            default -> List.of();
        };
        return options.stream().filter(option -> option.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    private ItemStack menuItem(Material material, String name, List<String> lore, String category, String id) {
        ItemStack item = named(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, category);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(RewardCommands.component(name));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(RewardCommands::component).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, "§r", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, pane);
    }

    private String path(Player player) {
        return "players." + player.getUniqueId();
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Nelze uložit kosmetiku: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> applySaved(event.getPlayer()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isCosmeticHat);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> applySaved(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        trailPreviews.remove(event.getPlayer().getUniqueId());
        save();
    }

    @EventHandler
    public void onHatInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isCosmeticHat(player.getInventory().getHelmet())) return;
        boolean replacingHelmet = event.getClickedInventory() == player.getInventory()
                && event.getSlot() == 39;
        ItemStack clicked = event.getCurrentItem();
        boolean shiftEquippingHelmet = event.isShiftClick() && clicked != null
                && clicked.getType().name().endsWith("_HELMET");
        if (replacingHelmet || shiftEquippingHelmet) {
            event.setCancelled(true);
            player.sendMessage("§7Nejprve si v kosmetickém menu sundej nasazenou čepici.");
        }
    }

    @EventHandler
    public void onHatInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isCosmeticHat(player.getInventory().getHelmet())) return;
        for (int rawSlot : event.getRawSlots()) {
            if (event.getView().getSlotType(rawSlot) == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
                player.sendMessage("§7Nejprve si v kosmetickém menu sundej nasazenou čepici.");
                return;
            }
        }
    }

    public void shutdown() {
        if (particleTask != null) particleTask.cancel();
        save();
    }

    private record Cosmetic(String id, String label, boolean vipOnly, Material icon, String description) { }
    private record TrailPreview(String effectId, long expiresAtMillis) { }

    private static final class MenuHolder implements InventoryHolder {
        private final String category;
        private final int page;
        private Inventory inventory;

        private MenuHolder(String category, int page) {
            this.category = category;
            this.page = page;
        }

        private String category() {
            return category;
        }

        private int page() {
            return page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }
}
