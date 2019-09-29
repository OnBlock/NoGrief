package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Indigo Amann
 */
@Mixin(PistonBlock.class)
public class PistonBlockMixin {
    @Inject(method = "isMovable", at = @At("HEAD"), cancellable = true)
    private static void youCantMoveMe(BlockState blockState_1, World world, BlockPos newPos, Direction direction, boolean boolean_1, Direction direction_2, CallbackInfoReturnable<Boolean> ci) {
        BlockPos oldPos = newPos.offset(direction.getOpposite());
        Claim oldClaim = ClaimManager.INSTANCE.getClaimAt(oldPos, world.getDimension().getType());
        Claim newClaim = ClaimManager.INSTANCE.getClaimAt(newPos, world.getDimension().getType());
        if (oldClaim != newClaim) {
            if (oldClaim == null) {
                if (!(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.PISTON_FROM_OUTSIDE)) ci.setReturnValue(false);
            }
            else if (newClaim == null) {
                if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.PISTON_FROM_INSIDE)) ci.setReturnValue(false);
            } else {
                if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.PISTON_FROM_INSIDE) ||
                        !(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.PISTON_FROM_OUTSIDE)) ci.setReturnValue(false);
            }
        }
    }
}
