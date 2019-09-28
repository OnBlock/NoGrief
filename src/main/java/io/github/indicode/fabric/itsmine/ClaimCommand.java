package io.github.indicode.fabric.itsmine;

import com.mojang.brigadier.CommandDispatcher;
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
            LiteralArgumentBuilder<ServerCommandSource> ignoreLimits = CommandManager.literal("ignore_limits");
            ignoreLimits.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.infinite_blocks", 4));
            ignoreLimits.executes(context -> createClaim(
                    StringArgumentType.getString(context, "name"),
                    context.getSource(),
                    BlockPosArgumentType.getBlockPos(context, "min"),
                    BlockPosArgumentType.getBlockPos(context, "max"),
                    true
            ));
            max.then(ignoreLimits);
            min.then(max);
            name.then(min);
            create.then(name);
            command.then(create);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> show = CommandManager.literal("show");
            show.executes(context -> showClaim(context.getSource(), ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos())));
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
        dispatcher.register(command);
    }
    
    private static int showClaim(ServerCommandSource source, Claim claim) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (claim != null) {
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
        Claim claim = new Claim(name, ownerID, min, max);
        if (!ClaimManager.INSTANCE.wouldIntersect(claim)) {
            // works because only the first statemet is evaluated if true
            if ((ignoreLimits && Thimble.hasPermissionOrOp(owner, "itsmine.infiniteblocks", 4)) || ClaimManager.INSTANCE.useClaimBlocks(ownerID, subInt)) {
                ClaimManager.INSTANCE.addClaim(claim);
                owner.sendFeedback(new LiteralText("").append(new LiteralText("Your claim was created.").formatted(Formatting.GREEN)).append(new LiteralText("(Area: " + sub.getX() + "x" + sub.getY() + "x" + sub.getZ() + ")").setStyle(new Style()
                        .setColor(Formatting.GREEN).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(subInt + " blocks").formatted(Formatting.YELLOW))))), false);
                checkPlayer(owner, owner.getPlayer().getGameProfile().getId());
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
}