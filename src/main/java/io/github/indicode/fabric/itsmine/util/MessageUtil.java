package io.github.indicode.fabric.itsmine.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public class MessageUtil {

    public static void sendPage(ServerCommandSource source, Text header, int entries, int page, String command, ArrayList<Text> content){
        System.out.println(1);
        //1
        //10 max entries
        //3 entries
        int pages = Math.floorDiv(content.size(), entries);
        if(content.size() % entries != 0) pages++;
        System.out.println("pages: " + pages);
        if(page < 1) return;
        System.out.println(2);
        if(page > pages) return;
        System.out.println(3);
        LiteralText message = new LiteralText("");
        System.out.println(4);
        message.append(header).append(new LiteralText("\n\n"));
        System.out.println(5);
        for(int i = (entries * (page - 1) + 1); i < (page * entries); i++) {
            System.out.println("i: " + i);
            message.append(content.get(i-1)).append(new LiteralText("\n"));
        }
        System.out.println(6);
        message.append(new LiteralText("\n"));
        System.out.println(7);
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
