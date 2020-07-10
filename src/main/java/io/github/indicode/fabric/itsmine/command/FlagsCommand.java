package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.command.HelpCommand.sendPage;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getFlags;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.executeFlag;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.queryFlags;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FlagsCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin, RequiredArgumentBuilder<ServerCommandSource, String> claim) {
            LiteralArgumentBuilder<ServerCommandSource> flags = literal("flags");

            if (!admin) {
                flags.executes((context) -> sendPage(context.getSource(), Messages.SETTINGS_AND_PERMISSIONS, 1, "Claim Permissions and Flags", "/claim help perms_and_flags %page%"));

                claim.executes((context) -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendError(Messages.INVALID_CLAIM);
                        return -1;
                    }
                    return queryFlags(context.getSource(), claim1);
                });
            }

            RequiredArgumentBuilder<ServerCommandSource, String> id = getFlags();
            RequiredArgumentBuilder<ServerCommandSource, Boolean> set = argument("set", BoolArgumentType.bool());

            id.executes((context) -> executeFlag(context.getSource(), getString(context, "flag"), getString(context, "claim"), true, false, admin));
            set.executes((context) -> executeFlag(context.getSource(), getString(context, "flag"), getString(context, "claim"), false, BoolArgumentType.getBool(context, "set"), admin));

            id.then(set);
            claim.then(id);
            flags.then(claim);
            command.then(flags);
        }


}
