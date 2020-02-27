package io.github.indicode.fabric.itsmine;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.indicode.fabric.itsmine.mixin.BlockUpdatePacketMixin;
import io.github.indicode.fabric.permissions.Thimble;
import io.github.indicode.fabric.permissions.command.PermissionCommand;
import io.github.voidpointerdev.minecraft.offlineinfo.OfflineInfo;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Indigo Amann
 */
public class ClaimCommand {
    public static final SuggestionProvider DIRECTION_SUGGESTION_BUILDER = (source, builder) -> {
        for (Direction direction: Direction.values()) {
            builder.suggest(direction.getName());
        };
        return builder.buildFuture();
    };
    public static final SuggestionProvider<ServerCommandSource> CLAIM_PROVIDER = (source, builder) -> {
        List<Claim> claims = ClaimManager.INSTANCE.getPlayerClaims(source.getSource().getPlayer().getGameProfile().getId());
        List<String> names = new ArrayList<>();
        for (Claim claim : claims) {
            names.add(claim.name);
        }
        CommandSource.suggestMatching(names, builder);
        return builder.buildFuture();
    };
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("claim");
        {
            LiteralArgumentBuilder<ServerCommandSource> create = CommandManager.literal("create");
            RequiredArgumentBuilder<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.word());

            name.executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            Pair<BlockPos, BlockPos> selectedPositions = ClaimManager.INSTANCE.stickPositions.get(player);
                if (selectedPositions == null) {
                    context.getSource().sendFeedback(new LiteralText("You need to specify block positions or select them with a stick.").formatted(Formatting.RED), false);
                } else if (selectedPositions.getLeft() == null) {
                    context.getSource().sendFeedback(new LiteralText("You need to specify block positions or select position #1(Right Click) with a stick.").formatted(Formatting.RED), false);
                } else if (selectedPositions.getRight() == null) {
                    context.getSource().sendFeedback(new LiteralText("You need to specify block positions or select position #2(Left Click) with a stick.").formatted(Formatting.RED), false);
                } else {
                    String cname = StringArgumentType.getString(context, "name");
                    if (createClaim(cname, context.getSource(), selectedPositions.getLeft(), selectedPositions.getRight(), false) > 0) {
                        ClaimManager.INSTANCE.stickPositions.remove(player);
                    }
                }
                return 0;
            });

            ArgumentBuilder min = CommandManager.argument("min", BlockPosArgumentType.blockPos());
            RequiredArgumentBuilder<ServerCommandSource, PosArgument> max = CommandManager.argument("max", BlockPosArgumentType.blockPos());
            max.executes(context -> createClaim(
                    StringArgumentType.getString(context, "name"),
                    context.getSource(),
                    BlockPosArgumentType.getBlockPos(context, "min"),
                    BlockPosArgumentType.getBlockPos(context, "max"),
                    false
            ));
            min.then(max);
            name.then(min);
            create.then(name);
            command.then(create);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> stick = CommandManager.literal("stick");
            stick.executes(context -> {
                Pair<BlockPos, BlockPos> posPair = ClaimManager.INSTANCE.stickPositions.get(context.getSource().getPlayer());
                context.getSource().sendFeedback(new LiteralText(posPair == null ? "You can now use a stick to create claims. Run this command again to disable" : "Claim stick disabled. Run this command again to enable").formatted(Formatting.DARK_PURPLE), false);
                if (posPair == null) {
                    ClaimManager.INSTANCE.stickPositions.put(context.getSource().getPlayer(), new Pair<>(null, null));
                } else {
                    ClaimManager.INSTANCE.stickPositions.remove(context.getSource().getPlayer());
                }
                return 0;
            });
            command.then(stick);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> show = CommandManager.literal("show");
            show.executes(context -> showClaim(context.getSource(), ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().dimension.getType()), false));
            RequiredArgumentBuilder<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.word());
            name.suggests(CLAIM_PROVIDER);
            name.executes(context -> showClaim(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "name")), false));
            show.then(name);
            command.then(show);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> hide = CommandManager.literal("hide");
            hide.executes(context -> showClaim(context.getSource(), null, true));
            command.then(hide);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> check = CommandManager.literal("check_blocks");
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> other = CommandManager.argument("player", EntityArgumentType.player());
            other.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.checkothers", 2));
            other.executes(ctx -> checkPlayer(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player").getGameProfile().getId()));
            check.then(other);
            check.executes(ctx -> checkPlayer(ctx.getSource(), ctx.getSource().getPlayer().getGameProfile().getId()));
            command.then(check);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> delete = CommandManager.literal("destroy");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            claim.suggests(CLAIM_PROVIDER);
            LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
            confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.then(confirm);
            delete.then(claim);
            delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), false));
            command.then(delete);
        }
        {
            {
                LiteralArgumentBuilder<ServerCommandSource> expand = CommandManager.literal("expand");
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
                RequiredArgumentBuilder<ServerCommandSource, String> direction = CommandManager.argument("direction", StringArgumentType.word());
                direction.suggests(DIRECTION_SUGGESTION_BUILDER);

                direction.executes(context -> expand(
                        ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                        IntegerArgumentType.getInteger(context, "distance"),
                        directionByName(StringArgumentType.getString(context, "direction")),
                        context.getSource(),
                        false
                ));

                amount.executes(context -> expand(
                        ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                        IntegerArgumentType.getInteger(context, "distance"),
                        Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                        context.getSource(),
                        false
                ));

                amount.then(direction);
                expand.then(amount);
                command.then(expand);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> shrink = CommandManager.literal("shrink");
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
                RequiredArgumentBuilder<ServerCommandSource, String> direction = CommandManager.argument("direction", StringArgumentType.word());
                direction.suggests(DIRECTION_SUGGESTION_BUILDER);

                direction.executes(context -> expand(
                        ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                        -IntegerArgumentType.getInteger(context, "distance"),
                        directionByName(StringArgumentType.getString(context, "direction")),
                        context.getSource(),
                        false
                ));

                amount.executes(context -> expand(
                        ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                        -IntegerArgumentType.getInteger(context, "distance"),
                        Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                        context.getSource(),
                        false
                ));

                amount.then(direction);
                shrink.then(amount);
                command.then(shrink);
            }
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> delete = CommandManager.literal("destroy");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            claim.suggests(CLAIM_PROVIDER);
            LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
            confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.then(confirm);
            delete.then(claim);
            delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), false));
            command.then(delete);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> transfer = CommandManager.literal("transfer");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            claim.suggests(CLAIM_PROVIDER);
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
            LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
            confirm.executes(context -> transfer(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), EntityArgumentType.getPlayer(context, "player"), false));
            player.executes(context -> requestTransfer(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), EntityArgumentType.getPlayer(context, "player"), false));
            player.then(confirm);
            claim.then(player);
            transfer.then(claim);
            transfer.executes(context -> requestTransfer(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), EntityArgumentType.getPlayer(context, "player"), false));
            command.then(transfer);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> transfer = CommandManager.literal("accept_transfer");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            claim.suggests(CLAIM_PROVIDER);
            claim.executes(context -> acceptTransfer(context.getSource()));
            transfer.then(claim);
            command.then(transfer);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> info = CommandManager.literal("info");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            claim.suggests(CLAIM_PROVIDER);
            info.executes(context -> info(
                    context.getSource(),
                    ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType())
            ));
            claim.executes(context -> info(
                    context.getSource(),
                    ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"))
            ));
            info.then(claim);
            command.then(info);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> list = CommandManager.literal("list");
            RequiredArgumentBuilder<ServerCommandSource, String> player = CommandManager.argument("player", StringArgumentType.word());
            player.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.check_others", 2));
            player.suggests(OfflineInfo.ONLINE_PROVIDER);
            list.executes(context -> list(context.getSource(), null));
            player.executes(context -> list(context.getSource(), StringArgumentType.getString(context, "player")));
            list.then(player);
            command.then(list);
        }
        createExceptionCommand(command, false);
        {
            LiteralArgumentBuilder<ServerCommandSource> settings = CommandManager.literal("flags");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            claim.suggests(CLAIM_PROVIDER);
            for (Claim.ClaimSettings.Setting setting: Claim.ClaimSettings.Setting.values()) {
                LiteralArgumentBuilder<ServerCommandSource> arg = CommandManager.literal(setting.id);
                arg.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!claim1.permissionManager.hasPermission(context.getSource().getPlayer().getGameProfile().getId(), Claim.Permission.CHANGE_FLAGS)) {
                        context.getSource().sendFeedback(new LiteralText("You cannot change flags in this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    context.getSource().sendFeedback(new LiteralText(setting.name + " is equal to " + claim1.settings.settings.get(setting)).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                RequiredArgumentBuilder<ServerCommandSource, ?> setter = CommandManager.argument("value", BoolArgumentType.bool());
                setter.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!claim1.permissionManager.hasPermission(context.getSource().getPlayer().getGameProfile().getId(), Claim.Permission.CHANGE_FLAGS)) {
                        context.getSource().sendFeedback(new LiteralText("You cannot change flags in this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    claim1.settings.settings.put(setting, BoolArgumentType.getBool(context, "value"));
                    context.getSource().sendFeedback(new LiteralText(setting.name + " is now equal to " + BoolArgumentType.getBool(context, "value")).formatted(Formatting.GREEN), false);
                    return 0;
                });
                arg.then(setter);
                claim.then(arg);
            }
            for (Claim.Permission value : Claim.Permission.values()) {
                LiteralArgumentBuilder<ServerCommandSource> permNode = CommandManager.literal(value.id);
                RequiredArgumentBuilder<ServerCommandSource, Boolean> allow = CommandManager.argument("allow", BoolArgumentType.bool());
                allow.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!claim1.permissionManager.hasPermission(context.getSource().getPlayer().getGameProfile().getId(), Claim.Permission.CHANGE_FLAGS)) {
                        context.getSource().sendFeedback(new LiteralText("You cannot change flags in this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    boolean permission = BoolArgumentType.getBool(context, "allow");
                    claim1.permissionManager.defaults.setPermission(value, permission);
                    context.getSource().sendFeedback(new LiteralText("Players" + (permission ? " now" : " no longer") + " have the permission " + value.name).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                permNode.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!claim1.permissionManager.hasPermission(context.getSource().getPlayer().getGameProfile().getId(), Claim.Permission.CHANGE_FLAGS)) {
                        context.getSource().sendFeedback(new LiteralText("You cannot change flags in this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    boolean permission = claim1.permissionManager.defaults.hasPermission(value);
                    context.getSource().sendFeedback(new LiteralText("Players" + (permission ? " do" : " does not") + " have the permission " + value.name).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                permNode.then(allow);
                claim.then(permNode);
            }
            settings.then(claim);
            command.then(settings);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> admin = CommandManager.literal("admin");
            //admin.requires(source -> Thimble.hasPermissionChildOrOp(source, "itsmine.admin", 4));
            {
                LiteralArgumentBuilder<ServerCommandSource> add = CommandManager.literal("add_blocks");
                add.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 2));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.players());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.addClaimBlocks(EntityArgumentType.getPlayers(context, "player"), IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Gave " + IntegerArgumentType.getInteger(context, "amount") + " claim blocks").formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                add.then(player);
                admin.then(add);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> remove = CommandManager.literal("remove_blocks");
                remove.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 2));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.players());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.addClaimBlocks(EntityArgumentType.getPlayers(context, "player"), -IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Took " + IntegerArgumentType.getInteger(context, "amount") + " claim blocks").formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                remove.then(player);
                admin.then(remove);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set_blocks");
                set.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 2));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.players());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.setClaimBlocks(EntityArgumentType.getPlayers(context, "player"), IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Set claim block amount to " + IntegerArgumentType.getInteger(context, "amount")).formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                set.then(player);
                admin.then(set);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> delete = CommandManager.literal("destroy");
                delete.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify", 4));
                RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
                claim.suggests(CLAIM_PROVIDER);
                LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
                confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), true));
                claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), true));
                claim.then(confirm);
                delete.then(claim);
                delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), true));
                admin.then(delete);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> create = CommandManager.literal("create");
                create.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.infinite_claim", 4));
                ArgumentBuilder name = CommandManager.argument("name", StringArgumentType.word());
                ArgumentBuilder min = CommandManager.argument("min", BlockPosArgumentType.blockPos());
                RequiredArgumentBuilder<ServerCommandSource, PosArgument> max = CommandManager.argument("max", BlockPosArgumentType.blockPos());
                max.executes(context -> createClaim(
                        StringArgumentType.getString(context, "name"),
                        context.getSource(),
                        BlockPosArgumentType.getBlockPos(context, "min"),
                        BlockPosArgumentType.getBlockPos(context, "max"),
                        true
                ));
                min.then(max);
                name.then(min);
                create.then(name);
                admin.then(create);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> ignore = CommandManager.literal("ignore_claims");
                ignore.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.ignore_claims", 4));
                ignore.executes(context -> {
                    UUID id = context.getSource().getPlayer().getGameProfile().getId();
                    boolean isIgnoring = ClaimManager.INSTANCE.ignoringClaims.contains(id);
                    if (isIgnoring) ClaimManager.INSTANCE.ignoringClaims.remove(id);
                    else ClaimManager.INSTANCE.ignoringClaims.add(id);
                    context.getSource().sendFeedback(new LiteralText("You are " + (isIgnoring ? "no longer" : "now") + " ignoring claims.").formatted(Formatting.GREEN), false);
                    return 0;
                });
                admin.then(ignore);
            }
            {
                {
                    LiteralArgumentBuilder<ServerCommandSource> expand = CommandManager.literal("expand");
                    expand.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.infinite_claim", 4) && Thimble.hasPermissionOrOp(source, "itsmine.admin.modify", 4));
                    RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
                    RequiredArgumentBuilder<ServerCommandSource, String> direction = CommandManager.argument("direction", StringArgumentType.word());
                    direction.suggests(DIRECTION_SUGGESTION_BUILDER);

                    direction.executes(context -> expand(
                            ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                            IntegerArgumentType.getInteger(context, "distance"),
                            directionByName(StringArgumentType.getString(context, "direction")),
                            context.getSource(),
                            true
                    ));

                    amount.executes(context -> expand(
                            ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                            IntegerArgumentType.getInteger(context, "distance"),
                            Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                            context.getSource(),
                            true
                    ));

                    amount.then(direction);
                    expand.then(amount);
                    admin.then(expand);
                }
                {
                    LiteralArgumentBuilder<ServerCommandSource> shrink = CommandManager.literal("shrink");
                    shrink.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.infinite_claim", 4) && Thimble.hasPermissionOrOp(source, "itsmine.admin.modify", 4));
                    RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
                    RequiredArgumentBuilder<ServerCommandSource, String> direction = CommandManager.argument("direction", StringArgumentType.word());
                    direction.suggests(DIRECTION_SUGGESTION_BUILDER);

                    direction.executes(context -> expand(
                            ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                            -IntegerArgumentType.getInteger(context, "distance"),
                            directionByName(StringArgumentType.getString(context, "direction")),
                            context.getSource(),
                            true
                    ));

                    amount.executes(context -> expand(
                            ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                            -IntegerArgumentType.getInteger(context, "distance"),
                            Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                            context.getSource(),
                            true
                    ));

                    amount.then(direction);
                    shrink.then(amount);
                    admin.then(shrink);
                }
            }
            createExceptionCommand(admin, true);
            command.then(admin);
        }
        dispatcher.register(command);
    }

    private static void createExceptionCommand(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin) {
        LiteralArgumentBuilder<ServerCommandSource> exceptions = CommandManager.literal("permissions");
        if (admin) exceptions.requires(source -> Thimble.hasPermissionOrOp(source, "claim.admin.modify_permissions", 2));
        RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
        claim.suggests(CLAIM_PROVIDER);
        LiteralArgumentBuilder<ServerCommandSource> playerLiteral = CommandManager.literal("player");
        {
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
            LiteralArgumentBuilder<ServerCommandSource> remove = CommandManager.literal("remove");
            remove.executes(context -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                if (verifyPermission(claim1, Claim.Permission.CHANGE_PERMISSIONS, context, admin)) {
                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                    claim1.permissionManager.resetPermissions(player1.getGameProfile().getId());
                    context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + " no longer has an exception in the claim").formatted(Formatting.YELLOW), false);
                }
                return 0;
            });
            player.then(remove);
            LiteralArgumentBuilder<ServerCommandSource> all = CommandManager.literal("*");
            RequiredArgumentBuilder<ServerCommandSource, Boolean> allstate = CommandManager.argument("allow", BoolArgumentType.bool());
            allstate.executes(context -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                if (verifyPermission(claim1, Claim.Permission.CHANGE_PERMISSIONS, context, admin)) {
                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                    boolean permission = BoolArgumentType.getBool(context, "allow");
                    claim1.permissionManager.playerPermissions.put(player1.getGameProfile().getId(), permission ? new Claim.InvertedPermissionMap() : new Claim.DefaultPermissionMap());
                    context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " now" : " no longer") + " has all the permissions").formatted(Formatting.YELLOW), false);
                }
                return 0;
            });
            all.then(allstate);
            player.then(all);
            for (Claim.Permission value : Claim.Permission.values()) {
                LiteralArgumentBuilder<ServerCommandSource> permNode = CommandManager.literal(value.id);
                RequiredArgumentBuilder<ServerCommandSource, Boolean> allow = CommandManager.argument("allow", BoolArgumentType.bool());
                allow.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (verifyPermission(claim1, Claim.Permission.CHANGE_PERMISSIONS, context, admin)) {
                        ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                        boolean permission = BoolArgumentType.getBool(context, "allow");
                        modifyException(claim1, player1, value, permission);
                        context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " now" : " no longer") + " has the permission " + value.name).formatted(Formatting.YELLOW), false);
                    }
                    return 0;
                });
                permNode.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (verifyPermission(claim1, Claim.Permission.CHANGE_PERMISSIONS, context, admin)) {
                        ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                        boolean permission = hasPermission(claim1, player1, value);
                        context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " has" : " does not have") + " the permission " + value.name).formatted(Formatting.YELLOW), false);
                    }
                    return 0;
                });
                permNode.then(allow);
                player.then(permNode);
            }
            playerLiteral.then(player);
        }
        LiteralArgumentBuilder<ServerCommandSource> groupLiteral = CommandManager.literal("group");
        groupLiteral.requires(sender -> Thimble.hasPermissionOrOp(sender, "itsmine.specify_groups", 2));
        {
            RequiredArgumentBuilder<ServerCommandSource, String> group = CommandManager.argument("group", StringArgumentType.word());
            group.suggests(PermissionCommand.SUGGESTIONS_BUILDER);
            LiteralArgumentBuilder<ServerCommandSource> remove = CommandManager.literal("remove");
            remove.executes(context -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                if (verifyPermission(claim1, Claim.Permission.CHANGE_PERMISSIONS, context, admin)) {
                    String group1 = StringArgumentType.getString(context, "group");
                    verifyGroup(group1);
                    claim1.permissionManager.resetPermissions(group1);
                    context.getSource().sendFeedback(new LiteralText("Members of " + group1 + " no longer have that exception in the claim").formatted(Formatting.YELLOW), false);
                }
                return 0;
            });
            group.then(remove);
            for (Claim.Permission value : Claim.Permission.values()) {
                LiteralArgumentBuilder<ServerCommandSource> permNode = CommandManager.literal(value.id);
                RequiredArgumentBuilder<ServerCommandSource, Boolean> allow = CommandManager.argument("allow", BoolArgumentType.bool());
                allow.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (verifyPermission(claim1, Claim.Permission.CHANGE_PERMISSIONS, context, admin)) {
                        String group1 = StringArgumentType.getString(context, "group");
                        verifyGroup(group1);
                        boolean permission = BoolArgumentType.getBool(context, "allow");
                        modifyException(claim1, group1, value, permission);
                        context.getSource().sendFeedback(new LiteralText("Members of " + group1 + (permission ? " now" : " no longer") + " has the permission " + value.name).formatted(Formatting.YELLOW), false);
                    }
                    return 0;
                });
                permNode.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (verifyPermission(claim1, Claim.Permission.CHANGE_PERMISSIONS, context, admin)) {
                        String group1 = StringArgumentType.getString(context, "group");
                        verifyGroup(group1);
                        boolean permission = hasPermission(claim1, group1, value);
                        context.getSource().sendFeedback(new LiteralText("Members of " + group1 + (permission ? " now" : " do not") + " have the permission " + value.name).formatted(Formatting.YELLOW), false);
                    }
                    return 0;
                });
                permNode.then(allow);
                group.then(permNode);
            }
            groupLiteral.then(group);
        }
        claim.then(playerLiteral);
        claim.then(groupLiteral);
        exceptions.then(claim);
        command.then(exceptions);
    }
    private static boolean verifyPermission(Claim claim, Claim.Permission permission, CommandContext<ServerCommandSource> context, boolean admin) throws CommandSyntaxException {
        if (verifyExists(claim, context)) {
            if (claim.permissionManager.hasPermission(context.getSource().getPlayer().getGameProfile().getId(), permission)) {
                return true;
            } else {
                context.getSource().sendFeedback(new LiteralText(admin ? "You are modifying a claim using admin privileges" : "You cannot modify exceptions for this claim").formatted(admin ? Formatting.DARK_RED : Formatting.RED), false);
                return admin;
            }
        } else {
            return false;
        }
    }
    private static boolean verifyExists(Claim claim, CommandContext<ServerCommandSource> context) {
        if (claim == null) {
            context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return false;
        } else {
            return true;
        }
    }
    private static void verifyGroup(String permission) {
        if (Thimble.PERMISSIONS.permissionExists(permission)) {
            throw new CommandException(new LiteralText("Nonexistant permission group").formatted(Formatting.RED));
        }
    }


    private static int showClaim(ServerCommandSource source, Claim claim, boolean reset) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (!reset && ((ClaimShower)player).getShownClaim() != null && !(!Config.claims2d &&((ClaimShower)player).getShownClaim() != claim)) showClaim(source, ((ClaimShower)player).getShownClaim(), true);
        if (reset && ((ClaimShower)player).getShownClaim() != null) claim = ((ClaimShower)player).getShownClaim();
        if (claim != null) {
            if (!claim.dimension.equals(source.getWorld().getDimension().getType())) {
                if (claim == ((ClaimShower)player).getShownClaim()) ((ClaimShower)player).setShownClaim(null); // just so we dont have extra packets on this
                source.sendFeedback(new LiteralText("That claim is not in this dimension").formatted(Formatting.RED), false);
                return 0;
            }
            source.sendFeedback(new LiteralText((!reset ? "Showing" : "Hiding") + " claim: " + claim.name).formatted(Formatting.GREEN), false);
            silentHideShow(player, claim, reset, true);

        } else {
            source.sendFeedback(new LiteralText("That is not a valid claim").formatted(Formatting.RED), false);
        }
        return 0;
    }
    private static void silentHideShow(ServerPlayerEntity player, Claim claim, boolean hide, boolean updateStatus) {
        BlockState block = hide ? null : Blocks.LAPIS_BLOCK.getDefaultState();
        int showRange = 5;
        int closeShowRange = 8;
        BlockPos pos = hide ? ((ClaimShower)player).getLastShowPos() : player.getBlockPos();
        ((ClaimShower)player).setLastShowPos(pos);
        for (int x = claim.min.getX(); x <= claim.min.getX() + showRange; x++) {
            sendBlockPacket(player, new BlockPos(x, claim.min.getY(), claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.max.getY(), claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.min.getY(), claim.max.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.max.getY(), claim.max.getZ()), block);
        }
        for (int x = claim.max.getX() - showRange; x <= claim.max.getX(); x++) {
            sendBlockPacket(player, new BlockPos(x, claim.min.getY(), claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.max.getY(), claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.min.getY(), claim.max.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.max.getY(), claim.max.getZ()), block);
        }
        if (claim.includesPosition(pos)) for (int x = pos.getX() - closeShowRange; x <= pos.getX() + closeShowRange; x++) {
            if (x < claim.min.getX() || x > claim.max.getX()) continue;
            sendBlockPacket(player, new BlockPos(x, pos.getY(), claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, pos.getY(), claim.max.getZ()), block);
        }
        for (int y = claim.min.getY(); y <= claim.min.getY() + showRange; y++) {
            sendBlockPacket(player, new BlockPos(claim.min.getX(), y, claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), y, claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.min.getX(), y, claim.max.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), y, claim.max.getZ()), block);
        }
        for (int y = claim.max.getY() - showRange; y <= claim.max.getY(); y++) {
            sendBlockPacket(player, new BlockPos(claim.min.getX(), y, claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), y, claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.min.getX(), y, claim.max.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), y, claim.max.getZ()), block);
        }
        if (claim.includesPosition(pos)) for (int y = pos.getY() - closeShowRange; y <= pos.getY() + closeShowRange; y++) {
            if (y < claim.min.getY() || y > claim.max.getY()) continue;
            sendBlockPacket(player, new BlockPos(pos.getX(), y, claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), y, pos.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.min.getX(), y, pos.getZ()), block);
            sendBlockPacket(player, new BlockPos(pos.getX(), y, claim.max.getZ()), block);
        }
        for (int z = claim.min.getZ(); z <= claim.min.getZ() + showRange; z++) {
            sendBlockPacket(player, new BlockPos(claim.min.getX(), claim.min.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), claim.min.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.min.getX(), claim.max.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), claim.max.getY(), z), block);
        }
        for (int z = claim.max.getZ() - showRange; z <= claim.max.getZ(); z++) {
            sendBlockPacket(player, new BlockPos(claim.min.getX(), claim.min.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), claim.min.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.min.getX(), claim.max.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), claim.max.getY(), z), block);
        }
        if (claim.includesPosition(pos)) for (int z = pos.getZ() - closeShowRange; z <= pos.getZ() + closeShowRange; z++) {
            if (z < claim.min.getZ() || z > claim.max.getZ()) continue;
            sendBlockPacket(player, new BlockPos(claim.min.getX(), pos.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), pos.getY(), z), block);
        }
        if (updateStatus) {
            if (!hide) ((ClaimShower) player).setShownClaim(claim);
            else ((ClaimShower) player).setShownClaim(null);
        }
    }
    private static void sendBlockPacket(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        BlockUpdateS2CPacket packet =  new BlockUpdateS2CPacket(player.world, pos);
        if (state != null) ((BlockUpdatePacketMixin)packet).setState(state);
        player.networkHandler.sendPacket(packet);
    }
    private static int createClaim(String name, ServerCommandSource owner, BlockPos posA, BlockPos posB, boolean admin) throws CommandSyntaxException {
        UUID ownerID = owner.getPlayer().getGameProfile().getId();
        int x, y = 0, z, mx, my = 255, mz;
        if (posA.getX() > posB.getX()) {
            x = posB.getX();
            mx = posA.getX();
        } else {
            x =  posA.getX();
            mx = posB.getX();
        }
        if (!Config.claims2d) {
            if (posA.getY() > posB.getY()) {
                y = posB.getY();
                my = posA.getY();
            } else {
                y = posA.getY();
                my = posB.getY();
            }
        }
        if (posA.getZ() > posB.getZ()) {
            z = posB.getZ();
            mz = posA.getZ();
        } else {
            z =  posA.getZ();
            mz = posB.getZ();
        }
        BlockPos min = new BlockPos(x, y, z);
        BlockPos max = new BlockPos(mx, my, mz);
        BlockPos sub = max.subtract(min);
        sub = sub.add(1,Config.claims2d ? 0 : 1,1);
        int subInt = sub.getX() * (Config.claims2d ? 1 : sub.getY()) * sub.getZ();

        Claim claim = new Claim(name, admin ? null : ownerID, min, max, owner.getWorld().getDimension().getType());
        claim.permissionManager.playerPermissions.put(ownerID, new Claim.InvertedPermissionMap());
        if (!ClaimManager.INSTANCE.claimsByName.containsKey(name)) {
            if (!ClaimManager.INSTANCE.wouldIntersect(claim)) {
                // works because only the first statement is evaluated if true
                if ((admin && Thimble.hasPermissionOrOp(owner, "itsmine.admin.infinite_blocks", 4)) || ClaimManager.INSTANCE.useClaimBlocks(ownerID, subInt)) {
                    ClaimManager.INSTANCE.addClaim(claim);
                    owner.sendFeedback(new LiteralText("").append(new LiteralText("Your claim was created").formatted(Formatting.GREEN)).append(new LiteralText("(Area: " + sub.getX() + "x" + sub.getY() + "x" + sub.getZ() + ")").setStyle(new Style()
                            .setColor(Formatting.GREEN).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(subInt + " blocks").formatted(Formatting.YELLOW))))), false);
                    checkPlayer(owner, owner.getPlayer().getGameProfile().getId());
                    showClaim(owner, claim, false);
                    if (admin)
                        owner.getMinecraftServer().sendMessage(new LiteralText(owner.getPlayer().getGameProfile().getName() + " Has created a new claim(" + claim.name + ") using the admin command."));
                    return 1;
                } else {
                    owner.sendFeedback(new LiteralText("You don't have enough claim blocks. You have " + ClaimManager.INSTANCE.getClaimBlocks(ownerID) + ", you need " + subInt + "(" + (subInt - ClaimManager.INSTANCE.getClaimBlocks(ownerID)) + " more)").formatted(Formatting.RED), false);
                }
            } else {
                owner.sendFeedback(new LiteralText("Your claim would overlap with another claim").formatted(Formatting.RED), false);
            }
        } else {
            owner.sendFeedback(new LiteralText("The name \"" + name + "\" is already taken.").formatted(Formatting.RED), false);
        }
        return 0;
    }
    private static int checkPlayer(ServerCommandSource ret, UUID player) throws CommandSyntaxException {
        int blocks = ClaimManager.INSTANCE.getClaimBlocks(player);
        ret.sendFeedback(new LiteralText((ret.getPlayer().getGameProfile().getId().equals(player) ? "You have " : "They have ") + ClaimManager.INSTANCE.getClaimBlocks(player) + " blocks left").setStyle(new Style()
                .setColor(Formatting.YELLOW).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Area of " + ItsMine.blocksToAreaString(ClaimManager.INSTANCE.getClaimBlocks(player))).formatted(Formatting.YELLOW)))), false);
        return 0;
    }
    private static int requestDelete(ServerCommandSource sender, Claim claim, boolean admin) throws CommandSyntaxException {
        if (claim == null) {
            sender.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.permissionManager.hasPermission(sender.getPlayer().getGameProfile().getId(), Claim.Permission.DELETE_CLAIM)) {
            if (admin && Thimble.hasPermissionOrOp(sender, "itsmine.admin.modify", 4)) {
                sender.sendFeedback(new LiteralText("WARNING: This is not your claim...").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("You cannot delete that claim").formatted(Formatting.RED), false);
                return 0;
            }
        }
        sender.sendFeedback(new LiteralText("").append(new LiteralText("Are you sure you want to delete the claim \"" + claim.name + "\"? ").formatted(Formatting.GOLD))
                .append(new LiteralText("[I'M SURE]").setStyle(new Style()
                        .setColor(Formatting.DARK_RED)
                        .setBold(true)
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, (admin ? "/claim admin" : "/claim") + " destroy " + claim.name + " confirm")))), false);
        return 0;
    }
    private static int delete(ServerCommandSource sender, Claim claim, boolean admin) throws CommandSyntaxException {
        if (claim == null) {
            sender.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.permissionManager.hasPermission(sender.getPlayer().getGameProfile().getId(), Claim.Permission.DELETE_CLAIM)) {
            if (admin && Thimble.hasPermissionOrOp(sender, "itsmine.admin.modify", 4)) {
                sender.sendFeedback(new LiteralText("Deleting a claim belonging to somebody else").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("You cannot delete that claim").formatted(Formatting.RED), false);
                return 0;
            }
        }
        ClaimManager.INSTANCE.releaseBlocksToOwner(claim);
        ClaimManager.INSTANCE.claimsByName.remove(claim.name);
        sender.getWorld().getPlayers().forEach(playerEntity -> {
            if (((ClaimShower)playerEntity).getShownClaim() != null && ((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, true, true);
        });
        sender.sendFeedback(new LiteralText("Deleted the claim \"" + claim.name + "\"").formatted(Formatting.GREEN), !claim.permissionManager.hasPermission(sender.getPlayer().getGameProfile().getId(), Claim.Permission.DELETE_CLAIM));
        return 0;
    }
    private static int requestTransfer(ServerCommandSource sender, Claim claim, ServerPlayerEntity player, boolean admin) throws CommandSyntaxException {
        if (claim == null) {
            sender.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.claimBlockOwner.equals(sender.getPlayer().getGameProfile().getId())) {
            if (admin && Thimble.hasPermissionOrOp(sender, "itsmine.admin.modify", 4)) {
                sender.sendFeedback(new LiteralText("WARNING: This is not your claim...").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("You cannot transfer ownership that claim").formatted(Formatting.RED), false);
                return 0;
            }
        }
        sender.sendFeedback(new LiteralText("").append(new LiteralText("Are you sure you want to transfer ownership of \"" + claim.name + "\" to " + player.getGameProfile().getName() + "? ").formatted(Formatting.GOLD))
                .append(new LiteralText("[I'M SURE]").setStyle(new Style()
                        .setColor(Formatting.DARK_RED)
                        .setBold(true)
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, (admin ? "/claim admin" : "/claim") + " transfer " + claim.name + " " + player.getGameProfile().getName() + " confirm")))), false);
        return 0;
    }
    private static Map<UUID, String> pendingClaimTransfers = new HashMap<>();
    private static int transfer(ServerCommandSource sender, Claim claim, ServerPlayerEntity player, boolean admin) throws CommandSyntaxException {
        if (claim == null) {
            sender.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.claimBlockOwner.equals(sender.getPlayer().getGameProfile().getId())) {
            if (admin && Thimble.hasPermissionOrOp(sender, "itsmine.admin.modify", 4)) {
                sender.sendFeedback(new LiteralText("Transfering ownership of a claim belonging to somebody else").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("You cannot transfer ownership that claim").formatted(Formatting.RED), false);
                return 0;
            }
        }
        sender.sendFeedback(new LiteralText("Transferring ownership of the claim \"" + claim.name + "\" to " + player.getGameProfile().getName() + " if they accept").formatted(Formatting.GREEN), claim.claimBlockOwner != player.getGameProfile().getId());
        player.sendMessage(new LiteralText("").append(new LiteralText("Do you want to accept ownership of the claim \"" + claim.name + "\" from " + OfflineInfo.getNameById(sender.getWorld().getServer().getUserCache(), claim.claimBlockOwner) + "? ").formatted(Formatting.GOLD))
                .append(new LiteralText("[ACCEPT OWNERSHIP]").setStyle(new Style()
                        .setColor(Formatting.GREEN)
                        .setBold(true)
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim accept_transfer " + claim.name)))));
        pendingClaimTransfers.put(player.getGameProfile().getId(), claim.name);
        return 0;
    }
    public static int acceptTransfer(ServerCommandSource sender) throws CommandSyntaxException {
        Claim claim = ClaimManager.INSTANCE.claimsByName.get(pendingClaimTransfers.get(sender.getPlayer().getGameProfile().getId()));
        if (claim == null) {
            sender.sendFeedback(new LiteralText("You have no pending claim transfers").formatted(Formatting.RED), false);
            return 0;
        }
        if (sender.getMinecraftServer().getPlayerManager().getPlayer(claim.claimBlockOwner) != null) {
            sender.getMinecraftServer().getPlayerManager().getPlayer(claim.claimBlockOwner).sendMessage(new LiteralText("").append(new LiteralText(sender.getPlayer().getGameProfile().getName() + " has taken ownership of the claim \"" + claim.name + "\"").formatted(Formatting.YELLOW)));
        }
        Claim.ClaimPermissionMap op = claim.permissionManager.playerPermissions.get(claim.claimBlockOwner);
        claim.permissionManager.playerPermissions.put(claim.claimBlockOwner, claim.permissionManager.playerPermissions.get(sender.getPlayer().getGameProfile().getId()));
        claim.permissionManager.playerPermissions.put(sender.getPlayer().getGameProfile().getId(), op);
        claim.claimBlockOwner = sender.getPlayer().getGameProfile().getId();
        return 0;
    }
    private static int showClaimInfo(ServerCommandSource sender, Claim claim) {
        sender.sendFeedback(new LiteralText("Claim Name: " + claim.name), false);
        //sender.sendFeedback(new LiteralText("Owner")); // how to do this...
        return 0;
    }
    private static int modifyException(Claim claim, ServerPlayerEntity exception, Claim.Permission permission, boolean allowed) {
        claim.permissionManager.setPermission(exception.getGameProfile().getId(), permission, allowed);
        return 0;
    }
    private static int modifyException(Claim claim, String exception, Claim.Permission permission, boolean allowed) {
        claim.permissionManager.setPermission(exception, permission, allowed);
        return 0;
    }
    private static boolean hasPermission(Claim claim, ServerPlayerEntity exception, Claim.Permission permission) {
        return claim.permissionManager.hasPermission(exception.getGameProfile().getId(), permission);
    }
    private static boolean hasPermission(Claim claim, String exception, Claim.Permission permission) {
        return claim.permissionManager.hasPermission(exception, permission);
    }
    private static Direction directionByName(String name) {
        for (Direction direction : Direction.values()) {
            if (name.equals(direction.getName())) return direction;
        }
        return null;
    }
    private static int expand(Claim claim, int amount, Direction direction, ServerCommandSource source, boolean admin) throws CommandSyntaxException {
        UUID ownerID = source.getPlayer().getGameProfile().getId();
        if (claim == null) {
            source.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (direction == null) {
            source.sendFeedback(new LiteralText("That is not a valid direction").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.permissionManager.hasPermission(ownerID, Claim.Permission.MODIFY_SIZE)) {
            source.sendFeedback(new LiteralText("You do not have border change permissions in that claim").formatted(Formatting.RED), false);
            if (!admin) return 0;
        }
        int oldArea = claim.getArea();
        if (amount > 0) claim.expand(direction, amount);
        else {
            if (!claim.shrink(direction, amount)) {
                source.sendFeedback(new LiteralText("You can't shrink your claim that far. It would pass its opposite wall.").formatted(Formatting.RED), false);
                return 0;
            }
        }
        if (ClaimManager.INSTANCE.wouldIntersect(claim)) {
            if (amount < 0) claim.expand(direction, amount);
            else claim.shrink(direction, amount);
            source.sendFeedback(new LiteralText("Expansion would result in hitting another claim").formatted(Formatting.RED), false);
            return 0;
        }
        int newArea = claim.getArea() - oldArea;
        if (!admin && claim.claimBlockOwner != null && ClaimManager.INSTANCE.getClaimBlocks(ownerID) < newArea) {
            if (amount < 0) claim.expand(direction, amount);
            else claim.shrink(direction, amount);
            source.sendFeedback(new LiteralText("You don't have enough claim blocks. You have " + ClaimManager.INSTANCE.getClaimBlocks(ownerID) + ", you need " + newArea + "(" + (newArea - ClaimManager.INSTANCE.getClaimBlocks(ownerID)) + " more)").formatted(Formatting.RED), false);
            checkPlayer(source, ownerID);
            return 0;
        } else {
            if (!admin && claim.claimBlockOwner != null) ClaimManager.INSTANCE.useClaimBlocks(ownerID, newArea);
            source.sendFeedback(new LiteralText("Your claim was " + (amount > 0 ? "expanded" : "shrunk") + " by " + (amount < 0 ? -amount : amount) + " blocks " + direction.getName()).formatted(Formatting.GREEN), false);
            checkPlayer(source, ownerID);
            if (amount < 0) claim.expand(direction, amount);
            else claim.shrink(direction, amount);
            source.getWorld().getPlayers().forEach(playerEntity -> {
                if (((ClaimShower)playerEntity).getShownClaim() != null && ((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, true, false);
            });
            if (amount > 0) claim.expand(direction, amount);
            else claim.shrink(direction, -amount);
            source.getWorld().getPlayers().forEach(playerEntity -> {
                if (((ClaimShower)playerEntity).getShownClaim() != null && ((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, false, false);
            });
        }
        return 0;
    }
    private static int info(ServerCommandSource source, Claim claim) {
        if (claim == null) {
            source.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        source.sendFeedback(new LiteralText("").append(new LiteralText("Claim Name: ").formatted(Formatting.YELLOW)).append(new LiteralText(claim.name).formatted(Formatting.GOLD)), false);
        GameProfile owner = claim.claimBlockOwner == null ? null : source.getMinecraftServer().getUserCache().getByUuid(claim.claimBlockOwner);
        source.sendFeedback(new LiteralText("").append(new LiteralText("Claim Owner: ").formatted(Formatting.YELLOW)).append(new LiteralText(owner == null ? "No owner" : owner.getName()).formatted(Formatting.GOLD)), false);
        BlockPos size = claim.getSize();
        source.sendFeedback(new LiteralText("").append(new LiteralText("Claim Size: ").formatted(Formatting.YELLOW)).append(new LiteralText(size.getX() + (claim.is2d() ? "x" : ("x" + size.getY() + "x")) + size.getZ()).formatted(Formatting.GOLD)), false);
        source.sendFeedback(new LiteralText("").append(new LiteralText("Start position: ").formatted(Formatting.YELLOW)).append(new LiteralText("X:" + claim.min.getX() + (claim.is2d() ? "" : " Y:" + claim.min.getY()) + " Z:" + claim.min.getZ()).formatted(Formatting.GOLD)), false);
        source.sendFeedback(new LiteralText("").append(new LiteralText("End position: ").formatted(Formatting.YELLOW)).append(new LiteralText("X:" + claim.max.getX() + (claim.is2d() ? "" : " Y:" + claim.max.getY()) + " Z:" + claim.max.getZ()).formatted(Formatting.GOLD)), false);
        return 0;
    }
    private static int list(ServerCommandSource source, String player) {
        source.sendFeedback(new LiteralText(player == null ? "Your Claims:" : player + "'s Claims:").formatted(Formatting.YELLOW), false);
        try {
                UUID id = player == null ? source.getPlayer().getGameProfile().getId() : OfflineInfo.getIdByName(source.getMinecraftServer().getUserCache(), player);
                List<Claim> claims = ClaimManager.INSTANCE.getPlayerClaims(id);
                if (claims.isEmpty()) {
                    source.sendFeedback(new LiteralText("None").formatted(Formatting.YELLOW), false);
                    return 0;
                }
                LiteralText feedback = new LiteralText("");
                for (int i = 0; i < claims.size(); i++) {
                    feedback.append(new LiteralText(claims.get(i).name).setStyle(
                            new Style().setColor(Formatting.GOLD)
                            .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim info " + claims.get(i).name))
                    ));
                    if (i < claims.size() - 1) {
                        feedback.append(new LiteralText(", ").formatted(Formatting.YELLOW));
                    }
                }
                source.sendFeedback(feedback, false);
        } catch (CommandSyntaxException e) {
            source.sendFeedback(new LiteralText("No player is specified").formatted(Formatting.RED), false);
        }
        return 0;
    }
}