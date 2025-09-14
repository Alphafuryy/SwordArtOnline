package com.listeners;

import com.SwordArtOnline;
import com.tasks.TeleportTask;
import com.utils.Messages;
import com.utils.SpawnPoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportListener implements Listener {

    private final SwordArtOnline plugin;
    private final Map<UUID, TeleportTask> activeTeleports = new HashMap<>();

    public TeleportListener(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    // Start a teleport task for a player
    public void startTeleport(Player player, long delayTicks) {
        if (activeTeleports.containsKey(player.getUniqueId())) {
            player.sendMessage(Messages.get("teleport-in-progress"));
            return;
        }

        // Find nearest spawn
        SpawnPoint nearest = plugin.getSpawnManager().getNearestSpawn(player.getLocation());
        if (nearest == null) {
            player.sendMessage(Messages.get("teleport-no-spawn"));
            return;
        }

        TeleportTask task = new TeleportTask(plugin, player, nearest, delayTicks);
        activeTeleports.put(player.getUniqueId(), task);
        task.start();
    }

    // Cancel teleport on movement
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        TeleportTask task = activeTeleports.get(player.getUniqueId());
        if (task == null) return;

        Location start = task.getStartLocation();
        if (!event.getTo().getBlock().equals(start.getBlock())) {
            task.cancel("teleport-cancel-move");
            activeTeleports.remove(player.getUniqueId());
        }
    }

    // Cancel teleport on damage
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        TeleportTask task = activeTeleports.get(player.getUniqueId());
        if (task != null) {
            task.cancel("teleport-cancel-damage");
            activeTeleports.remove(player.getUniqueId());
        }
    }
}
