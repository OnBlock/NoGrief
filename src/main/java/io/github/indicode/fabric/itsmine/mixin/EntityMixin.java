package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public World world;

    @Shadow public abstract Vec3d getPos();

    @Shadow public abstract void tick();

    @Shadow public boolean removed;
    private Claim pclaim = null;
    @Inject(method = "setPos", at = @At("HEAD"))
    public void doPrePosActions(double x, double y, double z, CallbackInfo ci) {
        if (!world.isClient && (Object)this instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (player.getBlockPos() == null) return;
            pclaim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.dimension.getType());
        }
    }
    @Inject(method = "setPos", at = @At("RETURN"))
    public void doPostPosActions(double x, double y, double z, CallbackInfo ci) {
        if (!world.isClient && (Object)this instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (player.getBlockPos() == null) return;
            Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.dimension.getType());
            if (pclaim != claim && player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)player;
                if (serverPlayerEntity.networkHandler != null) {
                    String message = null;
                    if (pclaim != null && claim == null)
                        message = getFormattedEventMessage(player, pclaim, false);
                    else if (claim != null)
                        message = getFormattedEventMessage(player, claim, true);

                    if (message != null)
                        serverPlayerEntity.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, new LiteralText(ChatColor.translate(message)), -1, Config.event_msg_stay_ticks, -1));

                    if (claim != null && claim.settings.getSetting(Claim.ClaimSettings.Setting.ENTER_SOUND)) {
                        serverPlayerEntity.networkHandler.sendPacket(new PlaySoundIdS2CPacket(Registry.SOUND_EVENT.getId(SoundEvents.BLOCK_CONDUIT_ACTIVATE), SoundCategory.MASTER, this.getPos(), 2, 1.2F));
                    }
                }
            }
        }
    }

    private String getFormattedEventMessage(PlayerEntity player, Claim claim, boolean enter) {
        if (player == null || claim == null)
            return "";

        String str = enter ? claim.enterMessage : claim.leaveMessage;
        return ChatColor.translate(str == null ? (enter ? Config.msg_enter_default : Config.msg_leave_default) : str).replace("%claim%", claim.name)
                .replace("%player%", player.getName().asString());
    }

    private static int tick = 0;

    @Inject(method = "tick", at = @At("RETURN"))
    public void doTickActions(CallbackInfo ci) {
        tick++;
        if (tick >= 5 && !world.isClient && ((Entity) (Object) this instanceof PlayerEntity)) {
            tick = 0;

            PlayerEntity player = (PlayerEntity) (Object) this;
            if (player.getBlockPos() == null) {
                return;
            }

            boolean old = player.abilities.allowFlying;
            Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.dimension.getType());

            if (player instanceof ServerPlayerEntity) {
                if (
                        player.abilities.allowFlying &&
                                shouldChange(player) &&
                                (
                                        (
                                                claim == null || !claim.settings.getSetting(Claim.ClaimSettings.Setting.FLIGHT_ALLOWED) ||
                                                !claim.hasPermission(player.getGameProfile().getId(), Claim.Permission.FLIGHT)
                                        )
                                                && Functions.isClaimFlying(player.getGameProfile().getId())
                                )
                ) {
                    player.abilities.allowFlying = false;
                    player.abilities.flying = false;
                    Functions.setClaimFlying(player.getGameProfile().getId(), false);

                    BlockPos pos = Functions.getPosOnGround(player.getBlockPos(), player.getEntityWorld());
                    player.teleport(pos.getX(), pos.getY(), pos.getZ());
                } else if (
                        !player.abilities.allowFlying &&
                                Claim.flyers.contains(player.getUuid()) &&
                                shouldChange(player) &&
                                claim != null &&
                                claim.settings.getSetting(Claim.ClaimSettings.Setting.FLIGHT_ALLOWED)
                                && claim.hasPermission(player.getGameProfile().getId(), Claim.Permission.FLIGHT)
                                && Functions.canClaimFly((ServerPlayerEntity) player)
                ) {
                    player.abilities.allowFlying = true;
                    Functions.setClaimFlying(player.getGameProfile().getId(), true);
                }

                if (player.abilities.allowFlying != old) {
                    player.sendAbilitiesUpdate();
                }
            }

        }
    }

    private boolean shouldChange(PlayerEntity player) {
        return !player.isSpectator() && !player.isCreative();
    }
}
