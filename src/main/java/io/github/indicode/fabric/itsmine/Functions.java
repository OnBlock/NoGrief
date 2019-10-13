package io.github.indicode.fabric.itsmine;

import io.github.indicode.fabric.itsmine.mixin.BlockActionPacketMixin;
import io.github.indicode.fabric.itsmine.mixin.BucketItemMixin;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.packet.BlockActionS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Indigo Amann
 */
public class Functions {
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
    public static boolean canPlayerActuallyModifyAt(ServerWorld world, PlayerEntity playerEntity_1, BlockPos blockPos_1) {
        return !world.getServer().isSpawnProtected(world, blockPos_1, playerEntity_1) && world.getWorldBorder().contains(blockPos_1);
    };
    public static boolean canModifyAtClaimed(PlayerEntity player, BlockPos blockPos_1) {
        if (player.world.isClient()) return false;
        Claim claim = ClaimManager.INSTANCE.getClaimAt(blockPos_1, player.getEntityWorld().getDimension().getType());
        if (claim != null) {
            if (!claim.hasPermission(player.getGameProfile().getId(), Claim.ClaimPermissions.Permission.SPAWN_PROTECT)) {
                return false;
            }
        }
        return true;
    }
    public static boolean isBucketEmpty(BucketItem item) {
        return ((BucketItemMixin)item).getFluid() != Fluids.EMPTY;
    }
}