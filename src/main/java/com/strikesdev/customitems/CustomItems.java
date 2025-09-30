package com.strikesdev.customitems;

import com.strikesdev.customitems.commands.CustomItemsCommand;
import com.strikesdev.customitems.listeners.*;
import com.strikesdev.customitems.managers.*;
import com.strikesdev.customitems.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class CustomItems extends JavaPlugin {

    private static CustomItems instance;
    private ConfigManager configManager;
    private ItemManager itemManager;
    private CooldownManager cooldownManager;
    private CombatManager combatManager;
    private RegionManager regionManager;
    private ActionBarManager actionBarManager;
    private EffectManager effectManager;

    // Cleanup tasks
    private BukkitTask cleanupTask;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers

        this.configManager = new ConfigManager(this);
        this.itemManager = new ItemManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.combatManager = new CombatManager(this);
        this.actionBarManager = new ActionBarManager(this);
        this.effectManager = new EffectManager(this);




        // Initialize region manager (check for WorldGuard)
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.regionManager = new RegionManager(this);
            getLogger().info("WorldGuard detected! Region restrictions enabled.");
        } else {
            getLogger().warning("WorldGuard not found! Region restrictions disabled.");
        }

        // Load configurations
        configManager.loadConfigs();
        itemManager.loadCustomItems();

        // Register listeners
        registerListeners();

        // Register commands
        getCommand("customitems").setExecutor(new CustomItemsCommand(this));

        // Start cleanup task (runs every 30 seconds)
        startCleanupTask();

        getLogger().info("CustomItems plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel cleanup task
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        // Cleanup managers
        if (cooldownManager != null) {
            cooldownManager.cleanup();
        }
        if (combatManager != null) {
            combatManager.cleanup();
        }
        if (actionBarManager != null) {
            actionBarManager.cleanup();
        }
        if (effectManager != null) {
            effectManager.cleanup();
        }

        getLogger().info("CustomItems plugin has been disabled!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            // Clean up expired data
            cooldownManager.cleanupExpiredCooldowns();
            combatManager.cleanupExpiredCombat();
            effectManager.cleanupExpiredEffects();
        }, 600L, 600L); // Run every 30 seconds
    }

    public void reload() {
        // Reload configurations
        configManager.loadConfigs();
        itemManager.loadCustomItems();

        // Clear caches
        cooldownManager.clearAll();
        combatManager.clearAll();

        getLogger().info("CustomItems plugin reloaded!");
    }

    // Getters
    public static CustomItems getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }
}