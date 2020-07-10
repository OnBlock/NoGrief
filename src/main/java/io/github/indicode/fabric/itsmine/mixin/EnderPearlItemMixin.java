package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderPearlItem.class)
public class EnderPearlItemMixin {

    @Inject(method = "use", at = @At(value = "HEAD"), cancellable = true)
    private void modifyUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack itemStack = user.getStackInHand(hand);
        Claim claim = ClaimManager.INSTANCE.getClaimAt(user.getBlockPos(), user.world.getDimension());

        if (claim != null && !claim.hasPermission(user.getUuid(), Claim.Permission.USE_ENDER_PEARL)) {
            user.sendSystemMessage(Messages.MSG_CANT_USE, user.getUuid());
            cir.setReturnValue(TypedActionResult.fail(itemStack));
        }
    }

}
