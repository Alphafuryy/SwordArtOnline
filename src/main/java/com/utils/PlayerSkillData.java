package com.utils;

import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSkillData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID playerId;
    private final Map<SkillType, Integer> skillLevels;
    private final Map<SkillType, Double> skillExperience;

    public PlayerSkillData(UUID playerId) {
        this.playerId = playerId;
        this.skillLevels = new HashMap<>();
        this.skillExperience = new HashMap<>();

        for (SkillType skill : SkillType.values()) {
            skillLevels.put(skill, 0);
            skillExperience.put(skill, 0.0);
        }
    }

    public int getSkillLevel(SkillType skill) {
        return skillLevels.getOrDefault(skill, 0);
    }

    public void setSkillLevel(SkillType skill, int level) {
        skillLevels.put(skill, Math.max(0, level));
    }

    public double getSkillExperience(SkillType skill) {
        return skillExperience.getOrDefault(skill, 0.0);
    }

    public void setSkillExperience(SkillType skill, double experience) {
        skillExperience.put(skill, Math.max(0, experience));
    }

    public void addSkillExperience(SkillType skill, double amount) {
        double currentExp = getSkillExperience(skill);
        currentExp += amount;
        setSkillExperience(skill, currentExp);
        checkLevelUp(skill);
    }

    public void removeSkillExperience(SkillType skill, double amount) {
        double currentExp = getSkillExperience(skill);
        setSkillExperience(skill, Math.max(0, currentExp - amount));
    }

    private void checkLevelUp(SkillType skill) {
        int currentLevel = getSkillLevel(skill);
        double currentExp = getSkillExperience(skill);
        boolean leveledUp = false;

        while (currentLevel < 1000) {
            double expNeeded = getExpRequiredForLevel(skill, currentLevel + 1);
            if (currentExp >= expNeeded) {
                currentLevel++;
                currentExp -= expNeeded;
                leveledUp = true;
            } else break;
        }

        // Prevent leftover XP exceeding next level
        if (currentLevel < 1000) {
            currentExp = Math.min(currentExp, getExpRequiredForLevel(skill, currentLevel + 1));
        }

        if (leveledUp) {
            setSkillLevel(skill, currentLevel);
            setSkillExperience(skill, currentExp);

            Player player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§aYour " + skill.getDisplayName() + " skill increased to level " + currentLevel + "!");
                if (currentLevel % 50 == 0) {
                    player.sendMessage("§6§lMilestone Reached! Your " + skill.getDisplayName() + " skill is now level " + currentLevel + "!");
                }
            }
        } else {
            setSkillExperience(skill, currentExp);
        }
    }

    /**
     * Updated XP curve: smaller base XP for realistic numbers
     */
    private double getExpRequiredForLevel(SkillType skill, int level) {
        if (level <= 0) return 0;
        if (level > 1000) return Double.MAX_VALUE;

        double baseExp = 20; // smaller for realistic gain
        double difficulty = skill.getDifficultyMultiplier();
        double exp;

        if (level <= 10) exp = baseExp * difficulty * Math.pow(level, 1.1);
        else if (level <= 50) exp = baseExp * difficulty * Math.pow(level, 1.25);
        else if (level <= 100) exp = baseExp * difficulty * Math.pow(level, 1.4);
        else if (level <= 300) exp = baseExp * difficulty * Math.pow(level, 1.55);
        else if (level <= 600) exp = baseExp * difficulty * Math.pow(level, 1.7);
        else exp = baseExp * difficulty * Math.pow(level, 1.85);

        return exp;
    }

    public double getProgressToNextLevel(SkillType skill) {
        int currentLevel = getSkillLevel(skill);
        if (currentLevel >= 1000) return 100.0;

        double currentExp = getSkillExperience(skill);
        double expNeeded = getExpRequiredForLevel(skill, currentLevel + 1);
        return Math.min((currentExp / expNeeded) * 100.0, 100.0);
    }

    public double getExpNeededForNextLevel(SkillType skill) {
        int currentLevel = getSkillLevel(skill);
        if (currentLevel >= 1000) return 0;
        return getExpRequiredForLevel(skill, currentLevel + 1);
    }

    public double getTotalExperienceInvested(SkillType skill) {
        int currentLevel = getSkillLevel(skill);
        double totalExp = getSkillExperience(skill);
        for (int level = 1; level <= currentLevel; level++) {
            totalExp += getExpRequiredForLevel(skill, level);
        }
        return totalExp;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Map<SkillType, Integer> getAllSkillLevels() {
        return new HashMap<>(skillLevels);
    }

    public Map<SkillType, Double> getAllSkillExperiences() {
        return new HashMap<>(skillExperience);
    }

    /**
     * Format XP nicely: 1.2K, 3.4M, etc.
     */
    private String formatXp(double xp) {
        if (xp >= 1_000_000) return String.format("%.2fM", xp / 1_000_000);
        if (xp >= 1_000) return String.format("%.2fK", xp / 1_000);
        return String.format("%.1f", xp);
    }

    public String getSkillInfo(SkillType skill) {
        int level = getSkillLevel(skill);
        double exp = getSkillExperience(skill);
        double nextLevelExp = getExpNeededForNextLevel(skill);
        double progress = getProgressToNextLevel(skill);

        return String.format("%s: Level %d (%s/%s XP - %.2f%%)",
                skill.getDisplayName(),
                level,
                formatXp(exp),
                formatXp(nextLevelExp),
                progress
        );
    }
}
