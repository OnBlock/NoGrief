package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Indigo Amann
 */
@Mixin(Explosion.class)
public class ExplosionMixin {
    @Redirect(method = "collectBlocksAndDamageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState theyCallMeBedrock(World world, BlockPos pos) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, world.getDimension().getType());
        if (claim != null && !world.isAir(pos) && !world.getBlockState(pos).getBlock().equals(Blocks.TNT)) {
            if (!claim.settings.getSetting(Claim.ClaimSettings.Setting.EXPLOSION_DESTRUCTION)) {
                System.out.println("Well hello there! a explosion is trying to blowup a block, and the setting is disabled in that claim, so imma just shadow the block as a Bedrock");

                return Blocks.BEDROCK.getDefaultState();
            }
        }
        return world.getBlockState(pos);
    }

    @Redirect(method = "collectBlocksAndDamageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isImmuneToExplosion()Z"))
    private boolean claimDeniesExplosion(Entity entity) {
        BlockPos blockPos_1 = entity.getBlockPos();
        Claim claim = ClaimManager.INSTANCE.getClaimAt(blockPos_1, entity.world.getDimension().getType());
        if (claim != null) {
            if (!claim.settings.getSetting(Claim.ClaimSettings.Setting.EXPLOSION_DAMAGE)) {
                System.out.println("Well hello there! a explosion is trying to damage a entity, and the setting is disabled in that claim, so imma quickly make them immune to explosions");

                return true;
            }
        }

        return entity.isImmuneToExplosion();
    }
}
