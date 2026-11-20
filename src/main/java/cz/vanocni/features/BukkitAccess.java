package cz.vanocni.features;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Package-local indirection so the BetterHud bridge can take Bukkit snapshots on the server thread. */
final class BukkitAccess {
    private BukkitAccess() {
    }

    static Collection<? extends Player> onlinePlayers() {
        return Bukkit.getOnlinePlayers();
    }
}
