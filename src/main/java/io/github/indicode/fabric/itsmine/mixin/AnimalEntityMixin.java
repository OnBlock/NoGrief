package io.github.indicode.fabric.itsmine.mixin;

import blue.endless.jankson.annotation.Nullable;
import com.mojang.authlib.GameProfile;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.util.ClaimUtil;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin {

    @Shadow private UUID lovingPlayer;

    @Shadow @Nullable
    public abstract ServerPlayerEntity getLovingPlayer();

    @Shadow public abstract void setLoveTicks(int loveTicks);

    @Shadow public abstract void resetLoveTicks();

    @Inject(method = "canBreedWith", at = @At(value = "HEAD"), cancellable = true)
    private void canBreed(AnimalEntity other, CallbackInfoReturnable<Boolean> cir){
        Claim claim = ClaimManager.INSTANCE.getClaimAt(other.getBlockPos(), other.dimension);
        if(claim != null){
            if(claim.isChild){
                claim = ClaimUtil.getParentClaim(claim);
            }
            if(claim.getEntities(other.getEntityWorld().getServer().getWorld(other.getEntityWorld().getDimension().getType())) > 50){
                ServerPlayerEntity player = this.getLovingPlayer();
                if(player != null){
                    player.sendMessage(new LiteralText("You reached the entity limit in your claim!").formatted(Formatting.RED));
                }
                this.resetLoveTicks();
                cir.setReturnValue(false);
            }
        }
    }
}
