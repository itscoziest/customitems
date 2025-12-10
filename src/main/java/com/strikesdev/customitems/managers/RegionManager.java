package com.strikesdev.customitems.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionManager {

    private final CustomItems plugin;
    private final boolean worldGuardAvailable;

    public RegionManager(CustomItems plugin) {
        this.plugin = plugin;
        this.worldGuardAvailable = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
    }

    /**
     * Determines the item cap based on the player's current region.
     * @param player The player holding the items.
     * @param item The custom item to check.
     * @return The cap for this region, or -1 if no cap exists.
     */
    public int getApplicableCap(Player player, CustomItem item) {
        // 1. Start with the global cap
        int limit = item.getCombatCap();

        // Debugging
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Checking cap for " + item.getId() + ". Global Limit: " + limit);
        }

        if (!worldGuardAvailable) {
            return limit;
        }

        Set<String> playerRegions = getRegionsAtLocation(player.getLocation());
        Map<String, Integer> regionCaps = item.getRegionCaps();

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Player is in regions: " + playerRegions);
            plugin.getLogger().info("Item has region caps: " + regionCaps.keySet());
        }

        // 2. Check for Region Specific Caps
        if (!regionCaps.isEmpty()) {
            for (String regionId : playerRegions) {
                // Check exact match first
                if (regionCaps.containsKey(regionId)) {
                    int regionLimit = regionCaps.get(regionId);
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Found MATCHING region cap for '" + regionId + "': " + regionLimit);
                    }
                    // If we found a specific region cap, WE USE IT.
                    // We prioritize the specific region setting over the global setting.
                    return regionLimit;
                }

                // Check case-insensitive match just in case
                for (String configRegion : regionCaps.keySet()) {
                    if (configRegion.equalsIgnoreCase(regionId)) {
                        int regionLimit = regionCaps.get(configRegion);
                        if (plugin.getConfigManager().isDebugMode()) {
                            plugin.getLogger().info("Found (case-insensitive) region cap for '" + regionId + "': " + regionLimit);
                        }
                        return regionLimit;
                    }
                }
            }
        }

        return limit;
    }

    public boolean isInRegion(Location location, String regionName) {
        if (location == null || regionName == null) return false;
        World world = location.getWorld();
        if (world == null) return false;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        com.sk89q.worldguard.protection.managers.RegionManager regions = container.get(BukkitAdapter.adapt(world));

        if (regions == null) return false;
        ProtectedRegion region = regions.getRegion(regionName);
        if (region == null) return false;

        return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean canUseItemInRegion(Player player, CustomItem item, Location location) {
        if (!plugin.getConfigManager().isRegionWhitelistEnabled() || !worldGuardAvailable) return true;
        if (player.hasPermission("customitems.bypass.region")) return true;

        List<String> allowedRegions = item.getAllowedRegions();
        if (allowedRegions.isEmpty()) return true;

        if (allowedRegions.contains("global") || allowedRegions.contains("__global__")) {
            return true;
        }

        Set<String> currentRegions = getRegionsAtLocation(location);
        for (String allowedRegion : allowedRegions) {
            if (currentRegions.contains(allowedRegion)) return true;
        }
        return false;
    }

    public Set<String> getRegionsAtLocation(Location location) {
        if (!worldGuardAvailable) return Set.of();

        try {
            com.sk89q.worldguard.protection.managers.RegionManager wgRegionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (wgRegionManager == null) return Set.of();

            ApplicableRegionSet regions = wgRegionManager.getApplicableRegions(
                    BukkitAdapter.asBlockVector(location)
            );

            return regions.getRegions().stream()
                    .map(ProtectedRegion::getId)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting regions at location: " + e.getMessage());
            return Set.of();
        }
    }

    public boolean isLocationInRegion(Location location, String regionId) {
        return getRegionsAtLocation(location).contains(regionId);
    }

    public String getRegionDenialMessage(CustomItem item) {
        List<String> allowedRegions = item.getAllowedRegions();
        if (allowedRegions.isEmpty()) return plugin.getConfigManager().getMessage("region.not-allowed");
        String regionList = String.join(", ", allowedRegions);
        return plugin.getConfigManager().getMessage("region.not-allowed-specific", "{regions}", regionList);
    }

    public boolean isWorldGuardAvailable() { return worldGuardAvailable; }
}