package com;

import com.commands.*;
import com.google.common.math.Stats;
import com.guis.CraftingGUI;
import com.guis.SmithingGUI;
import com.guis.SmithingMenuGUI;
import com.listeners.*;
import com.listeners.guis.*;
import com.listeners.skills.*;
import com.listeners.npcs.SmithingEntityListener;
import com.managers.*;
import com.skills.*;
import com.tabcompleters.*;
import com.tasks.SmithingTask;
import com.utils.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class SwordArtOnline extends JavaPlugin {

    private static SwordArtOnline instance;

    // === Managers ===
    private SpawnManager spawnManager;
    private FloorManager floorManager;
    private MessagesManager messagesManager;
    private TaskManager taskManager;
    private CursorManager cursorManager;
    private RegionManager regionManager;
    private SkillManager skillManager;
    private SoundManager soundManager;
    private SmithingTask smithingTask;

    private StatsManager statsManager;
    private StatusBarManager statusBarManager;
    private LoreManager loreManager;
    private ItemManager itemManager;
    private RecipeManager recipeManager;

    // === Listeners ===
    private TeleportListener teleportListener;
    private RegionListener regionListener;
    private SmithingListener smithingListener;
    private CraftingListener craftingListener;
    private SmithingEntityListener smithingEntityListener;
    private SmithingMenuGUIListener smithingMenuGUIListener;
    private StatusBarListener statusBarListener;
    private AcrobaticsSkillListener acrobaticsSkillListener;
    private SneakingSkillListener sneakingSkillListener;
    private PickingSkillListener pickingSkillListener;
    private CarpentrySkillListener carpentrySkillListener;

    // === Abilities ===
    private DoubleCleaveAbility doubleCleaveAbility;

    @Override
    public void onEnable() {
        instance = this;

        // Load GUI and NPC configs
        GUI.loadConfigs(this);
        NPC.loadNPCConfig(this);

        // Register systems
        registerManagers();
        registerTasks();
        registerListeners();
        registerCommands();

        // Load data
        loadAllData();

        // Start systems
        if (statusBarManager != null) {
            statusBarManager.start();
        }

        getLogger().info("SwordArtOnline enabled with StatusBar system and GUIs!");
    }

    @Override
    public void onDisable() {
        // Save data
        saveAllData();

        // Stop systems
        if (statusBarManager != null) {
            statusBarManager.stop();
        }

        getLogger().info("SwordArtOnline disabled.");
    }

    // === Registration ===
    private void registerTasks() {
        smithingTask = new SmithingTask(this);

    }
    private void registerManagers() {
        messagesManager = new MessagesManager(this);
        messagesManager.loadMessages("spawn");
        messagesManager.loadMessages("region");
        SwordSkillManager swordSkillManager = SwordSkillManager.getInstance();
        swordSkillManager.registerSkill("leapintothefuture", new LeapIntoTheFutureSkill());
        new SkillActivationListener();
        getCommand("swordskill").setExecutor(new SwordSkillCommand());
        getCommand("swordskill").setTabCompleter(new SwordSkillCommand());
        statsManager = new StatsManager(this);
        loreManager = new LoreManager(this, statsManager, null);
        itemManager = new ItemManager(this, null, statsManager);
        recipeManager = new RecipeManager(this, itemManager);
        floorManager = new FloorManager(this);
        spawnManager = new SpawnManager(this);

        taskManager = new TaskManager(this);
        taskManager.loadTasks();

        cursorManager = new CursorManager(this);
        regionManager = new RegionManager(this);
        skillManager = new SkillManager(this.getDataFolder());
        soundManager = new SoundManager(this);
        statusBarManager = new StatusBarManager(this);
        loreManager.setItemManager(itemManager);
        itemManager.setLoreManager(loreManager);
    }

    private void registerListeners() {
        teleportListener = new TeleportListener(this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        getServer().getPluginManager().registerEvents(new DamageReductionListener(), this);

        craftingListener = new CraftingListener(this, itemManager, recipeManager, soundManager);
        getServer().getPluginManager().registerEvents(craftingListener, this);

        doubleCleaveAbility = new DoubleCleaveAbility(this);
        getServer().getPluginManager().registerEvents(doubleCleaveAbility, this);

        smithingListener = new SmithingListener(this, itemManager, recipeManager, soundManager, loreManager);
        getServer().getPluginManager().registerEvents(smithingListener, this);

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

        smithingMenuGUIListener = new SmithingMenuGUIListener();
        getServer().getPluginManager().registerEvents(smithingMenuGUIListener, this);

        smithingEntityListener = new SmithingEntityListener();
        getServer().getPluginManager().registerEvents(smithingEntityListener, this);
    }

    private void registerCommands() {
        SpawnCommand spawnCommand = new SpawnCommand(this, teleportListener);
        getCommand("spawn").setExecutor(spawnCommand);
        getCommand("spawn").setTabCompleter(new SpawnTabCompleter(this));

        getCommand("item").setExecutor(new ItemCommand(itemManager, recipeManager));
        getCommand("item").setTabCompleter(new ItemTabCompleter(itemManager, recipeManager));

        RegionCommand regionCommand = new RegionCommand(regionManager, regionListener);
        getCommand("region").setExecutor(regionCommand);
        getCommand("region").setTabCompleter(new RegionTabCompleter(this));

        SkillCommand skillCommand = new SkillCommand(skillManager);
        getCommand("skill").setExecutor(skillCommand);
    }

    // === Data Handling ===

    private void loadAllData() {
        skillManager.loadAllData();
        getLogger().info("All data loaded successfully!");
    }

    private void saveAllData() {
        skillManager.saveAllData();
        getLogger().info("All data saved successfully!");
    }

    // === Getters ===

    public static SwordArtOnline getInstance() { return instance; }
    public ItemManager getItemManager() {
        return itemManager;
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
