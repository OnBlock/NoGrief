package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Functions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public World world;


    @Inject(method = "tick", at = @At("RETURN"))
    public void doTickActions(CallbackInfo ci) {
        if (!world.isClient && (Object)this instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) (Object)this;
            boolean old = player.abilities.allowFlying;
            Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.dimension.getType());
            if (claim != null && player instanceof ServerPlayerEntity) {
                player.abilities.allowFlying = player.abilities.creativeMode || (claim.settings.getSetting(Claim.ClaimSettings.Setting.FLIGHT_ALLOWED) && claim.hasPermission(player.getGameProfile().getId(), Claim.Permission.FLY) && Functions.canClaimFly((ServerPlayerEntity) player));

                if (old != player.abilities.allowFlying) {
                    if (!player.abilities.allowFlying) player.abilities.flying = false;
                    player.sendAbilitiesUpdate();
                }
            }
        }
    }
}
