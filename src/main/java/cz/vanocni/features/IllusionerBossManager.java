package cz.vanocni.features;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class IllusionerBossManager implements Listener {
    private static final double MAX_HEALTH = 650.0;
    private static final double PARTICIPANT_RANGE = 48.0;

    private final ChristmasFeaturesPlugin plugin;
    private final org.bukkit.NamespacedKey bossKey;
    private final org.bukkit.NamespacedKey minionKey;
    private final Random random = new Random();
    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> minions = new HashSet<>();
    private final Set<UUID> triggerOccupants = new HashSet<>();

    private Location spawnLocation;
    private Illusioner boss;
    private BossBar bossBar;
    private BukkitTask introTask;
    private BukkitTask fightTask;
    private BukkitTask triggerTask;
    private long lastAutoTrigger;
    private int fightTicks;
    private boolean phaseTwo;
    private boolean ending;

    public IllusionerBossManager(ChristmasFeaturesPlugin plugin) {
        this.plugin = plugin;
        bossKey = new org.bukkit.NamespacedKey(plugin, "illusioner_boss");
        minionKey = new org.bukkit.NamespacedKey(plugin, "illusioner_minion");
        loadSpawn();
    }

    private void loadSpawn() {
        String worldName = plugin.getConfig().getString("bosses.illusioner.spawn.world", "");
        if (worldName == null || worldName.isBlank()) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        spawnLocation = new Location(world,
                plugin.getConfig().getDouble("bosses.illusioner.spawn.x"),
                plugin.getConfig().getDouble("bosses.illusioner.spawn.y"),
                plugin.getConfig().getDouble("bosses.illusioner.spawn.z"),
                (float) plugin.getConfig().getDouble("bosses.illusioner.spawn.yaw"),
                (float) plugin.getConfig().getDouble("bosses.illusioner.spawn.pitch"));
    }

    public void startScheduler() {
        purgeStaleEncounterEntities();
        if (triggerTask != null) triggerTask.cancel();
        triggerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkEntryTrigger, 20L, 20L);
    }

    private void purgeStaleEncounterEntities() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isEncounterEntity(entity)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            plugin.getLogger().warning("Po startu bylo odstraněno " + removed
                    + " osiřelých entit The Illusioner z předchozího běhu.");
        }
    }

    private boolean isEncounterEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)
                || entity.getPersistentDataContainer().has(minionKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Also remove legacy persistent boss/minion entities from chunks that load later.
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        World world = event.getWorld();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!world.isChunkLoaded(chunkX, chunkZ)) return;
            for (Entity entity : world.getChunkAt(chunkX, chunkZ).getEntities()) {
                if (isEncounterEntity(entity)) entity.remove();
            }
        });
    }

    private void checkEntryTrigger() {
        if (spawnLocation == null || spawnLocation.getWorld() == null) return;
        if (!plugin.getConfig().getBoolean("bosses.illusioner.trigger-on-entry", true)) {
            triggerOccupants.clear();
            return;
        }
        double radius = Math.max(3.0, plugin.getConfig().getDouble("bosses.illusioner.trigger-radius", 9.0));
        Set<UUID> nowInside = new HashSet<>();
        Player newlyEntering = null;
        for (Player player : spawnLocation.getWorld().getPlayers()) {
            boolean inside = player.getLocation().distanceSquared(spawnLocation) <= radius * radius;
            if (!inside) continue;
            nowInside.add(player.getUniqueId());
            boolean entered = !triggerOccupants.contains(player.getUniqueId());
            if (entered && newlyEntering == null) newlyEntering = player;
        }
        triggerOccupants.clear();
        triggerOccupants.addAll(nowInside);

        if (newlyEntering == null || isActive()) return;
        long cooldown = Math.max(30L, plugin.getConfig().getLong("bosses.illusioner.entry-cooldown-seconds", 300L));
        if (System.currentTimeMillis() - lastAutoTrigger < cooldown * 1000L) return;
        lastAutoTrigger = System.currentTimeMillis();
        start(newlyEntering);
    }

    public void setSpawn(Player player) {
        spawnLocation = player.getLocation().clone();
        plugin.getConfig().set("bosses.illusioner.spawn.world", spawnLocation.getWorld().getName());
        plugin.getConfig().set("bosses.illusioner.spawn.x", spawnLocation.getX());
        plugin.getConfig().set("bosses.illusioner.spawn.y", spawnLocation.getY());
        plugin.getConfig().set("bosses.illusioner.spawn.z", spawnLocation.getZ());
        plugin.getConfig().set("bosses.illusioner.spawn.yaw", spawnLocation.getYaw());
        plugin.getConfig().set("bosses.illusioner.spawn.pitch", spawnLocation.getPitch());
        plugin.saveConfig();
        player.sendMessage("§aVstupní místo The Illusioner bylo uloženo. Při dalším příchodu se spustí jeho úvodní dialog.");
    }

    public boolean start(Player initiator) {
        if (isActive()) {
            if (initiator != null) initiator.sendMessage("§cThe Illusioner už má aktivní souboj.");
            return false;
        }
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            if (initiator == null) return false;
            spawnLocation = initiator.getLocation().clone();
            setSpawn(initiator);
        }

        lastAutoTrigger = System.currentTimeMillis();
        participants.clear();
        for (Player player : spawnLocation.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(spawnLocation) <= PARTICIPANT_RANGE * PARTICIPANT_RANGE) {
                participants.add(player.getUniqueId());
            }
        }
        if (participants.isEmpty()) return false;

        phaseTwo = false;
        ending = false;
        fightTicks = 0;
        playIntro();
        return true;
    }

    private void playIntro() {
        introTask = new BukkitRunnable() {
            private int step;

            @Override
            public void run() {
                if (ending || spawnLocation == null || spawnLocation.getWorld() == null) {
                    cancel();
                    return;
                }
                List<Player> players = onlineParticipants();
                if (players.isEmpty()) {
                    cancel();
                    cleanup(false);
                    return;
                }

                switch (step++) {
                    case 0 -> {
                        title(players, "§5§lZRCADLO SE OTEVÍRÁ", "§8Někdo tě sleduje...", 10, 55, 10);
                        sound("entity.illusioner.ambient", 0.8f, 0.65f);
                        riftBurst(spawnLocation, 48);
                    }
                    case 1 -> dialogue(players, "§5[???] §fKonečně jsi dorazil. Už jsem tě očekával.");
                    case 2 -> {
                        dialogue(players, "§5[???] §fCelou dobu jsi sledoval jen můj odraz.");
                        sound("entity.illusioner.prepare_mirror", 0.9f, 0.8f);
                        mirrorRing(spawnLocation, 2.5);
                    }
                    case 3 -> {
                        dialogue(players, "§5[???] §fZkus poznat, který z nás je skutečný.");
                        riftBurst(spawnLocation, 72);
                    }
                    case 4 -> {
                        title(players, "§5§lTHE ILLUSIONER", "§7Připrav se na skutečnou iluzi.", 10, 45, 10);
                        sound("entity.illusioner.cast_spell", 1.0f, 0.55f);
                    }
                    default -> {
                        cancel();
                        introTask = null;
                        spawnBoss();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void spawnBoss() {
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            cleanup(false);
            return;
        }
        Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation.clone(), EntityType.ILLUSIONER);
        if (!(entity instanceof Illusioner illusioner)) {
            entity.remove();
            cleanup(false);
            return;
        }
        boss = illusioner;
        boss.setCustomName("§5§lTHE ILLUSIONER");
        boss.setCustomNameVisible(true);
        // The encounter is runtime-only; a crash/restart must not leave a permanent boss in the arena.
        boss.setPersistent(false);
        boss.setRemoveWhenFarAway(false);
        boss.setCanPickupItems(false);
        boss.setGlowing(true);
        boss.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) 1);
        setAttribute(boss, Attribute.MAX_HEALTH, MAX_HEALTH);
        setAttribute(boss, Attribute.ATTACK_DAMAGE, 15.0);
        setAttribute(boss, Attribute.MOVEMENT_SPEED, 0.34);
        setAttribute(boss, Attribute.FOLLOW_RANGE, 48.0);
        boss.setHealth(MAX_HEALTH);

        Player target = closestParticipant(boss.getLocation());
        if (target != null) boss.setTarget(target);
        bossBar = Bukkit.createBossBar("§5✦ THE ILLUSIONER ✦", BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bossBar.setProgress(1.0);
        for (Player player : onlineParticipants()) bossBar.addPlayer(player);
        Bukkit.broadcastMessage("§5§l✦ THE ILLUSIONER ✦ §fSouboj začíná!");
        sound("entity.illusioner.cast_spell", 1.0f, 0.7f);
        mirrorRing(boss.getLocation(), 4.0);
        fightTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickFight, 10L, 5L);
    }

    private void tickFight() {
        if (ending || boss == null || !boss.isValid() || boss.isDead()) {
            if (!ending) cleanup(false);
            return;
        }
        if (onlineParticipants().isEmpty()) {
            cleanup(false);
            return;
        }

        fightTicks += 5;
        double maxHealth = boss.getAttribute(Attribute.MAX_HEALTH) == null
                ? MAX_HEALTH : boss.getAttribute(Attribute.MAX_HEALTH).getValue();
        double health = Math.max(0.0, boss.getHealth());
        if (bossBar != null) {
            bossBar.setProgress(Math.min(1.0, Math.max(0.0, health / maxHealth)));
            for (Player player : onlineParticipants()) bossBar.addPlayer(player);
        }
        if (!phaseTwo && health <= maxHealth * 0.5) enterPhaseTwo();

        Player target = closestParticipant(boss.getLocation());
        if (target != null) boss.setTarget(target);

        int interval = phaseTwo ? 65 : 100;
        if (fightTicks >= interval) {
            fightTicks = 0;
            castNextAttack();
        }
    }

    private void enterPhaseTwo() {
        phaseTwo = true;
        boss.setCustomName("§c§lTHE ILLUSIONER §4✦ TRUE FORM");
        if (bossBar != null) {
            bossBar.setTitle("§c✦ THE ILLUSIONER — TRUE FORM ✦");
            bossBar.setColor(BarColor.RED);
        }
        List<Player> players = onlineParticipants();
        title(players, "§4§lILUZE BYLA ROZPTÝLENA", "§cJeho skutečná síla se probouzí!", 10, 65, 15);
        dialogue(players, "§4[The Illusioner] §cTeď už přestanu předstírat.");
        sound("entity.illusioner.prepare_blindness", 1.0f, 0.65f);
        riftBurst(boss.getLocation(), 100);
        boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 20 * 8, 0, true, false, true));
    }

    private void castNextAttack() {
        if (boss == null || !boss.isValid()) return;
        int choice = random.nextInt(phaseTwo ? 5 : 3);
        switch (choice) {
            case 0 -> mirrorClones();
            case 1 -> meteorSwarm();
            case 2 -> phantomTornado();
            case 3 -> {
                if (phaseTwo) ghostedRavager();
                else ghostedGuardians();
            }
            default -> prismGrid();
        }
    }

    private void mirrorClones() {
        Location center = boss.getLocation();
        World world = center.getWorld();
        if (world == null) return;
        dialogue(onlineParticipants(), "§5[The Illusioner] §fKterý z nás je ten pravý?");
        sound("entity.illusioner.prepare_mirror", 0.9f, 1.1f);

        List<Illusioner> clones = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI * 2.0 * i / 3.0 + random.nextDouble() * 0.3;
            Location location = center.clone().add(Math.cos(angle) * 4.0, 0, Math.sin(angle) * 4.0);
            Entity entity = world.spawnEntity(location, EntityType.ILLUSIONER);
            if (!(entity instanceof Illusioner clone)) {
                entity.remove();
                continue;
            }
            clone.setAI(false);
            clone.setInvulnerable(true);
            clone.setSilent(true);
            clone.setCustomName("§5Odraz");
            clone.setCustomNameVisible(false);
            clone.setGlowing(true);
            clone.setPersistent(false);
            clone.setRemoveWhenFarAway(false);
            clone.getPersistentDataContainer().set(minionKey, PersistentDataType.BYTE, (byte) 1);
            minions.add(clone.getUniqueId());
            clones.add(clone);
        }
        for (Player player : onlineParticipants()) player.sendActionBar(Component.text("§5✦ ODRAZY — najdi skutečného bosse"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Illusioner clone : clones) {
                if (clone.isValid()) {
                    riftBurst(clone.getLocation(), 12);
                    clone.remove();
                }
                minions.remove(clone.getUniqueId());
            }
            if (boss != null && boss.isValid()) {
                boss.teleport(center.clone().add(random.nextDouble() * 4 - 2, 0, random.nextDouble() * 4 - 2));
                sound("entity.enderman.teleport", 0.9f, 0.8f);
            }
        }, 55L);
    }

    private void meteorSwarm() {
        if (boss == null) return;
        List<Location> impacts = new ArrayList<>();
        for (Player player : onlineParticipants()) {
            Location target = player.getLocation().clone().add(
                    random.nextDouble() * 3.0 - 1.5, 0, random.nextDouble() * 3.0 - 1.5);
            impacts.add(target);
            warningRing(target, 2.25);
            player.sendActionBar(Component.text("§c☄ METEORITICKÝ ÚTOK — uhni!"));
        }
        sound("entity.blaze.shoot", 0.9f, 0.65f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (boss == null || !boss.isValid()) return;
            for (Location impact : impacts) {
                World world = impact.getWorld();
                if (world == null) continue;
                world.spawnParticle(Particle.EXPLOSION, impact.clone().add(0, 0.25, 0), 1);
                world.spawnParticle(Particle.FLAME, impact, 28, 0.7, 0.3, 0.7, 0.04);
                world.spawnParticle(Particle.SMOKE, impact, 18, 0.6, 0.25, 0.6, 0.03);
                world.playSound(impact, "entity.generic.explode", 0.85f, 0.75f);
                for (Player player : onlineParticipants()) {
                    if (!player.getWorld().equals(world)) continue;
                    if (player.getLocation().distanceSquared(impact) <= 7.0) {
                        player.damage(11.0, boss);
                        Vector push = player.getLocation().toVector().subtract(impact.toVector()).normalize().multiply(0.85);
                        push.setY(0.42);
                        player.setVelocity(push);
                    }
                }
            }
        }, 32L);
    }

    private void phantomTornado() {
        if (boss == null) return;
        Location center = boss.getLocation();
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < 20; i++) {
            double angle = i * Math.PI * 2.0 / 20.0;
            double radius = 2.0 + (i % 4) * 0.55;
            double height = 0.25 + (i % 8) * 0.32;
            Location point = center.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            world.spawnParticle(Particle.PORTAL, point, 0);
            if (i % 2 == 0) world.spawnParticle(Particle.SOUL, point, 0);
        }
        dialogue(onlineParticipants(), "§5[The Illusioner] §fNech se pohltit proudem iluzí.");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (boss == null || !boss.isValid()) return;
            for (Player player : onlineParticipants()) {
                if (player.getWorld().equals(world) && player.getLocation().distanceSquared(center) <= 49.0) {
                    player.damage(8.0, boss);
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS, 50, 1, true, true, true));
                    player.setVelocity(player.getVelocity().add(new Vector(0, 0.38, 0)));
                }
            }
            world.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 1, 0), 42, 2.0, 1.2, 2.0, 0.05);
            world.playSound(center, "entity.evoker.prepare_attack", 1.0f, 0.65f);
        }, 35L);
    }

    private void ghostedGuardians() {
        if (boss == null) return;
        World world = boss.getWorld();
        Location center = boss.getLocation();
        dialogue(onlineParticipants(), "§5[The Illusioner] §fMoji strážci tě zdrží.");
        for (int i = 0; i < 2; i++) {
            Location location = center.clone().add(random.nextDouble() * 5 - 2.5, 0, random.nextDouble() * 5 - 2.5);
            Entity entity = world.spawnEntity(location, EntityType.VEX);
            if (!(entity instanceof Vex vex)) {
                entity.remove();
                continue;
            }
            vex.setCustomName("§5Přízračný strážce");
            vex.setCustomNameVisible(false);
            vex.setPersistent(false);
            vex.setRemoveWhenFarAway(false);
            vex.getPersistentDataContainer().set(minionKey, PersistentDataType.BYTE, (byte) 1);
            setAttribute(vex, Attribute.MAX_HEALTH, 24.0);
            setAttribute(vex, Attribute.ATTACK_DAMAGE, phaseTwo ? 8.0 : 6.0);
            Player target = closestParticipant(location);
            if (target != null) vex.setTarget(target);
            minions.add(vex.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (vex.isValid()) vex.remove();
                minions.remove(vex.getUniqueId());
            }, 20L * 18L);
        }
        sound("entity.vex.charge", 0.9f, 0.7f);
    }

    private void ghostedRavager() {
        if (boss == null || !boss.isValid()) return;
        Location center = boss.getLocation();
        Location location = center.clone().add(3.0, 0, 0);
        Entity entity = center.getWorld().spawnEntity(location, EntityType.RAVAGER);
        if (!(entity instanceof Ravager ravager)) {
            entity.remove();
            return;
        }
        ravager.setCustomName("§4§lPřízračný Ravager");
        ravager.setCustomNameVisible(true);
        ravager.setGlowing(true);
        ravager.setPersistent(false);
        ravager.setRemoveWhenFarAway(false);
        ravager.getPersistentDataContainer().set(minionKey, PersistentDataType.BYTE, (byte) 1);
        setAttribute(ravager, Attribute.MAX_HEALTH, 110.0);
        setAttribute(ravager, Attribute.ATTACK_DAMAGE, 15.0);
        setAttribute(ravager, Attribute.MOVEMENT_SPEED, 0.34);
        ravager.setHealth(110.0);
        Player target = closestParticipant(location);
        if (target != null) ravager.setTarget(target);
        minions.add(ravager.getUniqueId());
        dialogue(onlineParticipants(), "§4[The Illusioner] §cAni tvá odvaha tě neochrání před mým přízračným jezdcem.");
        sound("entity.ravager.roar", 1.0f, 0.7f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ravager.isValid()) {
                riftBurst(ravager.getLocation(), 26);
                ravager.remove();
            }
            minions.remove(ravager.getUniqueId());
        }, 20L * 22L);
    }

    private void prismGrid() {
        if (boss == null) return;
        Location center = boss.getLocation().clone();
        World world = center.getWorld();
        if (world == null) return;
        dialogue(onlineParticipants(), "§4[The Illusioner] §cPohleď na dokonalou geometrii bolesti.");
        List<Location> lines = new ArrayList<>();
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                Location point = center.clone().add(i * 1.15, 0.12, j * 1.15);
                lines.add(point);
                if (i == 0 || j == 0 || i == j || i == -j) {
                    dust(world, point, Color.fromRGB(180, 90, 255), 1.15f);
                }
            }
        }
        for (Player player : onlineParticipants()) player.sendActionBar(Component.text("§d✦ PRISM GRID — opusť vyznačené linie"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (boss == null || !boss.isValid()) return;
            for (Location point : lines) {
                if (point.getBlockX() % 2 == 0 || point.getBlockZ() % 2 == 0) {
                    world.spawnParticle(Particle.END_ROD, point, 2, 0.05, 0.1, 0.05, 0.01);
                }
            }
            for (Player player : onlineParticipants()) {
                if (player.getWorld().equals(world)) {
                    Location p = player.getLocation();
                    double x = Math.abs(p.getX() - center.getX());
                    double z = Math.abs(p.getZ() - center.getZ());
                    boolean hit = x <= 3.6 && (z <= 0.35 || Math.abs(x - z) <= 0.35)
                            || z <= 3.6 && (x <= 0.35 || Math.abs(x - z) <= 0.35);
                    if (hit && Math.abs(p.getY() - center.getY()) < 2.5) {
                        player.damage(14.0, boss);
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.BLINDNESS, 35, 0, true, true, true));
                    }
                }
            }
            world.playSound(center, "entity.illusioner.cast_spell", 1.0f, 0.45f);
            riftBurst(center, 70);
        }, 30L);
    }

    private void warningRing(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2.0 * i / 28.0;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.08, Math.sin(angle) * radius);
            dust(world, point, Color.fromRGB(255, 65, 65), 1.2f);
            if (i % 4 == 0) world.spawnParticle(Particle.FLAME, point, 0);
        }
    }

    private void mirrorRing(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < 36; i++) {
            double angle = Math.PI * 2.0 * i / 36.0;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.12, Math.sin(angle) * radius);
            world.spawnParticle(Particle.PORTAL, point, 0);
            if (i % 2 == 0) world.spawnParticle(Particle.ENCHANT, point.clone().add(0, 0.2, 0), 0);
        }
    }

    private void riftBurst(Location center, int count) {
        World world = center.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.PORTAL, center.clone().add(0, 1.0, 0), count, 0.9, 1.0, 0.9, 0.08);
        world.spawnParticle(Particle.WITCH, center.clone().add(0, 1.0, 0), Math.max(8, count / 3), 0.8, 0.8, 0.8, 0.04);
    }

    private void dust(World world, Location location, Color color, float size) {
        world.spawnParticle(Particle.DUST, location.getX(), location.getY(), location.getZ(),
                0, 0, 0, 0, 0, new Particle.DustOptions(color, size));
    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        if (entity.getAttribute(attribute) != null) entity.getAttribute(attribute).setBaseValue(value);
    }

    private Player closestParticipant(Location from) {
        Player closest = null;
        double best = Double.MAX_VALUE;
        for (Player player : onlineParticipants()) {
            if (!player.getWorld().equals(from.getWorld())) continue;
            double distance = player.getLocation().distanceSquared(from);
            if (distance < best) {
                best = distance;
                closest = player;
            }
        }
        return closest;
    }

    private List<Player> onlineParticipants() {
        if (spawnLocation == null || spawnLocation.getWorld() == null) return List.of();
        List<Player> online = new ArrayList<>();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && !player.isDead()
                    && player.getWorld().equals(spawnLocation.getWorld())
                    && player.getLocation().distanceSquared(spawnLocation) <= PARTICIPANT_RANGE * PARTICIPANT_RANGE) {
                online.add(player);
            }
        }
        return online;
    }

    private void dialogue(List<Player> players, String line) {
        for (Player player : players) player.sendMessage(line);
    }

    private void title(List<Player> players, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : players) player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    private void sound(String key, float volume, float pitch) {
        if (spawnLocation != null && spawnLocation.getWorld() != null) {
            spawnLocation.getWorld().playSound(spawnLocation, key, volume, pitch);
        }
    }

    private void cleanup(boolean victory) {
        ending = true;
        if (introTask != null) {
            introTask.cancel();
            introTask = null;
        }
        if (fightTask != null) {
            fightTask.cancel();
            fightTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
        if (boss != null && boss.isValid()) boss.remove();
        boss = null;
        for (UUID uuid : new HashSet<>(minions)) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) entity.remove();
        }
        minions.clear();

        if (victory) {
            for (Player player : onlineParticipants()) {
                player.sendTitle("§b§lILUZE ROZPTÝLENA", "§7The Illusioner byl poražen.", 10, 75, 20);
                player.playSound(player.getLocation(), "ui.toast.challenge_complete", 0.8f, 0.8f);
            }
            Bukkit.broadcastMessage("§5§l✦ THE ILLUSIONER ✦ §fbyl poražen!");
        }
        participants.clear();
        ending = false;
    }

    public boolean isActive() {
        return !ending && (introTask != null || boss != null);
    }

    public void stop() {
        cleanup(false);
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;
        event.getDrops().clear();
        event.setDroppedExp(500);
        cleanup(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (bossBar != null) bossBar.removePlayer(event.getPlayer());
        participants.remove(event.getPlayer().getUniqueId());
        if (participants.isEmpty() && isActive()) cleanup(false);
    }

    public void shutdown() {
        if (triggerTask != null) triggerTask.cancel();
        cleanup(false);
    }
}
