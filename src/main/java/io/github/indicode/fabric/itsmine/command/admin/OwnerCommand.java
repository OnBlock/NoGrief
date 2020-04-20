package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import net.minecraft.command.arguments.GameProfileArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.Collection;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class OwnerCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        {
            LiteralArgumentBuilder<ServerCommandSource> set = literal("setOwner");
            RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> newOwner = argument("newOwner", GameProfileArgumentType.gameProfile());
            RequiredArgumentBuilder<ServerCommandSource, String> claimArgument = ArgumentUtil.getClaims();

            newOwner.executes((context) -> {
                Claim claim = ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getPlayer().dimension);
                if (claim == null) {
                    context.getSource().sendError(Messages.INVALID_CLAIM);
                    return -1;
                }

                Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "newOwner");

                if (profiles.size() > 1) {
                    context.getSource().sendError(Messages.TOO_MANY_SELECTIONS);
                    return -1;
                }
                return setOwner(context.getSource(), claim, profiles.iterator().next());
            });

            claimArgument.executes((context) -> {
                Claim claim = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                if (claim == null) {
                    context.getSource().sendError(Messages.INVALID_CLAIM);
                    return -1;
                }

                Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "newOwner");

                if (profiles.size() > 1) {
                    context.getSource().sendError(Messages.TOO_MANY_SELECTIONS);
                    return -1;
                }
                return setOwner(context.getSource(), claim, profiles.iterator().next());
            });

            newOwner.then(claimArgument);
            set.then(newOwner);
            command.then(set);
        }

        {
            LiteralArgumentBuilder<ServerCommandSource> set = literal("setOwnerName");
            RequiredArgumentBuilder<ServerCommandSource, String> nameArgument = argument("newName", word());
            RequiredArgumentBuilder<ServerCommandSource, String> claimArgument = ArgumentUtil.getClaims();

            nameArgument.executes((context) -> {
                Claim claim = ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getPlayer().dimension);
                if (claim == null) {
                    context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                    return -1;
                }
                return setOwnerName(context.getSource(), claim, getString(context, "newName"));
            });

            claimArgument.executes((context) -> {
                Claim claim = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
                if (claim == null) {
                    context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                    return -1;
                }
                return setOwnerName(context.getSource(), claim, getString(context, "newName"));
            });

            nameArgument.then(claimArgument);
            set.then(nameArgument);
            command.then(set);
        }
    }
    private static int setOwnerName(ServerCommandSource source, Claim claim, String input) {
        String name = input.equals("reset") ? null : input;
        source.sendFeedback(new LiteralText("Set the Custom Owner Name to ")
                        .formatted(Formatting.YELLOW).append(new LiteralText(name == null ? "Reset" : name).formatted(Formatting.GOLD)).append(new LiteralText(" from "))
                        .append(new LiteralText(claim.customOwnerName == null ? "Not Present" : claim.customOwnerName).formatted(Formatting.GOLD))
                        .append(new LiteralText(" for ").formatted(Formatting.YELLOW)).append(new LiteralText(claim.name).formatted(Formatting.GOLD))
                , false);
        claim.customOwnerName = input;
        return 1;
    }
    private static int setOwner(ServerCommandSource source, Claim claim, GameProfile profile) {
        GameProfile oldOwner = source.getMinecraftServer().getUserCache().getByUuid(claim.claimBlockOwner);
        source.sendFeedback(new LiteralText("Set the Claim Owner to ")
                        .formatted(Formatting.YELLOW).append(new LiteralText(profile.getName()).formatted(Formatting.GOLD)).append(new LiteralText(" from "))
                        .append(new LiteralText(oldOwner == null ? "(" + claim.claimBlockOwner + ")" : oldOwner.getName()).formatted(Formatting.GOLD))
                        .append(new LiteralText(" for ").formatted(Formatting.YELLOW)).append(new LiteralText(claim.name).formatted(Formatting.GOLD))
                , false);
        claim.claimBlockOwner = profile.getId();
        return 1;
    }
}
