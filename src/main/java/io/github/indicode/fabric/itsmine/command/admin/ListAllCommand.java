package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.Claim;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getPlayers;
import static io.github.indicode.fabric.itsmine.util.ClaimUtil.getClaims;
import static net.minecraft.server.command.CommandManager.literal;

public class ListAllCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> listall = literal("listall");
        RequiredArgumentBuilder<ServerCommandSource, String> player = getPlayers();
        listall.executes(context -> listall(context.getSource()));
        command.then(listall);
    }
    private static int listall(ServerCommandSource source){
        List<Claim> claims = getClaims();
        Text text = new LiteralText("\n").append(new LiteralText("Claims").formatted(Formatting.GOLD)).append("\n ");
        for (Claim claim : claims) {
            Text cText = new LiteralText(claim.name).formatted(Formatting.GOLD);
            text.append(cText.append(" "));
        }
        source.sendFeedback(text.append("\n"), false);
        return 1;
    }



}
