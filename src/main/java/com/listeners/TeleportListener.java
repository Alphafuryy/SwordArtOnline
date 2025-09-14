package com.listeners;

import com.SwordArtOnline;
import com.tasks.TeleportTask;
import com.utils.SpawnPoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportListener implements Listener {

    private final SwordArtOnline plugin;
    private final Map<UUID, TeleportTask> activeTeleports = new HashMap<>();

    public TeleportListener(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    // start teleport; delayTicks is how many ticks until the actual teleport
    public void startTeleport(Player player, long delayTicks) {
        FileConfiguration cfg = plugin.getTaskManager().getTaskConfig();

        if (activeTeleports.containsKey(player.getUniqueId())) {
            String inProgress = cfg.getString("teleport-task.in-progress",
                    "&cYou already have a teleport in progress!");
            player.sendMessage(inProgress);
            return;
        }

        SpawnPoint nearest = plugin.getSpawnManager().getNearestSpawn(player.getLocation());
        if (nearest == null) {
            String noSpawn = cfg.getString("teleport-task.no-spawn", "&cNo spawn found!");
            player.sendMessage(noSpawn);
            return;
        }

        TeleportTask task = new TeleportTask(plugin, player, nearest, delayTicks,
                () -> activeTeleports.remove(player.getUniqueId()), // onFinish
                () -> activeTeleports.remove(player.getUniqueId())  // onCancel
        );

        activeTeleports.put(player.getUniqueId(), task);
        task.start();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        TeleportTask task = activeTeleports.get(player.getUniqueId());
        if (task == null) return;

        Location start = task.getStartLocation();
        if (start == null) return;

        FileConfiguration cfg = plugin.getTaskManager().getTaskConfig();
        double allowedRadius = cfg.getDouble("teleport-task.cancel.allow-movement-radius", 0.3);
        if (event.getTo() == null) return;

        // compare distance squared for performance
        double distSq = start.distanceSquared(event.getTo());
        if (distSq > allowedRadius * allowedRadius) {
            task.cancel("move");
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        TeleportTask task = activeTeleports.get(player.getUniqueId());
        if (task != null) {
            task.cancel("damage");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TeleportTask task = activeTeleports.get(player.getUniqueId());
        if (task != null) {
            task.cancel("quit");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity() == null) return;
        Player player = event.getEntity();
        TeleportTask task = activeTeleports.get(player.getUniqueId());
        if (task != null) {
            task.cancel("death");
        }
    }
}
