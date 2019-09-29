package io.github.indicode.fabric.itsmine.mixin;

import com.mojang.authlib.GameProfile;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Indigo Amann
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Inject(method = "canPlayerModifyAt", at = @At("HEAD"), cancellable = true)
    public void canMine(PlayerEntity player, BlockPos blockPos_1, CallbackInfoReturnable ci) {
        if (player.world.isClient()) return;
        Claim claim = ClaimManager.INSTANCE.getClaimAt(blockPos_1, player.getEntityWorld().getDimension().getType());
        if (claim != null) {
            if (!claim.hasPermission(player.getGameProfile().getId(), Claim.ClaimPermissions.Permission.SPAWN_PROTECT)) {
                player.sendMessage(new LiteralText("").append(new LiteralText("You can't use that here. This claim is spawn-protected").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                ci.setReturnValue(false);
            }
        }
    }
}
