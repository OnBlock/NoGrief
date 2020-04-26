package io.github.indicode.fabric.itsmine.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.ClaimShower;
import io.github.indicode.fabric.itsmine.ItsMine;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import io.github.indicode.fabric.itsmine.util.ClaimUtil;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import io.github.indicode.fabric.itsmine.util.ShowerUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.util.ShowerUtil.silentHideShow;
import static net.minecraft.server.command.CommandManager.literal;

public class RemoveCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, RequiredArgumentBuilder<ServerCommandSource, String> claim, boolean admin) {
        LiteralArgumentBuilder<ServerCommandSource> delete = literal("remove");
        LiteralArgumentBuilder<ServerCommandSource> confirm = literal("confirm");
        confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), admin));
        claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(getString(context, "claim")), admin));
        claim.then(confirm);
        delete.then(claim);
        delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), admin));
        command.then(delete);
    }
    public static int requestDelete(ServerCommandSource sender, Claim claim, boolean admin) throws CommandSyntaxException {
        if (claim == null) {
            sender.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.permissionManager.hasPermission(sender.getPlayer().getGameProfile().getId(), Claim.Permission.REMOVE_CLAIM)) {
            if (admin && ItsMine.permissions().hasPermission(sender, PermissionUtil.Command.ADMIN_MODIFY, 2)) {
                sender.sendFeedback(new LiteralText("WARNING: This is not your claim...").formatted(Formatting.DARK_RED).formatted(Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("You cannot delete that claim").formatted(Formatting.RED), false);
                return 0;
            }
        }
        sender.sendFeedback(new LiteralText("").append(new LiteralText("Are you sure you want to delete the claim \"" + claim.name + "\"? ").formatted(Formatting.GOLD))
                .append(new LiteralText("[I'M SURE]").styled(style -> {
                        style.withColor(Formatting.DARK_RED);
                            style.withBold(true);
                            style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, (admin ? "/claim admin" : "/claim") + " remove " + claim.name + " confirm"));
                            return style;
                })), false);
        return 0;
    }
    public static int delete(ServerCommandSource source, Claim claim, boolean admin) throws CommandSyntaxException {
        ServerWorld world = source.getWorld();
        if (claim == null) {
            source.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.permissionManager.hasPermission(source.getPlayer().getGameProfile().getId(), Claim.Permission.REMOVE_CLAIM)) {
            if (admin && ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY, 2)) {
                source.sendFeedback(new LiteralText("Deleting a claim belonging to somebody else").formatted(Formatting.DARK_RED).formatted(Formatting.BOLD), false);
            } else {
                source.sendFeedback(new LiteralText("You cannot delete that claim").formatted(Formatting.RED), false);
                return 0;
            }
        }
        if(!claim.isChild){
            ClaimManager.INSTANCE.releaseBlocksToOwner(claim);
            ShowerUtil.update(claim, world, true);
            ClaimManager.INSTANCE.claimsByName.remove(claim.name);
            for(Claim subzone : claim.children){
                ClaimManager.INSTANCE.claimsByName.remove(subzone.name);
            }
        }else{
            Claim parent = ClaimUtil.getParentClaim(claim);
            ShowerUtil.update(parent, world, true);
            ClaimUtil.getParentClaim(claim).removeSubzone(claim);
            ClaimManager.INSTANCE.claimsByName.remove(claim.name);
            ShowerUtil.update(parent, world, false);
        }
        source.getWorld().getPlayers().forEach(playerEntity -> {
            if (((ClaimShower)playerEntity).getShownClaim() != null && ((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, true, true);
        });
        source.sendFeedback(new LiteralText("Deleted the claim \"" + claim.name + "\"").formatted(Formatting.GREEN), !claim.permissionManager.hasPermission(source.getPlayer().getGameProfile().getId(), Claim.Permission.REMOVE_CLAIM));
        return 0;
    }
}
