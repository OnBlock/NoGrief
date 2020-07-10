package io.github.indicode.fabric.itsmine.util;

import blue.endless.jankson.annotation.Nullable;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.indicode.fabric.itsmine.*;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.claim.ClaimFlags;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ClaimUtil {
    public static BlockPos getPosOnGround(BlockPos pos, World world) {
        BlockPos blockPos = new BlockPos(pos.getX(), 256, pos.getZ());

        do {
            blockPos = blockPos.down();
            if(blockPos.getY() < 1){
                    return pos;
            }
        } while (!world.getBlockState(blockPos).isFullCube(world, pos));

        return blockPos.up();
    }

    public static Claim getParentClaim(Claim subzone){
        AtomicReference<Claim> parentClaim = new AtomicReference<>();
        if(subzone.isChild){
            ClaimManager.INSTANCE.claimsByName.forEach((name, claim) -> {
                for(Claim subzone2 : claim.children){
                    if(subzone2 == subzone){
                        parentClaim.set(claim);
                    }
                }
            });
            return parentClaim.get();
        }
        return subzone;
    }

    public static void validateClaim(Claim claim) throws CommandSyntaxException {
        if (claim == null) throw new SimpleCommandExceptionType(Messages.INVALID_CLAIM).create();
    }

    public static boolean verifyPermission(Claim claim, Claim.Permission permission, CommandContext<ServerCommandSource> context, boolean admin) throws CommandSyntaxException {
        if (verifyExists(claim, context)) {
            if (claim.permissionManager.hasPermission(context.getSource().getPlayer().getGameProfile().getId(), permission)) {
                return true;
            } else {
                context.getSource().sendFeedback(new LiteralText(admin ? "You are modifying a claim using admin privileges" : "You cannot modify exceptions for this claim").formatted(admin ? Formatting.DARK_RED : Formatting.RED), false);
                return admin;
            }
        } else {
            return false;
        }
    }
    private static boolean verifyExists(Claim claim, CommandContext<ServerCommandSource> context) {
        if (claim == null) {
            context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return false;
        } else {
            return true;
        }
    }

    public static ArrayList<Claim> getClaims(){
        ArrayList<Claim> claims = new ArrayList<>();
        ClaimManager.INSTANCE.claimsByName.forEach((s, claim) -> {
            claims.add(claim);
        });
        return claims;
    }

    public static void validateCanAccess(ServerPlayerEntity player, Claim claim, boolean admin) throws CommandSyntaxException {
        if (claim == null) {
            throw new SimpleCommandExceptionType(Messages.INVALID_CLAIM).create();
        }

        if (!admin && !claim.permissionManager.hasPermission(player.getGameProfile().getId(), Claim.Permission.MODIFY_SETTINGS)) {
            throw new SimpleCommandExceptionType(Messages.NO_PERMISSION).create();
        }
    }

    public static int queryFlag(ServerCommandSource source, Claim claim, ClaimFlags.Flag flag) {
        boolean enabled = claim.flags.getFlag(flag);
        source.sendFeedback(new LiteralText(ChatColor.translate("&eFlag &6" + flag.name + " &e is set to " + (enabled ? "&a" : "&c") + enabled + "&e for &6" + claim.name)), false);
        return 1;
    }
    public static int setFlag(ServerCommandSource source, Claim claim, ClaimFlags.Flag flag, boolean set) {
        claim.flags.flags.put(flag, set);
        source.sendFeedback(new LiteralText(ChatColor.translate("&eSet flag &6" + flag.name + "&e to " + (set ? "&a" : "&c") + set + "&e for &6" + claim.name)), false);
        return 0;
    }
    public static int queryPermission(ServerCommandSource source, Claim claim, Claim.Permission permission) {
        boolean defaultPerm = claim.permissionManager.defaults.hasPermission(permission);
        source.sendFeedback(new LiteralText(ChatColor.translate("&ePermission &6" + permission.id + "&e is set to " + (defaultPerm ? "&a" : "&c") + defaultPerm + "&e for &6" + claim.name)), false);
        return 1;
    }
    public static int setPermission(ServerCommandSource source, Claim claim, Claim.Permission permission, boolean set) {
        claim.permissionManager.defaults.setPermission(permission, set);
        source.sendFeedback(new LiteralText(ChatColor.translate("&eSet permission &6" + permission.id + "&e to " + (set ? "&a" : "&c") + set + "&e for &6" + claim.name)), false);
        return 1;
    }
    public static int queryFlags(ServerCommandSource source, Claim claim) {
        source.sendFeedback(new LiteralText("\n").append(new LiteralText("Flags: " + claim.name).formatted(Formatting.YELLOW)).append(new LiteralText("\n"))
                .append(Messages.Command.getFlags(claim)).append(new LiteralText("\n")), false);
        return 1;
    }
    public static int executeFlag(ServerCommandSource source, String input, @Nullable String claimName, boolean isQuery, boolean value, boolean admin) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        Claim claim1 = claimName == null || claimName.isEmpty() ? ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.getDimension()) :
                ClaimManager.INSTANCE.claimsByName.get(claimName);
        if (claim1 == null) {
            source.sendError(Messages.INVALID_CLAIM);
            return -1;
        }

        if (input == null) {
            return queryFlags(source, claim1);
        }

        validateCanAccess(player, claim1, admin);
        ClaimFlags.Flag flag = ClaimFlags.Flag.byId(input);
        Claim.Permission permission = Claim.Permission.byId(input);

        if (flag != null && permission == null)
            return isQuery ? queryFlag(source, claim1, flag) : setFlag(source, claim1, flag, value);

        if (flag == null && permission != null)
            return isQuery ? queryPermission(source, claim1, permission) : setPermission(source, claim1, permission, value);

        source.sendError(Messages.INVALID_SETTING);
        return -1;
    }
    public static int executePermission(ServerCommandSource source, String input, @Nullable String claimName, boolean isQuery, boolean value, boolean admin) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        Claim claim1 = claimName == null ? ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.getDimension()) :
                ClaimManager.INSTANCE.claimsByName.get(claimName);
        if (claim1 == null) {
            source.sendError(Messages.INVALID_CLAIM);
            return -1;
        }

        validateCanAccess(player, claim1, admin);
        Claim.Permission permission = Claim.Permission.byId(input);
        if (permission != null)
            return !isQuery ? setPermission(source, claim1, permission, value) : queryPermission(source, claim1, permission);

        source.sendError(Messages.INVALID_SETTING);
        return -1;
    }

    public static int modifyException(Claim claim, ServerPlayerEntity exception, Claim.Permission permission, boolean allowed) {
        claim.permissionManager.setPermission(exception.getGameProfile().getId(), permission, allowed);
        return 0;
    }
    private static int modifyException(Claim claim, String exception, Claim.Permission permission, boolean allowed) {
        claim.permissionManager.setPermission(exception, permission, allowed);
        return 0;
    }

    public static boolean hasPermission(Claim claim, ServerPlayerEntity exception, Claim.Permission permission) {
        return claim.permissionManager.hasPermission(exception.getGameProfile().getId(), permission);
    }

    public static int setEventMessage(ServerCommandSource source, Claim claim, Claim.Event event, String message) {
        switch (event) {
            case ENTER_CLAIM:
                claim.enterMessage = message.equalsIgnoreCase("reset") ? null : message;
                break;
            case LEAVE_CLAIM:
                claim.leaveMessage = message.equalsIgnoreCase("reset") ? null : message;
                break;
        }

        if (message.equalsIgnoreCase("reset")) {
            source.sendFeedback(new LiteralText("Reset ").append(new LiteralText(event.id).formatted(Formatting.GOLD)
                            .append(new LiteralText(" Event Message for claim ").formatted(Formatting.YELLOW))
                            .append(new LiteralText(claim.name).formatted(Formatting.GOLD))).formatted(Formatting.YELLOW)
                    , false);
            return -1;
        }

        source.sendFeedback(new LiteralText("Set ").append(new LiteralText(event.id).formatted(Formatting.GOLD)
                        .append(new LiteralText(" Event Message for claim ").formatted(Formatting.YELLOW))
                        .append(new LiteralText(claim.name).formatted(Formatting.GOLD)).append(new LiteralText(" to:").formatted(Formatting.YELLOW)))
                        .append(new LiteralText("\n")).append(new LiteralText(ChatColor.translate(message)))
                        .formatted(Formatting.YELLOW)
                , false);
        return 1;
    }

    public static void readWorldProperties() {
        File claims = new File(ItsMine.getDirectory() + "/world/claims.dat");
        File claims_old = new File(ItsMine.getDirectory() + "/world/claims.dat_old");
        if (!claims.exists()) {
            if (claims_old.exists()) {}
            else return;
        }
        try {
            if (!claims.exists() && claims_old.exists()) throw new FileNotFoundException();
            ClaimManager.INSTANCE.fromNBT(NbtIo.readCompressed(new FileInputStream(claims)));
        } catch (IOException e) {
            System.out.println("Could not load " + claims.getName() + ":");
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

}
