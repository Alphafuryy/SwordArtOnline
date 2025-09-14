package com;

import com.commands.SpawnCommand;
import com.listeners.TeleportListener;
import com.managers.FloorManager;
import com.managers.MessagesManager;
import com.managers.SpawnManager;
import com.managers.TaskManager;
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

    @Override
    public void onEnable() {
        instance = this;

        registerManagers();
        registerListeners();
        registerCommands();

        getLogger().info("SAO Linked!");
    }

    private void registerManagers() {
        messagesManager = new MessagesManager(this);
        messagesManager.loadMessages();

        floorManager = new FloorManager(this);
        spawnManager = new SpawnManager(this);

        taskManager = new TaskManager(this);
        taskManager.loadTasks();
    }

    private void registerListeners() {
        teleportListener = new TeleportListener(this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
    }

    private void registerCommands() {
        // Create command executor and tab completer
        SpawnCommand spawnCommand = new SpawnCommand(this, teleportListener);
        SpawnTabCompleter spawnTabCompleter = new SpawnTabCompleter(this);

        // Register the command executor and tab completer
        PluginCommand command = getCommand("spawn");
        if (command != null) {
            command.setExecutor(spawnCommand);
            command.setTabCompleter(spawnTabCompleter);
        } else {
            getLogger().warning("Failed to register /spawn command!");
        }
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
}
