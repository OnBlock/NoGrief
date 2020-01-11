package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Functions;
import net.minecraft.client.network.packet.TitleS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public World world;

    private Claim pclaim = null;
    @Inject(method = "setPos", at = @At("HEAD"))
    public void doPrePosActions(double x, double y, double z, CallbackInfo ci) {
        if (!world.isClient && (Object)this instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            pclaim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.dimension.getType());
        }
    }
    @Inject(method = "setPos", at = @At("RETURN"))
    public void doPostPosActions(double x, double y, double z, CallbackInfo ci) {
        if (!world.isClient && (Object)this instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.dimension.getType());
            if (pclaim != claim && player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)player;
                if (serverPlayerEntity.networkHandler != null) {
                    serverPlayerEntity.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, new LiteralText("Now " + (claim == null ? "leaving" : "entering") + " claim ").formatted(Formatting.YELLOW).append(new LiteralText(claim == null ? pclaim.name : claim.name).formatted(Formatting.GOLD))));
                }
            }
        }
    }
    @Inject(method = "tick", at = @At("RETURN"))
    public void doTickActions(CallbackInfo ci) {
        if (!world.isClient && (Object)this instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) (Object)this;
            boolean old = player.abilities.allowFlying;
            Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.dimension.getType());
            if (player instanceof ServerPlayerEntity) {

                player.abilities.allowFlying = player.abilities.creativeMode || player.isSpectator() || (claim != null && claim.settings.getSetting(Claim.ClaimSettings.Setting.FLIGHT_ALLOWED) && claim.hasPermission(player.getGameProfile().getId(), Claim.Permission.FLY) && Functions.canClaimFly((ServerPlayerEntity) player));
                if (old && !player.abilities.allowFlying && !Functions.isClaimFlying(player.getGameProfile().getId())) {
                    player.abilities.allowFlying = true;
                }
                Functions.setClaimFlying(player.getGameProfile().getId(), !(player.abilities.creativeMode || player.isSpectator()) && player.abilities.allowFlying);
                if (old != player.abilities.allowFlying) {
                    if (!player.abilities.allowFlying) player.abilities.flying = false;
                    player.sendAbilitiesUpdate();
                }
            }

        }
    }
}
