package io.github.indicode.fabric.itsmine;

import io.github.indicode.fabric.permissions.Thimble;
import io.github.indicode.fabric.permissions.command.NoSavePermission;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.command.ServerCommandSource;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class ItsMine implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.sync(false);
        Thimble.permissionWriters.add(pair -> {
            try {
                for (int it : Config.claimCountPerms) {
                    pair.getLeft().getPermission("itsmine.claimamount." + it, NoSavePermission.class);
                }
                pair.getLeft().getPermission("itsmine.claimamount.infinite", NoSavePermission.class);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                System.err.println("Claim permissions could not be loaded:");
                e.printStackTrace();
            }
        });
    }
    public static boolean hasPermissionForNewClaim(ServerCommandSource player, int number) {
        if (Thimble.hasPermissionOrOp(player, "itsmine.claimamount.infinite", 3)) return true;
        for (int i: Config.claimCountPerms) {
            if (i <= number) continue;
            if (Thimble.hasPermissionOrOp(player, "itsmine.claimamount." + i, 2)) {
                return true;
            }
        }
        return false;
    }
}
