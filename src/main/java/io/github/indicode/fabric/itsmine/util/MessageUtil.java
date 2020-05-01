package io.github.indicode.fabric.itsmine.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public class MessageUtil {

    public static void sendPage(ServerCommandSource source, Text header, int entries, int page, String command, ArrayList<Text> content){

        for (int i=0; i < content.size(); i += entries) {
            System.out.println(content.subList(i, Math.min(content.size(), i + entries)));
        }

        int pages = Math.floorDiv(content.size(), entries);
        if(content.size() % entries != 0) pages++;
        if(page < 1) return;
        if(page > pages) return;
        LiteralText message = new LiteralText("");
        message.append(header).append(new LiteralText("\n\n"));
        for (int i = 0; i < content.size(); i += entries) {
            content.subList(i, Math.min(content.size(), i + entries)).forEach(text -> {
                message.append(text).append(new LiteralText("\n"));
            });
        }

        message.append(new LiteralText("\n"));
        Text button_prev = new LiteralText("")
                .append(new LiteralText("<-").formatted(Formatting.WHITE).formatted(Formatting.BOLD))
                .append(new LiteralText(" ")).append(new LiteralText("Prev").formatted(Formatting.GOLD))
                .styled((style) -> {
                    if (page - 1 >= 0) {
                        return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText((page - 1 >= 0) ? "<<<" : "|<").formatted(Formatting.GRAY))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command.replace("%page%",  String.valueOf(page - 1))));
                    }
                    return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText((page - 1 >= 0) ? "<<<" : "|<").formatted(Formatting.GRAY)));

                }).append(new LiteralText(" "));
        int finalPages = pages;
        Text button_next = new LiteralText(" ")
                .append(new LiteralText("Next").formatted(Formatting.GOLD))
                .append(new LiteralText(" ")).append(new LiteralText("->").formatted(Formatting.WHITE).formatted(Formatting.BOLD)).append(new LiteralText(" "))
                .styled((style) -> {
                    if (page + 1 <= finalPages){
                        return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText((page + 1 <= finalPages) ? ">>>" : ">|").formatted(Formatting.GRAY))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command.replace("%page%",  String.valueOf(page + 1))));
                    }
                    return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText((page + 1 <= finalPages) ? ">>>" : ">|").formatted(Formatting.GRAY)));
                });
        Text center = new LiteralText("").append(new LiteralText(String.valueOf(page)).formatted(Formatting.GREEN).append(new LiteralText("/").formatted(Formatting.GRAY)).append(new LiteralText(String.valueOf(pages)).formatted(Formatting.GREEN)));
        System.out.println(8);
        if(page > 1) message.append(button_prev);
        message.append(center);
        if(page < pages) message.append(button_next);
        System.out.println(10);
        sendText(source, message);
    }

    public static void sendText(ServerCommandSource source, LiteralText text){
        source.sendFeedback(text, false);
    }



}
