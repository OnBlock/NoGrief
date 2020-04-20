package io.github.indicode.fabric.itsmine.mixin.projectile;

import blue.endless.jankson.annotation.Nullable;
import io.github.indicode.fabric.itsmine.Functions;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Indigo Amann
 */
@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @Shadow protected abstract void onEntityHit(EntityHitResult entityHitResult);

    @Shadow @Nullable
    public abstract Entity getOwner();

    @Redirect(method = "onCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileEntity;onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V"))
    private void imInvincible(ProjectileEntity projectileEntity, EntityHitResult entityHitResult) {
        if (Functions.canDamageWithProjectile((ProjectileEntity) (Object) this, projectileEntity)) {
            this.onEntityHit(entityHitResult);
        } else {
            if (this.getOwner() instanceof PlayerEntity) {
                this.getOwner().sendSystemMessage(Messages.MSG_DAMAGE_ENTITY);
            }
        }
    }
//    public boolean imInvincible(Entity entity, DamageSource damageSource_1, float float_1) {
//        if (entity.world.isClient()) return entity.damage(damageSource_1, float_1);
//        ProjectileEntity projectile = (ProjectileEntity)(Object)this;
//
//        if (((ProjectileEntity)(Object)this).getServer().getPlayerManager().getPlayer(((OwnedProjectile)projectile).getOwner()) != null) {
//            PlayerEntity playerEntity_1 = ((ProjectileEntity)(Object)this).getServer().getPlayerManager().getPlayer(((OwnedProjectile)projectile).getOwner());
//            Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getSenseCenterPos(), entity.world.getDimension().getType());
//            if (claim != null && entity != playerEntity_1) {
//                if (!claim.hasPermission(playerEntity_1.getGameProfile().getId(), Claim.Permission.DAMAGE_ENTITY)) {
//                    playerEntity_1.sendSystemMessage(Messages.MSG_DAMAGE_ENTITY);
//                    projectile.kill(); // You do not want an arrow bouncing between two armor stands
//                    return false;
//                }
//            }
//        }
//        return entity.damage(damageSource_1, float_1);
//    }
}
