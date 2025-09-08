package com.strikesdev.customitems.managers;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemManager {

    private final CustomItems plugin;
    private final Map<String, CustomItem> customItems;

    public ItemManager(CustomItems plugin) {
        this.plugin = plugin;
        this.customItems = new ConcurrentHashMap<>();
    }

    public void loadCustomItems() {
        customItems.clear();

        FileConfiguration config = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");

        if (itemsSection == null) {
            plugin.getLogger().warning("No items section found in items.yml!");
            return;
        }

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection != null) {
                try {
                    CustomItem item = CustomItem.fromConfig(itemId, itemSection);
                    customItems.put(itemId, item);

                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Loaded custom item: " + itemId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load custom item: " + itemId);
                    e.printStackTrace();
                }
            }
        }

        plugin.getLogger().info("Loaded " + customItems.size() + " custom items");
    }

    public CustomItem getCustomItem(String id) {
        return customItems.get(id);
    }

    public CustomItem getCustomItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return null;
        }

        String itemId = itemStack.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "custom_item"),
                org.bukkit.persistence.PersistentDataType.STRING
        );

        return itemId != null ? customItems.get(itemId) : null;
    }

    public boolean isCustomItem(ItemStack itemStack) {
        return getCustomItem(itemStack) != null;
    }

    public ItemStack createCustomItem(String id) {
        CustomItem item = customItems.get(id);
        return item != null ? item.createItemStack() : null;
    }

    public ItemStack createCustomItem(String id, int amount) {
        CustomItem item = customItems.get(id);
        return item != null ? item.createItemStack(amount) : null;
    }

    public Set<String> getCustomItemIds() {
        return new HashSet<>(customItems.keySet());
    }

    public Collection<CustomItem> getAllCustomItems() {
        return new ArrayList<>(customItems.values());
    }

    public boolean hasCustomItem(String id) {
        return customItems.containsKey(id);
    }

    public void addCustomItem(CustomItem item) {
        customItems.put(item.getId(), item);
    }

    public void removeCustomItem(String id) {
        customItems.remove(id);
    }

    public void clearCustomItems() {
        customItems.clear();
    }

    public int getCustomItemCount() {
        return customItems.size();
    }

    // Utility methods for specific item types
    public List<CustomItem> getItemsByType(String type) {
        List<CustomItem> items = new ArrayList<>();
        for (CustomItem item : customItems.values()) {
            String itemType = item.getCustomDataString("type", "");
            if (itemType.equalsIgnoreCase(type)) {
                items.add(item);
            }
        }
        return items;
    }

    public List<CustomItem> getItemsWithCombatCap() {
        List<CustomItem> items = new ArrayList<>();
        for (CustomItem item : customItems.values()) {
            if (item.getCombatCap() > 0) {
                items.add(item);
            }
        }
        return items;
    }

    public List<CustomItem> getItemsWithRegionRestrictions() {
        List<CustomItem> items = new ArrayList<>();
        for (CustomItem item : customItems.values()) {
            if (!item.getAllowedRegions().isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }
}