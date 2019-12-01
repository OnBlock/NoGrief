package io.github.indicode.fabric.itsmine;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonPrimitive;
import io.github.indicode.fabric.permissions.Thimble;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonArray;
import io.github.indicode.fabric.tinyconfig.ModConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Indigo Amann
 */
public class Config {
    public static int baseClaimBlocks3d = 15625;
    public static int baseClaimBlocks2d = 2500;
    public static boolean claims2d = true;
    private static ModConfig modConfig = new ModConfig("itsmine");
    static void sync(boolean overwrite) {
        modConfig.configure(overwrite, config -> {
            claims2d = config.getBool("2D claims", claims2d, "Claims extending from y 0 to y 256");
            baseClaimBlocks2d = config.getInt("2D base claim blocks", baseClaimBlocks2d, "Area Filled: " + ItsMine.blocksToAreaString2d(baseClaimBlocks2d));
            baseClaimBlocks3d = config.getInt("3D base claim blocks", baseClaimBlocks3d, "Area Filled: " + ItsMine.blocksToAreaString3d(baseClaimBlocks3d));
        });
    }

}
