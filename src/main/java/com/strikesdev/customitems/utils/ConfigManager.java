package com.strikesdev.customitems.utils;

import com.strikesdev.customitems.CustomItems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final CustomItems plugin;
    private FileConfiguration config;
    private FileConfiguration itemsConfig;
    private FileConfiguration messagesConfig;

    private File configFile;
    private File itemsFile;
    private File messagesFile;

    public ConfigManager(CustomItems plugin) {
        this.plugin = plugin;
        saveDefaultConfigs();
    }

    public void loadConfigs() {
        // Load main config
        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // Load items config
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

        // Load messages config
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults
        loadDefaults();
    }

    private void saveDefaultConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        saveDefaultConfig("config.yml");
        saveDefaultConfig("items.yml");
        saveDefaultConfig("messages.yml");
    }

    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private void loadDefaults() {
        try {
            loadDefault("config.yml", config);
            loadDefault("items.yml", itemsConfig);
            loadDefault("messages.yml", messagesConfig);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not load default configurations", e);
        }
    }

    private void loadDefault(String fileName, FileConfiguration targetConfig) {
        InputStream defStream = plugin.getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            targetConfig.setDefaults(defConfig);
        }
    }

    public void saveConfigs() {
        try {
            config.save(configFile);
            itemsConfig.save(itemsFile);
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save configurations", e);
        }
    }

    // --- GETTERS ---

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getItemsConfig() { return itemsConfig; }
    public FileConfiguration getMessagesConfig() { return messagesConfig; }

    public boolean isActionBarCooldownEnabled() {
        return config.getBoolean("cooldown.display.action-bar.enabled", true);
    }

    public boolean isItemNameCooldownEnabled() {
        return config.getBoolean("cooldown.display.item-name.enabled", false);
    }

    public boolean isCombatCapEnabled() {
        return config.getBoolean("combat.cap.enabled", true);
    }

    public boolean isRegionWhitelistEnabled() {
        return config.getBoolean("region.whitelist.enabled", true);
    }

    public boolean isDebugMode() {
        return config.getBoolean("debug", false);
    }

    public int getCombatDuration() {
        return config.getInt("combat.duration", 15);
    }

    // --- FIX: READ FROM ITEMS CONFIG INSTEAD OF MAIN CONFIG ---
    public boolean isRegularItemCapsEnabled() {
        // Changed 'config' to 'itemsConfig'
        return itemsConfig.getBoolean("regular-items.combat-caps.enabled", false);
    }

    public Map<Material, Integer> getRegularItemCaps() {
        Map<Material, Integer> caps = new HashMap<>();

        // Changed 'config' to 'itemsConfig'
        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("regular-items.combat-caps.items");

        if (itemsSection != null) {
            for (String materialName : itemsSection.getKeys(false)) {
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    int cap = itemsSection.getInt(materialName, -1);
                    if (cap > 0) {
                        caps.put(material, Integer.valueOf(cap));
                    }
                } else {
                    plugin.getLogger().warning("Invalid material in regular-items cap: " + materialName);
                }
            }
        }
        return caps;
    }
    // ----------------------------------------------------------

    public String getMessage(String key, String... replacements) {
        String message = messagesConfig.getString("messages." + key, "&cMessage not found: " + key);
        String prefix = messagesConfig.getString("messages.prefix", "&8[&6CustomItems&8]&r ");
        message = message.replace("{prefix}", prefix);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String value = replacements[i + 1];
                if (value == null) value = "";
                message = message.replace(replacements[i], value);
            }
        }
        return ChatUtils.colorize(message);
    }
}