package com;

import com.commands.SpawnCommand;
import com.commands.RegionCommand;
import com.commands.SkillCommand;
import com.guis.CraftingGUI;
import com.guis.SmithingGUI;
import com.guis.SmithingMenuGUI;
import com.listeners.StatusBarListener;
import com.listeners.TeleportListener;
import com.listeners.RegionListener;
import com.listeners.guis.SmithingMenuGUIListener;
import com.listeners.skills.AcrobaticsSkillListener;
import com.listeners.skills.CarpentrySkillListener;
import com.listeners.skills.PickingSkillListener;
import com.listeners.skills.SneakingSkillListener;
import com.listeners.npcs.SmithingEntityListener;
import com.managers.*;
import com.tabcompleters.RegionTabCompleter;
import com.tabcompleters.SpawnTabCompleter;
import com.utils.GUI;
import com.utils.NPC;
import com.utils.PlayerSkillData;
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
    private SkillManager skillManager;
    private AcrobaticsSkillListener acrobaticsSkillListener;
    private StatusBarManager statusBarManager;
    private StatusBarListener statusBarListener;
    private SneakingSkillListener sneakingSkillListener;
    private PickingSkillListener pickingSkillListener;
    private CarpentrySkillListener carpentrySkillListener;
    private SmithingEntityListener smithingEntityListener;
    private SmithingMenuGUIListener smithingMenuGUIListener;

    @Override
    public void onEnable() {
        instance = this;

        // Load GUI and NPC configs
        GUI.loadConfigs(this);
        NPC.loadNPCConfig(this);

        // Register managers, listeners, commands
        registerManagers();
        registerListeners();
        registerCommands();

        // Load all data
        loadAllData();

        // Start the status bar system
        if (statusBarManager != null) {
            statusBarManager.start();
        }

        getLogger().info("SwordArtOnline enabled with StatusBar system and GUIs!");
    }

    @Override
    public void onDisable() {
        // Save all data when plugin disables
        saveAllData();

        // Clear status bar manager
        if (statusBarManager != null) {
            statusBarManager.stop();
        }

        getLogger().info("SwordArtOnline disabled.");
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

        // Initialize SkillManager
        skillManager = new SkillManager(this.getDataFolder());

        // Initialize StatusBarManager
        statusBarManager = new StatusBarManager(this);
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

        carpentrySkillListener = new CarpentrySkillListener(this, skillManager, 1000);
        getServer().getPluginManager().registerEvents(carpentrySkillListener, this);
        pickingSkillListener = new PickingSkillListener(this, skillManager);
        getServer().getPluginManager().registerEvents(pickingSkillListener, this);
        acrobaticsSkillListener = new AcrobaticsSkillListener(skillManager);
        getServer().getPluginManager().registerEvents(acrobaticsSkillListener, this);
        sneakingSkillListener = new SneakingSkillListener(skillManager);
        getServer().getPluginManager().registerEvents(sneakingSkillListener, this);

        statusBarListener = new StatusBarListener(statusBarManager);
        getServer().getPluginManager().registerEvents(statusBarListener, this);

        // Register Smithing NPC listener
        smithingMenuGUIListener = new SmithingMenuGUIListener();
        getServer().getPluginManager().registerEvents(smithingMenuGUIListener, this);
        smithingEntityListener = new SmithingEntityListener();
        getServer().getPluginManager().registerEvents(smithingEntityListener, this);
    }

    private void registerCommands() {
        SpawnCommand spawnCommand = new SpawnCommand(this, teleportListener);
        getCommand("spawn").setExecutor(spawnCommand);
        getCommand("spawn").setTabCompleter(new SpawnTabCompleter(this));

        RegionCommand regionCommand = new RegionCommand(regionManager, regionListener);
        getCommand("region").setExecutor(regionCommand);
        getCommand("region").setTabCompleter(new RegionTabCompleter(this));

        SkillCommand skillCommand = new SkillCommand(skillManager);
        getCommand("skill").setExecutor(skillCommand);
    }

    private void loadAllData() {
        skillManager.loadAllData();
        getLogger().info("All data loaded successfully!");
    }

    private void saveAllData() {
        skillManager.saveAllData();
        getLogger().info("All data saved successfully!");
    }

    public static SwordArtOnline getInstance() {
        return instance;
    }

    public SpawnManager getSpawnManager() { return spawnManager; }
    public FloorManager getFloorManager() { return floorManager; }
    public MessagesManager getMessagesManager() { return messagesManager; }
    public TaskManager getTaskManager() { return taskManager; }
    public TeleportListener getTeleportListener() { return teleportListener; }
    public RegionManager getRegionManager() { return regionManager; }
    public RegionListener getRegionListener() { return regionListener; }
    public SkillManager getSkillManager() { return skillManager; }
    public StatusBarManager getStatusBarManager() { return statusBarManager; }
}