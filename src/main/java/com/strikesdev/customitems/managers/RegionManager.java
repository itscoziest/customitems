package com.strikesdev.customitems.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class RegionManager {

    private final CustomItems plugin;
    private final boolean worldGuardAvailable;

    public RegionManager(CustomItems plugin) {
        this.plugin = plugin;
        this.worldGuardAvailable = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
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