package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ClaimShower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Indigo Amann
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements ClaimShower {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    private Claim shownClaim = null;

    @Redirect(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Z"))
    private boolean dontYouDareTouchMe(Entity entity, PlayerEntity playerEntity_1, Hand hand_1) {
        if (entity.world.isClient()) return entity.interact(playerEntity_1, hand_1);
        Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getBlockPos(), entity.world.getDimension().getType());
        if (claim != null) {
            if (!claim.hasPermission(playerEntity_1.getGameProfile().getId(), Claim.ClaimPermissions.Permission.ENTITY_INTERACT)) {
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
            if (!claim.hasPermission(playerEntity_1.getGameProfile().getId(), Claim.ClaimPermissions.Permission.ENTITY_DAMAGE)) {
                playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You are in a claim that does not allow you to hurt entities").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                ci.cancel();
            }
        }
    }
    /*@Inject(method = "canPlaceOn", at = @At("HEAD"))
    public void iDontWantYerStuff(BlockPos blockPos_1, Direction direction_1, ItemStack itemStack_1, CallbackInfoReturnable<Boolean> cir) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(blockPos_1, world.getDimension().getType());
        if (claim != null) {
            cir.setReturnValue(false);
        }
    }*/ // Replace with specific undos on certain methods(buttons, containers, etc)
    @Override
    public void setShownClaim(Claim claim) {
        shownClaim = claim;
    }
    @Override
    public Claim getShownClaim() {
        return shownClaim;
    }
}
