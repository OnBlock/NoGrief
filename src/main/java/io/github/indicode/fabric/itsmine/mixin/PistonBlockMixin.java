package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Indigo Amann
 */
@Mixin(PistonBlock.class)
public class PistonBlockMixin {
    private PistonHandler handler;
    @Inject(method = "isMovable", at = @At("HEAD"), cancellable = true)
    private static void youCantMoveMe(BlockState blockState_1, World world, BlockPos newPos, Direction direction, boolean boolean_1, Direction direction_2, CallbackInfoReturnable<Boolean> ci) {
        boolean pushing = direction == direction_2;
        BlockPos oldPos = newPos.offset(direction_2.getOpposite());
        Claim oldClaim = ClaimManager.INSTANCE.getClaimAt(oldPos, world.getDimension().getType());
        Claim newClaim = ClaimManager.INSTANCE.getClaimAt(newPos, world.getDimension().getType());
        if (oldClaim != newClaim) {
            if (oldClaim == null) {
                if (!newClaim.settings.getSetting(Claim.ClaimSettings.Setting.PISTON_FROM_OUTSIDE)) ci.setReturnValue(false);
            }
            else if (newClaim == null) {
                if (!oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.PISTON_FROM_INSIDE)) ci.setReturnValue(false);
            } else {
                if (!oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.PISTON_FROM_INSIDE) ||
                        !newClaim.settings.getSetting(Claim.ClaimSettings.Setting.PISTON_FROM_OUTSIDE)) ci.setReturnValue(false);
            }
        }
    }
    /*@Redirect(method = "tryMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addBlockAction(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V"))
    public void fixDesync(World world, BlockPos blockPos_1, Block block_1, int int_1, int int_2) {
        world.addBlockAction(blockPos_1, block_1, int_1, int_2); // Convert to @Inject if it works
        System.out.println("doh");
        if (world instanceof ServerWorld) {
            ((ServerWorld) world).method_14178().markForUpdate(blockPos_1.offset(Direction.byId(int_2)));
        }
    }*/
    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/piston/PistonHandler;getMovedBlocks()Ljava/util/List;"))
    public List<BlockPos> saveDatHandler(PistonHandler pistonHandler) {
        handler = pistonHandler;
        return pistonHandler.getMovedBlocks();
    }/*
    @Inject(method = "move", at = @At("RETURN"))
    public void iAmUnmoved(World world_1, BlockPos blockPos_1, Direction direction_1, boolean boolean_1, CallbackInfoReturnable<Boolean> cir) {
        if (!(world_1 instanceof ServerWorld)) return;
        List<BlockPos> updates = new ArrayList<>();
        updates.addAll(handler.getMovedBlocks());
        updates.addAll(handler.getBrokenBlocks());
        updates.forEach(((ServerWorld)world_1).method_14178()::markForUpdate);
    }*/
}
