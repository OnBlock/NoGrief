package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.LecternScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LecternScreenHandler.class)
public abstract class LecternScreenHandlerMixin {
    @Redirect(method = "onButtonClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/LecternScreenHandler;onButtonClick(Lnet/minecraft/entity/player/PlayerEntity;I)Z"))
    private boolean canTake(LecternScreenHandler lecternScreenHandler, PlayerEntity player, int id) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension);
        if (claim != null && !claim.hasPermission(player.getUuid(), Claim.Permission.INTERACT_LECTERN)) {
            player.sendMessage(Messages.MSG_CANT_DO);
            return false;
        }
        return true;

    }
}