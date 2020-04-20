package io.github.indicode.fabric.itsmine.command.subzone;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Config;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static io.github.indicode.fabric.itsmine.command.ShowCommand.executeShowClaim;

public class CreateCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        {
            LiteralArgumentBuilder<ServerCommandSource> create = CommandManager.literal("create");

            RequiredArgumentBuilder<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> addZone(ctx.getSource(), StringArgumentType.getString(ctx, "name"), null, false));

            RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims()
                    .executes(ctx -> addZone(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "claim"), false));

            name.then(claim);
            create.then(name);
            command.then(create);
        }
    }

    private static int addZone(ServerCommandSource source, String name, @Nullable String claimName, boolean admin) throws CommandSyntaxException {
        if (name.length() > 30) {
            source.sendError(Messages.MSG_LONG_NAME);
            return -1;
        }

        ServerPlayerEntity player = source.getPlayer();
        Claim claim = validateAndGet(source, claimName, admin);
        Claim subZone = null;

        if (!admin && !claim.permissionManager.hasPermission(player.getGameProfile().getId(), Claim.Permission.MODIFY_SUBZONE)) {
            throw new SimpleCommandExceptionType(Messages.NO_PERMISSION).create();
        }
        if(!name.matches("[A-Za-z0-9]+")){
            source.sendError(new LiteralText("Invalid claim name"));
            return -1;
        }

        if (ClaimManager.INSTANCE.claimsByName.containsKey(name)) {
            source.sendFeedback(new LiteralText("The name \"" + name + "\" is already taken.").formatted(Formatting.RED), false);
            return -1;
        }
        Pair<BlockPos, BlockPos> selectedPositions = ClaimManager.INSTANCE.stickPositions.get(player);
        if (selectedPositions == null) {
            source.sendFeedback(new LiteralText("You need to specify block positions or select them with a stick.").formatted(Formatting.RED), false);
        } else if (selectedPositions.getLeft() == null) {
            source.sendFeedback(new LiteralText("You need to specify block positions or select position #1(Right Click) with a stick.").formatted(Formatting.RED), false);
        } else if (selectedPositions.getRight() == null) {
            source.sendFeedback(new LiteralText("You need to specify block positions or select position #2(Left Click) with a stick.").formatted(Formatting.RED), false);
        } else {
            name = claim.name + "." + name;
            subZone = createSubzone(source, name, selectedPositions.getLeft(), selectedPositions.getRight(), admin);
            if (subZone.dimension == claim.dimension && claim.includesPosition(subZone.min) && claim.includesPosition(subZone.max) && !claim.isChild){
                if (!ClaimManager.INSTANCE.wouldSubzoneIntersect((subZone))){
                    claim.addSubzone(subZone);
                    ClaimManager.INSTANCE.addClaim(subZone);
                    subZone.permissionManager = claim.permissionManager;
                    executeShowClaim(source, claim, false);
                    source.sendFeedback(new LiteralText("").append(new LiteralText("Your subzone was created.").formatted(Formatting.GREEN)), false);
                }else{
                    player.sendMessage(new LiteralText("Your subzone would overlap with another subzone").formatted(Formatting.RED));
                }
                if (subZone != null) {
                    ClaimManager.INSTANCE.stickPositions.remove(player);
                    return 1;
                }
                return 0;
            }else{
                player.sendMessage(new LiteralText("Subzone must be inside the original claim, in the same dimension").formatted(Formatting.RED));
            }
        }
        return 0;
    }

    private static Claim createSubzone(ServerCommandSource source, String name, BlockPos posA, BlockPos posB, boolean admin) throws CommandSyntaxException {
        UUID ownerID = source.getPlayer().getGameProfile().getId();
        int x, y = 0, z, mx, my = 255, mz;
        if (posA.getX() > posB.getX()) {
            x = posB.getX();
            mx = posA.getX();
        } else {
            x =  posA.getX();
            mx = posB.getX();
        }
        if (!Config.claims2d) {
            if (posA.getY() > posB.getY()) {
                y = posB.getY();
                my = posA.getY();
            } else {
                y = posA.getY();
                my = posB.getY();
            }
        }
        if (posA.getZ() > posB.getZ()) {
            z = posB.getZ();
            mz = posA.getZ();
        } else {
            z =  posA.getZ();
            mz = posB.getZ();
        }
        BlockPos min = new BlockPos(x, y, z);
        BlockPos max = new BlockPos(mx, my, mz);
        BlockPos sub = max.subtract(min);
        sub = sub.add(1, Config.claims2d ? 0 : 1,1);
        return new Claim(name, admin ? null : ownerID, min, max, source.getWorld().getDimension().getType(), source.getPlayer().getBlockPos(), true);
    }

    private static Claim validateAndGet(ServerCommandSource source, @Nullable String  claimName, boolean admin) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        Claim claim = claimName == null ? ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension) :
                ClaimManager.INSTANCE.claimsByName.get(claimName);

        if (claim == null) {
            throw new SimpleCommandExceptionType(Messages.INVALID_CLAIM).create();
        }
        if (!admin && !claim.permissionManager.hasPermission(player.getGameProfile().getId(), Claim.Permission.MODIFY_SUBZONE)) {
            throw new SimpleCommandExceptionType(Messages.NO_PERMISSION).create();
        }

        return claim;
    }
}
