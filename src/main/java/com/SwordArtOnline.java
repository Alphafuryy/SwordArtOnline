package com;

import com.commands.SpawnCommand;
import com.commands.RegionCommand;
import com.listeners.TeleportListener;
import com.listeners.RegionListener;
import com.managers.*;
import com.tabcompleters.RegionTabCompleter;
import com.tabcompleters.SpawnTabCompleter;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SwordArtOnline extends JavaPlugin {

    private static SwordArtOnline instance;

    private SpawnManager spawnManager;
    private FloorManager floorManager;
    private MessagesManager messagesManager;
    private TaskManager taskManager;
    private TeleportListener teleportListener;

    private RegionManager regionManager;
    private RegionListener regionListener;

    @Override
    public void onEnable() {
        instance = this;

        registerManagers();
        registerListeners();
        registerCommands();

        getLogger().info("⚔️ SAO Linked!");
    }

    private void registerManagers() {
        messagesManager = new MessagesManager(this);
        messagesManager.loadMessages("spawn");
        messagesManager.loadMessages("region");
        floorManager = new FloorManager(this);
        spawnManager = new SpawnManager(this);

        taskManager = new TaskManager(this);
        taskManager.loadTasks();

        regionManager = new RegionManager(this);
    }

    private void registerListeners() {
        teleportListener = new TeleportListener(this);
        getServer().getPluginManager().registerEvents(teleportListener, this);

        regionListener = new RegionListener();
        getServer().getPluginManager().registerEvents(regionListener, this);
    }

    private void registerCommands() {
        SpawnCommand spawnCommand = new SpawnCommand(this, teleportListener);
        getCommand("spawn").setExecutor(spawnCommand);
        getCommand("spawn").setTabCompleter(new SpawnTabCompleter(this));
        RegionCommand regionCommand = new RegionCommand(regionManager, regionListener);
        getCommand("region").setExecutor(regionCommand);
        getCommand("region").setTabCompleter(new RegionTabCompleter(this));

    }

    public static SwordArtOnline getInstance() {
        return instance;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public FloorManager getFloorManager() {
        return floorManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public TeleportListener getTeleportListener() {
        return teleportListener;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public RegionListener getRegionListener() {
        return regionListener;
    }
}
