package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Indigo Amann
 */
@Mixin(ProjectileEntity.class)
public class ProjectileEntityMixin {
    @Redirect(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"))
    public boolean imInvincible(Entity entity, DamageSource damageSource_1, float float_1) {
        if (entity.world.isClient()) return entity.damage(damageSource_1, float_1);
        ProjectileEntity projectile = (ProjectileEntity)(Object)this;
        if (projectile.getOwner() instanceof PlayerEntity) {
            PlayerEntity playerEntity_1 = (PlayerEntity)projectile.getOwner();
            Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getBlockPos(), entity.world.getDimension().getType());
            if (claim != null && entity != playerEntity_1) {
                if (!claim.getPermissionsAt(playerEntity_1.getGameProfile().getId(), entity.getBlockPos()).hasPermission(Claim.ClaimPermissions.Permission.ENTITY_DAMAGE)) {
                    playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You are in a claim that does not allow you to hurt entities").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                    return false;
                }
            }
        }
        return entity.damage(damageSource_1, float_1);
    }
}
