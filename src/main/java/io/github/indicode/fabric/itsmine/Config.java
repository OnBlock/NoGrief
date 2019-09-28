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
    public static int baseClaimBlocks = 15625;
    private static ModConfig modConfig = new ModConfig("itsmine");
    static void sync(boolean overwrite) {
        modConfig.configure(overwrite, config -> {
            baseClaimBlocks = config.getInt("base_claim_blocks", baseClaimBlocks, "Area Filled: " + ItsMine.blocksToAreaString(baseClaimBlocks));
        });
    }

}
