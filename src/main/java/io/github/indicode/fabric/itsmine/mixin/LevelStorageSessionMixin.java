package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.SaveProperties;
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
    public void readWorldProperties(CallbackInfoReturnable<SaveProperties> cir) {
        System.out.println("LevelStorage.Session");
    }

    //    @Inject(method = "readLevelProperties", at = @At("HEAD"))
//    public void readWorldProperties() {
//        ClaimManager.INSTANCE = new ClaimManager();
//        File claims = new File(directory.toFile() + "/claims.dat");
//        File claims_old = new File(directory.toFile() + "/claims.dat_old");
//        if (!claims.exists()) {
//            if (claims_old.exists()) {}
//            else return;
//        }
//        try {
//            if (!claims.exists() && claims_old.exists()) throw new FileNotFoundException();
//            ClaimManager.INSTANCE.fromNBT(NbtIo.readCompressed(new FileInputStream(claims)));
//        } catch (IOException e) {
//            System.out.println("Could not load " + claims.getName() + ":");
//            e.printStackTrace();
//            if (claims_old.exists()) {
//                System.out.println("Attempting to load backup claims...");
//                try {
//                    ClaimManager.INSTANCE.fromNBT(NbtIo.readCompressed(new FileInputStream(claims_old)));
//                } catch (IOException e2) {
//                    throw new RuntimeException("Could not load claims.dat_old - Crashing server to save data. Remove or fix claims.dat or claims.dat_old to continue");
//                }
//            }
//        }
//    }

    @Inject(method = "method_27426", at = @At("HEAD"))
    public void saveWorld(SaveProperties levelProperties, CompoundTag compoundTag, CallbackInfo info) {
        if (ClaimManager.INSTANCE != null) {
            File claimDataFile = new File(directory.toFile(), "claims.dat");
            if (claimDataFile.exists()) {
                File old = new File(directory.toFile(), "claims.dat_old");
                System.out.println("Saving NBT File: " + claimDataFile.getName() + " " + claimDataFile.length()+ "b " + claimDataFile.getAbsolutePath());
                if(claimDataFile.length() > 45){
                    System.out.println("Creating backup of NBT File: " + claimDataFile.getName());
                    if (old.exists()) old.delete();
                    claimDataFile.renameTo(old);
                    claimDataFile.delete();
                } else {
                    System.out.println("Aborting backup!" + claimDataFile.getName() + " may be broken, keeping " + old.getName());
                }
            }
            try {
                claimDataFile.createNewFile();
                CompoundTag tag = ClaimManager.INSTANCE.toNBT();
                NbtIo.writeCompressed(tag, new FileOutputStream(claimDataFile));
            } catch (IOException e) {
                System.out.println("Could not save " + claimDataFile.getName() + ":");
                e.printStackTrace();
            }
        }
    }

}
