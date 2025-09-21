package com.listeners.skills;

import com.managers.SkillManager;
import com.utils.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SneakingSkillListener implements Listener {

    private final SkillManager skillManager;
    private final double BASE_EXP = 1.5;

    // Track last sneaking location per player
    private final Map<UUID, Vector> lastSneakLocation = new HashMap<>();

    public SneakingSkillListener(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only track if player is sneaking
        if (!player.isSneaking()) {
            lastSneakLocation.remove(player.getUniqueId());
            return;
        }

        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();
        UUID playerId = player.getUniqueId();

        // Initialize last location if missing
        lastSneakLocation.putIfAbsent(playerId, from);
        Vector lastLocation = lastSneakLocation.get(playerId);

        // Horizontal distance moved
        double distanceMoved = to.clone().setY(0).distance(lastLocation.clone().setY(0));

        // Gain XP only if moved at least 1 block
        if (distanceMoved >= 1.0) {
            double sneakXP = calculateSneakExperience(player, distanceMoved);
            skillManager.addSkillExperience(playerId, SkillType.SNEAKING, sneakXP);

            // Update speed based on current level
            applySpeed(player);

            // Update last location
            lastSneakLocation.put(playerId, to);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Only reset speed when player stops sneaking
        if (!event.isSneaking()) {
            resetSpeed(player);
            lastSneakLocation.remove(player.getUniqueId());
        }
    }

    private double calculateSneakExperience(Player player, double distanceMoved) {
        return BASE_EXP * distanceMoved; // XP scales with distance
    }
    private double calculateSpeedBonus(Player player) {
        int level = skillManager.getSkillLevel(player.getUniqueId(), SkillType.SNEAKING);
        int maxLevel = getMaxLevel(); // 1000
        double baseSpeed = 0.2;
        double maxSpeed = 1.0;

        // Linear scaling: at max level, player reaches maxSpeed
        double speed = baseSpeed + (maxSpeed - baseSpeed) * Math.min((double)level / maxLevel, 1.0);
        return speed;
    }

    private void applySpeed(Player player) {
        double newSpeed = calculateSpeedBonus(player);
        player.setWalkSpeed((float) newSpeed);
    }


    private void resetSpeed(Player player) {
        player.setWalkSpeed(0.2f);
    }

    public int getMaxLevel() {
        return 1000;
    }

    public double getCurrentSpeedBonus(Player player) {
        return calculateSpeedBonus(player);
    }
}
