package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.claim.ClaimFlags;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CancellationException;

/**
 * @author Indigo Amann
 */
@Mixin(FlowableFluid.class)
public class FluidMixin {
    @Inject(method = "flow", at = @At("HEAD"), cancellable = true)
    private void dontFlow(WorldAccess world, BlockPos newPos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci) {
        BlockPos oldPos = newPos.offset(direction.getOpposite());
        Claim oldClaim = ClaimManager.INSTANCE.getClaimAt(oldPos, world.getDimension());
        Claim newClaim = ClaimManager.INSTANCE.getClaimAt(newPos, world.getDimension());
        if (oldClaim != newClaim) {
            if (oldClaim == null) {
                if (!newClaim.flags.getFlag(ClaimFlags.Flag.FLUID_CROSSES_BORDERS)) ci.cancel();
            }
            else if (newClaim == null) {
                if (!oldClaim.flags.getFlag(ClaimFlags.Flag.FLUID_CROSSES_BORDERS)) ci.cancel();
            } else {
                if (!oldClaim.flags.getFlag(ClaimFlags.Flag.FLUID_CROSSES_BORDERS) ||
                        !newClaim.flags.getFlag(ClaimFlags.Flag.FLUID_CROSSES_BORDERS)) ci.cancel();
            }
        }
    }
}
