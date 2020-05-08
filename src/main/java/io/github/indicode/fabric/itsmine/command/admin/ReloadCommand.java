package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.ItsMineConfig;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.time.StopWatch;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class ReloadCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> reload = LiteralArgumentBuilder.literal("reload");
        reload.executes(ReloadCommand::execute);
        command.then(reload);
    }


    public static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        StopWatch watch = new StopWatch();
        watch.start();
        ItsMineConfig.reload();
        watch.stop();
        String timeElapsed = new DecimalFormat("##.##").format(watch.getTime(TimeUnit.MICROSECONDS));
        context.getSource().getPlayer().sendSystemMessage(new LiteralText("Reloaded! Took " + timeElapsed + "μs").formatted(Formatting.YELLOW));
        return 1;
    }
}
