package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.Claim;
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
import static io.github.indicode.fabric.itsmine.util.ShowerUtil.silentHideShow;
import static net.minecraft.server.command.CommandManager.literal;

public class ShowCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> show = literal("show");
        show.executes(context -> executeShowClaim(context.getSource(), ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().dimension.getType()), false));
        RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims();
        claim.executes(context -> executeShowClaim(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "name")), false));
        show.then(claim);
        command.then(show);

        LiteralArgumentBuilder<ServerCommandSource> hide = literal("hide");
        hide.executes(context -> executeShowClaim(context.getSource(), null, true));
        command.then(hide);
    }

    public static int executeShowClaim(ServerCommandSource source, Claim claim, boolean reset) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (!reset && ((ClaimShower)player).getShownClaim() != null && !(!Config.claims2d &&((ClaimShower)player).getShownClaim() != claim)) executeShowClaim(source, ((ClaimShower)player).getShownClaim(), true);
        if (reset && ((ClaimShower)player).getShownClaim() != null) claim = ((ClaimShower)player).getShownClaim();
        if (claim != null) {
            if (!claim.dimension.equals(source.getWorld().getDimension().getType())) {
                if (claim == ((ClaimShower)player).getShownClaim()) ((ClaimShower)player).setShownClaim(null); // just so we dont have extra packets on this
                source.sendFeedback(new LiteralText("That claim is not in this dimension").formatted(Formatting.RED), false);
                return 0;
            }
            source.sendFeedback(new LiteralText((!reset ? "Showing" : "Hiding") + " claim: " + claim.name).formatted(Formatting.GREEN), false);
            if(claim.isChild) silentHideShow(player, ClaimUtil.getParentClaim(claim), reset, true);
            else silentHideShow(player, claim, reset, true);

        } else {
            source.sendFeedback(new LiteralText("That is not a valid claim").formatted(Formatting.RED), false);
        }
        return 0;
    }
}
