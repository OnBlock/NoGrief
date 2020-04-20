package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.literal;

public class FlyCommand {
    static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> fly = literal("fly");
        fly.requires(src -> ItsMine.permissions().hasPermission(src, PermissionUtil.Command.CLAIM_FLY, 2));
        RequiredArgumentBuilder<ServerCommandSource, Boolean> setArgument = CommandManager.argument("set", BoolArgumentType.bool());
        fly.executes((context) -> executeSetFly(context, !ClaimManager.INSTANCE.flyers.contains(context.getSource().getPlayer().getUuid())));
        setArgument.executes((context) -> executeSetFly(context, BoolArgumentType.getBool(context, "set")));
        fly.then(setArgument);
        command.then(fly);
    }

    private static int executeSetFly(CommandContext<ServerCommandSource> context, boolean set) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (set) {
            ClaimManager.INSTANCE.flyers.add(player.getUuid());
            player.sendSystemMessage(new LiteralText("Enabled Ability to fly in Claims").formatted(Formatting.GREEN));
            return 1;
        }

        player.sendSystemMessage(new LiteralText("Disabled Ability to fly in Claims").formatted(Formatting.RED));
        ClaimManager.INSTANCE.flyers.remove(player.getUuid());
        return -1;
    }
}
