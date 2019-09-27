package io.github.indicode.fabric.itsmine.mixin;

import com.google.gson.JsonElement;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Indigo Amann
 */
@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(method = "loadWorld", at = @At("RETURN"))
    private void loadClaims(String string_1, String string_2, long long_1, LevelGeneratorType levelGeneratorType_1, JsonElement jsonElement_1, CallbackInfo ci) {
        File claims = new File(((MinecraftServer)(Object)this).getLevelStorage().getSavesDirectory() + "/" + string_1 + "/claims.dat");
        File claims_old = new File(((MinecraftServer)(Object)this).getLevelStorage().getSavesDirectory() + "/" + string_1 + "/claims.dat_old");
        ClaimManager.INSTANCE = new ClaimManager();
        if (!claims.exists()) {
            if (claims_old.exists()) {}
            else return;
        }
        try {
            if (!claims.exists() && claims_old.exists()) throw new FileNotFoundException();
            ListTag tag = (ListTag) NbtIo.readCompressed(new FileInputStream(claims)).getTag("claims");
            ClaimManager.INSTANCE.fromNBT(tag);
        } catch (IOException e) {
            System.err.println("Could not load claims.dat:");
            e.printStackTrace();
            if (claims_old.exists()) {
                System.out.println("Attempting to load backup claims...");
                try {
                    ListTag tag = (ListTag) NbtIo.readCompressed(new FileInputStream(claims_old)).getTag("claims");
                    ClaimManager.INSTANCE.fromNBT(tag);
                } catch (IOException e2) {
                    throw new RuntimeException("Could not load claims.dat_old - Crashing server to save data. Remove or fix claims.dat or claims.dat_old to continue");

                }
            }
        }
    }
}
