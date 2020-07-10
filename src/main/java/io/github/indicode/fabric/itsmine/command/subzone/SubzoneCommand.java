package io.github.indicode.fabric.itsmine.command.subzone;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.itsmine.command.*;
import net.minecraft.server.command.ServerCommandSource;

import static io.github.indicode.fabric.itsmine.command.admin.AdminCommand.PERMISSION_CHECK_ADMIN;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getSubzones;
import static net.minecraft.server.command.CommandManager.literal;

public class SubzoneCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher, boolean admin) {
        LiteralArgumentBuilder<ServerCommandSource> subzone = literal("subzone");
        subzone.requires(PERMISSION_CHECK_ADMIN);
        registerSubzone(subzone, dispatcher, admin);
        command.then(subzone);
    }

    public static void registerSubzone(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher, boolean admin) {
        CreateCommand.register(command, admin);
        ExpandCommand.register(command, admin);
        InfoCommand.register(command, getSubzones());
        MessageCommand.register(command, admin, getSubzones());
        PermissionCommand.register(command, admin, getSubzones());
        RemoveCommand.register(command, getSubzones(), admin);
        RenameCommand.register(command, admin);
        RentableCommand.register(command, getSubzones());
        RentCommand.register(command, getSubzones());
        RevenueCommand.register(command, getSubzones());
        FlagsCommand.register(command, admin, getSubzones());
        TrustCommand.register(command, dispatcher, getSubzones(), admin);
        TrustedCommand.register(command);
    }
}