package com.strikesdev.customitems.models;

import com.strikesdev.customitems.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CustomItem {

    private final String id;
    private String name;
    private Material material;
    private List<String> lore;
    private int duration;
    private List<PotionEffect> effects;
    private double damage;
    private double range;
    private double radius;
    private int cooldown;
    private int combatCap;
    private Map<String, Integer> regionCaps; // New: Region specific caps
    private List<String> allowedRegions;
    private String permission;
    private Map<String, Object> customData;
    private Map<Enchantment, Integer> enchantments;
    private boolean unbreakable;
    private int customModelData;

    public CustomItem(String id) {
        this.id = id;
        this.name = "&f" + id;
        this.material = Material.PAPER;
        this.lore = new ArrayList<>();
        this.duration = 0;
        this.effects = new ArrayList<>();
        this.damage = 0.0;
        this.range = 0.0;
        this.radius = 0.0;
        this.cooldown = 0;
        this.combatCap = -1; // -1 means no limit
        this.regionCaps = new HashMap<>();
        this.allowedRegions = new ArrayList<>();
        this.permission = "";
        this.customData = new HashMap<>();
        this.enchantments = new HashMap<>();
        this.unbreakable = false;
        this.customModelData = 0;
    }

    public static CustomItem fromConfig(String id, ConfigurationSection section) {
        CustomItem item = new CustomItem(id);

        // Basic properties
        item.name = section.getString("name", "&f" + id);
        item.material = Material.matchMaterial(section.getString("material", "PAPER"));
        if (item.material == null) {
            item.material = Material.PAPER;
        }

        item.lore = section.getStringList("lore");
        item.duration = section.getInt("duration", 0);
        item.damage = section.getDouble("damage", 0.0);
        item.range = section.getDouble("range", 0.0);
        item.radius = section.getDouble("radius", 0.0);
        item.cooldown = section.getInt("cooldown", 0);
        item.combatCap = section.getInt("combat-cap", -1);

        // Load Region Specific Caps
        if (section.isConfigurationSection("region-caps")) {
            ConfigurationSection regionCapSection = section.getConfigurationSection("region-caps");
            for (String regionName : regionCapSection.getKeys(false)) {
                // Explicitly box the int to Integer to fix your error
                item.regionCaps.put(regionName, Integer.valueOf(regionCapSection.getInt(regionName)));
            }
        }

        item.allowedRegions = section.getStringList("allowed-regions");
        item.permission = section.getString("permission", "");
        item.unbreakable = section.getBoolean("unbreakable", false);
        item.customModelData = section.getInt("custom-model-data", 0);

        // Load effects
        ConfigurationSection effectsSection = section.getConfigurationSection("effects");
        if (effectsSection != null) {
            for (String effectKey : effectsSection.getKeys(false)) {
                ConfigurationSection effectSection = effectsSection.getConfigurationSection(effectKey);
                if (effectSection != null) {
                    PotionEffectType type = PotionEffectType.getByName(effectKey.toUpperCase());
                    if (type != null) {
                        int amplitude = effectSection.getInt("amplifier", 0);
                        int duration = effectSection.getInt("duration", item.duration);
                        boolean ambient = effectSection.getBoolean("ambient", false);
                        boolean particles = effectSection.getBoolean("particles", true);
                        boolean icon = effectSection.getBoolean("icon", true);

                        PotionEffect effect = new PotionEffect(type, duration * 20, amplitude, ambient, particles, icon);
                        item.effects.add(effect);
                    }
                }
            }
        }

        // Load enchantments
        ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String enchantKey : enchantSection.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByName(enchantKey.toUpperCase());
                if (enchantment != null) {
                    int level = enchantSection.getInt(enchantKey, 1);
                    // Explicitly box the int to Integer to fix your error
                    item.enchantments.put(enchantment, Integer.valueOf(level));
                }
            }
        }

        // Load custom data
        ConfigurationSection customSection = section.getConfigurationSection("custom-data");
        if (customSection != null) {
            for (String key : customSection.getKeys(false)) {
                item.customData.put(key, customSection.get(key));
            }
        }

        return item;
    }

    public ItemStack createItemStack() {
        return createItemStack(1);
    }

    public ItemStack createItemStack(int amount) {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            // Set name
            meta.setDisplayName(ChatUtils.colorize(name));

            // Set lore
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatUtils.colorize(line));
                }
                meta.setLore(coloredLore);
            }

            // Set unbreakable
            if (unbreakable) {
                meta.setUnbreakable(true);
            }

            // Set custom model data
            if (customModelData > 0) {
                // Explicitly box the int to Integer to fix your error
                meta.setCustomModelData(Integer.valueOf(customModelData));
            }

            // Add custom NBT tag to identify the item
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(com.strikesdev.customitems.CustomItems.getInstance(), "custom_item"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    id
            );

            itemStack.setItemMeta(meta);
        }

        // Add enchantments
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            itemStack.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        return itemStack;
    }

    public boolean isCustomItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return false;
        }

        String itemId = itemStack.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(com.strikesdev.customitems.CustomItems.getInstance(), "custom_item"),
                org.bukkit.persistence.PersistentDataType.STRING
        );

        return id.equals(itemId);
    }

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public List<PotionEffect> getEffects() { return effects; }
    public void setEffects(List<PotionEffect> effects) { this.effects = effects; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public double getRange() { return range; }
    public void setRange(double range) { this.range = range; }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public int getCombatCap() { return combatCap; }
    public void setCombatCap(int combatCap) { this.combatCap = combatCap; }

    // Region Cap Getters
    public Map<String, Integer> getRegionCaps() { return regionCaps; }
    public void setRegionCaps(Map<String, Integer> regionCaps) { this.regionCaps = regionCaps; }

    public List<String> getAllowedRegions() { return allowedRegions; }
    public void setAllowedRegions(List<String> allowedRegions) { this.allowedRegions = allowedRegions; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public Map<String, Object> getCustomData() { return customData; }
    public void setCustomData(Map<String, Object> customData) { this.customData = customData; }
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    public void setEnchantments(Map<Enchantment, Integer> enchantments) { this.enchantments = enchantments; }
    public boolean isUnbreakable() { return unbreakable; }
    public void setUnbreakable(boolean unbreakable) { this.unbreakable = unbreakable; }
    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }

    public Object getCustomDataValue(String key) {
        return customData.get(key);
    }

    public String getCustomDataString(String key, String defaultValue) {
        Object value = customData.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    public int getCustomDataInt(String key, int defaultValue) {
        Object value = customData.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    public double getCustomDataDouble(String key, double defaultValue) {
        Object value = customData.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    public boolean getCustomDataBoolean(String key, boolean defaultValue) {
        Object value = customData.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}