package io.github.indicode.fabric.itsmine.command.subzone;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.itsmine.command.*;
import net.minecraft.server.command.ServerCommandSource;

import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getSubzones;
import static net.minecraft.server.command.CommandManager.literal;

public class SubzoneCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher, boolean admin) {
        LiteralArgumentBuilder<ServerCommandSource> subzone = literal("subzone");
        registerSubzone(subzone, dispatcher, admin);
        command.then(subzone);
    }

    public static void registerSubzone(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher, boolean admin) {
        CreateCommand.register(command, admin);
        ExceptionCommand.register(command, admin, getSubzones());
        ExpandCommand.register(command, admin);
        InfoCommand.register(command, getSubzones());
        PermissionsCommand.register(command, admin);
        RemoveCommand.register(command, getSubzones(), admin);
        RenameCommand.register(command, admin);
        RentableCommand.register(command, getSubzones());
        RentCommand.register(command, getSubzones());
        RevenueCommand.register(command, getSubzones());
        TrustCommand.register(command, getSubzones(), true);
        TrustedCommand.register(command);
    }
}