package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.claim.ClaimFlags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Indigo Amann
 */
@Mixin(Explosion.class)
public class ExplosionMixin {
    @Shadow @Final private World world;

    @Redirect(method = "collectBlocksAndDamageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState theyCallMeBedrock(World world, BlockPos pos) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, world.getDimension());
        if (claim != null && !world.isAir(pos) && !world.getBlockState(pos).getBlock().equals(Blocks.TNT)) {
            if (!claim.flags.getFlag(ClaimFlags.Flag.EXPLOSION_DESTRUCTION)) {
                return Blocks.BEDROCK.getDefaultState();
            }
        }
        return world.getBlockState(pos);
    }

    @Redirect(method = "collectBlocksAndDamageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isImmuneToExplosion()Z"))
    private boolean claimDeniesExplosion(Entity entity) {
        BlockPos blockPos_1 = entity.getBlockPos();
        Claim claim = ClaimManager.INSTANCE.getClaimAt(blockPos_1, entity.world.getDimension());
        if (claim != null) {
            if (!claim.flags.getFlag(ClaimFlags.Flag.EXPLOSION_DAMAGE)) {
                return true;
            }
        }

        return entity.isImmuneToExplosion();
    }
}
