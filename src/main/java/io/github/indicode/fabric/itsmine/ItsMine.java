package io.github.indicode.fabric.itsmine;

import io.github.indicode.fabric.permissions.Thimble;
import io.github.indicode.fabric.permissions.command.CommandPermission;
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
                pair.getLeft().getPermission("itsmine", CommandPermission.class);
                pair.getLeft().getPermission("itsmine.admin", CommandPermission.class);
                pair.getLeft().getPermission("itsmine.admin.infinite_claim", CommandPermission.class);
                pair.getLeft().getPermission("itsmine.admin.check_others", CommandPermission.class);
                pair.getLeft().getPermission("itsmine.admin.modify_balance", CommandPermission.class);
                pair.getLeft().getPermission("itsmine.admin.destroy", CommandPermission.class);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                System.err.println("Claim permissions could not be loaded:");
                e.printStackTrace();
            }
        });
    }
    public static String blocksToAreaString(int blocks) {
        int base = (int) Math.floor(Math.cbrt(blocks));
        int additionalBlocks = blocks - (int) Math.pow(base, 3);
        int extraRows = (int) Math.floor(Math.cbrt(Math.floor((float)additionalBlocks / base)));
        int leftoverBlocks = additionalBlocks % base;
        return (base + extraRows) + "x" + base + "x" + base + "(+" + leftoverBlocks + ")";
    }
}
