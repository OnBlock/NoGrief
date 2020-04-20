package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static io.github.indicode.fabric.itsmine.command.admin.AdminCommand.PERMISSION_CHECK_ADMIN;
import static io.github.indicode.fabric.itsmine.command.admin.ClaimsCommand.list;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.PLAYERS_PROVIDER;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ListCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> list = literal("list");
        RequiredArgumentBuilder<ServerCommandSource, String> player = argument("player", word());
        player.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_CHECK_OTHERS, 2));
        player.suggests(PLAYERS_PROVIDER);
        list.executes(context -> list(context.getSource(), context.getSource().getName()));
        player.requires(PERMISSION_CHECK_ADMIN);
        player.executes(context -> list(context.getSource(), getString(context, "player")));
        command.then(list);
    }

}
