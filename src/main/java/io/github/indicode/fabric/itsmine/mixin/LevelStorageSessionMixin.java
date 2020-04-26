package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.class_5219;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.*;
import java.nio.file.Path;

@Mixin(LevelStorage.Session.class)
public class LevelStorageSessionMixin {
    @Shadow
    @Final
    private Path directory;

    @Inject(method = "readLevelProperties", at = @At("HEAD"))
    public void readWorldProperties(CallbackInfoReturnable<class_5219> callbackInfo) {
        ClaimManager.INSTANCE = new ClaimManager();
        File claims = new File(directory.toFile() + "/claims.dat");
        File claims_old = new File(directory.toFile() + "/claims.dat_old");
        if (!claims.exists()) {
            if (claims_old.exists()) {}
            else return;
        }
        try {
            if (!claims.exists() && claims_old.exists()) throw new FileNotFoundException();
            ClaimManager.INSTANCE.fromNBT(NbtIo.readCompressed(new FileInputStream(claims)));
        } catch (IOException e) {
            System.err.println("Could not load claims.dat:");
            e.printStackTrace();
            if (claims_old.exists()) {
                System.out.println("Attempting to load backup claims...");
                try {
                    ClaimManager.INSTANCE.fromNBT(NbtIo.readCompressed(new FileInputStream(claims_old)));
                } catch (IOException e2) {
                    throw new RuntimeException("Could not load claims.dat_old - Crashing server to save data. Remove or fix claims.dat or claims.dat_old to continue");
                }
            }
        }
    }

    @Inject(method = "method_27426", at = @At("HEAD"))
    public void saveWorld(class_5219 levelProperties, CompoundTag compoundTag, CallbackInfo info) {
        if (ClaimManager.INSTANCE != null) {
            File claimDataFile = new File(directory.toFile(), "claims.dat");
            if (claimDataFile.exists()) {
                File old = new File(directory.toFile(), "claims.dat_old");
                if (old.exists()) old.delete();
                claimDataFile.renameTo(old);
                claimDataFile.delete();
            }
            try {
                claimDataFile.createNewFile();
                CompoundTag tag = ClaimManager.INSTANCE.toNBT();
                NbtIo.writeCompressed(tag, new FileOutputStream(claimDataFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
