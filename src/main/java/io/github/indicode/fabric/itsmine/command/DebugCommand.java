package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.ItsMine;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static io.github.indicode.fabric.itsmine.util.ChatColor.translateStringToText;

public class DebugCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> debug = LiteralArgumentBuilder.literal("debug");
        RequiredArgumentBuilder<ServerCommandSource, String> string = RequiredArgumentBuilder.argument("string", StringArgumentType.greedyString());
        debug.executes(DebugCommand::execute);
        debug.then(string);
        command.then(debug);

    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
//        context.getSource().getPlayer().sendSystemMessage(translateStringToText('&', StringArgumentType.getString(context, "string")));
        ServerPlayerEntity player = context.getSource().getPlayer();
        player.sendSystemMessage(translateStringToText('&', "&eTime elapsed: &6" + ItsMine.time + "μs"), player.getUuid());
        player.sendSystemMessage(translateStringToText('&', "&eExecuted: &6" + ItsMine.executed), player.getUuid());
        player.sendSystemMessage(translateStringToText('&', "&eAverage: &6" + ItsMine.time / ItsMine.executed + "μs"), player.getUuid());

        return 1;
    }

}
