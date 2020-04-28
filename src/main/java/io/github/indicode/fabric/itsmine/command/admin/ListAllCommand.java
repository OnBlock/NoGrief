package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.util.MessageUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

import static io.github.indicode.fabric.itsmine.util.ClaimUtil.getClaims;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ListAllCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> listall = literal("listall");
        RequiredArgumentBuilder<ServerCommandSource, Integer> page = argument("page", IntegerArgumentType.integer(1));
        RequiredArgumentBuilder<ServerCommandSource, Integer> entries = argument("entries", IntegerArgumentType.integer(1, 100));
        RequiredArgumentBuilder<ServerCommandSource, Boolean> offline = argument("offline", BoolArgumentType.bool());
        entries.executes(context -> listall(context.getSource(), IntegerArgumentType.getInteger(context, "page"), IntegerArgumentType.getInteger(context, "entries"), BoolArgumentType.getBool(context, "offline")));
        page.executes(context -> listall(context.getSource(), IntegerArgumentType.getInteger(context, "page"), 10, BoolArgumentType.getBool(context, "offline")));
        listall.executes(context -> listall(context.getSource(), 1, 10, true));
        offline.executes(context -> listall(context.getSource(), 1, 10, BoolArgumentType.getBool(context, "offline")));
        page.then(entries);
        offline.then(page);
        listall.then(offline);
        command.then(listall);
    }
    private static int listall(ServerCommandSource source, int page, int entries, boolean offline){
        List<Claim> claims = getClaims();
        Text title = new LiteralText("Claim List:").formatted(Formatting.AQUA).formatted(Formatting.UNDERLINE);
        ArrayList<Text> claimList = new ArrayList<>();
        if(!offline){
            for(ServerPlayerEntity serverPlayerEntity : source.getMinecraftServer().getPlayerManager().getPlayerList()){
                for(Claim claim : ClaimManager.INSTANCE.getPlayerClaims(serverPlayerEntity.getUuid())){
                    claimList.add(new LiteralText(claim.name).formatted(Formatting.GOLD).append(new LiteralText(" ")).append(new LiteralText(String.valueOf(claim.getEntities(source.getWorld()))).formatted(Formatting.YELLOW)));
                }
            }
        } else {
            for (Claim claim : claims) {
                claimList.add(new LiteralText(claim.name).formatted(Formatting.GOLD).append(new LiteralText(" ")));
            }
        }
        MessageUtil.sendPage(source, title, entries, page, "/claim admin listall " + offline + " %page% " + entries, claimList);
        return 1;
        }



}
