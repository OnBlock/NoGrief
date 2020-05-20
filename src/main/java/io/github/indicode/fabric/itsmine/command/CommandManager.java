package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.itsmine.ItsMineConfig;
import io.github.indicode.fabric.itsmine.command.admin.AdminCommand;
import io.github.indicode.fabric.itsmine.command.subzone.SubzoneCommand;
import net.minecraft.server.command.ServerCommandSource;

import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getClaims;


public class CommandManager {

    public static CommandDispatcher<ServerCommandSource> dispatcher;


    public static void register() {
        LiteralArgumentBuilder<ServerCommandSource> main = LiteralArgumentBuilder.literal("itsmine");
        LiteralArgumentBuilder<ServerCommandSource> alias = LiteralArgumentBuilder.literal("claim");
        register(main, dispatcher);
        register(alias, dispatcher);
        dispatcher.register(main);
        dispatcher.register(alias);
    }


    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher) {
        AdminCommand.register(command, dispatcher);
        BlockCommand.register(command);
        CreateCommand.register(command);
        ClaimsCommand.register(command, dispatcher);
//        DebugCommand.register(command);
        ExpandCommand.register(command, false);
        FlyCommand.register(command);
        HelpCommand.register(command);
        InfoCommand.register(command, getClaims());
        ListCommand.register(command);
        MessageCommand.register(command, false, getClaims());
        PermissionCommand.register(command, false, getClaims());
        RemoveCommand.register(command, getClaims(), false);
        RenameCommand.register(command, false);
        RentableCommand.register(command, getClaims());
        RentCommand.register(command, getClaims());
        RevenueCommand.register(command, getClaims());
        FlagsCommand.register(command, false, getClaims());
        ShowCommand.register(command);
        StickCommand.register(command);
        SubzoneCommand.register(command, dispatcher, false);
        TransferCommand.register(command);
        TrustCommand.register(command, dispatcher, getClaims(), false);
        TrustedCommand.register(command);
    }
}
