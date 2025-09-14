package com.tasks;

import com.SwordArtOnline;
import com.utils.Messages;
import com.utils.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class TeleportTask {

    private final SwordArtOnline plugin;
    private final Player player;
    private final SpawnPoint destination;
    private final long delayTicks;
    private Location startLocation;
    private int taskId;
    private boolean cancelled = false;

    public TeleportTask(SwordArtOnline plugin, Player player, SpawnPoint destination, long delayTicks) {
        this.plugin = plugin;
        this.player = player;
        this.destination = destination;
        this.delayTicks = delayTicks;
    }

    public void start() {
        startLocation = player.getLocation();

        // Countdown messages from task config
        String actionbar = plugin.getTaskManager().getTaskConfig()
                .getString("teleport-task.countdown.actionbar", "[MSG]: %teleport-start%");
        String extraMessage = plugin.getTaskManager().getTaskConfig()
                .getString("teleport-task.countdown.extra-message", null);
        String title = plugin.getTaskManager().getTaskConfig()
                .getString("teleport-task.countdown.title", "");
        String subtitle = plugin.getTaskManager().getTaskConfig()
                .getString("teleport-task.countdown.subtitle", "");
        int fadeIn = plugin.getTaskManager().getTaskConfig().getInt("teleport-task.countdown.fade-in", 5);
        int stay = plugin.getTaskManager().getTaskConfig().getInt("teleport-task.countdown.stay", 20);
        int fadeOut = plugin.getTaskManager().getTaskConfig().getInt("teleport-task.countdown.fade-out", 5);

        // Send messages
        player.sendActionBar(Messages.getFromTask(applyPlaceholders(actionbar)));
        if (extraMessage != null && !extraMessage.isEmpty()) {
            player.sendMessage(Messages.getFromTask(applyPlaceholders(extraMessage)));
        }
        if (!title.isEmpty() || !subtitle.isEmpty()) {
            player.sendTitle(Messages.getFromTask(applyPlaceholders(title)),
                    Messages.getFromTask(applyPlaceholders(subtitle)),
                    fadeIn, stay, fadeOut);
        }

        // Particle effect during countdown
        playParticles("teleport-task.countdown.particle");

        // Schedule teleport
        taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (cancelled) return;

            player.teleport(destination.getLocation());

            // Success section
            String successActionbar = plugin.getTaskManager().getTaskConfig()
                    .getString("teleport-task.success.actionbar", "[MSG]: %teleport-success%");
            String successTitle = plugin.getTaskManager().getTaskConfig()
                    .getString("teleport-task.success.title", "");
            String successSubtitle = plugin.getTaskManager().getTaskConfig()
                    .getString("teleport-task.success.subtitle", "");

            player.sendActionBar(Messages.getFromTask(applyPlaceholders(successActionbar)));
            player.sendTitle(Messages.getFromTask(applyPlaceholders(successTitle)),
                    Messages.getFromTask(applyPlaceholders(successSubtitle)),
                    5, 20, 5);

            playParticles("teleport-task.success.particle");

        }, delayTicks).getTaskId();
    }

    public void cancel(String reasonKey) {
        if (!cancelled) {
            cancelled = true;
            Bukkit.getScheduler().cancelTask(taskId);

            // Load cancel message from config
            String cancelMessage = plugin.getTaskManager().getTaskConfig()
                    .getString("teleport-task.cancel." + reasonKey, "[MSG]: %teleport-cancel%");
            player.sendMessage(Messages.getFromTask(applyPlaceholders(cancelMessage)));
        }
    }

    private void playParticles(String path) {
        if (!plugin.getTaskManager().getTaskConfig().getBoolean(path + ".enabled", false)) return;

        String typeName = plugin.getTaskManager().getTaskConfig().getString(path + ".type", "VILLAGER_HAPPY");
        Particle type;
        try {
            type = Particle.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = Particle.HAPPY_VILLAGER;
        }

        int count = plugin.getTaskManager().getTaskConfig().getInt(path + ".count", 5);
        double speed = plugin.getTaskManager().getTaskConfig().getDouble(path + ".speed", 0.1);
        double offsetX = plugin.getTaskManager().getTaskConfig().getDouble(path + ".offset-x", 0.5);
        double offsetY = plugin.getTaskManager().getTaskConfig().getDouble(path + ".offset-y", 1.0);
        double offsetZ = plugin.getTaskManager().getTaskConfig().getDouble(path + ".offset-z", 0.5);

        player.getWorld().spawnParticle(type,
                player.getLocation().add(0, 1, 0),
                count, offsetX, offsetY, offsetZ, speed);
    }

    private String applyPlaceholders(String text) {
        return text
                .replace("%player%", player.getName())
                .replace("%spawn%", destination.getName())
                .replace("%floor%", destination.getFloor().getName())
                .replace("%seconds%", String.valueOf(delayTicks / 20));
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getStartLocation() {
        return startLocation;
    }
}
