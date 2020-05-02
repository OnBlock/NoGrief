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
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getSettings;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.executeSetting;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.querySettings;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SettingsCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin, RequiredArgumentBuilder<ServerCommandSource, String> claim) {
            LiteralArgumentBuilder<ServerCommandSource> settings = literal("settings");

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

            RequiredArgumentBuilder<ServerCommandSource, String> id = getSettings();
            RequiredArgumentBuilder<ServerCommandSource, Boolean> set = argument("set", BoolArgumentType.bool());

            id.executes((context) -> executeSetting(context.getSource(), getString(context, "setting"), getString(context, "claim"), true, false, admin));
            set.executes((context) -> executeSetting(context.getSource(), getString(context, "setting"), null, false, BoolArgumentType.getBool(context, "set"), admin));

            id.then(set);
            claim.then(id);
            settings.then(claim);
            command.then(settings);
        }


}
