package io.github.indicode.fabric.itsmine;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Messages {
    public static final Text PREFIX = new LiteralText(ChatColor.translate(Config.prefix));

    public static final Text INVALID_CLAIM = PREFIX.copy().append(new LiteralText("Can not find a claim with that name or a claim at your position").formatted(Formatting.RED));

    public static final Text INVALID_SETTING = PREFIX.copy().append(new LiteralText("Invalid Claim Setting!").formatted(Formatting.RED));

    public static final Text NO_PERMISSION = PREFIX.copy().append(new LiteralText(ChatColor.translate(Config.msg_no_perm)));

    public static final Text INVALID_MESSAGE_EVENT = PREFIX.copy().append(new LiteralText("Invalid Message Event!"));

    public static final Text INVALID_PLAYER = PREFIX.copy().append(new LiteralText("Can not find a Player with that Name!"));

    public static final Text TOO_MANY_SELECTIONS = PREFIX.copy().append(new LiteralText("Only one selection is allowed!"));

    public static final Text MSG_PLACE_BLOCK = PREFIX.copy().append(new LiteralText(ChatColor.translate(Config.msg_place_block)).formatted(Formatting.RED));

    public static final Text MSG_BREAK_BLOCK = PREFIX.copy().append(new LiteralText(ChatColor.translate(Config.msg_break_block)).formatted(Formatting.RED));

    public static final Text MSG_CANT_ENTER = PREFIX.copy().append(new LiteralText(ChatColor.translate(Config.msg_cant_enter)).formatted(Formatting.RED));

    public static final Text MSG_INTERACT_ENTITY = PREFIX.copy().append(new LiteralText(ChatColor.translate(Config.msg_interact_entity)).formatted(Formatting.RED));

    public static final Text MSG_INTERACT_BLOCK = PREFIX.copy().append(new LiteralText(ChatColor.translate(Config.msg_interact_block)).formatted(Formatting.RED));

    public static final Text MSG_OPEN_CONTAINER = PREFIX.copy().append(new LiteralText(ChatColor.translate(Config.msg_open_container)).formatted(Formatting.RED));

    public static final Text MSG_DAMAGE_ENTITY = PREFIX.copy().append(new LiteralText(ChatColor.translate(Config.msg_attack_entity)).formatted(Formatting.RED));

    public static final Text[] GET_STARTED = new Text[]{
            header("How to Claim (Basics)")
                    .append(line(1, "Type &6/claim stick&e then Left click with a stick on a block to set the &6first&e corner of your claim"))
                    .append(line(2, "Right click to set the other corner"))
                    .append(line(3, "Type &6/claim create <name> &e to create your claim!"))
                    .append(line(4, "To trust a player in your claim type &6/claim trust <player>"))
                    .append(line(5, "To untrust a player in your claim type &6/claim distrust <player>")),
            header("How to Claim (Settings)")
                    .append(line("Settings allow you to change some properties of your claim, they are basically global permissions").formatted(Formatting.LIGHT_PURPLE))
                    .append(line(1, "To change a setting, type ").append(text("/claim settings <setting> [true | false]").formatted(Formatting.GOLD)))
                    .append(line(2, "To check a setting, type ").append(text("/claim settings <setting>").formatted(Formatting.GOLD)))
                    .append(line(3, "To see a list of settings, type ").append(text("/claim settings").formatted(Formatting.GOLD))),
            header("How to Claim (Player Permissions)")
                    .append(line("You can set different permissions for each trusted player!").formatted(Formatting.LIGHT_PURPLE))
                    .append(line(1, "To set a permission, type ").append(text("/claim permissions <claim> player <player> <permission> [true | false]").formatted(Formatting.GOLD)))
                    .append(line(2, "To check someone's permission, type ").append(text("/claim permissions <claim> player <player> <permission>").formatted(Formatting.GOLD)))
                    .append(line(3, "To see a list of trusted players, type ").append(text("/claim trusted").formatted(Formatting.GOLD))),
            header("How to resize claim")
                    .append(line("You can always change the size of your claim if you aren't happy with it!").formatted(Formatting.LIGHT_PURPLE))
                    .append(line(1, "To expand your claim in a direction, type ").append(text("/claim expand <distance>").formatted(Formatting.GOLD)))
                    .append(line(2, "If you want to specify a direction, you can type ").append(text("/claim expand <distance> <direction>").formatted(Formatting.GOLD)))
                    .append(line(3, "To shrink a claim you do the same thing but replace \"expand\" with \"shrink\""))
    };

    public static final Text[] HELP = new Text[]{
            header("Arguments")
                    .append(line("&6create ").append(text("&eCreates a claim")))
                    .append(line("&6blocks ").append(text("&eShows how many blocks you have left")))
                    .append(line("&6trust ").append(text("&eLets you trust a player in your claim")))
                    .append(line("&6distrust ").append(text("&eLets you distrust a player in your claim")))
                    .append(line("&6trusted ").append(text("&eShows you a list of all the trusted players")))
                    .append(line("&6expand ").append(text("&eLets you expand your claim"))),

            header("Arguments")
                    .append(line("&6shrink ").append(text("&eLets you shrink your claim")))
                    .append(line("&6help ").append(text("&eShows this list")))
                    .append(line("&6show ").append(text("&eShows the borders of your claim")))
                    .append(line("&6hide ").append(text("&eHides the borders of your claim")))
                    .append(line("&6info ").append(text("&eShows you some information about a claim")))
                    .append(line("&6list ").append(text("&eShows you the list of your claims"))),

            header("Arguments")
                    .append(line("&6permissions ").append(text("&eLets you modify the permissions of players and add exceptions")))
                    .append(line("&6remove ").append(text("&eRemoves your claim")))
                    .append(line("&6rename ").append(text("&eLets you rename your claim")))
                    .append(line("&6settings ").append(text("&eLets you modify the global permissions and setting of your claim")))
                    .append(line("&6message ").append(text("&eLets you modify the different message events")))
                    .append(line("&6stick ").append(text("&eEnables/disables the claim stick for marking the positions"))),

            header("Arguments")
                    .append(line("&6transfer ").append(text("&eLets you transfer the ownership of a claim")))
                    .append(line("&6message ").append(text("&eLets you modify the different message events")))
    };

    public static final Text[] SETTINGS = new Text[]{
            header("&6build ").append(text(""))
    };

    private static Text header(String title) {
        return new LiteralText("").append(new LiteralText(title + ":").formatted(Formatting.AQUA, Formatting.UNDERLINE)).formatted(Formatting.WHITE).append("\n");
    }

    private static Text line(int num, String string) {
        return line("&e" + num + ".&e " + string);
    }

    private static Text line(String string) {
        return new LiteralText("\n").append(ChatColor.translate(string)).formatted(Formatting.YELLOW);
    }

    private static Text text(String text) {
        return new LiteralText(ChatColor.translate(text));
    }

    public static class Command {
        public static Text getSettings(Claim claim) {
            Text claimSettings = new LiteralText("");
            boolean nextEnabled = false;
            boolean nextDisabled = false;
            for (Claim.ClaimSettings.Setting value : Claim.ClaimSettings.Setting.values()) {
                boolean enabled = claim.settings.getSetting(value);
                Formatting formatting;
                if (enabled) {
                    if (nextEnabled) formatting = Formatting.GREEN;
                    else formatting = Formatting.DARK_GREEN;
                    nextEnabled = !nextEnabled;
                } else {
                    if (nextDisabled) formatting = Formatting.RED;
                    else formatting = Formatting.DARK_RED;
                    nextDisabled = !nextDisabled;
                }

                claimSettings.append(" ").append(new LiteralText(value.id).formatted(formatting));
            }

            return claimSettings;
        }

    }
}
