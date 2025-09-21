package com.managers;

import com.utils.PlayerSkillData;
import com.utils.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SkillManager {
    private final Map<UUID, PlayerSkillData> playerSkills;
    private final File dataFolder;

    public SkillManager(File pluginDataFolder) {
        this.playerSkills = new HashMap<>();
        this.dataFolder = new File(pluginDataFolder, "Datas");

        // Create the Datas folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerSkillData getPlayerData(UUID playerId) {
        return playerSkills.computeIfAbsent(playerId, PlayerSkillData::new);
    }

    public PlayerSkillData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public int getSkillLevel(UUID playerId, SkillType skill) {
        return getPlayerData(playerId).getSkillLevel(skill);
    }

    public void setSkillLevel(UUID playerId, SkillType skill, int level) {
        getPlayerData(playerId).setSkillLevel(skill, level);
    }

    public double getSkillExperience(UUID playerId, SkillType skill) {
        return getPlayerData(playerId).getSkillExperience(skill);
    }

    public void setSkillExperience(UUID playerId, SkillType skill, double experience) {
        getPlayerData(playerId).setSkillExperience(skill, experience);
    }

    public boolean addSkillExperience(UUID playerId, SkillType skill, double amount) {
        getPlayerData(playerId).addSkillExperience(skill, amount);
        return false;
    }

    public void removeSkillExperience(UUID playerId, SkillType skill, double amount) {
        getPlayerData(playerId).removeSkillExperience(skill, amount);
    }

    public void saveAllData() {
        for (Map.Entry<UUID, PlayerSkillData> entry : playerSkills.entrySet()) {
            savePlayerData(entry.getKey());
        }
    }

    public void loadAllData() {
        // This method would typically be called on server startup
        // Load all player data files from the Datas folder
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".dat"));

        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    UUID playerId = UUID.fromString(fileName.substring(0, fileName.lastIndexOf('.')));
                    loadPlayerData(playerId);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().log(Level.WARNING, "Invalid filename format: " + file.getName());
                }
            }
        }
    }

    public void savePlayerData(UUID playerId) {
        PlayerSkillData data = playerSkills.get(playerId);
        if (data == null) return;

        File playerFile = new File(dataFolder, playerId.toString() + ".dat");

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(playerFile))) {
            oos.writeObject(data);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save player skill data for " + playerId, e);
        }
    }

    public void loadPlayerData(UUID playerId) {
        File playerFile = new File(dataFolder, playerId.toString() + ".dat");

        if (!playerFile.exists()) {
            // Create new data if file doesn't exist
            playerSkills.put(playerId, new PlayerSkillData(playerId));
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(playerFile))) {
            PlayerSkillData data = (PlayerSkillData) ois.readObject();
            playerSkills.put(playerId, data);
        } catch (IOException | ClassNotFoundException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load player skill data for " + playerId, e);
            // Create new data if loading fails
            playerSkills.put(playerId, new PlayerSkillData(playerId));
        }
    }

    public void removePlayerData(UUID playerId) {
        playerSkills.remove(playerId);
        File playerFile = new File(dataFolder, playerId.toString() + ".dat");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }
}