package io.github.indicode.fabric.itsmine.util;

import net.minecraft.block.*;

public class BlockUtil {

    public static boolean isContainer(Block block) {
        return block instanceof AbstractChestBlock<?> || isChest(block) || isEnderchest(block) || isShulkerBox(block);
    }

    public static boolean isChest(Block block) {
        return block == Blocks.CHEST || block == Blocks.BARREL;
    }

    public static boolean isEnderchest(Block block) {
        return block == Blocks.ENDER_CHEST;
    }

    public static boolean isShulkerBox(Block block) {
        return block == Blocks.SHULKER_BOX || block == Blocks.WHITE_SHULKER_BOX || block == Blocks.ORANGE_SHULKER_BOX || block == Blocks.MAGENTA_SHULKER_BOX || block == Blocks.LIGHT_BLUE_SHULKER_BOX || block == Blocks.YELLOW_SHULKER_BOX || block == Blocks.LIME_SHULKER_BOX || block == Blocks.PINK_SHULKER_BOX || block == Blocks.GRAY_SHULKER_BOX || block == Blocks.LIGHT_GRAY_SHULKER_BOX || block == Blocks.CYAN_SHULKER_BOX || block == Blocks.PURPLE_SHULKER_BOX || block == Blocks.BLUE_SHULKER_BOX || block == Blocks.BROWN_SHULKER_BOX || block == Blocks.GREEN_SHULKER_BOX || block == Blocks.RED_SHULKER_BOX || block == Blocks.BLACK_SHULKER_BOX;
    }

    public static boolean isButton(Block block) {
        return block instanceof AbstractButtonBlock;
    }

    public static boolean isLever(Block block) {
        return block instanceof LeverBlock;
    }

    public static boolean isDoor(Block block) {
        return block instanceof DoorBlock || block instanceof TrapdoorBlock;
    }

    public static boolean isLectern(Block block) {
        return block instanceof LecternBlock;
    }
}
