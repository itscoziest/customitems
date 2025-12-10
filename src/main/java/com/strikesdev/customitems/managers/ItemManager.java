package com.strikesdev.customitems.managers;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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

    /**
     * Scans the player's inventory and removes items that exceed the limit.
     * Enforces logic: Main Inventory First, Offhand Last.
     * Supports Region-Specific Caps.
     */
    public void checkAndEnforceCaps(Player player) {
        if (player == null) return;

        // Track counts per custom item ID
        Map<String, Integer> itemCounts = new HashMap<>();
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents(); // Includes armor and offhand in newer versions, but we treat offhand special

        // 1. COUNT total items first
        for (ItemStack is : contents) {
            CustomItem ci = getCustomItem(is);
            if (ci != null) {
                // FIXED LINE: Explicitly use Integer.valueOf(0)
                int currentCount = itemCounts.getOrDefault(ci.getId(), Integer.valueOf(0));
                itemCounts.put(ci.getId(), Integer.valueOf(currentCount + is.getAmount()));
            }
        }

        // 2. CHECK against caps
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String itemId = entry.getKey();
            int currentCount = entry.getValue(); // Auto-unboxing usually works here, but if not: entry.getValue().intValue()

            CustomItem ci = customItems.get(itemId);
            if (ci == null) continue;

            // Get cap based on Region
            int limit = plugin.getRegionManager().getApplicableCap(player, ci);

            // If limit is -1 (infinite), skip
            if (limit == -1) continue;

            if (currentCount > limit) {
                int toRemove = currentCount - limit;

                // 3. REMOVE items
                // Strict Order: Main Inventory (0-35) -> Offhand

                // Scan Main Storage + Hotbar (Slots 0 to 35)
                for (int i = 0; i < 36; i++) {
                    if (toRemove <= 0) break;

                    ItemStack slotItem = inv.getItem(i);
                    CustomItem slotCustomItem = getCustomItem(slotItem);

                    if (slotCustomItem != null && slotCustomItem.getId().equals(itemId)) {
                        int amount = slotItem.getAmount();

                        if (amount <= toRemove) {
                            // Remove entire stack
                            inv.setItem(i, null);
                            toRemove -= amount;
                        } else {
                            // Reduce stack
                            slotItem.setAmount(amount - toRemove);
                            toRemove = 0;
                        }
                    }
                }

                // Check Offhand LAST (to prevent accidental totem loss)
                if (toRemove > 0) {
                    ItemStack offhand = inv.getItemInOffHand();
                    CustomItem offhandCustomItem = getCustomItem(offhand);

                    if (offhandCustomItem != null && offhandCustomItem.getId().equals(itemId)) {
                        int amount = offhand.getAmount();
                        if (amount <= toRemove) {
                            inv.setItemInOffHand(null);
                            // toRemove -= amount; // technically done
                        } else {
                            offhand.setAmount(amount - toRemove);
                        }
                    }
                }

                // Notify player (Optional)
                player.sendMessage(plugin.getConfigManager().getMessage("cap-exceeded", "{item}", ci.getName()));
            }
        }
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