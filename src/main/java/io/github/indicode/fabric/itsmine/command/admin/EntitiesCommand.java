package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.server.command.CommandManager.literal;

public class EntitiesCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> entity = literal("entities");
        entity.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY, 2));
        RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims();
        entity.executes(context -> {
            Claim claim1 = ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getPlayer().dimension);
            context.getSource().sendFeedback(new LiteralText("Entities (" + claim1.name + "): ").formatted(Formatting.GOLD).append(new LiteralText(String.valueOf(claim1.getEntities(context.getSource().getWorld()))).formatted(Formatting.AQUA)), true);
            claim1.getEntitySorted(claim1.getEntityMap(claim1, context.getSource().getWorld())).forEach((entityType, integer) -> {
                context.getSource().sendFeedback(new LiteralText(entityType.getName().asString() + ": ").formatted(Formatting.YELLOW).append(new LiteralText(String.valueOf(integer)).formatted(Formatting.GOLD)), true);
            });
            return 1;
        });
        claim.executes(context -> {
            Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
            context.getSource().sendFeedback(new LiteralText("Entities (" + claim1.name + "): ").formatted(Formatting.GOLD).append(new LiteralText(String.valueOf(claim1.getEntities(context.getSource().getWorld()))).formatted(Formatting.AQUA)), true);
            claim1.getEntitySorted(claim1.getEntityMap(claim1, context.getSource().getWorld())).forEach((entityType, integer) -> {
                context.getSource().sendFeedback(new LiteralText(entityType.getName().asString() + ": ").formatted(Formatting.YELLOW).append(new LiteralText(String.valueOf(integer)).formatted(Formatting.GOLD)), true);
            });
            return 1;
        });
        entity.then(claim);
        command.then(entity);
    }

}
