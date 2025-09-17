package com.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Main class to manage the  cursor system
public class CursorManager implements Listener {

    private final JavaPlugin plugin;
    private final CursorDataManager dataManager;
    private final CursorTeamManager teamManager;
    private final Map<UUID, PlayerCursorData> playerData = new HashMap<>();

    public CursorManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = new CursorDataManager(plugin);
        this.teamManager = new CursorTeamManager();

        loadData();
        startDecayTask();
    }

    private void loadData() {
        Map<UUID, PlayerCursorData> loadedData = dataManager.loadData();
        playerData.putAll(loadedData);

        // Apply colors to online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerCursorData data = playerData.get(player.getUniqueId());
            if (data != null) {
                teamManager.applyCursorColor(player, data.color);
            } else {
                data = new PlayerCursorData(CursorColor.GREEN, 0);
                playerData.put(player.getUniqueId(), data);
                teamManager.applyCursorColor(player, CursorColor.GREEN);
            }
        }
    }

    public void saveData() {
        dataManager.saveData(playerData);
    }

    private void startDecayTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerCursorData data = playerData.get(player.getUniqueId());
                if (data != null && data.color != CursorColor.GREEN) {
                    // Criminal points decay over time (1 point per minute)
                    if (currentTime - data.lastAttackTime > 60000) {
                        data.criminalPoints = Math.max(0, data.criminalPoints - 1);
                        data.lastAttackTime = currentTime;

                        if (data.criminalPoints == 0) {
                            setCursorColor(player, CursorColor.GREEN);
                        } else if (data.criminalPoints < 5) {
                            setCursorColor(player, CursorColor.ORANGE);
                        }
                    }
                }
            }
        }, 1200L, 1200L); // Run every minute
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (playerData.containsKey(uuid)) {
            teamManager.applyCursorColor(player, playerData.get(uuid).color);
        } else {
            playerData.put(uuid, new PlayerCursorData(CursorColor.GREEN, 0));
            teamManager.applyCursorColor(player, CursorColor.GREEN);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        saveData();
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        PlayerCursorData data = playerData.get(attacker.getUniqueId());
        if (data == null) {
            data = new PlayerCursorData(CursorColor.GREEN, 0);
            playerData.put(attacker.getUniqueId(), data);
        }

        // Increase criminal points
        data.criminalPoints += 2;
        data.lastAttackTime = System.currentTimeMillis();

        // Update cursor color based on criminal points
        if (data.criminalPoints >= 10) {
            setCursorColor(attacker, CursorColor.RED);
        } else if (data.criminalPoints >= 3) {
            setCursorColor(attacker, CursorColor.ORANGE);
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            PlayerCursorData data = playerData.get(killer.getUniqueId());
            if (data == null) {
                data = new PlayerCursorData(CursorColor.GREEN, 0);
                playerData.put(killer.getUniqueId(), data);
            }

            // Significant increase for killing
            data.criminalPoints += 5;
            data.lastAttackTime = System.currentTimeMillis();

            setCursorColor(killer, CursorColor.RED);
        }
    }

    private void setCursorColor(Player player, CursorColor color) {
        PlayerCursorData data = playerData.get(player.getUniqueId());
        if (data != null) {
            data.color = color;
        }
        teamManager.applyCursorColor(player, color);
    }

    public CursorColor getCursorColor(Player player) {
        PlayerCursorData data = playerData.get(player.getUniqueId());
        return data != null ? data.color : CursorColor.GREEN;
    }

    public int getCriminalPoints(Player player) {
        PlayerCursorData data = playerData.get(player.getUniqueId());
        return data != null ? data.criminalPoints : 0;
    }

    public void onDisable() {
        saveData();
        teamManager.cleanupTeams();
    }
}

// Data class for player cursor information
class PlayerCursorData {
    public CursorColor color;
    public int criminalPoints;
    public long lastAttackTime;

    public PlayerCursorData(CursorColor color, int criminalPoints) {
        this.color = color;
        this.criminalPoints = criminalPoints;
        this.lastAttackTime = System.currentTimeMillis();
    }
}

// Enum for cursor colors
enum CursorColor {
    GREEN(ChatColor.GREEN, "Green"),
    ORANGE(ChatColor.GOLD, "Orange"),
    RED(ChatColor.RED, "Red");

    public final ChatColor chatColor;
    public final String teamName;

    CursorColor(ChatColor chatColor, String teamName) {
        this.chatColor = chatColor;
        this.teamName = teamName;
    }
}

// Manager class for handling cursor teams
class CursorTeamManager {
    private Team greenTeam;
    private Team orangeTeam;
    private Team redTeam;

    public CursorTeamManager() {
        setupTeams();
    }

    private void setupTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Clean up any existing teams
        removeExistingTeams(scoreboard);

        // Create new teams
        greenTeam = scoreboard.registerNewTeam(CursorColor.GREEN.teamName);
        greenTeam.setColor(CursorColor.GREEN.chatColor);
        greenTeam.setPrefix(CursorColor.GREEN.chatColor.toString());

        orangeTeam = scoreboard.registerNewTeam(CursorColor.ORANGE.teamName);
        orangeTeam.setColor(CursorColor.ORANGE.chatColor);
        orangeTeam.setPrefix(CursorColor.ORANGE.chatColor.toString());

        redTeam = scoreboard.registerNewTeam(CursorColor.RED.teamName);
        redTeam.setColor(CursorColor.RED.chatColor);
        redTeam.setPrefix(CursorColor.RED.chatColor.toString());
    }

    private void removeExistingTeams(Scoreboard scoreboard) {
        for (CursorColor color : CursorColor.values()) {
            Team existingTeam = scoreboard.getTeam(color.teamName);
            if (existingTeam != null) {
                existingTeam.unregister();
            }
        }
    }

    public void applyCursorColor(Player player, CursorColor color) {
        // Remove from all teams first
        removeFromAllTeams(player);

        // Add to appropriate team
        switch (color) {
            case GREEN:
                greenTeam.addEntry(player.getName());
                break;
            case ORANGE:
                orangeTeam.addEntry(player.getName());
                break;
            case RED:
                redTeam.addEntry(player.getName());
                break;
        }

        // Update tab list and display name
        player.setPlayerListName(color.chatColor + player.getName());
        player.setDisplayName(color.chatColor + player.getName());
    }

    private void removeFromAllTeams(Player player) {
        greenTeam.removeEntry(player.getName());
        orangeTeam.removeEntry(player.getName());
        redTeam.removeEntry(player.getName());
    }

    public void cleanupTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        removeExistingTeams(scoreboard);
    }
}

// Manager class for handling data persistence
class CursorDataManager {
    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public CursorDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "cursor_data.yml");
    }

    public Map<UUID, PlayerCursorData> loadData() {
        Map<UUID, PlayerCursorData> loadedData = new HashMap<>();

        if (!dataFile.exists()) {
            plugin.saveResource("cursor_data.yml", false);
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                CursorColor color = CursorColor.valueOf(dataConfig.getString("players." + key + ".color"));
                int points = dataConfig.getInt("players." + key + ".criminalPoints");

                loadedData.put(uuid, new PlayerCursorData(color, points));
            }
        }

        return loadedData;
    }

    public void saveData(Map<UUID, PlayerCursorData> playerData) {
        dataConfig.set("players", null);

        for (UUID uuid : playerData.keySet()) {
            PlayerCursorData data = playerData.get(uuid);
            dataConfig.set("players." + uuid.toString() + ".color", data.color.toString());
            dataConfig.set("players." + uuid.toString() + ".criminalPoints", data.criminalPoints);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save  cursor data: " + e.getMessage());
        }
    }
}