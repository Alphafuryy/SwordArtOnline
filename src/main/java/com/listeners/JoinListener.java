package com.listeners;

import com.SwordArtOnline;
import com.utils.Floor;
import com.utils.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final SwordArtOnline plugin;

    public JoinListener(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            SpawnPoint nearest = plugin.getSpawnManager().getNearestSpawn(player.getLocation());
            if (nearest != null) {
                Location loc = nearest.getLocation();
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.teleport(loc), 1L);
            }

        }
    }
}