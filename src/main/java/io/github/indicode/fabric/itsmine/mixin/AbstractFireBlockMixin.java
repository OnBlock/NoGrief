package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.claim.ClaimSettings;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractFireBlock.class)
public abstract class AbstractFireBlockMixin {

    @Redirect(method = "onEntityCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isFireImmune()Z"))
    private boolean neverGonnaBurnYouUp(Entity entity) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getBlockPos(), entity.dimension);
        if (claim != null && !claim.settings.getSetting(ClaimSettings.Setting.FIRE_DAMAGE)) {
            return true;
        }

        return entity.isFireImmune();
    }

}
