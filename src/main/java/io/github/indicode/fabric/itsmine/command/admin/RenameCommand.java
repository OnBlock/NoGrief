package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static io.github.indicode.fabric.itsmine.command.RenameCommand.rename;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RenameCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> rename = literal("rename");
        RequiredArgumentBuilder<ServerCommandSource, String> claimArgument = ArgumentUtil.getClaims();
        RequiredArgumentBuilder<ServerCommandSource, String> nameArgument = argument("name", word());
        nameArgument.executes((context) -> rename(context, true));
        claimArgument.then(nameArgument);
        rename.then(claimArgument);
        command.then(rename);
    }
}
