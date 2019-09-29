package io.github.indicode.fabric.itsmine;

import io.github.indicode.fabric.itsmine.mixin.BlockActionPacketMixin;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.packet.BlockActionS2CPacket;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * @author Indigo Amann
 */
public class MixinFunc {
    public static void doPistonUpdate(ServerWorld world, Packet packet) {
        if (packet instanceof BlockActionS2CPacket) {
            BlockActionPacketMixin accessor = (BlockActionPacketMixin) packet;
            System.out.println(accessor.getBlock());
            if ((accessor.getBlock() == Blocks.PISTON || accessor.getBlock() == Blocks.PISTON_HEAD || accessor.getBlock() == Blocks.STICKY_PISTON)) {
                Direction direction = Direction.byId(accessor.getData());
                BlockPos pos = accessor.getPos().offset(direction, 2);
                world.method_14178().markForUpdate(pos);
                System.out.println("UPDATING: " + pos);
            }
        }
    }
}
