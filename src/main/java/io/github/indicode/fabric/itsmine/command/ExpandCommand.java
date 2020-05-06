package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.util.ClaimUtil;
import io.github.indicode.fabric.itsmine.util.ShowerUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.command.BlockCommand.blocksLeft;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getDirections;
import static io.github.indicode.fabric.itsmine.util.DirectionUtil.directionByName;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ExpandCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin) {
        {
            LiteralArgumentBuilder<ServerCommandSource> expand = literal("expand");
            RequiredArgumentBuilder<ServerCommandSource, Integer> amount = argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
            RequiredArgumentBuilder<ServerCommandSource, String> direction = getDirections();

            direction.executes(context -> expand(
                    ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                    IntegerArgumentType.getInteger(context, "distance"),
                    directionByName(getString(context, "direction")),
                    context.getSource(),
                    admin
            ));

            amount.executes(context -> expand(
                    ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                    IntegerArgumentType.getInteger(context, "distance"),
                    Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                    context.getSource(),
                    admin
            ));

            amount.then(direction);
            expand.then(amount);
            command.then(expand);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> shrink = literal("shrink");
            RequiredArgumentBuilder<ServerCommandSource, Integer> amount = argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
            RequiredArgumentBuilder<ServerCommandSource, String> direction = getDirections();

            direction.executes(context -> expand(
                    ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                    -IntegerArgumentType.getInteger(context, "distance"),
                    directionByName(getString(context, "direction")),
                    context.getSource(),
                    admin
            ));

            amount.executes(context -> expand(
                    ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                    -IntegerArgumentType.getInteger(context, "distance"),
                    Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                    context.getSource(),
                    admin
            ));

            amount.then(direction);
            shrink.then(amount);
            command.then(shrink);
        }
    }

    private static void undoExpand(Claim claim, Direction direction, int amount){
        if (amount < 0) claim.expand(direction, -amount);
        else claim.shrink(direction, amount);
    }

    public static int expand(Claim claim, int amount, Direction direction, ServerCommandSource source, boolean admin) throws CommandSyntaxException {
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


        if (amount > 0) {
            claim.expand(direction, amount);
        }
        else {
            claim.shrink(direction, -amount);
        }

        if (!claim.canShrinkWithoutHittingOtherSide(new BlockPos(direction.getOffsetX() * amount, direction.getOffsetY() * amount, direction.getOffsetZ() * amount))) {
            source.sendFeedback(new LiteralText("You can't shrink your claim that far. It would pass its opposite wall.").formatted(Formatting.RED), false);
            undoExpand(claim, direction, amount);
            return 0;
        }

        if(!claim.isChild){
            if (ClaimManager.INSTANCE.wouldIntersect(claim)) {
                undoExpand(claim, direction, amount);
                source.sendFeedback(new LiteralText("Expansion would result in hitting another claim").formatted(Formatting.RED), false);
                return 0;
            }

            //Check if shrinking would reset a subzone to be outside of its parent claim
            AtomicBoolean returnValue = new AtomicBoolean();
            returnValue.set(false);
            claim.children.forEach(claim1 -> {
                if(!claim1.isInside(claim)){
                    undoExpand(claim, direction, amount);
                    source.sendFeedback(new LiteralText("Shrinking would result in " + claim1.name + " being outside of " + claim.name).formatted(Formatting.RED), true);
                    returnValue.set(true);
                }
            });
            if(returnValue.get()) return 0;

            int newArea = claim.getArea() - oldArea;
            if (!admin && claim.claimBlockOwner != null && ClaimManager.INSTANCE.getClaimBlocks(ownerID) < newArea) {
                undoExpand(claim, direction, amount);
                source.sendFeedback(new LiteralText("You don't have enough claim blocks. You have " + ClaimManager.INSTANCE.getClaimBlocks(ownerID) + ", you need " + newArea + "(" + (newArea - ClaimManager.INSTANCE.getClaimBlocks(ownerID)) + " more)").formatted(Formatting.RED), false);
                blocksLeft(source);
                return 0;
            } else {
                if (!admin && claim.claimBlockOwner != null) ClaimManager.INSTANCE.useClaimBlocks(ownerID, newArea);
                source.sendFeedback(new LiteralText("Your claim was " + (amount > 0 ? "expanded" : "shrunk") + " by " + (amount < 0 ? -amount : amount) + (amount == 1 ? " block " : " blocks ") + direction.getName()).formatted(Formatting.GREEN), false);
                blocksLeft(source);
                undoExpand(claim, direction, amount);
                ShowerUtil.update(claim, source.getWorld(), true);
                if (amount > 0) claim.expand(direction, amount);
                else claim.shrink(direction, -amount);
                ShowerUtil.update(claim, source.getWorld(), false);
            }
            return 0;
        }else{
            Claim parent = ClaimUtil.getParentClaim(claim);
            if(!claim.isInside(parent)) {
                source.sendFeedback(new LiteralText("Expansion would result in expanding outside of your main claim").formatted(Formatting.RED), false);
                undoExpand(claim, direction, amount);
            } else if(ClaimManager.INSTANCE.wouldSubzoneIntersect((claim))){
                source.sendFeedback(new LiteralText("Expansion would result in overlapping with another subzone").formatted(Formatting.RED), false);
                undoExpand(claim, direction, amount);
            }else{
                source.sendFeedback(new LiteralText("Your subzone was " + (amount > 0 ? "expanded" : "shrunk") + " by " + (amount < 0 ? -amount : amount) + " blocks " + direction.getName()).formatted(Formatting.GREEN), false);
                //The expansion is undone to hide the claimshower
                undoExpand(claim, direction, amount);
                ShowerUtil.update(parent, source.getWorld(), true);
                if (amount > 0) claim.expand(direction, amount);
                else claim.shrink(direction, -amount);
                ShowerUtil.update(parent, source.getWorld(), false);
            }
        }
        return 0;
    }
}
