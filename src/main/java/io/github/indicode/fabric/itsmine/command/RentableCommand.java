package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Config;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import io.github.indicode.fabric.itsmine.util.TimeUtil;
import net.minecraft.command.arguments.ItemStackArgument;
import net.minecraft.command.arguments.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.literal;

public class RentableCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> rentable = literal("rentable");
        RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims();

        RequiredArgumentBuilder<ServerCommandSource, ItemStackArgument> currency = net.minecraft.server.command.CommandManager.argument("item", ItemStackArgumentType.itemStack()).suggests(ArgumentUtil::itemsSuggestion);
        RequiredArgumentBuilder<ServerCommandSource, Integer> amount = net.minecraft.server.command.CommandManager.argument("count", IntegerArgumentType.integer(1));
        RequiredArgumentBuilder<ServerCommandSource, String> days = net.minecraft.server.command.CommandManager.argument("days", StringArgumentType.string()
        );
        RequiredArgumentBuilder<ServerCommandSource, String> maxdays = CommandManager.argument("maxdays", string());
        maxdays.executes(context -> makeRentable(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false), IntegerArgumentType.getInteger(context, "count"), getString(context, "days"), getString(context, "maxdays")));
        claim.executes(context -> {
            Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"));
            if(claim1.rent.getCurrency() !=null || claim1.rent.getAmount() != 0 || claim1.rent.getRentAbleTime() != 0 || claim1.rent.getMaxrentAbleTime() != 0 && claim1.rent.isRentable()) {
                context.getSource().sendFeedback(new LiteralText("Can't enable rent for " + claim1.name + ", because no values are set").formatted(Formatting.RED), true);
                return 0;
            }
            if(claim1.rent.getTenant() == null){
                String state = claim1.rent.isRentable() ? "disabled" : "enabled";
                claim1.rent.setRentable(!claim1.rent.isRentable());
                context.getSource().sendFeedback(new LiteralText("Renting for " + claim1.name + " has been " + state).formatted(Formatting.GREEN), true);
                return 1;
            } else {
                context.getSource().sendFeedback(new LiteralText("Can't disable rent for " + claim1.name + ", because it is currently being rented").formatted(Formatting.RED), true);
                return 0;
            }
        });

        days.then(maxdays);
        amount.then(days);
        currency.then(amount);
        claim.then(currency);
        rentable.then(claim);
        command.then(rentable);
    }

    private static int makeRentable(ServerCommandSource source, Claim claim, ItemStack item, int amount, String rentString, String maxrentString) throws CommandSyntaxException {
        int rentTime = TimeUtil.convertStringtoSeconds(rentString);
        int maxrentTime = TimeUtil.convertStringtoSeconds(maxrentString);
        if(claim != null){
            if(rentTime <= maxrentTime){
                Claim.Rent rent = claim.rent;
                rent.setRentable(true);
                item.setCount(amount);
                rent.setCurrency(item);
                rent.setRentAbleTime(rentTime);
                if(maxrentTime > Config.rent_maxseconds) maxrentTime = Config.rent_maxseconds;
                rent.setMaxrentAbleTime(maxrentTime);
                source.sendFeedback(new LiteralText("Claim " + claim.name + " can now be rented for " + amount + " " + item.getName().asString() + " every " + TimeUtil.convertSecondsToString(rentTime, '2', 'a')).formatted(Formatting.GREEN), true);
                return 1;
            }
        }
        return 0;
    }

}
