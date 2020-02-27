package io.github.indicode.fabric.itsmine.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author Indigo Amann
 */
@Mixin(BlockUpdateS2CPacket.class)
public interface BlockUpdatePacketMixin {
    @Accessor("state")
    void setState(BlockState state);
}
