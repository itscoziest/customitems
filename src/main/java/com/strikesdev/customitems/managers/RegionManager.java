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
import java.util.Map;
import java.util.Set;

public class RegionManager {

    private final CustomItems plugin;
    private final boolean worldGuardAvailable;

    public RegionManager(CustomItems plugin) {
        this.plugin = plugin;
        this.worldGuardAvailable = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
    }

    public int getApplicableCap(Player player, CustomItem item) {
        return getApplicableLimit(player, item.getCombatCap(), item.getRegionCaps());
    }

    public int getApplicableLimit(Player player, int defaultLimit, Map<String, Integer> regionCaps) {
        int limit = defaultLimit;

        if (!worldGuardAvailable) return limit;
        if (regionCaps == null || regionCaps.isEmpty()) return limit;

        Set<String> playerRegions = getRegionsAtLocation(player.getLocation());

        for (String regionId : playerRegions) {
            if (regionCaps.containsKey(regionId)) {
                return regionCaps.get(regionId);
            }
            for (String configRegion : regionCaps.keySet()) {
                if (configRegion.equalsIgnoreCase(regionId)) {
                    return regionCaps.get(configRegion);
                }
            }
        }
        return limit;
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

    // --- FIX: ADDED MISSING METHOD ---
    public boolean isInRegion(Location location, String regionName) {
        if (!worldGuardAvailable) return false;
        Set<String> regions = getRegionsAtLocation(location);
        return regions.contains(regionName);
    }
    // ---------------------------------

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
            plugin.getLogger().warning("Error getting regions: " + e.getMessage());
            return Set.of();
        }
    }

    public String getRegionDenialMessage(CustomItem item) {
        List<String> allowedRegions = item.getAllowedRegions();
        if (allowedRegions.isEmpty()) return plugin.getConfigManager().getMessage("region.not-allowed");
        String regionList = String.join(", ", allowedRegions);
        return plugin.getConfigManager().getMessage("region.not-allowed-specific", "{regions}", regionList);
    }

    public boolean isWorldGuardAvailable() { return worldGuardAvailable; }
}