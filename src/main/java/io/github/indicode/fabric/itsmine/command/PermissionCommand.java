package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.command.HelpCommand.sendPage;
import static io.github.indicode.fabric.itsmine.command.TrustCommand.setTrust;
import static io.github.indicode.fabric.itsmine.command.TrustedCommand.showTrustedList;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getPermissions;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PermissionCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin, RequiredArgumentBuilder<ServerCommandSource, String> claim) {
        LiteralArgumentBuilder<ServerCommandSource> exception = literal("permissions");
        RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = argument("player", EntityArgumentType.player());
        LiteralArgumentBuilder<ServerCommandSource> all = literal("*");
        RequiredArgumentBuilder<ServerCommandSource, Boolean> allstate = argument("allow", BoolArgumentType.bool());
        RequiredArgumentBuilder<ServerCommandSource, String> permNode = getPermissions();
        RequiredArgumentBuilder<ServerCommandSource, Boolean> allow = argument("allow", BoolArgumentType.bool());

        exception.executes((context) -> sendPage(context.getSource(), Messages.SETTINGS_AND_PERMISSIONS, 1, "Claim Permissions and Flags", "/claim help perms_and_flags %page%"));
        claim.executes((context) -> {
            Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
            if (claim1 == null) {
                context.getSource().sendError(Messages.INVALID_CLAIM);
                return -1;
            }
            return showTrustedList(context, claim1, false);
        });

        claim.executes((context) -> {
            Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
            if (claim1 == null) {
                context.getSource().sendError(new LiteralText("That claim does not exist"));
                return -1;
            }
            return showTrustedList(context, claim1, true);
        });

        allstate.executes(context -> {
            Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
            ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
            validateClaim(claim1);
            return setTrust(context, claim1, player1.getGameProfile(), BoolArgumentType.getBool(context, "allow"), admin);
        });

        allow.executes(context -> allow(context, admin, BoolArgumentType.getBool(context, "allow")));
        permNode.executes(context -> query(context, admin));
        all.then(allstate);
        permNode.then(allow);
        player.then(permNode);
        player.then(all);
        claim.then(player);
        exception.then(claim);
        command.then(exception);
    }

    private static int allow(CommandContext<ServerCommandSource> context, boolean admin, boolean allow) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
        if (verifyPermission(claim1, Claim.Permission.MODIFY_PERMISSIONS, context, admin)) {
            ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
            boolean permission = BoolArgumentType.getBool(context, "allow");
            Claim.Permission value = Claim.Permission.byId(StringArgumentType.getString(context, "permission"));
            if(value == null) {
                source.sendError(Messages.INVALID_PERMISSION);
                return 0;
            }
            modifyException(claim1, player1, value, permission);
            source.sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " now" : " no longer") + " has the permission " + value).formatted(Formatting.YELLOW), false);
            return 1;
        }
        return 0;
    }

    private static int query(CommandContext<ServerCommandSource> context, boolean admin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
        if (verifyPermission(claim1, Claim.Permission.MODIFY_PERMISSIONS, context, admin)) {
            ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
            Claim.Permission value = Claim.Permission.byId(StringArgumentType.getString(context, "permission"));
            if(value == null) {
                source.sendError(Messages.INVALID_PERMISSION);
                return 0;
            }
            boolean permission = hasPermission(claim1, player1, value);
            source.sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " has" : " does not have") + " the permission " + value.name).formatted(Formatting.YELLOW), false);
            return 1;
        }
        return 0;
    }

}
