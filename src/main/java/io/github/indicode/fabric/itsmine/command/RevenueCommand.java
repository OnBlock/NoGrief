package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.command.admin.AdminCommand.PERMISSION_CHECK_ADMIN;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RevenueCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, RequiredArgumentBuilder<ServerCommandSource, String> claim) {
        LiteralArgumentBuilder<ServerCommandSource> revenue = literal("revenue");
        RequiredArgumentBuilder<ServerCommandSource, Boolean> claimRevenue = argument("claimRevenue", BoolArgumentType.bool());
        revenue.executes(context -> revenue(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), false));
        revenue.requires(PERMISSION_CHECK_ADMIN);
        claim.executes(context -> revenue(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), false));
        claimRevenue.executes(context -> revenue(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), true));
        claim.then(claimRevenue);
        revenue.then(claim);
        command.then(revenue);
    }

    private static int revenue(ServerCommandSource source, Claim claim, boolean claimrevenue) throws CommandSyntaxException {
        //Show subzones (so you can just claim everything in one place) maybe just all claims
        if(claim == null){
            source.sendFeedback(Messages.INVALID_CLAIM,true);
            return 0;
        }
        if(!claim.claimBlockOwner.toString().equalsIgnoreCase(source.getPlayer().getUuid().toString())){
            source.sendFeedback(Messages.NO_PERMISSION, true);
            return 0;
        }

        if(claimrevenue){
            for(ItemStack itemStack : claim.rent.getRevenue()){
                source.getPlayer().inventory.insertStack(itemStack);
            }
            claim.rent.clearRevenue();
            return 1;

        } else {
            if(claim.rent.getRevenue().isEmpty()){
                source.sendFeedback(new LiteralText("No Revenue").formatted(Formatting.RED), true);
                return 0;
            }
            MutableText text = new LiteralText("Revenue\n").formatted(Formatting.AQUA);
            HashMap<Item, Integer> hashMap = new HashMap<>();
            for(ItemStack itemStack : claim.rent.getRevenue()){
                Item value = itemStack.getItem();
                if(hashMap.containsKey(value)){
                    hashMap.put(value, hashMap.get(value) + itemStack.getCount());
                } else {
                    hashMap.put(value, + itemStack.getCount());
                }

                hashMap.forEach((item, integer) -> {
                    boolean color = true;
                    text.append(new LiteralText(String.valueOf(integer)).append(new LiteralText(" ")).append(new TranslatableText(item.getTranslationKey())).append(new LiteralText(" ")).formatted(color ? Formatting.GOLD : Formatting.YELLOW)).styled(style -> {
                        return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to claim revenue!").formatted(Formatting.GREEN))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim revenue " + claim.name + " true"));
                    });
                    color = !color;
                });

            }
            text.append(new LiteralText("\n"));
            source.sendFeedback(text, true);
            return 1;
        }
    }

}
