package io.github.indicode.fabric.itsmine.util;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;

public class Messenger {

    public static MutableText toText(String text) {
        return new LiteralText(ChatColor.translate(text));
    }

}
