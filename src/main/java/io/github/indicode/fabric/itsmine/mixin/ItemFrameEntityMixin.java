package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin extends AbstractDecorationEntity {

    @Shadow private float itemDropChance;

    protected ItemFrameEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
    }

    //Don't allow a player without perms to change the item inside
    @Redirect(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private boolean canITochieTheItem(ItemStack itemStack, PlayerEntity player, Hand hand) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(this.getDecorationBlockPos(), this.dimension);

        if (claim != null && !claim.hasPermission(player.getUuid(), Claim.Permission.INTERACT_ITEM_FRAME)) {
            return false;
        }

        return itemStack.isEmpty();
    }

    @Redirect(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ItemFrameEntity;setRotation(I)V"))
    private void canIRotate(ItemFrameEntity itemFrameEntity, int value, PlayerEntity player, Hand hand) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(this.getDecorationBlockPos(), this.dimension);

        if (claim != null && claim.hasPermission(player.getUuid(), Claim.Permission.INTERACT_ITEM_FRAME)) {
            itemFrameEntity.setRotation(value);
        }
    }

}
