package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Direction;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.command.ExpandCommand.expand;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getDirections;
import static io.github.indicode.fabric.itsmine.util.DirectionUtil.directionByName;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ExpandCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        {
            LiteralArgumentBuilder<ServerCommandSource> expand = literal("expand");
            expand.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_INFINITE_CLAIM, 2) && ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY, 2));
            RequiredArgumentBuilder<ServerCommandSource, Integer> amount = argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
            RequiredArgumentBuilder<ServerCommandSource, String> direction = getDirections();

            direction.executes(context -> expand(
                    ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension()),
                    IntegerArgumentType.getInteger(context, "distance"),
                    directionByName(getString(context, "direction")),
                    context.getSource(),
                    true
            ));

            amount.executes(context -> expand(
                    ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension()),
                    IntegerArgumentType.getInteger(context, "distance"),
                    Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                    context.getSource(),
                    true
            ));

            amount.then(direction);
            expand.then(amount);
            command.then(expand);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> shrink = literal("shrink");
            shrink.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_INFINITE_CLAIM, 2) && ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY, 2));
            RequiredArgumentBuilder<ServerCommandSource, Integer> amount = argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
            RequiredArgumentBuilder<ServerCommandSource, String> direction = getDirections();

            direction.executes(context -> expand(
                    ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension()),
                    -IntegerArgumentType.getInteger(context, "distance"),
                    directionByName(getString(context, "direction")),
                    context.getSource(),
                    true
            ));

            amount.executes(context -> expand(
                    ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension()),
                    -IntegerArgumentType.getInteger(context, "distance"),
                    Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                    context.getSource(),
                    true
            ));

            amount.then(direction);
            shrink.then(amount);
            command.then(shrink);
        }
    }

}
