package io.github.indicode.fabric.itsmine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.mixin.BlockUpdatePacketMixin;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.packet.BlockUpdateS2CPacket;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class ClaimCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("claim");
        {
            LiteralArgumentBuilder<ServerCommandSource> create = CommandManager.literal("create");
            ArgumentBuilder name = CommandManager.argument("name", StringArgumentType.word());
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
            LiteralArgumentBuilder<ServerCommandSource> show = CommandManager.literal("show");
            show.executes(context -> showClaim(context.getSource(), ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().dimension.getType())));
            RequiredArgumentBuilder<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.word());
            name.executes(context -> showClaim(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "name"))));
            show.then(name);
            command.then(show);
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
            LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
            confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.then(confirm);
            delete.then(claim);
            delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), false));
            command.then(delete);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> info = CommandManager.literal("info");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());

            //TODO
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> exceptions = CommandManager.literal("exceptions");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
            LiteralArgumentBuilder<ServerCommandSource> remove = CommandManager.literal("remove");
            remove.executes(context -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                if (claim1 == null) {
                    context.getSource().sendFeedback(new LiteralText("That claim does not exist.").formatted(Formatting.RED), false);
                    return 0;
                }
                if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                    context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim.").formatted(Formatting.RED), false);
                    return 0;
                }
                ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                claim1.permssionsMap.remove(player1.getGameProfile().getId());
                context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + " no longer has an exception in the claim").formatted(Formatting.YELLOW), false);
                return 0;
            });
            player.then(remove);
            for (Claim.ClaimPermissions.Permission value : Claim.ClaimPermissions.Permission.values()) {
                LiteralArgumentBuilder<ServerCommandSource> permNode = CommandManager.literal(value.id);
                RequiredArgumentBuilder<ServerCommandSource, Boolean> allow = CommandManager.argument("allow", BoolArgumentType.bool());
                allow.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist.").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                        context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim.").formatted(Formatting.RED), false);
                        return 0;
                    }
                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                    boolean permission = BoolArgumentType.getBool(context, "allow");
                    modifyException(claim1, player1, value, permission);
                    context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " now" : " no longer") + " has the permission " + value.name).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                permNode.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist.").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                        context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim.").formatted(Formatting.RED), false);
                        return 0;
                    }
                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                    boolean permission = hasPermission(claim1, player1, value);
                    context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " does" : " does not") + " have the permission " + value.name).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                permNode.then(allow);
                player.then(permNode);
            }
            claim.then(player);
            exceptions.then(claim);
            command.then(exceptions);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> admin = CommandManager.literal("admin");
            //admin.requires(source -> Thimble.hasPermissionChildOrOp(source, "itsmine.admin", 4));
            {
                LiteralArgumentBuilder<ServerCommandSource> add = CommandManager.literal("add_blocks");
                add.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 4));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.addClaimBlocks(EntityArgumentType.getPlayer(context, "player").getGameProfile().getId(), IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Gave " + IntegerArgumentType.getInteger(context, "amount") + " claim blocks to " + EntityArgumentType.getPlayer(context, "player").getGameProfile().getName()).formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                add.then(player);
                admin.then(add);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> remove = CommandManager.literal("remove_blocks");
                remove.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 4));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.addClaimBlocks(EntityArgumentType.getPlayer(context, "player").getGameProfile().getId(), -IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Took " + IntegerArgumentType.getInteger(context, "amount") + " claim blocks from " + EntityArgumentType.getPlayer(context, "player").getGameProfile().getName()).formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                remove.then(player);
                admin.then(remove);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set_blocks");
                set.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 4));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.setClaimBlocks(EntityArgumentType.getPlayer(context, "player").getGameProfile().getId(), IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Set " + EntityArgumentType.getPlayer(context, "player").getGameProfile().getName() + "'s claim block amount to " + IntegerArgumentType.getInteger(context, "amount")).formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                set.then(player);
                admin.then(set);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> delete = CommandManager.literal("destroy");
                delete.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.destroy", 4));
                RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
                LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
                confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), true));
                claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), true));
                claim.then(confirm);
                delete.then(claim);
                delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), true));
                admin.then(delete);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> create = CommandManager.literal("create_free");
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
            command.then(admin);
        }
        dispatcher.register(command);
    }

    private static int showClaim(ServerCommandSource source, Claim claim) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (claim != null) {
            if (!claim.dimension.equals(source.getWorld().getDimension().getType())) {
                source.sendFeedback(new LiteralText("That claim is not in this dimension").formatted(Formatting.RED), false);
                return 0;
            }
            source.sendFeedback(new LiteralText("Showing claim: " + claim.name).formatted(Formatting.GREEN), false);
            for (int x = claim.min.getX(); x < claim.max.getX(); x++) {
                sendBlockPacket(player, new BlockPos(x, claim.min.getY(), claim.min.getZ()), Blocks.SPONGE.getDefaultState());
                sendBlockPacket(player, new BlockPos(x, claim.max.getY(), claim.min.getZ()), Blocks.SPONGE.getDefaultState());
                sendBlockPacket(player, new BlockPos(x, claim.min.getY(), claim.max.getZ()), Blocks.SPONGE.getDefaultState());
                sendBlockPacket(player, new BlockPos(x, claim.max.getY(), claim.max.getZ()), Blocks.SPONGE.getDefaultState());
            }
            for (int y = claim.min.getY(); y < claim.max.getY(); y++) {
                sendBlockPacket(player, new BlockPos(claim.min.getX(), y, claim.min.getZ()), Blocks.SEA_LANTERN.getDefaultState());
                sendBlockPacket(player, new BlockPos(claim.max.getX(), y, claim.min.getZ()), Blocks.SEA_LANTERN.getDefaultState());
                sendBlockPacket(player, new BlockPos(claim.min.getX(), y, claim.max.getZ()), Blocks.SEA_LANTERN.getDefaultState());
                sendBlockPacket(player, new BlockPos(claim.max.getX(), y, claim.max.getZ()), Blocks.SEA_LANTERN.getDefaultState());
            }
            for (int z = claim.min.getZ(); z < claim.max.getZ(); z++) {
                sendBlockPacket(player, new BlockPos(claim.min.getX(), claim.min.getY(), z), Blocks.REDSTONE_BLOCK.getDefaultState());
                sendBlockPacket(player, new BlockPos(claim.max.getX(), claim.min.getY(), z), Blocks.REDSTONE_BLOCK.getDefaultState());
                sendBlockPacket(player, new BlockPos(claim.min.getX(), claim.max.getY(), z), Blocks.REDSTONE_BLOCK.getDefaultState());
                sendBlockPacket(player, new BlockPos(claim.max.getX(), claim.max.getY(), z), Blocks.REDSTONE_BLOCK.getDefaultState());
            }
        } else {
            source.sendFeedback(new LiteralText("That is not a valid claim").formatted(Formatting.RED), false);
        }
        return 0;
    }
    private static void sendBlockPacket(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        BlockUpdateS2CPacket packet =  new BlockUpdateS2CPacket(player.world, pos);
        if (state != null) ((BlockUpdatePacketMixin)packet).setState(state);
        player.networkHandler.sendPacket(packet);
    }
    private static int createClaim(String name, ServerCommandSource owner, BlockPos posA, BlockPos posB, boolean ignoreLimits) throws CommandSyntaxException {
        UUID ownerID = owner.getPlayer().getGameProfile().getId();
        int x, y, z, mx, my, mz;
        if (posA.getX() > posB.getX()) {
            x = posB.getX();
            mx = posA.getX();
        } else {
            x =  posA.getX();
            mx = posB.getX();
        }
        if (posA.getY() > posB.getY()) {
            y = posB.getY();
            my = posA.getY();
        } else {
            y =  posA.getY();
            my = posB.getY();
        }
        if (posA.getZ() > posB.getZ()) {
            z = posB.getZ();
            mz = posA.getZ();
        } else {
            z =  posA.getZ();
            mz = posB.getZ();
        }
        BlockPos min = new BlockPos(x,y, z);
        BlockPos max = new BlockPos(mx, my, mz);
        BlockPos sub = max.subtract(min);
        int subInt = sub.getX() * sub.getY() * sub.getZ();
        System.out.println("X" + sub.getX() + "Y" + sub.getY() + "Z" + sub.getZ() + "T" + subInt);

        Claim claim = new Claim(name, ownerID, min, max, owner.getWorld().getDimension().getType());
        if (!ClaimManager.INSTANCE.wouldIntersect(claim)) {
            // works because only the first statemet is evaluated if true
            if ((ignoreLimits && Thimble.hasPermissionOrOp(owner, "itsmine.admin.infinite_blocks", 4)) || ClaimManager.INSTANCE.useClaimBlocks(ownerID, subInt)) {
                ClaimManager.INSTANCE.addClaim(claim);
                owner.sendFeedback(new LiteralText("").append(new LiteralText("Your claim was created.").formatted(Formatting.GREEN)).append(new LiteralText("(Area: " + sub.getX() + "x" + sub.getY() + "x" + sub.getZ() + ")").setStyle(new Style()
                        .setColor(Formatting.GREEN).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(subInt + " blocks").formatted(Formatting.YELLOW))))), false);
                checkPlayer(owner, owner.getPlayer().getGameProfile().getId());
                if (ignoreLimits)owner.getMinecraftServer().sendMessage(new LiteralText(owner.getPlayer().getGameProfile().getName() + " Has created a new claim(" + claim.name + ") using the admin command."));
            } else {
                owner.sendFeedback(new LiteralText("You don't have enough claim blocks. You have " + ClaimManager.INSTANCE.getClaimBlocks(ownerID) + ", you need " + subInt + "(" + (subInt - ClaimManager.INSTANCE.getClaimBlocks(ownerID)) + " more)").formatted(Formatting.RED), false);
            }
        } else {
            owner.sendFeedback(new LiteralText("Your claim would overlap with another claim.").formatted(Formatting.RED), false);
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
            sender.sendFeedback(new LiteralText("That claim does not exist.").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.owner.equals(sender.getPlayer().getGameProfile().getId())) {
            if (admin && Thimble.hasPermissionOrOp(sender, "itsmine.admin.delete_others", 4)) {
                sender.sendFeedback(new LiteralText("WARNING: This is not your claim.").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("That is not your claim.").formatted(Formatting.RED), false);
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
            sender.sendFeedback(new LiteralText("That claim does not exist.").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.owner.equals(sender.getPlayer().getGameProfile().getId())) {
            if (admin && Thimble.hasPermissionOrOp(sender, "itsmine.admin.destroy", 4)) {
                sender.sendFeedback(new LiteralText("Deleting a claim belonging to somebody else.").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("That is not your claim.").formatted(Formatting.RED), false);
                return 0;
            }
        }
        ClaimManager.INSTANCE.releaseBlocksToOwner(claim);
        ClaimManager.INSTANCE.claimsByName.remove(claim.name);
        sender.sendFeedback(new LiteralText("Deleted the claim \"" + claim.name + "\"").formatted(Formatting.GREEN), claim.owner != sender.getPlayer().getGameProfile().getId());
        return 0;
    }
    private static int showClaimInfo(ServerCommandSource sender, Claim claim) {
        sender.sendFeedback(new LiteralText("Claim Name: " + claim.name), false);
        //sender.sendFeedback(new LiteralText("Owner")); // how to do this...
        return 0;
    }
    private static int modifyException(Claim claim, ServerPlayerEntity exception, Claim.ClaimPermissions.Permission permission, boolean allowed) {
        UUID exceptionID = exception.getGameProfile().getId();
        Claim.ClaimPermissions permissions;
        if(claim.permssionsMap.containsKey(exceptionID)) {
            permissions = claim.permssionsMap.get(exceptionID);
        } else {
            permissions = claim.initializePermissions();
            claim.permssionsMap.put(exceptionID, permissions);
        }
        permissions.setPermission(permission, allowed);
        return 0;
    }
    private static boolean hasPermission(Claim claim, ServerPlayerEntity exception, Claim.ClaimPermissions.Permission permission) {
        UUID exceptionID = exception.getGameProfile().getId();
        Claim.ClaimPermissions permissions;
        if(claim.permssionsMap.containsKey(exceptionID)) {
            permissions = claim.permssionsMap.get(exceptionID);
        } else {
            permissions = claim.initializePermissions();
            claim.permssionsMap.put(exceptionID, permissions);
        }
        return permissions.hasPermission(permission);
    }
}