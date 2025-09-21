package com.listeners;

import com.managers.StatusBarManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class StatusBarListener implements Listener {

    private final StatusBarManager manager;

    public StatusBarListener(StatusBarManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        manager.update(player);
        // Schedule a follow-up update to ensure it shows
        Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                () -> manager.update(player), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.clear(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                () -> manager.update(player), 5L);
    }

    @EventHandler
    public void onResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                    () -> manager.update(player), 5L);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        manager.clear(player);
        // Schedule an update for when player respawns
        Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                () -> manager.update(player), 5L);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            manager.update(player);
        }
    }

    @EventHandler
    public void onHeal(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            manager.update(player);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            manager.update(player);
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                    () -> manager.update(player), 5L);
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        event.getAffectedEntities().forEach(entity -> {
            if (entity instanceof Player player) {
                Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                        () -> manager.update(player), 5L);
            }
        });
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            manager.update(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            manager.update(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                    () -> manager.update(player), 2L);
        }
    }

    @EventHandler
    public void onArmorChange(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                    () -> manager.update(player), 2L);
        }
    }
}