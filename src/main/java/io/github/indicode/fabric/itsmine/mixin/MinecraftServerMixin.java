package io.github.indicode.fabric.itsmine.mixin;

import com.google.gson.JsonElement;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Indigo Amann
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "loadWorld", at = @At("RETURN"))
    private void loadClaims(String string_1, String string_2, long long_1, LevelGeneratorType levelGeneratorType_1, JsonElement jsonElement_1, CallbackInfo ci) {
        File claims = new File(((MinecraftServer)(Object)this).getLevelStorage().getSavesDirectory() + "/" + string_1 + "/claims.dat");
        ClaimManager.INSTANCE = new ClaimManager();
        if (!claims.exists()) return;
        try {
            ListTag tag = (ListTag) NbtIo.readCompressed(new FileInputStream(claims)).getTag("data");
            ClaimManager.INSTANCE.fromNBT(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
