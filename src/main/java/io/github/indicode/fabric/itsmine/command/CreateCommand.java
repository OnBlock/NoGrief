package io.github.indicode.fabric.itsmine.command;

import blue.endless.jankson.annotation.Nullable;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.*;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static io.github.indicode.fabric.itsmine.command.ShowCommand.executeShowClaim;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CreateCommand {
    static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> create = literal("create");
        RequiredArgumentBuilder<ServerCommandSource, String> name = argument("name", word());

        name.executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            Pair<BlockPos, BlockPos> selectedPositions = ClaimManager.INSTANCE.stickPositions.get(player);
            if (selectedPositions == null) {
                context.getSource().sendFeedback(new LiteralText("You need to specify block positions or select them with a stick.").formatted(Formatting.RED), false);
            } else if (selectedPositions.getLeft() == null) {
                context.getSource().sendFeedback(new LiteralText("You need to specify block positions or select position #1(Right Click) with a stick.").formatted(Formatting.RED), false);
            } else if (selectedPositions.getRight() == null) {
                context.getSource().sendFeedback(new LiteralText("You need to specify block positions or select position #2(Left Click) with a stick.").formatted(Formatting.RED), false);
            } else {
                String cname = getString(context, "name");
                if (createClaim(cname, context.getSource(), selectedPositions.getLeft(), selectedPositions.getRight(), false, null) > 0) {
                    ClaimManager.INSTANCE.stickPositions.remove(player);
                }
            }
            return 0;
        });

        ArgumentBuilder<ServerCommandSource, ?> min = argument("min", BlockPosArgumentType.blockPos());
        RequiredArgumentBuilder<ServerCommandSource, PosArgument> max = argument("max", BlockPosArgumentType.blockPos());
        max.executes(context -> createClaim(getString(context, "name"), context.getSource(), BlockPosArgumentType.getBlockPos(context, "min"), BlockPosArgumentType.getBlockPos(context, "max"), false, null));
        min.then(max);
        name.then(min);
        create.then(name);
        command.then(create);
    }

    public static int createClaim(String name, ServerCommandSource owner, BlockPos posA, BlockPos posB, boolean admin, @Nullable String cOwnerName) throws CommandSyntaxException {
        if (name.length() > 30) {
            owner.sendError(Messages.MSG_LONG_NAME);
            return -1;
        }
        if(!name.matches("[A-Za-z0-9]+")){
            owner.sendError(new LiteralText("Invalid claim name"));
            return -1;
        }
        UUID ownerID = owner.getPlayer().getGameProfile().getId();
        int x, y = 0, z, mx, my = 255, mz;
        if (posA.getX() > posB.getX()) {
            x = posB.getX();
            mx = posA.getX();
        } else {
            x =  posA.getX();
            mx = posB.getX();
        }
        if (!ItsMineConfig.main().claims2d) {
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
        sub = sub.add(1, ItsMineConfig.main().claims2d ? 0 : 1,1);
        int subInt = sub.getX() * (ItsMineConfig.main().claims2d ? 1 : sub.getY()) * sub.getZ();

        Claim claim = new Claim(name, admin ? null : ownerID, min, max, owner.getWorld().getDimension().getType(), owner.getPlayer().getBlockPos(), false);
        if (cOwnerName != null) claim.customOwnerName = cOwnerName;
        claim.permissionManager.playerPermissions.put(ownerID, new Claim.InvertedPermissionMap());
        if (!ClaimManager.INSTANCE.claimsByName.containsKey(name)) {
            if (!ClaimManager.INSTANCE.wouldIntersect(claim)) {
                // works because only the first statement is evaluated if true
                if ((admin && ItsMine.permissions().hasPermission(owner, PermissionUtil.Command.INFINITE_BLOCKS, 2)) || ClaimManager.INSTANCE.useClaimBlocks(ownerID, subInt)) {
                    ClaimManager.INSTANCE.addClaim(claim);
                    MutableText message = new LiteralText("");
                    message.append(new LiteralText("Your claim was created").formatted(Formatting.GREEN));
                    message.append(new LiteralText("(Area: " + sub.getX() + "x" + sub.getY() + "x" + sub.getZ() + ")").styled(style -> {
                        return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(subInt + " blocks").formatted(Formatting.YELLOW)));
                    }));
                    owner.sendFeedback(message, false);
                    BlockCommand.blocksLeft(owner);
                    executeShowClaim(owner, claim, false);
                    if (admin)
                        owner.getMinecraftServer().sendSystemMessage(new LiteralText(owner.getPlayer().getGameProfile().getName() + " Has created a new claim(" + claim.name + ") using the admin command."));
                    return 1;
                } else {
                    owner.sendFeedback(new LiteralText("You don't have enough claim blocks. You have " + ClaimManager.INSTANCE.getClaimBlocks(ownerID) + ", you need " + subInt + "(" + (subInt - ClaimManager.INSTANCE.getClaimBlocks(ownerID)) + " more)").formatted(Formatting.RED), false);
                }
            } else {
                owner.sendFeedback(new LiteralText("Your claim would overlap with another claim").formatted(Formatting.RED), false);
            }
        } else {
            owner.sendFeedback(new LiteralText("The name \"" + name + "\" is already taken.").formatted(Formatting.RED), false);
        }
        return 0;
    }
}
