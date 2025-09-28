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

public class CursorManager implements Listener {

    private final JavaPlugin plugin;
    private final CursorDataManager dataManager;
    private final CursorConfigManager configManager;
    private CursorTeamManager teamManager;
    private final Map<UUID, PlayerCursorData> playerData = new HashMap<>();

    public CursorManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = new CursorDataManager(plugin);
        this.configManager = new CursorConfigManager(plugin);

        // Load config FIRST
        configManager.loadConfig();

        // THEN initialize team manager
        this.teamManager = new CursorTeamManager(configManager);

        loadData();
        startDecayTask();
    }

    private void loadData() {
        Map<UUID, PlayerCursorData> loadedData = dataManager.loadData();
        playerData.putAll(loadedData);

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
            int decayAmount = configManager.getDecayPerMinute();

            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerCursorData data = playerData.get(player.getUniqueId());
                if (data != null && data.color != CursorColor.GREEN) {
                    if (currentTime - data.lastAttackTime > 60000) {
                        data.criminalPoints = Math.max(0, data.criminalPoints - decayAmount);
                        data.lastAttackTime = currentTime;

                        if (data.criminalPoints == 0) {
                            setCursorColor(player, CursorColor.GREEN);
                        } else if (data.criminalPoints < configManager.getOrangeThreshold()) {
                            setCursorColor(player, CursorColor.ORANGE);
                        }
                    }
                }
            }
        }, 1200L, 1200L);
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

        data.criminalPoints += configManager.getAttackPoints();
        data.lastAttackTime = System.currentTimeMillis();

        if (data.criminalPoints >= configManager.getRedThreshold()) {
            setCursorColor(attacker, CursorColor.RED);
        } else if (data.criminalPoints >= configManager.getOrangeThreshold()) {
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

            data.criminalPoints += configManager.getKillPoints();
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

    public void reloadConfig() {
        configManager.loadConfig();
        teamManager.reloadConfig(configManager);

        // Update all online players with new formats
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerCursorData data = playerData.get(player.getUniqueId());
            if (data != null) {
                teamManager.applyCursorColor(player, data.color);
            }
        }
    }

    public void onDisable() {
        saveData();
        teamManager.cleanupTeams();
    }
}

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

enum CursorColor {
    GREEN("Green"),
    ORANGE("Orange"),
    RED("Red");

    public final String teamName;

    CursorColor(String teamName) {
        this.teamName = teamName;
    }
}

class CursorTeamManager {
    private CursorConfigManager configManager;
    private Team greenTeam;
    private Team orangeTeam;
    private Team redTeam;

    public CursorTeamManager(CursorConfigManager configManager) {
        this.configManager = configManager;
        // Don't setup teams here, wait until config is definitely loaded
    }

    private void setupTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        removeExistingTeams(scoreboard);

        // Add null check for config
        if (configManager == null || !configManager.useTeams()) return;

        greenTeam = scoreboard.registerNewTeam(CursorColor.GREEN.teamName);
        orangeTeam = scoreboard.registerNewTeam(CursorColor.ORANGE.teamName);
        redTeam = scoreboard.registerNewTeam(CursorColor.RED.teamName);

        updateTeamFormats();
    }

    private void updateTeamFormats() {
        if (greenTeam != null) {
            greenTeam.setPrefix(ChatColor.translateAlternateColorCodes('&',
                    configManager.getFormat(CursorColor.GREEN).replace("%player%", "")));
        }
        if (orangeTeam != null) {
            orangeTeam.setPrefix(ChatColor.translateAlternateColorCodes('&',
                    configManager.getFormat(CursorColor.ORANGE).replace("%player%", "")));
        }
        if (redTeam != null) {
            redTeam.setPrefix(ChatColor.translateAlternateColorCodes('&',
                    configManager.getFormat(CursorColor.RED).replace("%player%", "")));
        }
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
        // Lazy initialization - setup teams if not already done
        if (greenTeam == null && orangeTeam == null && redTeam == null) {
            setupTeams();
        }

        removeFromAllTeams(player);

        if (configManager != null && configManager.useTeams()) {
            switch (color) {
                case GREEN:
                    if (greenTeam != null) greenTeam.addEntry(player.getName());
                    break;
                case ORANGE:
                    if (orangeTeam != null) orangeTeam.addEntry(player.getName());
                    break;
                case RED:
                    if (redTeam != null) redTeam.addEntry(player.getName());
                    break;
            }
        }

        // Update display names based on config
        if (configManager != null && configManager.updateTabList()) {
            String format = configManager.getFormat(color);
            String displayName = format.replace("%player%", player.getName());
            player.setPlayerListName(ChatColor.translateAlternateColorCodes('&', displayName));
        }

        if (configManager != null && configManager.updateDisplayName()) {
            String format = configManager.getFormat(color);
            String displayName = format.replace("%player%", player.getName());
            player.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        }
    }

    private void removeFromAllTeams(Player player) {
        if (greenTeam != null) greenTeam.removeEntry(player.getName());
        if (orangeTeam != null) orangeTeam.removeEntry(player.getName());
        if (redTeam != null) redTeam.removeEntry(player.getName());
    }

    public void reloadConfig(CursorConfigManager newConfigManager) {
        this.configManager = newConfigManager;
        setupTeams(); // Re-setup teams with new config
    }

    public void cleanupTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        removeExistingTeams(scoreboard);
    }
}

class CursorConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final File configFile;

    public CursorConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "cursor.yml");
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Verify config was loaded successfully
        if (config == null) {
            plugin.getLogger().severe("Failed to load cursor.yml configuration!");
            // Create emergency default config
            createEmergencyConfig();
        }
    }

    private void createDefaultConfig() {
        try {
            plugin.getDataFolder().mkdirs();

            // Create default configuration content
            YamlConfiguration defaultConfig = new YamlConfiguration();

            // Formats section
            defaultConfig.set("formats.GREEN", "&a● %player%");
            defaultConfig.set("formats.ORANGE", "&6● %player%");
            defaultConfig.set("formats.RED", "&c● %player%");

            // Thresholds section
            defaultConfig.set("thresholds.ORANGE", 3);
            defaultConfig.set("thresholds.RED", 10);

            // Points section
            defaultConfig.set("points.attack", 2);
            defaultConfig.set("points.kill", 5);
            defaultConfig.set("points.decay_per_minute", 1);

            // Settings section
            defaultConfig.set("settings.update_tab_list", true);
            defaultConfig.set("settings.update_display_name", true);
            defaultConfig.set("settings.use_teams", true);

            // Add comments and documentation
            defaultConfig.options().header(
                    "Cursor Display System Configuration\n" +
                            "Customize how player names appear based on their criminal activity level\n\n" +
                            "Formats: Use & for color codes and %player% for player name placeholder\n" +
                            "Thresholds: Points needed for each color level\n" +
                            "Points: How points are gained/lost\n" +
                            "Settings: Display behavior options\n"
            );

            // Save the default config
            defaultConfig.save(configFile);
            plugin.getLogger().info("Created default cursor.yml configuration file");

        } catch (IOException e) {
            plugin.getLogger().severe("Could not create default cursor.yml: " + e.getMessage());
            createEmergencyConfig();
        }
    }

    private void createEmergencyConfig() {
        // Create a basic in-memory config as fallback
        config = new YamlConfiguration();
        config.set("formats.GREEN", "&a● %player%");
        config.set("formats.ORANGE", "&6● %player%");
        config.set("formats.RED", "&c● %player%");
        config.set("thresholds.ORANGE", 3);
        config.set("thresholds.RED", 10);
        config.set("points.attack", 2);
        config.set("points.kill", 5);
        config.set("points.decay_per_minute", 1);
        config.set("settings.update_tab_list", true);
        config.set("settings.update_display_name", true);
        config.set("settings.use_teams", true);

        plugin.getLogger().warning("Using emergency in-memory configuration");
    }

    public String getFormat(CursorColor color) {
        if (config == null) {
            // Fallback defaults
            switch (color) {
                case GREEN: return "&a● %player%";
                case ORANGE: return "&6● %player%";
                case RED: return "&c● %player%";
                default: return "&f%player%";
            }
        }

        String format = config.getString("formats." + color.name());
        if (format == null) {
            // Return default format if not found in config
            switch (color) {
                case GREEN: return "&a● %player%";
                case ORANGE: return "&6● %player%";
                case RED: return "&c● %player%";
                default: return "&f%player%";
            }
        }
        return format;
    }

    public int getOrangeThreshold() {
        return config != null ? config.getInt("thresholds.ORANGE", 3) : 3;
    }

    public int getRedThreshold() {
        return config != null ? config.getInt("thresholds.RED", 10) : 10;
    }

    public int getAttackPoints() {
        return config != null ? config.getInt("points.attack", 2) : 2;
    }

    public int getKillPoints() {
        return config != null ? config.getInt("points.kill", 5) : 5;
    }

    public int getDecayPerMinute() {
        return config != null ? config.getInt("points.decay_per_minute", 1) : 1;
    }

    public boolean updateTabList() {
        return config != null && config.getBoolean("settings.update_tab_list", true);
    }

    public boolean updateDisplayName() {
        return config != null && config.getBoolean("settings.update_display_name", true);
    }

    public boolean useTeams() {
        return config != null && config.getBoolean("settings.use_teams", true);
    }
}

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
            return loadedData;
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    CursorColor color = CursorColor.valueOf(dataConfig.getString("players." + key + ".color"));
                    int points = dataConfig.getInt("players." + key + ".criminalPoints");
                    loadedData.put(uuid, new PlayerCursorData(color, points));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid data for player: " + key);
                }
            }
        }

        return loadedData;
    }

    public void saveData(Map<UUID, PlayerCursorData> playerData) {
        dataConfig = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerCursorData> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey().toString();
            dataConfig.set(path + ".color", entry.getValue().color.toString());
            dataConfig.set(path + ".criminalPoints", entry.getValue().criminalPoints);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save cursor data: " + e.getMessage());
        }
    }
}