package io.github.indicode.fabric.itsmine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class ClaimCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("claim");
        {
            LiteralArgumentBuilder<ServerCommandSource> create = CommandManager.literal("create");
            ArgumentBuilder name = CommandManager.argument("name", StringArgumentType.word());
            ArgumentBuilder min = CommandManager.argument("min", BlockPosArgumentType.blockPos());
            RequiredArgumentBuilder<ServerCommandSource, PosArgument> max = CommandManager.argument("max", BlockPosArgumentType.blockPos());
            max.executes(context -> createClaim(
                    StringArgumentType.getString(context, "name"),
                    context.getSource().getPlayer().getGameProfile().getId(),
                    BlockPosArgumentType.getBlockPos(context, "min"),
                    BlockPosArgumentType.getBlockPos(context, "max")
            ));
            min.then(max);
            name.then(min);
            create.then(name);
            command.then(create);
        }
        dispatcher.register(command);
    }
    private static int createClaim(String name, UUID owner, BlockPos min, BlockPos max) {
        ClaimManager.INSTANCE.addClaim(new Claim(name, owner, min, max));
        return 0;
    }
}
