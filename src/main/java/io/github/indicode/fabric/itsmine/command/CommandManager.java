package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sun.org.apache.xpath.internal.Arg;
import io.github.indicode.fabric.itsmine.command.admin.AdminCommand;
import io.github.indicode.fabric.itsmine.command.subzone.SubzoneCommand;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import net.minecraft.server.command.ServerCommandSource;

import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getClaims;
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
        ExceptionCommand.register(command, false, getClaims());
        ExpandCommand.register(command, false);
        FlyCommand.register(command);
        HelpCommand.register(command);
        InfoCommand.register(command, getClaims());
        ListCommand.register(command);
        PermissionsCommand.register(command, false);
        RemoveCommand.register(command, getClaims(), false);
        RenameCommand.register(command, false);
        RentableCommand.register(command, getClaims());
        RentCommand.register(command, getClaims());
        RevenueCommand.register(command, getClaims());
//        SettingsCommand.register(command, false);
        ShowCommand.register(command);
        StickCommand.register(command);
        SubzoneCommand.register(command, dispatcher, false);
        TransferCommand.register(command);
        TrustCommand.register(command, getClaims(), false);
        TrustedCommand.register(command);
    }
}
