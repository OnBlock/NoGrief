package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Indigo Amann
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Shadow public abstract void attack(Entity entity_1);

    @Redirect(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Z"))
    private boolean dontYouDareTouchMe(Entity entity, PlayerEntity playerEntity_1, Hand hand_1) {
        if (entity.world.isClient()) return entity.interact(playerEntity_1, hand_1);
        Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getBlockPos(), entity.world.getDimension().getType());
        if (claim != null) {
            if (!claim.hasPermissionAt(playerEntity_1.getGameProfile().getId(), Claim.ClaimPermissions.Permission.ENTITY_INTERACT, entity.getBlockPos())) {
                playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You are in a claim that does not allow you to interact with entities").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                return false;
            }
        }
        return entity.interact(playerEntity_1, hand_1);
    }
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    public void hittingIsRude(Entity entity, CallbackInfo ci) {
        if (entity.world.isClient()) return;
        PlayerEntity playerEntity_1 = (PlayerEntity)(Object)this;
        Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getBlockPos(), entity.world.getDimension().getType());
        if (claim != null) {
            if (!claim.hasPermissionAt(playerEntity_1.getGameProfile().getId(), Claim.ClaimPermissions.Permission.ENTITY_DAMAGE, entity.getBlockPos())) {
                playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You are in a claim that does not allow you to hurt entities").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                ci.cancel();
            }
        }
    }
}
