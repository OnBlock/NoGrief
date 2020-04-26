package io.github.indicode.fabric.itsmine.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public class MessageUtil {

    public static void sendPage(ServerCommandSource source, Text header, int entrys, int page, String command, ArrayList<Text> content){
        int pages = Math.floorDiv(content.size(), entrys);
        if(content.size() % entrys != 0) pages++;
        if(page < 1) return;
        if(page > pages) return;
        LiteralText message = new LiteralText("");
        message.append(header).append(new LiteralText("\n\n"));

        for(int i = (entrys * (page - 1) + 1); i <= page * entrys; i++) message.append(content.get(i-1)).append(new LiteralText("\n"));
        message.append(new LiteralText("\n"));
        Text button_prev = new LiteralText("")
                .append(new LiteralText("<-").formatted(Formatting.WHITE).formatted(Formatting.BOLD))
                .append(new LiteralText(" ")).append(new LiteralText("Prev").formatted(Formatting.GOLD))
                .styled((style) -> {
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText((page - 1 >= 0) ? "<<<" : "|<").formatted(Formatting.GRAY)));
                    if (page - 1 >= 0) {
                        style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command.replace("%page%",  String.valueOf(page - 1))));
                    }
                    return style;
                }).append(new LiteralText(" "));
        int finalPages = pages;
        Text button_next = new LiteralText(" ")
                .append(new LiteralText("Next").formatted(Formatting.GOLD))
                .append(new LiteralText(" ")).append(new LiteralText("->").formatted(Formatting.WHITE).formatted(Formatting.BOLD)).append(new LiteralText(" "))
                .styled((style) -> {
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText((page + 1 <= finalPages) ? ">>>" : ">|").formatted(Formatting.GRAY)));
                    if (page + 1 <= finalPages)
                        style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command.replace("%page%",  String.valueOf(page + 1))));
                    return style;
                });
        Text center = new LiteralText("").append(new LiteralText(String.valueOf(page)).formatted(Formatting.GREEN).append(new LiteralText("/").formatted(Formatting.GRAY)).append(new LiteralText(String.valueOf(pages)).formatted(Formatting.GREEN)));
        if(page > 1) message.append(button_prev);
        message.append(center);
        if(page < pages) message.append(button_next);
        sendText(source, message);
    }

    public static void sendText(ServerCommandSource source, LiteralText text){
        source.sendFeedback(text, false);
    }



}
