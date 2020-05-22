package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.command.RemoveCommand.delete;
import static io.github.indicode.fabric.itsmine.command.RemoveCommand.requestDelete;
import static net.minecraft.server.command.CommandManager.literal;

public class RemoveCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> delete = literal("remove");
        delete.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY, 2));
        RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims();
        LiteralArgumentBuilder<ServerCommandSource> confirm = literal("confirm");
        confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), true));
        claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), true));
        claim.then(confirm);
        delete.then(claim);
        delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension()), true));
        command.then(delete);
    }

}
