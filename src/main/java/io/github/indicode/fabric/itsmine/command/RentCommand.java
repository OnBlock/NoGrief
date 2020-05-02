package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.TimeUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static io.github.indicode.fabric.itsmine.command.admin.AdminCommand.PERMISSION_CHECK_ADMIN;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RentCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, RequiredArgumentBuilder<ServerCommandSource, String> claim) {
        LiteralArgumentBuilder<ServerCommandSource> rent = literal("rent");
        RequiredArgumentBuilder<ServerCommandSource, String> days = argument("days", word());
        days.executes(context -> rent(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), getString(context, "days")));
        rent.requires(PERMISSION_CHECK_ADMIN);
        claim.then(days);
        rent.then(claim);
        command.then(rent);
    }
    private static int rent(ServerCommandSource source, Claim claim, String rentString) throws CommandSyntaxException {
        if(claim == null){
            source.sendFeedback(Messages.INVALID_CLAIM, true);
            return 0;
        }
        int rentTime = (int) TimeUtil.convertStringtoSeconds(rentString);
        int rentAbleTime = claim.rent.getRentAbleTime();
        int maxrentAbleTime = claim.rent.getMaxrentAbleTime();
        ItemStack currency = claim.rent.getCurrency();
        ItemStack handItem = source.getPlayer().inventory.getMainHandStack();
        ItemStack revenue = handItem.copy();
        if(rentTime % claim.rent.getRentAbleTime() != 0){
            source.sendFeedback(new LiteralText("You have to rent this claim for a time by a multiple of " + TimeUtil.convertSecondsToString(rentAbleTime, 'c', 'c')).formatted(Formatting.RED), true);
            return 0;
        }
        int rentAmount = rentTime / claim.rent.getRentAbleTime();
        if(currency.getItem() != handItem.getItem() || handItem.getCount() < claim.rent.getAmount() * rentAmount){
            source.sendFeedback(new LiteralText("You don't have enough " + currency.getName().asString()).formatted(Formatting.RED), true);
            return 0;
        }
        if (!claim.rent.isRentable()) {
            source.sendFeedback(new LiteralText(claim.name + " is not for rent").formatted(Formatting.RED), true);
            return 0;
        }
        if (rentTime > claim.rent.getMaxrentAbleTime()) {
            source.sendFeedback(new LiteralText("You can't rent this claim for longer than " + TimeUtil.convertSecondsToString(maxrentAbleTime, 'c', 'c')).formatted(Formatting.RED), true);
            return 0;
        }
        if(claim.rent.getTenant() == null){
            //Setup for claim rent
            claim.rent.setTenant(source.getPlayer().getUuid());
            claim.rent.setRentedUntil(claim.rent.getUnixTime() + rentTime);
            //Remove items from player
            handItem.setCount(handItem.getCount() - claim.rent.getAmount() * rentAmount);
            revenue.setCount(claim.rent.getAmount() * rentAmount);
            claim.rent.addRevenue(revenue);
            //Give Permissions to Tenant
            claim.permissionManager.playerPermissions.put(source.getPlayer().getUuid(), new Claim.InvertedPermissionMap());
            source.sendFeedback(new LiteralText("Renting " + claim.name + " for " + claim.rent.getAmount() * rentAmount + " " + claim.rent.getCurrency().getName().asString() + " for " + TimeUtil.convertSecondsToString(rentTime, '2', 'a')).formatted(Formatting.GREEN), true);
            return 1;
        } else if(claim.rent.getTenant().toString().equalsIgnoreCase(source.getPlayer().getUuid().toString())){
            if (claim.rent.getRentTimeLeft() + rentTime <= maxrentAbleTime) {
                //Setup for claim rent
                claim.rent.setRentedUntil(claim.rent.getUnixTime() + rentTime + claim.rent.getRentTimeLeft());
                //Remove items from player
                handItem.setCount(handItem.getCount() - claim.rent.getAmount() * rentAmount);

                revenue.setCount(claim.rent.getAmount() * rentAmount);
                claim.rent.addRevenue(revenue);
                //Give Permissions to Tenant
                source.sendFeedback(new LiteralText("Extended rent " + claim.name + " by " + TimeUtil.convertSecondsToString(rentTime, '2', 'a') + "for " + claim.rent.getAmount() * rentAmount + " " + claim.rent.getCurrency().getName().asString()).formatted(Formatting.GREEN), true);
                return 1;
            } else {
                source.sendFeedback(new LiteralText("Rent would exceed the limit by " + TimeUtil.convertSecondsToString(claim.rent.getRentTimeLeft() + rentTime - maxrentAbleTime, 'c', 'c')).formatted(Formatting.RED), true);
                return 0;
            }
        } else {
            source.sendFeedback(new LiteralText("This claim is already rented").formatted(Formatting.RED), true);
            return 0;
        }
    }

}
