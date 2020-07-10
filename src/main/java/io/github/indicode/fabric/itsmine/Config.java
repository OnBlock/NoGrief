/*
package io.github.indicode.fabric.itsmine;

import io.github.indicode.fabric.tinyconfig.ModConfig;

*/
/**
 * @author Indigo Amann
 *//*

public class Config {
    public static int baseClaimBlocks3d = 15625;
    public static int baseClaimBlocks2d = 2500;
    public static boolean claims2d = true;
    public static int rent_maxseconds = 7776000;
    public static int claim_max_entities_passive = 75;
    public static String permissionManager = "luckperms";
    public static String prefix = "&8[&aClaims&8]&r ";
    public static String msg_no_perm = "&c&lHey!&r&c Sorry but you don't have permission to do that";
    public static String msg_interact_entity = "&c&lHey!&r&c Sorry but you can't interact with Entities here!";
    public static String msg_interact_block = "&c&lHey!&r&c Sorry but you can't interact with Blocks here!";
    public static String msg_open_container = "&c&lHey!&r&c Sorry but you can't open containers here!";
    public static String msg_break_block = "&c&lHey!&r&c Sorry but you can't Break Blocks here!";
    public static String msg_place_block = "&c&lHey!&r&c Sorry but you can't Place Blocks here!";
    public static String msg_attack_entity = "&c&lHey!&r&c Sorry but you can't Attack Entities here!";
    public static String msg_enter_default = "&eNow entering claim &6%claim%";
    public static String msg_leave_default = "&eNow leaving claim &6%claim%";
    public static String msg_cant_enter = "&c&lHey!&r&c Sorry but you don't have permission to enter this claim!";
    public static String msg_cant_use = "&c&lHey!&r&c Sorry but you can't to use that here!";
    public static String msg_long_name = "&cThe name of the Claim must be less than 30 characters!";
    public static String msg_cant_do ="&c&lHey!&r&c Sorry but you can't do that!";

    public static int event_msg_stay_ticks = -1;
    private static ModConfig modConfig = new ModConfig("itsmine");
    static void sync() {
        modConfig.configure(false, config -> {
            claims2d = config.getBool("2D claims", claims2d, "Claims extending from y 0 to y 256");
            baseClaimBlocks2d = config.getInt("2D base claim blocks", baseClaimBlocks2d, "Area Filled: " + ItsMine.blocksToAreaString2d(baseClaimBlocks2d));
            baseClaimBlocks3d = config.getInt("3D base claim blocks", baseClaimBlocks3d, "Area Filled: " + ItsMine.blocksToAreaString3d(baseClaimBlocks3d));
            rent_maxseconds = config.getInt("rent.maxdays", rent_maxseconds, "Maximum rent time in seconds (eg. 90d = 7776000)");
            claim_max_entities_passive = config.getInt("claim.max.passive", claim_max_entities_passive, "Maximum allowed passive entities per claim");
            permissionManager = config.getString("permissionManager", permissionManager, "Values: thimble, luckperms, vanilla");
            prefix = config.getString("msg.prefix", prefix);
            msg_interact_entity = config.getString("msg.interact.entity", msg_interact_entity);
            msg_interact_block = config.getString("msg.interact.block", msg_interact_block);
            msg_open_container = config.getString("msg.open.container", msg_open_container);
            msg_break_block = config.getString("msg.break.block", msg_break_block);
            msg_place_block = config.getString("msg.place.block", msg_place_block);
            msg_attack_entity = config.getString("msg.attack.entity", msg_attack_entity);
            msg_enter_default = config.getString("msg.enter_claim", msg_enter_default, "Variables: %claim% %player%");
            msg_leave_default = config.getString("msg.leave_claim", msg_leave_default, "Variables: %claim% %player%");
            msg_cant_enter = config.getString("msg.cant_enter", msg_cant_enter);
            msg_cant_use = config.getString("msg.cant_use", msg_cant_use);
            msg_long_name = config.getString("msg.long_name", msg_long_name);
            msg_cant_do = config.getString("msg.cant_do", msg_cant_do);
            event_msg_stay_ticks = config.getInt("event.msg.stay_ticks", event_msg_stay_ticks, "Sets how many ticks an event message will stay on action bar, Default: -1");
        });
    }

}
*/
