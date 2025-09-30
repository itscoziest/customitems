package com.strikesdev.customitems.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Set;

public class RegionManager {

    private final CustomItems plugin;
    private final boolean worldGuardAvailable;

    public RegionManager(CustomItems plugin) {
        this.plugin = plugin;
        this.worldGuardAvailable = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
    }

    /**
     * Checks if a specific location is inside a named WorldGuard region.
     * @param location The Bukkit location to check.
     * @param regionName The name of the WorldGuard region (case-insensitive).
     * @return true if the location is inside the specified region, false otherwise.
     */
    public boolean isInRegion(Location location, String regionName) {
        if (location == null || regionName == null) {
            return false;
        }

        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        // Get the WorldGuard region container
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        com.sk89q.worldguard.protection.managers.RegionManager regions = container.get(BukkitAdapter.adapt(world));

        // Check if the region manager for the world exists
        if (regions == null) {
            return false;
        }

        // Check if a region with the given name exists
        ProtectedRegion region = regions.getRegion(regionName);
        if (region == null) {
            return false;
        }

        // Finally, check if the location's coordinates are inside the region
        return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean canUseItemInRegion(Player player, CustomItem item, Location location) {
        // Debug logging
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("=== REGION CHECK DEBUG ===");
            plugin.getLogger().info("Player: " + player.getName());
            plugin.getLogger().info("Item: " + item.getId());
            plugin.getLogger().info("WorldGuard Available: " + worldGuardAvailable);
        }

        // No region restrictions if WorldGuard not available
        if (!plugin.getConfigManager().isRegionWhitelistEnabled() || !worldGuardAvailable) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Region restrictions disabled - allowing");
            }
            return true;
        }

        // Check bypass permission
        if (player.hasPermission("customitems.bypass.region")) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Player has bypass permission - allowing");
            }
            return true;
        }

        List<String> allowedRegions = item.getAllowedRegions();
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Allowed regions for " + item.getId() + ": " + allowedRegions);
        }

        // No region restrictions for this item
        if (allowedRegions.isEmpty()) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("No region restrictions for this item - allowing");
            }
            return true;
        }

        Set<String> currentRegions = getRegionsAtLocation(location);
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Current regions at location: " + currentRegions);
        }

        // Check if player is in any of the allowed regions
        for (String allowedRegion : allowedRegions) {
            if (currentRegions.contains(allowedRegion)) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Found allowed region '" + allowedRegion + "' - allowing");
                }
                return true;
            }
        }

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Not in any allowed region - BLOCKING");
        }
        return false; // Not in any allowed region
    }

    public Set<String> getRegionsAtLocation(Location location) {
        if (!worldGuardAvailable) {
            return Set.of();
        }

        try {
            com.sk89q.worldguard.protection.managers.RegionManager wgRegionManager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (wgRegionManager == null) {
                return Set.of();
            }

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
        if (allowedRegions.isEmpty()) {
            return plugin.getConfigManager().getMessage("region.not-allowed");
        }

        String regionList = String.join(", ", allowedRegions);
        return plugin.getConfigManager().getMessage("region.not-allowed-specific",
                "{regions}", regionList);
    }

    public boolean isWorldGuardAvailable() {
        return worldGuardAvailable;
    }
}