package io.github.indicode.fabric.itsmine.mixin;

import com.mojang.authlib.GameProfile;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
        Claim claim = ClaimManager.INSTANCE.getClaimAt(blockPos_1);
        if (claim != null) {
            if (!player.getGameProfile().getId().equals(claim.owner) && !claim.getPermissionsAt(player.getGameProfile().getId(), blockPos_1).modifyBlocks) {
                ci.setReturnValue(false);
            }
        }
    }
}
