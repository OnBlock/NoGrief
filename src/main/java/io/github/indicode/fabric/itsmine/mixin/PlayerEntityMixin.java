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
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Indigo Amann
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Redirect(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Z"))
    private boolean dontYouDareTouchMe(Entity entity, PlayerEntity playerEntity_1, Hand hand_1) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getBlockPos(), entity.world.getDimension().getType());
        if (claim != null) {
            if (!claim.getPermissionsAt(playerEntity_1.getGameProfile().getId(), entity.getBlockPos()).hasPermission(Claim.ClaimPermissions.Permission.ENTITY_INTERACT)) {
                playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You are in a claim that does not allow you to interact with entities.").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                return false;
            }
        }
        return entity.interact(playerEntity_1, hand_1);
    }
}
