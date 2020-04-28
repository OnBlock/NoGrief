package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getEventMessage;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getMessageEvent;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.setEventMessage;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.verifyPermission;
import static net.minecraft.server.command.CommandManager.literal;

public class MessageCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin, RequiredArgumentBuilder<ServerCommandSource, String> claim) {
        LiteralArgumentBuilder<ServerCommandSource> message = literal("message");
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
        claim.then(messageEvent);
        message.then(claim);
        command.then(message);
    }

}
