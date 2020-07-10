package io.github.indicode.fabric.itsmine.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.concurrent.atomic.AtomicInteger;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.server.command.CommandManager.literal;

public class TrustedCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> trusted = literal("trusted");
        RequiredArgumentBuilder<ServerCommandSource, String> claimArgument = ArgumentUtil.getClaims();
        trusted.executes((context)-> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            Claim claim = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.world.getDimension());
            if (claim == null) {
                context.getSource().sendError(Messages.INVALID_CLAIM);
                return -1;
            }
            return showTrustedList(context, claim, false);
        });

        claimArgument.executes((context) -> {
            Claim claim = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
            if (claim == null) {
                context.getSource().sendError(Messages.INVALID_CLAIM);
                return -1;
            }
            return showTrustedList(context, claim, false);
        });
        trusted.then(claimArgument);
        command.then(trusted);
    }
    
    static int showTrustedList(CommandContext<ServerCommandSource> context, Claim claim, boolean showSelf) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        int mapSize = claim.permissionManager.playerPermissions.size();

        if (mapSize == 1 && !showSelf) {
            source.sendError(new LiteralText(claim.name + " is not trusting anyone!"));
            return -1;
        }

        MutableText text = new LiteralText("\n");
        text.append(new LiteralText("Trusted players for Claim ").formatted(Formatting.YELLOW))
                .append(new LiteralText(claim.name).formatted(Formatting.GOLD)).append(new LiteralText("\n"));

        AtomicInteger atomicInteger = new AtomicInteger();
        claim.permissionManager.playerPermissions.forEach((uuid, perm) -> {
            atomicInteger.incrementAndGet();
            MutableText pText = new LiteralText("");
            MutableText owner;
            GameProfile profile = source.getMinecraftServer().getUserCache().getByUuid(uuid);
            if (profile != null) {
                owner = new LiteralText(profile.getName());
            } else {
                owner = new LiteralText(uuid.toString()).styled((style) -> {
                    return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to Copy"))).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()));
                });
            }

            pText.append(new LiteralText(atomicInteger.get() + ". ").formatted(Formatting.GOLD))
                    .append(owner.formatted(Formatting.YELLOW));

            MutableText hover = new LiteralText("");
            hover.append(new LiteralText("Permissions:").formatted(Formatting.WHITE)).append(new LiteralText("\n"));

            int allowed = 0;
            int i = 0;
            boolean nextColor = false;
            MutableText perms = new LiteralText("");

            for (Claim.Permission value : Claim.Permission.values()) {
                if (claim.permissionManager.hasPermission(uuid, value)) {
                    Formatting formatting = nextColor ? Formatting.GREEN : Formatting.DARK_GREEN;
                    perms.append(new LiteralText(value.id).formatted(formatting)).append(new LiteralText(" "));
                    if (i == 3) perms.append(new LiteralText("\n"));
                    allowed++;
                    i++;
                    nextColor = !nextColor;
                }
            }

            if (allowed == Claim.Permission.values().length) {
                hover.append(new LiteralText("All " + allowed + " Permissions").formatted(Formatting.YELLOW).formatted(Formatting.ITALIC));
            } else {
                hover.append(perms);
            }

            pText.append(new LiteralText(" ")
                    .append(new LiteralText("(").formatted(Formatting.GOLD))
                    .append(new LiteralText(String.valueOf(allowed)).formatted(Formatting.GREEN))
                    .append(new LiteralText("/").formatted(Formatting.GOLD))
                    .append(new LiteralText(String.valueOf(Claim.Permission.values().length)).formatted(Formatting.YELLOW))
                    .append(new LiteralText(")").formatted(Formatting.GOLD))
            );

            pText.styled((style) -> {
                return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
            });
            text.append(pText).append(new LiteralText("\n"));
        });

        source.sendFeedback(text, false);
        return 1;
    }
}
