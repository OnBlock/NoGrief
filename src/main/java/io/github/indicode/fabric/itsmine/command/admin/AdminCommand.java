package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.command.ExceptionCommand;
import io.github.indicode.fabric.itsmine.command.PermissionsCommand;
import io.github.indicode.fabric.itsmine.command.SettingsCommand;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.literal;

public class AdminCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> admin = literal("admin");
        admin.requires(PERMISSION_CHECK_ADMIN);
        registerAdmin(admin, dispatcher);
        command.then(admin);
    }

    private static void registerAdmin(LiteralArgumentBuilder<ServerCommandSource> admin, CommandDispatcher dispatcher) {
        BlocksCommand.register(admin);
        ClaimsCommand.register(admin, dispatcher);
        CreateCommand.register(admin);
        EntitiesCommand.register(admin);
        ExceptionCommand.register(admin, true, false);
        ExpandCommand.register(admin);
        IgnoreCommand.register(admin);
        ListAllCommand.register(admin);
        OwnerCommand.register(admin);
        PermissionsCommand.register(admin, true);
        RemoveCommand.register(admin);
        RenameCommand.register(admin);
        SettingsCommand.register(admin, true);
    }

    private static Predicate<ServerCommandSource> perm(String str) {
        return perm(str, 2);
    }
    private static Predicate<ServerCommandSource> perm(String str, int op) {
        return source -> ItsMine.permissions().hasPermission(source, "itsmine." + str, op);
    }
    public static final Predicate<ServerCommandSource> PERMISSION_CHECK_ADMIN = src -> perm("admin").test(src);

}
