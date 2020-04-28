package io.github.indicode.fabric.itsmine.util;

import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

public class WorldUtil {

    public static ServerWorld getServerWorld(DimensionType dimensionType){
        return ClaimManager.INSTANCE.server.getWorld(dimensionType);
    }

}
