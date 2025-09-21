package com.listeners.skills;

import com.managers.SkillManager;
import com.utils.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PickingSkillListener implements Listener {

    private final SkillManager skillManager;
    private final int MAX_LEVEL = 1000;

    // Track items with priority owner and expiration timestamp
    private final Map<UUID, PriorityItem> priorityItems = new HashMap<>();
    private final long GRACE_PERIOD = 3000; // 3 seconds in milliseconds

    private final JavaPlugin plugin;

    public PickingSkillListener(JavaPlugin plugin, SkillManager skillManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Item item = event.getItem();
        UUID itemId = item.getUniqueId();

        long now = System.currentTimeMillis();

        PriorityItem priorityItem = priorityItems.get(itemId);

        // If this item is not tracked yet, initialize it
        if (priorityItem == null) {
            Player highest = findHighestPriorityPlayerNearby(item);
            if (highest != null) {
                priorityItem = new PriorityItem(highest.getUniqueId(), now + GRACE_PERIOD);
                priorityItems.put(itemId, priorityItem);

                // Schedule removal after grace period
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        priorityItems.remove(itemId);
                    }
                }.runTaskLater(plugin, GRACE_PERIOD / 50); // convert ms to ticks
            }
        }

        // If grace period is active, only highest-level player can pick
        if (priorityItem != null && now < priorityItem.expiryTime) {
            if (!playerId.equals(priorityItem.priorityOwner)) {
                event.setCancelled(true);
                return;
            }
        }

        // Add XP
        int level = skillManager.getSkillLevel(playerId, SkillType.PICKING);
        double totalXP = 1.0 + calculateXPBonus(level);
        skillManager.addSkillExperience(playerId, SkillType.PICKING, totalXP);

        // Remove tracking once item is picked
        priorityItems.remove(itemId);
    }

    private Player findHighestPriorityPlayerNearby(Item item) {
        double highestPriority = -1;
        Player topPlayer = null;

        for (Player player : item.getNearbyEntities(3, 3, 3).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .toList()) {

            int level = skillManager.getSkillLevel(player.getUniqueId(), SkillType.PICKING);
            double priority = calculatePickingPriority(level);

            if (priority > highestPriority) {
                highestPriority = priority;
                topPlayer = player;
            }
        }
        return topPlayer;
    }

    private double calculateXPBonus(int level) {
        return (double) level / MAX_LEVEL;
    }

    private double calculatePickingPriority(int level) {
        return Math.min((double) level / MAX_LEVEL, 1.0);
    }

    private static class PriorityItem {
        UUID priorityOwner;
        long expiryTime;

        PriorityItem(UUID owner, long expiry) {
            this.priorityOwner = owner;
            this.expiryTime = expiry;
        }
    }
}
