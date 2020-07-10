package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BlockCommand {

    static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> check = literal("blocks");
        RequiredArgumentBuilder<ServerCommandSource, EntitySelector> other = argument("player", EntityArgumentType.player());
        other.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_CHECK_OTHERS, 2));
        other.executes(ctx -> blocksLeft(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player").getGameProfile().getId()));
        check.then(other);
        check.executes(ctx -> blocksLeft(ctx.getSource()));
        command.then(check);
    }

    static int blocksLeft(ServerCommandSource source, UUID player) throws CommandSyntaxException {
        int blocks = ClaimManager.INSTANCE.getClaimBlocks(player);
        source.sendFeedback(new LiteralText((source.getPlayer().getGameProfile().getId().equals(player) ? "You have " : "They have ") + blocks + " blocks left").formatted(Formatting.YELLOW), false);
        return 1;
    }
    static int blocksLeft(ServerCommandSource source) throws CommandSyntaxException {
        int blocks = ClaimManager.INSTANCE.getClaimBlocks(source.getPlayer().getUuid());
        source.sendFeedback(new LiteralText("You have "  + blocks + " blocks left").formatted(Formatting.YELLOW), false);
        return 1;
    }
}
