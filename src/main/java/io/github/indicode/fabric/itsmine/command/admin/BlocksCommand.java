package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BlocksCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        {
            LiteralArgumentBuilder<ServerCommandSource> add = literal("addBlocks");
            add.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY_BALANCE, 2));
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = argument("player", EntityArgumentType.players());
            RequiredArgumentBuilder<ServerCommandSource, Integer> amount = argument("amount", IntegerArgumentType.integer());
            amount.executes(context -> {
                ClaimManager.INSTANCE.addClaimBlocks(EntityArgumentType.getPlayers(context, "player"), IntegerArgumentType.getInteger(context, "amount"));
                context.getSource().sendFeedback(new LiteralText("Gave " + IntegerArgumentType.getInteger(context, "amount") + " claim blocks").formatted(Formatting.GREEN), true);
                return 0;
            });
            player.then(amount);
            add.then(player);
            command.then(add);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> remove = literal("removeBlocks");
            remove.requires(source ->ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY_BALANCE, 2));
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = argument("player", EntityArgumentType.players());
            RequiredArgumentBuilder<ServerCommandSource, Integer> amount = argument("amount", IntegerArgumentType.integer());
            amount.executes(context -> {
                ClaimManager.INSTANCE.addClaimBlocks(EntityArgumentType.getPlayers(context, "player"), -IntegerArgumentType.getInteger(context, "amount"));
                context.getSource().sendFeedback(new LiteralText("Took " + IntegerArgumentType.getInteger(context, "amount") + " claim blocks").formatted(Formatting.GREEN), true);
                return 0;
            });
            player.then(amount);
            remove.then(player);
            command.then(remove);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> set = literal("setBlocks");
            set.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY_BALANCE, 2));
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = argument("player", EntityArgumentType.players());
            RequiredArgumentBuilder<ServerCommandSource, Integer> amount = argument("amount", IntegerArgumentType.integer());
            amount.executes(context -> {
                ClaimManager.INSTANCE.setClaimBlocks(EntityArgumentType.getPlayers(context, "player"), IntegerArgumentType.getInteger(context, "amount"));
                context.getSource().sendFeedback(new LiteralText("Set claim block amount to " + IntegerArgumentType.getInteger(context, "amount")).formatted(Formatting.GREEN), true);
                return 0;
            });
            player.then(amount);
            set.then(player);
            command.then(set);
        }
    }

}
