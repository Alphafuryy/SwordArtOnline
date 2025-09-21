package com.managers;

import com.SwordArtOnline;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatusBarManager {

    private final SwordArtOnline plugin;
    private final ConcurrentHashMap<UUID, Double> lastHp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Double> lastArmor = new ConcurrentHashMap<>();
    private BukkitTask updater;

    public StatusBarManager(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    // 🔹 Provide getPlugin() so listener can schedule tasks
    public SwordArtOnline getPlugin() {
        return plugin;
    }

    // Start periodic updates
    public void start() {
        // Update more frequently to ensure it shows
        updater = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, 10L);
    }

    public void stop() {
        if (updater != null) updater.cancel();
        lastHp.clear();
        lastArmor.clear();
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    // Update ActionBar for a player
    public void update(Player player) {
        if (player == null || !player.isOnline()) return;

        double hp = player.getHealth();
        double maxHp = safeAttr(player, Attribute.MAX_HEALTH, 20.0);
        double armor = safeAttr(player, Attribute.ARMOR, 0.0);

        double lastHpVal = lastHp.getOrDefault(player.getUniqueId(), -1.0);
        double lastArmorVal = lastArmor.getOrDefault(player.getUniqueId(), -1.0);

        // Always show the ActionBar, regardless of whether values changed
        lastHp.put(player.getUniqueId(), hp);
        lastArmor.put(player.getUniqueId(), armor);

        // Simple colors: red for HP, blue for armor
        String hpColor = "§c"; // Red
        String armorColor = "§9"; // Blue

        String message = String.format("%s❤ %.1f/%.1f   %s🛡 %.0f", hpColor, hp, maxHp, armorColor, armor);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void clear(Player player) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
        lastHp.remove(player.getUniqueId());
        lastArmor.remove(player.getUniqueId());
    }

    private double safeAttr(Player player, Attribute attr, double def) {
        return player.getAttribute(attr) != null ? player.getAttribute(attr).getValue() : def;
    }
}