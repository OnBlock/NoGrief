package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.ClaimUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.LecternScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternScreenHandler.class)
public abstract class LecternScreenHandlerMixin {
    @Inject(method = "onButtonClick", at = @At(value = "HEAD"), cancellable = true)
            private void canTake(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir){
                Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension);
                if(claim != null){
                    if(claim.isChild) claim = ClaimUtil.getParentClaim(claim);
                    if(!claim.hasPermission(player.getUuid(), Claim.Permission.INTERACT_LECTERN)){
                        player.sendMessage(Messages.MSG_CANT_DO);
                        cir.setReturnValue(false);
                    }
                }
            }
}