package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.command.HelpCommand.sendPage;
import static io.github.indicode.fabric.itsmine.command.TrustedCommand.showTrustedList;
import static net.minecraft.server.command.CommandManager.literal;

public class PermissionsCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin) {
        LiteralArgumentBuilder<ServerCommandSource> exceptions = literal("permissions");
        if (admin)
            exceptions.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY_PERMISSIONS, 2));
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
    }
}
