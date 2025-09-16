package com;

import com.commands.SpawnCommand;
import com.commands.RegionCommand;
import com.commands.SkillCommand;

import com.listeners.TeleportListener;
import com.listeners.RegionListener;
import com.managers.*;
import com.tabcompleters.RegionTabCompleter;
import com.tabcompleters.SpawnTabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class SwordArtOnline extends JavaPlugin {

    private static SwordArtOnline instance;

    private SpawnManager spawnManager;
    private FloorManager floorManager;
    private MessagesManager messagesManager;
    private TaskManager taskManager;
    private TeleportListener teleportListener;
    private DoubleCleaveAbility doubleCleaveAbility;
    private CursorManager cursorManager;

    private RegionManager regionManager;
    private RegionListener regionListener;

    @Override
    public void onEnable() {
        instance = this;

        // Register managers, listeners, commands
        registerManagers();
        registerListeners();
        registerCommands();

        getLogger().info("⚔️  Plugin Enabled with Abilities!");
    }

    private void registerManagers() {
        messagesManager = new MessagesManager(this);
        messagesManager.loadMessages("spawn");
        messagesManager.loadMessages("region");

        floorManager = new FloorManager(this);
        spawnManager = new SpawnManager(this);
        taskManager = new TaskManager(this);
        taskManager.loadTasks();
        cursorManager = new CursorManager(this);

        regionManager = new RegionManager(this);
    }

    private void registerListeners() {
        teleportListener = new TeleportListener(this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        doubleCleaveAbility = new DoubleCleaveAbility(this);
        getServer().getPluginManager().registerEvents(doubleCleaveAbility, this);

        regionListener = new RegionListener();
        getServer().getPluginManager().registerEvents(regionListener, this);
        cursorManager = new CursorManager(this);

        getServer().getPluginManager().registerEvents(cursorManager, this);

    }

    private void registerCommands() {
        SpawnCommand spawnCommand = new SpawnCommand(this, teleportListener);
        getCommand("spawn").setExecutor(spawnCommand);
        getCommand("spawn").setTabCompleter(new SpawnTabCompleter(this));

        RegionCommand regionCommand = new RegionCommand(regionManager, regionListener);
        getCommand("region").setExecutor(regionCommand);
        getCommand("region").setTabCompleter(new RegionTabCompleter(this));
        SkillCommand skillCommand = new SkillCommand();
        getCommand("skill").setExecutor(skillCommand);
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