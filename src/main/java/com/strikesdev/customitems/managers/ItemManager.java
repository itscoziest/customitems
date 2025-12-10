package com.strikesdev.customitems.managers;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Material;
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

        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                if (itemSection != null) {
                    try {
                        customItems.put(itemId, CustomItem.fromConfig(itemId, itemSection));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load: " + itemId);
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded " + customItems.size() + " custom items");
    }

    public void checkAndEnforceCaps(Player player) {
        if (player == null) return;
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        Map<String, Integer> customCounts = new HashMap<>();
        Map<Material, Integer> vanillaCounts = new HashMap<>();

        for (ItemStack is : contents) {
            if (is == null || is.getType() == Material.AIR) continue;
            CustomItem ci = getCustomItem(is);
            if (ci != null) {
                customCounts.put(ci.getId(), customCounts.getOrDefault(ci.getId(), 0) + is.getAmount());
            } else {
                vanillaCounts.put(is.getType(), vanillaCounts.getOrDefault(is.getType(), 0) + is.getAmount());
            }
        }

        // Custom Items
        for (Map.Entry<String, Integer> entry : customCounts.entrySet()) {
            String itemId = entry.getKey();
            int currentCount = entry.getValue();
            CustomItem ci = customItems.get(itemId);
            if (ci == null) continue;

            int limit = plugin.getRegionManager().getApplicableCap(player, ci);
            if (limit == -1) continue;

            if (currentCount > limit) {
                removeItemByType(player, itemId, null, currentCount - limit, ci.getName());
            }
        }

        // Vanilla Items (Reads from config.yml)
        if (plugin.getConfigManager().isRegularItemCapsEnabled()) {
            Map<Material, Integer> regularCaps = plugin.getConfigManager().getRegularItemCaps();
            for (Map.Entry<Material, Integer> entry : vanillaCounts.entrySet()) {
                Material mat = entry.getKey();
                int currentCount = entry.getValue();

                if (regularCaps.containsKey(mat)) {
                    int limit = regularCaps.get(mat);
                    if (limit != -1 && currentCount > limit) {
                        // Pass the material name (formatted nicely) as the item name
                        String itemName = mat.toString().toLowerCase().replace("_", " ");
                        removeItemByType(player, null, mat, currentCount - limit, itemName);
                    }
                }
            }
        }
    }

    private void removeItemByType(Player player, String customId, Material material, int amountToRemove, String itemName) {
        PlayerInventory inv = player.getInventory();
        int remaining = amountToRemove;

        // 1. Remove from Main Inventory First
        for (int i = 0; i < 36; i++) {
            if (remaining <= 0) break;
            ItemStack slotItem = inv.getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR) continue;

            if (isMatch(slotItem, customId, material)) {
                int amount = slotItem.getAmount();
                if (amount <= remaining) {
                    inv.setItem(i, null);
                    remaining -= amount;
                } else {
                    slotItem.setAmount(amount - remaining);
                    remaining = 0;
                }
            }
        }

        // 2. Remove from Offhand Last
        if (remaining > 0) {
            ItemStack offhand = inv.getItemInOffHand();
            if (offhand != null && isMatch(offhand, customId, material)) {
                int amount = offhand.getAmount();
                if (amount <= remaining) {
                    inv.setItemInOffHand(null);
                } else {
                    offhand.setAmount(amount - remaining);
                }
            }
        }

        // Send message (Uses 'cap-exceeded' from messages.yml)
        player.sendMessage(plugin.getConfigManager().getMessage("cap-exceeded", "{item}", itemName));
    }

    private boolean isMatch(ItemStack item, String customId, Material material) {
        if (customId != null) {
            CustomItem ci = getCustomItem(item);
            return ci != null && ci.getId().equals(customId);
        } else if (material != null) {
            return item.getType() == material && !isCustomItem(item);
        }
        return false;
    }

    public CustomItem getCustomItem(String id) { return customItems.get(id); }

    public CustomItem getCustomItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return null;
        String itemId = itemStack.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "custom_item"),
                org.bukkit.persistence.PersistentDataType.STRING
        );
        return itemId != null ? customItems.get(itemId) : null;
    }

    public boolean isCustomItem(ItemStack itemStack) { return getCustomItem(itemStack) != null; }
    public ItemStack createCustomItem(String id) { return createCustomItem(id, 1); }
    public ItemStack createCustomItem(String id, int amount) {
        CustomItem item = customItems.get(id);
        return item != null ? item.createItemStack(amount) : null;
    }

    // Required Getters for Commands/Combat
    public Set<String> getCustomItemIds() { return new HashSet<>(customItems.keySet()); }
    public Collection<CustomItem> getAllCustomItems() { return new ArrayList<>(customItems.values()); }
    public List<CustomItem> getItemsWithCombatCap() {
        List<CustomItem> list = new ArrayList<>();
        for (CustomItem i : customItems.values()) if (i.getCombatCap() > 0) list.add(i);
        return list;
    }
}