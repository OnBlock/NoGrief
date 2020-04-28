package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.ClaimUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternScreenHandler.class)
public abstract class LecternScreenHandlerMixin {
    @Inject(method = "canUse", at = @At(value = "HEAD"), cancellable = true)
    private void canUseLectern(PlayerEntity player, CallbackInfoReturnable<Boolean> cir){

    }

    @Inject(method = "onButtonClick", at = @At(value = "HEAD"), cancellable = true)
    private void canTake(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir){
        player.sendSystemMessage(new LiteralText("onButtonClick"));
        Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension);
        player.sendSystemMessage(new LiteralText(claim.name));
        if(claim != null){
            if(claim.isChild) claim = ClaimUtil.getParentClaim(claim);
            player.sendSystemMessage(new LiteralText(claim.name));
            if(!claim.hasPermission(player.getUuid(), Claim.Permission.INTERACT_LECTERN)){
                player.sendSystemMessage(new LiteralText("You dont have permission to do that").formatted(Formatting.RED));
                cir.setReturnValue(true);
            } else {
                player.sendSystemMessage(new LiteralText("You have permission to do that").formatted(Formatting.GREEN));
                cir.setReturnValue(false);
            }
        }
    }
}