package io.github.indicode.fabric.itsmine.command.subzone;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.itsmine.command.*;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public class SubzoneCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> subzone = literal("subzone");
        registerSubzone(subzone, dispatcher);
        command.then(subzone);
    }

    public static void registerSubzone(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher) {
        CreateCommand.register(command);
        ExceptionCommand.register(command, false, true);
        ExpandCommand.register(command);
        InfoCommand.register(command);
        PermissionsCommand.register(command, false);
        RemoveCommand.register(command);
        RenameCommand.register(command);
        RentableCommand.register(command);
        RentCommand.register(command);
        RevenueCommand.register(command);
        SettingsCommand.register(command, false);
        ShowCommand.register(command);
//        SubzoneCommand.register(command, dispatcher);
        TrustCommand.register(command);
        TrustedCommand.register(command);
    }
}