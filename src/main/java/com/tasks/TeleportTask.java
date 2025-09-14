package com.tasks;

import com.SwordArtOnline;
import com.utils.SpawnPoint;
import com.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TeleportTask {

    private final SwordArtOnline plugin;
    private final Player player;
    private final SpawnPoint destination;
    private final long delayTicks;
    private final Runnable onFinish;
    private final Runnable onCancel;

    private Location startLocation;
    private int teleportTaskId;
    private int countdownTaskId;
    private boolean cancelled = false;

    public TeleportTask(SwordArtOnline plugin, Player player, SpawnPoint destination, long delayTicks,
                        Runnable onFinish, Runnable onCancel) {
        this.plugin = plugin;
        this.player = player;
        this.destination = destination;
        this.delayTicks = delayTicks;
        this.onFinish = onFinish;
        this.onCancel = onCancel;
    }

    public void start() {
        this.startLocation = player.getLocation();
        FileConfiguration cfg = plugin.getTaskManager().getTaskConfig();

        int secondsTotal = (int) (delayTicks / 20);

        // Start countdown updater
        countdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            int secondsLeft = secondsTotal;

            @Override
            public void run() {
                if (cancelled) {
                    Bukkit.getScheduler().cancelTask(countdownTaskId);
                    return;
                }

                String actionbar = cfg.getString("teleport-task.countdown.actionbar", "");
                if (!actionbar.isEmpty()) {
                    player.sendActionBar(format(actionbar, secondsLeft));
                }

                String extra = cfg.getString("teleport-task.countdown.extra-message", "");
                if (!extra.isEmpty()) {
                    player.sendMessage(format(extra, secondsLeft));
                }

                String title = cfg.getString("teleport-task.countdown.title", "");
                String subtitle = cfg.getString("teleport-task.countdown.subtitle", "");
                if (!title.isEmpty() || !subtitle.isEmpty()) {
                    int fadeIn = cfg.getInt("teleport-task.countdown.fade-in", 5);
                    int stay = cfg.getInt("teleport-task.countdown.stay", 20);
                    int fadeOut = cfg.getInt("teleport-task.countdown.fade-out", 5);
                    player.sendTitle(format(title, secondsLeft), format(subtitle, secondsLeft), fadeIn, stay, fadeOut);
                }

                playParticles("teleport-task.countdown.particle");

                secondsLeft--;
                if (secondsLeft < 0) {
                    Bukkit.getScheduler().cancelTask(countdownTaskId);
                }
            }
        }, 0L, 20L);

        // Schedule teleport after delay
        teleportTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (cancelled) return;

            player.teleport(destination.getLocation());

            sendConfigured("teleport-task.success", 0);
            playParticles("teleport-task.success.particle");

            if (onFinish != null) onFinish.run();

        }, delayTicks).getTaskId();
    }

    public void cancel(String reasonKey) {
        if (!cancelled) {
            cancelled = true;
            Bukkit.getScheduler().cancelTask(teleportTaskId);
            Bukkit.getScheduler().cancelTask(countdownTaskId);

            FileConfiguration cfg = plugin.getTaskManager().getTaskConfig();
            String msg = cfg.getString("teleport-task.cancel." + reasonKey, "&cTeleport cancelled.");
            player.sendMessage(format(msg, 0));

            playParticles("teleport-task.cancel.particle");

            if (onCancel != null) onCancel.run();
        }
    }

    private void sendConfigured(String section, int seconds) {
        FileConfiguration cfg = plugin.getTaskManager().getTaskConfig();

        String actionbar = cfg.getString(section + ".actionbar", "");
        if (!actionbar.isEmpty()) {
            player.sendActionBar(format(actionbar, seconds));
        }

        String extra = cfg.getString(section + ".extra-message", "");
        if (!extra.isEmpty()) {
            player.sendMessage(format(extra, seconds));
        }

        String title = cfg.getString(section + ".title", "");
        String subtitle = cfg.getString(section + ".subtitle", "");
        if (!title.isEmpty() || !subtitle.isEmpty()) {
            int fadeIn = cfg.getInt(section + ".fade-in", 5);
            int stay = cfg.getInt(section + ".stay", 20);
            int fadeOut = cfg.getInt(section + ".fade-out", 5);
            player.sendTitle(format(title, seconds), format(subtitle, seconds), fadeIn, stay, fadeOut);
        }
    }

    private void playParticles(String path) {
        FileConfiguration cfg = plugin.getTaskManager().getTaskConfig();
        if (!cfg.getBoolean(path + ".enabled", false)) return;

        String typeName = cfg.getString(path + ".type", "VILLAGER_HAPPY");
        Particle type;
        try {
            type = Particle.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = Particle.HAPPY_VILLAGER;
        }

        int count = cfg.getInt(path + ".count", 5);
        double radius = cfg.getDouble(path + ".radius", 1.0);
        String shape = cfg.getString(path + ".shape", "point").toLowerCase();

        Location loc = player.getLocation().add(0, 1, 0);

        switch (shape) {
            case "circle":
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    player.getWorld().spawnParticle(type, loc.clone().add(x, 0, z), 1);
                }
                break;
            case "sphere":
                for (int i = 0; i < count; i++) {
                    double theta = Math.random() * 2 * Math.PI;
                    double phi = Math.acos(2 * Math.random() - 1);
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.sin(phi) * Math.sin(theta);
                    double z = radius * Math.cos(phi);
                    player.getWorld().spawnParticle(type, loc.clone().add(x, y, z), 1);
                }
                break;
            case "point":
            default:
                player.getWorld().spawnParticle(type, loc, count, 0.5, 1, 0.5, 0.1);
                break;
        }
    }

    private String format(String raw, int secondsLeft) {
        if (raw == null) return "";
        return TextUtil.colorize(raw
                .replace("%player%", player.getName())
                .replace("%spawn%", destination.getName())
                .replace("%floor%", destination.getFloor().getName())
                .replace("%seconds%", String.valueOf((int) Math.ceil(delayTicks / 20.0)))
                .replace("%countdown%", String.valueOf(secondsLeft)));
    }

    public Location getStartLocation() {
        return startLocation;
    }
}
