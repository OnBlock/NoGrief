package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ClaimShower;
import io.github.indicode.fabric.itsmine.Config;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import io.github.indicode.fabric.itsmine.util.ClaimUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.util.ArgumentUtil.getShowMode;
import static io.github.indicode.fabric.itsmine.util.ShowerUtil.silentHideShow;
import static net.minecraft.server.command.CommandManager.literal;

public class ShowCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        {
            RequiredArgumentBuilder<ServerCommandSource, String> mode = getShowMode();
            LiteralArgumentBuilder<ServerCommandSource> show = literal("show");
            show.executes(context -> executeShowClaim(context.getSource(), ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().dimension.getType()), false));
            RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims();
            claim.executes(context -> executeShowClaim(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), false));
            mode.executes(context -> executeShowClaim(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), false, StringArgumentType.getString(context, "mode")));
            claim.then(mode);
            show.then(claim);
            command.then(show);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> hide = literal("hide");
            hide.executes(context -> executeShowClaim(context.getSource(), null, true));
            RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims();
            claim.executes(context -> executeShowClaim(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), true));
            hide.then(claim);
            command.then(hide);
        }


        LiteralArgumentBuilder<ServerCommandSource> hide = literal("hide");
        hide.executes(context -> executeShowClaim(context.getSource(), null, true));
        command.then(hide);
    }
    public static int executeShowClaim(ServerCommandSource source, Claim claim, boolean reset) throws CommandSyntaxException {
        return executeShowClaim(source, claim, reset, null);
    }

    public static int executeShowClaim(ServerCommandSource source, Claim claim, boolean reset, String mode) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (!reset && ((ClaimShower)player).getShownClaim() != null && !(!Config.claims2d &&((ClaimShower)player).getShownClaim() != claim)) executeShowClaim(source, ((ClaimShower)player).getShownClaim(), true, ((ClaimShower)player).getMode());
        if (reset && ((ClaimShower)player).getShownClaim() != null) claim = ((ClaimShower)player).getShownClaim();
        if (claim != null) {
            if (!claim.dimension.equals(source.getWorld().getDimension().getType())) {
                if (claim == ((ClaimShower)player).getShownClaim()) ((ClaimShower)player).setShownClaim(null); // just so we dont have extra packets on this
                source.sendFeedback(new LiteralText("That claim is not in this dimension").formatted(Formatting.RED), false);
                return 0;
            }
            source.sendFeedback(new LiteralText((!reset ? "Showing" : "Hiding") + " claim: " + claim.name).formatted(Formatting.GREEN), false);
            if(claim.isChild) silentHideShow(player, ClaimUtil.getParentClaim(claim), reset, true, mode);
            else silentHideShow(player, claim, reset, true, mode);

        } else {
            source.sendFeedback(new LiteralText("That is not a valid claim").formatted(Formatting.RED), false);
        }
        return 0;
    }
}
