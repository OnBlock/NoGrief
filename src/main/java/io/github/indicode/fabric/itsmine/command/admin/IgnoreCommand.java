package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class IgnoreCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> ignore = literal("ignoreClaims");
        ignore.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_IGNORE_CLAIMS, 2));
        ignore.executes(context -> {
            UUID id = context.getSource().getPlayer().getGameProfile().getId();
            boolean isIgnoring = ClaimManager.INSTANCE.ignoringClaims.contains(id);
            if (isIgnoring) ClaimManager.INSTANCE.ignoringClaims.remove(id);
            else ClaimManager.INSTANCE.ignoringClaims.add(id);
            context.getSource().sendFeedback(new LiteralText("You are " + (isIgnoring ? "no longer" : "now") + " ignoring claims.").formatted(Formatting.GREEN), false);
            return 0;
        });
        command.then(ignore);
    }

}
