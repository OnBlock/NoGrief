package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.sun.org.apache.xpath.internal.Arg;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.command.HelpCommand.sendPage;
import static io.github.indicode.fabric.itsmine.command.TrustCommand.setTrust;
import static io.github.indicode.fabric.itsmine.command.TrustedCommand.showTrustedList;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.*;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ExceptionCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin, RequiredArgumentBuilder<ServerCommandSource, String> argumentBuilder) {
        {
            LiteralArgumentBuilder<ServerCommandSource> settings = literal("settings");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = argumentBuilder;

            if (!admin) {
                settings.executes((context) -> sendPage(context.getSource(), Messages.SETTINGS_AND_PERMISSIONS, 1, "Claim Permissions and Settings", "/claim help perms_and_settings %page%"));

                claim.executes((context) -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendError(Messages.INVALID_CLAIM);
                        return -1;
                    }
                    return querySettings(context.getSource(), claim1);
                });
            }

            RequiredArgumentBuilder<ServerCommandSource, String> id = argument("setting", word()).suggests(SETTINGS_PROVIDER);
            RequiredArgumentBuilder<ServerCommandSource, Boolean> set = argument("set", BoolArgumentType.bool());

            id.executes((context) -> executeSetting(context.getSource(), getString(context, "setting"), getString(context, "claim"), true, false, admin));
            set.executes((context) -> executeSetting(context.getSource(), getString(context, "setting"), null, false, BoolArgumentType.getBool(context, "set"), admin));

            id.then(set);
            claim.then(id);
            settings.then(claim);
            command.then(settings);
        }

        LiteralArgumentBuilder<ServerCommandSource> exceptions = literal("permissions");
        if (admin) exceptions.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY_PERMISSIONS, 2));
        RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims();
        if (!admin) {
            exceptions.executes((context) -> sendPage(context.getSource(), Messages.SETTINGS_AND_PERMISSIONS, 1, "Claim Permissions and Settings", "/claim help perms_and_settings %page%"));

            claim.executes((context) -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                if (claim1 == null) {
                    context.getSource().sendError(Messages.INVALID_CLAIM);
                    return -1;
                }
                return showTrustedList(context, claim1, false);
            });
        }

        if (!admin) {
            claim.executes((context) -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                if (claim1 == null) {
                    context.getSource().sendError(new LiteralText("That claim does not exist"));
                    return -1;
                }
                return showTrustedList(context, claim1, true);
            });
        }

        {
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = argument("player", EntityArgumentType.player());
            LiteralArgumentBuilder<ServerCommandSource> remove = literal("remove");
            remove.executes(context -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                if (verifyPermission(claim1, Claim.Permission.MODIFY_PERMISSIONS, context, admin)) {
                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                    claim1.permissionManager.resetPermissions(player1.getGameProfile().getId());
                    context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + " no longer has an exception in the claim").formatted(Formatting.YELLOW), false);
                }
                return 0;
            });
            player.then(remove);
            LiteralArgumentBuilder<ServerCommandSource> all = literal("*");
            RequiredArgumentBuilder<ServerCommandSource, Boolean> allstate = argument("allow", BoolArgumentType.bool());
            allstate.executes(context -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                validateClaim(claim1);
                return setTrust(context, claim1, player1.getGameProfile(), BoolArgumentType.getBool(context, "allow"), admin);
            });
            all.then(allstate);
            player.then(all);
            for (Claim.Permission value : Claim.Permission.values()) {
                LiteralArgumentBuilder<ServerCommandSource> permNode = literal(value.id);
                RequiredArgumentBuilder<ServerCommandSource, Boolean> allow = argument("allow", BoolArgumentType.bool());
                allow.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                    if (verifyPermission(claim1, Claim.Permission.MODIFY_PERMISSIONS, context, admin)) {
                        ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                        boolean permission = BoolArgumentType.getBool(context, "allow");
                        modifyException(claim1, player1, value, permission);
                        context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " now" : " no longer") + " has the permission " + value.name).formatted(Formatting.YELLOW), false);
                    }
                    return 0;
                });
                permNode.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                    if (verifyPermission(claim1, Claim.Permission.MODIFY_PERMISSIONS, context, admin)) {
                        ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                        boolean permission = hasPermission(claim1, player1, value);
                        context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " has" : " does not have") + " the permission " + value.name).formatted(Formatting.YELLOW), false);
                    }
                    return 0;
                });
                permNode.then(allow);
                player.then(permNode);
            }
            claim.then(player);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> message = literal("message");
            RequiredArgumentBuilder<ServerCommandSource, String> claimArgument = ArgumentUtil.getClaims();
            RequiredArgumentBuilder<ServerCommandSource, String> messageEvent = getMessageEvent();
            RequiredArgumentBuilder<ServerCommandSource, String> messageArgument = getEventMessage();
            messageArgument.executes(context -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                if (verifyPermission(claim1, Claim.Permission.MODIFY_PROPERTIES, context, admin)) {
                    Claim.Event event = Claim.Event.getById(getString(context, "messageEvent"));

                    if (event == null) {
                        context.getSource().sendError(Messages.INVALID_MESSAGE_EVENT);
                        return -1;
                    }

                    return setEventMessage(context.getSource(), claim1, event, getString(context, "message"));
                }

                return -1;
            });

            messageEvent.then(messageArgument);
            claimArgument.then(messageEvent);
            message.then(claimArgument);
            command.then(message);
        }


        exceptions.then(claim);
        command.then(exceptions);
    }
}
