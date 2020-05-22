package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.util.ClaimUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "createWorlds", at = @At(value = "RETURN"))
    private void loadClaims(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci){
        ClaimUtil.readWorldProperties();
    }

}
