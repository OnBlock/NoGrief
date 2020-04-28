package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

public class ClaimsCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, CommandDispatcher dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> claims = literal("claims");
        claims.executes(context -> list(context.getSource(), context.getSource().getName()));
        dispatcher.register(claims);
    }

    public static int list(ServerCommandSource source, String target) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        GameProfile profile = target == null ? player.getGameProfile() : source.getMinecraftServer().getUserCache().findByName(target);

        if (profile == null) {
            source.sendError(Messages.INVALID_PLAYER);
            return -1;
        }

        List<Claim> claims = ClaimManager.INSTANCE.getPlayerClaims(profile.getId());
        if (claims.isEmpty()) {
            source.sendFeedback(new LiteralText("No Claims").formatted(Formatting.RED), false);
            return -1;
        }


        MutableText text = new LiteralText("\n").append(new LiteralText("Claims (" + target + "): ").formatted(Formatting.GOLD)).append("\n ");
        boolean nextColor = false;
        for (Claim claim : claims) {
            if(!claim.isChild) {
                MutableText cText = new LiteralText(claim.name).formatted(nextColor ? Formatting.YELLOW : Formatting.GOLD).styled((style) -> {
                    MutableText hoverText = new LiteralText("Click for more Info").formatted(Formatting.GREEN);
                    if (claim.children.size() > 0) {
                        hoverText.append("\n\nSubzones:");
                        for (Claim subzone : claim.children) {
                            hoverText.append("\n- " + subzone.name);
                        }
                    }
                    return style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/claim info " + claim.name)).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                });

                nextColor = !nextColor;
                text.append(cText.append(" "));
            }
        }

        source.sendFeedback(text.append("\n"), false);
        return 1;
    }
}
