package io.github.indicode.fabric.itsmine.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Messages;
import io.github.indicode.fabric.itsmine.util.TimeUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.server.command.CommandManager.literal;

public class InfoCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, RequiredArgumentBuilder<ServerCommandSource, String> claim) {
        LiteralArgumentBuilder<ServerCommandSource> info = literal("info");
        info.executes(context -> info(
                context.getSource(),
                ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType())
        ));
        claim.executes(context -> info(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim"))
        ));
        info.then(claim);
        command.then(info);
    }

    private static int info(ServerCommandSource source, Claim claim) throws CommandSyntaxException {
        if (claim == null) {
            source.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }

        GameProfile owner = claim.claimBlockOwner == null ? null : source.getMinecraftServer().getUserCache().getByUuid(claim.claimBlockOwner);
        BlockPos size = claim.getSize();

        MutableText text = new LiteralText("\n");
        text.append(new LiteralText("Claim Info: " + claim.name).formatted(Formatting.GOLD)).append(new LiteralText("\n"));
        text.append(newInfoLine("Name", new LiteralText(claim.name).formatted(Formatting.WHITE)));
        text.append(newInfoLine("Entities", new LiteralText(String.valueOf(claim.getEntities(source.getWorld()))).formatted(Formatting.AQUA)));
        text.append(newInfoLine("Owner",
                owner != null && claim.customOwnerName == null ? new LiteralText(owner.getName()).formatted(Formatting.GOLD) :
                        claim.customOwnerName != null ? new LiteralText(claim.customOwnerName).formatted(Formatting.GOLD) :
                                new LiteralText("No Owner").formatted(Formatting.RED).formatted(Formatting.ITALIC)));
        text.append(newInfoLine("Size", new LiteralText(size.getX() + (claim.is2d() ? "x" : ("x" + size.getY() + "x")) + size.getZ()).formatted(Formatting.GREEN)));


        text.append(new LiteralText("").append(new LiteralText("* Flags:").formatted(Formatting.YELLOW))
                .append(Messages.Command.getFlags(claim)).append(new LiteralText("\n")));
        MutableText pos = new LiteralText("");
        Text min = newPosLine(claim.min, Formatting.AQUA, Formatting.DARK_AQUA);
        Text max = newPosLine(claim.max, Formatting.LIGHT_PURPLE, Formatting.DARK_PURPLE);


        pos.append(newInfoLine("Position", new LiteralText("")
                .append(new LiteralText("Min ").formatted(Formatting.WHITE).append(min))
                .append(new LiteralText(" "))
                .append(new LiteralText("Max ").formatted(Formatting.WHITE).append(max))));
        text.append(pos);
        text.append(newInfoLine("Dimension", new LiteralText(Registry.DIMENSION_TYPE.getId(claim.dimension).getPath())));
        if(claim.rent.isRented()){
            GameProfile tenant = claim.rent.getTenant() == null ? null : source.getMinecraftServer().getUserCache().getByUuid(claim.rent.getTenant());
            text.append(newInfoLine("Status", new LiteralText("Rented").formatted(Formatting.RED).styled(style -> {
                java.util.Date time=new java.util.Date((long) claim.rent.getRentedUntil()*1000);
                return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, newInfoLine("Until", new LiteralText(time.toString()).formatted(Formatting.WHITE)).append(newInfoLine("By", new LiteralText(tenant.getName()).formatted(Formatting.WHITE))).append(newInfoLine("Price", new LiteralText(claim.rent.getAmount() + " " + claim.rent.getCurrency().getName().asString() + " every " + TimeUtil.convertSecondsToString(claim.rent.getRentAbleTime(),'f', 'f')).formatted(Formatting.WHITE)))));
            })));
        } else if (claim.rent.isRentable() && !claim.rent.isRented()){
            text.append(newInfoLine("Status", new LiteralText("For Rent").formatted(Formatting.GREEN).styled(style -> {
                return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, newInfoLine("Price", new LiteralText(claim.rent.getAmount() + " " + claim.rent.getCurrency().getName().asString() + " every " + TimeUtil.convertSecondsToString(claim.rent.getRentAbleTime(),'f', 'f')).formatted(Formatting.WHITE)).append(newInfoLine("Max Rent", new LiteralText(claim.rent.getMaxrentAbleTime() / 86400 + " days")).formatted(Formatting.WHITE))));
            })));

        } else {
            text.append(newInfoLine("Status", new LiteralText("Not For Rent").formatted(Formatting.GREEN)));

        }
        source.sendFeedback(text, false);
        return 1;
    }

    private static MutableText newPosLine(BlockPos pos, Formatting form1, Formatting form2) {
        return new LiteralText("")
                .append(new LiteralText(String.valueOf(pos.getX())).formatted(form1))
                .append(new LiteralText(" "))
                .append(new LiteralText(String.valueOf(pos.getY())).formatted(form2))
                .append(new LiteralText(" "))
                .append(new LiteralText(String.valueOf(pos.getZ())).formatted(form1));
    }
    private static MutableText newInfoLine(String title, Text text) {
        return new LiteralText("").append(new LiteralText("* " + title + ": ").formatted(Formatting.YELLOW))
                .append(text).append(new LiteralText("\n"));
    }
}
