package com.managers;

import com.SwordArtOnline;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class TaskManager {

    private final SwordArtOnline plugin;
    private FileConfiguration taskConfig;
    private File taskFile;

    public TaskManager(SwordArtOnline plugin) {
        this.plugin = plugin;
    }

    public void loadTasks() {
        File folder = new File(plugin.getDataFolder(), "Tasks");
        if (!folder.exists()) folder.mkdirs();

        taskFile = new File(folder, "spawn.yml");
        if (!taskFile.exists()) {
            plugin.saveResource("Tasks/spawn.yml", false);
        }

        taskConfig = YamlConfiguration.loadConfiguration(taskFile);
    }

    public void saveTasks() {
        try {
            taskConfig.save(taskFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getTaskConfig() {
        return taskConfig;
    }
}
