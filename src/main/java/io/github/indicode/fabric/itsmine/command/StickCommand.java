package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;

public class StickCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> stick = literal("stick");
        stick.executes(context -> {
            Pair<BlockPos, BlockPos> posPair = ClaimManager.INSTANCE.stickPositions.get(context.getSource().getPlayer());
            context.getSource().sendFeedback(new LiteralText(posPair == null ? "You can now use a stick to create claims. Run this command again to disable" : "Claim stick disabled. Run this command again to enable").formatted(Formatting.DARK_PURPLE), false);
            if (posPair == null) {
                ClaimManager.INSTANCE.stickPositions.put(context.getSource().getPlayer(), new Pair<>(null, null));
            } else {
                ClaimManager.INSTANCE.stickPositions.remove(context.getSource().getPlayer());
            }
            return 0;
        });
        command.then(stick);
    }

}
