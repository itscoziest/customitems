package com.strikesdev.customitems.handlers;

import com.strikesdev.customitems.CustomItems;
import com.strikesdev.customitems.handlers.items.*;
import com.strikesdev.customitems.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public class ItemHandler {

    private final CustomItems plugin;
    private final Map<String, ItemAction> itemActions;

    public ItemHandler(CustomItems plugin) {
        this.plugin = plugin;
        this.itemActions = new HashMap<>();
        registerItemActions();
    }

    private void registerItemActions() {
        itemActions.put("instant_tnt", new InstantTNTAction(plugin));
        itemActions.put("force_field", new ForceFieldAction(plugin));
        itemActions.put("exploding_snowman", new ExplodingSnowmanAction(plugin));
        itemActions.put("speed_berries", new SpeedBerriesAction(plugin));
        itemActions.put("strength_cookie", new StrengthCookieAction(plugin));
        itemActions.put("illusion", new IllusionAction(plugin));
        itemActions.put("golem_fists", new GolemFistsAction(plugin));
        itemActions.put("snap_ring", new SnapRingAction(plugin));
        itemActions.put("super_wind_charge", new SuperWindChargeAction(plugin));
        itemActions.put("fireball", new FireballAction(plugin));
        itemActions.put("healing_campfire", new HealingCampfireAction(plugin));
        itemActions.put("dynamite", new DynamiteAction(plugin));
        itemActions.put("leap", new LeapAction(plugin));
        itemActions.put("creeper", new CreeperAction(plugin));
        itemActions.put("vampire_sword", new VampireSwordAction(plugin));
        itemActions.put("egg_bridger", new EggBridgerAction(plugin));
        itemActions.put("ice_fire", new IceFireAction(plugin));
        itemActions.put("slowness_snowball", new SlownessSnowballAction(plugin));
        itemActions.put("swap_ball", new SwapBallAction(plugin));
        itemActions.put("homing_dart", new HomingDartAction(plugin));
        itemActions.put("beekeeper", new BeekeeperAction(plugin));
        itemActions.put("smoke_bomb", new SmokeBombAction(plugin));
        itemActions.put("haste_potion", new HastePotionAction(plugin));
        itemActions.put("explosive_crossbow", new ExplosiveCrossbowAction(plugin));
        itemActions.put("lightning_wand", new LightningWandAction(plugin));
        itemActions.put("sonic_boom", new SonicBoomAction(plugin));
        itemActions.put("dragon_breath", new DragonBreathAction(plugin));
        itemActions.put("dog_army", new DogArmyAction(plugin));
        itemActions.put("lasso", new LassoAction(plugin));
        itemActions.put("taser", new TaserAction(plugin));
        itemActions.put("grave_digger", new GraveDiggerAction(plugin));
        itemActions.put("grappling_hook", new GrapplingHookAction(plugin));
    }

    public boolean handleItemUse(Player player, CustomItem item, PlayerInteractEvent event) {
        String itemType = item.getCustomDataString("type", "");

        ItemAction action = itemActions.get(itemType);
        if (action != null) {
            try {
                return action.execute(player, item, event);
            } catch (Exception e) {
                plugin.getLogger().warning("Error executing item action for " + item.getId() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        return handleDefaultItem(player, item, event);
    }

    private boolean handleDefaultItem(Player player, CustomItem item, PlayerInteractEvent event) {
        if (!item.getEffects().isEmpty()) {
            item.getEffects().forEach(player::addPotionEffect);
        }

        boolean consumeItem = item.getCustomDataBoolean("consume", true);
        if (consumeItem && event.getItem() != null) {
            if (event.getItem().getAmount() > 1) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
            } else {
                player.getInventory().setItem(event.getHand(), null);
            }
        }

        return true;
    }

    public void registerItemAction(String type, ItemAction action) {
        itemActions.put(type, action);
    }

    public void unregisterItemAction(String type) {
        itemActions.remove(type);
    }

    public boolean hasItemAction(String type) {
        return itemActions.containsKey(type);
    }
}