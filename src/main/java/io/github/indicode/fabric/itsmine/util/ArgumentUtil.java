package io.github.indicode.fabric.itsmine.util;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Config;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;

public class ArgumentUtil {


    public static RequiredArgumentBuilder<ServerCommandSource, String> getSubAndClaims() {
        return argument("claim", word()).suggests(ArgumentUtil::claimSubzoneProvider);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> getClaims() {
        return argument("claim", word()).suggests(ArgumentUtil::claimProvider);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> getSubzones() {
        return argument("claim", word()).suggests(ArgumentUtil::subzoneProvider);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> getDirections() {
        return argument("direction", word()).suggests(DIRECTION_PROVIDER);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> getHelpId() {
        return argument("id", word()).suggests(HELPID_PROVIDER);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> getPlayers(){
        return argument("player", word()).suggests(PLAYERS_PROVIDER);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> getSettings(){
        return argument("setting", word()).suggests(SETTINGS_PROVIDER);
    }
    public static RequiredArgumentBuilder<ServerCommandSource, String> getPermissions(){
        return argument("permission", word()).suggests(SETTINGS_PROVIDER);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> getMessageEvent(){
        return argument("messageEvent", word()).suggests(MESSAGE_EVENTS_PROVIDER);
    }

    public static RequiredArgumentBuilder<ServerCommandSource, String> getEventMessage(){
        return argument("message", greedyString()).suggests(EVENT_MESSAGE_PROVIDER);
    }



    private static CompletableFuture<Suggestions> claimSubzoneProvider(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        List<String> names = new ArrayList<>();
        Claim current = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension);
        if (current != null) names.add(current.name);
        for (Claim claim : ClaimManager.INSTANCE.getPlayerClaims(player.getGameProfile().getId())) {
            if (claim != null) {
                names.add(claim.name);
            }
        }
        return CommandSource.suggestMatching(names, builder);
    };

    private static CompletableFuture<Suggestions> claimProvider(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        List<String> names = new ArrayList<>();
        Claim current = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension);
        if (current != null && !current.isChild) names.add(current.name);
        for (Claim claim : ClaimManager.INSTANCE.getPlayerClaims(player.getGameProfile().getId())) {
            if (claim != null && !claim.isChild) {
                names.add(claim.name);
            }
        }
        return CommandSource.suggestMatching(names, builder);
    }

    private static CompletableFuture<Suggestions> subzoneProvider(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        List<String> names = new ArrayList<>();
        Claim current = ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension);
        if (current != null && current.isChild) names.add(current.name);
        for (Claim claim : ClaimManager.INSTANCE.getPlayerClaims(player.getGameProfile().getId())) {
            if (claim != null && claim.isChild) {
                names.add(claim.name);
            }
        }
        return CommandSource.suggestMatching(names, builder);
    }


    private static final SuggestionProvider<ServerCommandSource> DIRECTION_PROVIDER = (source, builder) -> {
        List<String> strings = new ArrayList<>();
        for (Direction direction: Direction.values()) {
            if (Config.claims2d && (direction == Direction.DOWN || direction == Direction.UP)) continue;
            strings.add(direction.getName());
        };
        return CommandSource.suggestMatching(strings, builder);
    };

    public static final SuggestionProvider<ServerCommandSource> HELPID_PROVIDER = (source, builder) -> {
        List<String> strings = new ArrayList<>();
        for (Claim.HelpBook value : Claim.HelpBook.values()) {
            strings.add(value.id);
        }
        return CommandSource.suggestMatching(strings, builder);
    };

    public static CompletableFuture<Suggestions> itemsSuggestion(final CommandContext<ServerCommandSource> ctx, final SuggestionsBuilder builder) {
        List<String> strings = new ArrayList<>();
        Registry.ITEM.forEach((item) -> {
            strings.add(Registry.ITEM.getId(item).getPath());
        });

        return CommandSource.suggestMatching(strings, builder);
    }

    public static final SuggestionProvider<ServerCommandSource> PLAYERS_PROVIDER = (source, builder) -> {
        List<String> strings = new ArrayList<>();
        for (ServerPlayerEntity player : source.getSource().getMinecraftServer().getPlayerManager().getPlayerList()) {
            strings.add(player.getEntityName());
        }
        return CommandSource.suggestMatching(strings, builder);
    };

    public static final SuggestionProvider<ServerCommandSource> SETTINGS_PROVIDER = (source, builder) -> {
        List<String> strings = new ArrayList<>();
        for (Claim.ClaimSettings.Setting value : Claim.ClaimSettings.Setting.values()) {
            strings.add(value.id);
        }
        for (Claim.Permission value : Claim.Permission.values()) {
            strings.add(value.id);
        }
        return CommandSource.suggestMatching(strings, builder);
    };

    public static final SuggestionProvider<ServerCommandSource> PERMISSIONS_PROVIDER = (source, builder) -> {
        List<String> strings = new ArrayList<>();
        for (Claim.Permission value : Claim.Permission.values()) {
            strings.add(value.id);
        }
        for (Claim.Permission value : Claim.Permission.values()) {
            strings.add(value.id);
        }
        return CommandSource.suggestMatching(strings, builder);
    };

    public static final SuggestionProvider<ServerCommandSource> MESSAGE_EVENTS_PROVIDER = (source, builder) -> {
        List<String> strings = new ArrayList<>();
        for (Claim.Event value : Claim.Event.values()) {
            strings.add(value.id);
        }
        return CommandSource.suggestMatching(strings, builder);
    };
    public static final SuggestionProvider<ServerCommandSource> EVENT_MESSAGE_PROVIDER = (source, builder) -> {
        if (!builder.getRemaining().isEmpty())
            return builder.buildFuture();

        List<String> strings = new ArrayList<>();
        strings.add("reset");
        try {
            Claim claim = ClaimManager.INSTANCE.claimsByName.get(getString(source, "claim"));
            Claim.Event eventType = Claim.Event.getById(getString(source, "messageEvent"));

            if (eventType != null && claim != null) {
                String message = eventType == Claim.Event.ENTER_CLAIM ? claim.enterMessage : claim.leaveMessage;
                if (message != null) strings.add(message);
            }

        } catch (Exception ignored) {
        }

        return CommandSource.suggestMatching(strings, builder);
    };

}
