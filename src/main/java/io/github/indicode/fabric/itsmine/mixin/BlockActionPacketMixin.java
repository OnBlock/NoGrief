package io.github.indicode.fabric.itsmine.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.network.packet.s2c.play.BlockActionS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Indigo Amann
 */
@Mixin(BlockActionS2CPacket.class)
public interface BlockActionPacketMixin {
    @Accessor("pos")
    BlockPos getPos();

    @Accessor("type")
    int getType();

    @Accessor("data")
    int getData();

    @Accessor("block")
    Block getBlock();
}
