package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.claim.ClaimFlags;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(SpawnRestriction.class)
public class SpawnRestrictionMixin {
	@Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
	private static void canSpawnInClaim(EntityType<?> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> cir) {
		if (ClaimManager.INSTANCE == null) {
			cir.setReturnValue(false);
			return;
		}
		Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, world.getDimension());
		if (claim != null && !claim.flags.getFlag(ClaimFlags.Flag.MOB_SPAWNING)) {
			cir.setReturnValue(false);
		}
	}
}
