package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static io.github.indicode.fabric.itsmine.command.CreateCommand.createClaim;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CreateCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> create = literal("create");
        create.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_INFINITE_CLAIM, 2));
        ArgumentBuilder<ServerCommandSource, ?> name = argument("name", word());
        ArgumentBuilder<ServerCommandSource, ?> customOwner = argument("customOwnerName", word());
        ArgumentBuilder<ServerCommandSource, ?> min = argument("min", BlockPosArgumentType.blockPos());
        RequiredArgumentBuilder<ServerCommandSource, PosArgument> max = argument("max", BlockPosArgumentType.blockPos());
        max.executes(context -> createClaim(
                getString(context, "name"),
                context.getSource(),
                BlockPosArgumentType.getBlockPos(context, "min"),
                BlockPosArgumentType.getBlockPos(context, "max"),
                true,
                null
        ));
        name.executes(context ->  {
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
                if (createClaim(cname, context.getSource(), selectedPositions.getLeft(), selectedPositions.getRight(), true, null) > 0) {
                    ClaimManager.INSTANCE.stickPositions.remove(player);
                }
            }
            return 0;
        });
        customOwner.executes(context ->  {
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
                if (createClaim(cname, context.getSource(), selectedPositions.getLeft(), selectedPositions.getRight(), true, getString(context, "customOwnerName")) > 0) {
                    ClaimManager.INSTANCE.stickPositions.remove(player);
                }
            }
            return 0;
        });
        min.then(max);
        name.then(customOwner);
        name.then(min);
        create.then(name);
        command.then(create);
    }

}
