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
import java.util.Map;
import java.util.HashMap;
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

        // Save default configs if they don't exist
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
        // Load default values from resources
        try {
            InputStream defConfigStream = plugin.getResource("config.yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
                config.setDefaults(defConfig);
            }

            InputStream defItemsStream = plugin.getResource("items.yml");
            if (defItemsStream != null) {
                YamlConfiguration defItemsConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defItemsStream));
                itemsConfig.setDefaults(defItemsConfig);
            }

            InputStream defMessagesStream = plugin.getResource("messages.yml");
            if (defMessagesStream != null) {
                YamlConfiguration defMessagesConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defMessagesStream));
                messagesConfig.setDefaults(defMessagesConfig);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not load default configurations", e);
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

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    // Utility methods
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

    public String getMessage(String key) {
        String prefix = messagesConfig.getString("messages.prefix", "&8[&6CustomItems&8]&r ");
        String message = messagesConfig.getString("messages." + key, "&cMessage not found: " + key);

        // Replace {prefix} first, then colorize everything
        message = message.replace("{prefix}", prefix);
        return ChatUtils.colorize(message);
    }

    public boolean isRegularItemCapsEnabled() {
        return config.getBoolean("regular-items.combat-caps.enabled", true);
    }

    public Map<Material, Integer> getRegularItemCaps() {
        Map<Material, Integer> caps = new HashMap<>();

        ConfigurationSection itemsSection = config.getConfigurationSection("regular-items.combat-caps.items");
        if (itemsSection != null) {
            for (String materialName : itemsSection.getKeys(false)) {
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    int cap = itemsSection.getInt(materialName, -1);
                    if (cap > 0) {
                        caps.put(material, cap);
                    }
                }
            }
        }

        return caps;
    }

    public String getMessage(String key, String... replacements) {
        String message = ChatUtils.colorize(messagesConfig.getString("messages." + key, "&cMessage not found: " + key));

        // Replace {prefix} first
        String prefix = ChatUtils.colorize(messagesConfig.getString("messages.prefix", "&8[&6CustomItems&8]&r "));
        message = message.replace("{prefix}", prefix);

        // Replace other placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }
}