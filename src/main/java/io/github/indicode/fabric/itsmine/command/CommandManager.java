package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.itsmine.command.admin.AdminCommand;
import io.github.indicode.fabric.itsmine.command.subzone.SubzoneCommand;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public class CommandManager {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> main = literal("itsmine");
        LiteralArgumentBuilder<ServerCommandSource> alias = literal("claim");
        register(main, dispatcher);
        register(alias, dispatcher);
        dispatcher.register(main);
        dispatcher.register(alias);
    }
    private static void register(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher) {
        AdminCommand.register(command, dispatcher);
        BlockCommand.register(command);
        CreateCommand.register(command);
        ExceptionCommand.register(command, false, false);
        ExpandCommand.register(command);
        FlyCommand.register(command);
        HelpCommand.register(command);
        InfoCommand.register(command);
        ListCommand.register(command);
        PermissionsCommand.register(command, false);
        RemoveCommand.register(command);
        RenameCommand.register(command);
        RentableCommand.register(command);
        RentCommand.register(command);
        RevenueCommand.register(command);
        SettingsCommand.register(command, false);
        ShowCommand.register(command);
        StickCommand.register(command);
        SubzoneCommand.register(command, dispatcher);
        TransferCommand.register(command);
        TrustCommand.register(command);
        TrustedCommand.register(command);
    }
}
