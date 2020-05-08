package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;

import static io.github.indicode.fabric.itsmine.util.ChatColor.translateStringToText;

public class DebugCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> debug = LiteralArgumentBuilder.literal("debug");
        RequiredArgumentBuilder<ServerCommandSource, String> string = RequiredArgumentBuilder.argument("string", StringArgumentType.greedyString());
        string.executes(DebugCommand::execute);
        debug.then(string);
        command.then(debug);

    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        context.getSource().getPlayer().sendSystemMessage(translateStringToText('&', StringArgumentType.getString(context, "string")));
        return 1;
    }

}
