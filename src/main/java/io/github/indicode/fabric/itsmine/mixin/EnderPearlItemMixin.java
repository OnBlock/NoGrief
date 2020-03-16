package io.github.indicode.fabric.itsmine.mixin;

import blue.endless.jankson.annotation.Nullable;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stat;
import net.minecraft.util.Hand;
import net.minecraft.world.ModifiableWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderPearlItem.class)
public class EnderPearlItemMixin {
    @Nullable
    private Claim cachedClaim = null;

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ModifiableWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean canSpawn(ModifiableWorld modifiableWorld, Entity entity, World world, PlayerEntity user, Hand hand) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getSenseCenterPos(), entity.dimension);

        if (claim != null && !claim.hasPermission(user.getUuid(), Claim.Permission.USE_ENDER_PEARL)) {
            cachedClaim = claim;
            return false;
        }

        return world.spawnEntity(entity);
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;decrement(I)V"))
    private void dontRemoveItFromMyInv(ItemStack itemStack, int amount, Entity entity, World world, PlayerEntity user, Hand hand) {
        if (cachedClaim != null && cachedClaim.hasPermission(user.getUuid(), Claim.Permission.USE_ENDER_PEARL)) {
            return;
        }

        itemStack.decrement(amount);
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;incrementStat(Lnet/minecraft/stat/Stat;)V"))
    private void dontIncreaseStat(PlayerEntity playerEntity, Stat<?> stat, Entity entity, World world, PlayerEntity user, Hand hand) {
        if (cachedClaim != null && cachedClaim.hasPermission(user.getUuid(), Claim.Permission.USE_ENDER_PEARL)) {
            return;
        }

        user.incrementStat(stat);
    }
}
