package com.skills;

import com.SwordArtOnline;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillSlot {
    private final Skill skill;
    private final Map<UUID, Long> cooldowns;

    public SkillSlot(Skill skill) {
        this.skill = skill;
        this.cooldowns = new HashMap<>();
    }

    public boolean activate(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if player can activate (permission + weapon)
        if (!skill.canActivate(player)) {
            return false;
        }

        // Check cooldown
        if (cooldowns.containsKey(playerId)) {
            long cooldownEnd = cooldowns.get(playerId);
            long currentTime = System.currentTimeMillis();

            if (currentTime < cooldownEnd) {
                long remaining = (cooldownEnd - currentTime) / 1000;
                player.sendMessage(ChatColor.RED + skill.getName() + " on cooldown! " + remaining + "s remaining");
                return false;
            }
        }

        // Activate skill
        try {
            skill.activate(player);

            // Set cooldown
            if (skill.getCooldown() > 0) {
                cooldowns.put(playerId, System.currentTimeMillis() + (skill.getCooldown() * 1000L));

                // Schedule cooldown removal (optional, for memory management)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        cooldowns.remove(playerId);
                    }
                }.runTaskLater(SwordArtOnline.getInstance(), skill.getCooldown() * 20L);
            }

            return true;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to activate skill: " + e.getMessage());
            Bukkit.getLogger().warning("Skill activation failed: " + e.getMessage());
            return false;
        }
    }

    public Skill getSkill() {
        return skill;
    }

    public long getRemainingCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId)) {
            return 0;
        }

        long remaining = cooldowns.get(playerId) - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
}