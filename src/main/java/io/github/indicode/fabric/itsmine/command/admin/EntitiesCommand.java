package io.github.indicode.fabric.itsmine.command.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.indicode.fabric.itsmine.*;
import io.github.indicode.fabric.itsmine.util.ArgumentUtil;
import io.github.indicode.fabric.itsmine.util.EntityUtil;
import io.github.indicode.fabric.itsmine.util.PermissionUtil;
import io.github.indicode.fabric.itsmine.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static io.github.indicode.fabric.itsmine.Messages.header;
import static io.github.indicode.fabric.itsmine.Messages.text;
import static io.github.indicode.fabric.itsmine.util.EntityUtil.*;
import static net.minecraft.server.command.CommandManager.literal;

public class EntitiesCommand {
    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> entity = literal("entities");
        entity.requires(source -> ItsMine.permissions().hasPermission(source, PermissionUtil.Command.ADMIN_MODIFY, 2));
        RequiredArgumentBuilder<ServerCommandSource, String> claim = ArgumentUtil.getClaims();
        entity.executes(context -> execute(context, ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getPlayer().dimension)));
        claim.executes(context -> execute(context, ClaimManager.INSTANCE.getClaim(StringArgumentType.getString(context, "claim"))));
        entity.then(claim);
        command.then(entity);
    }

    public static int execute(CommandContext<ServerCommandSource> context, Claim claim){
        ServerCommandSource source = context.getSource();
        if(claim == null){
            source.sendError(Messages.INVALID_CLAIM);
            return 1;
        }

        ArrayList<Entity> entityList = getEntities(claim);
        MutableText message = header("Entities (" + claim.name +") - " + entityList.size()).append(new LiteralText("\n\n"));
        for(EntityCategory entityCategory : EntityCategory.values()){
            MutableText entities = new LiteralText("");
            Map<EntityType, Integer> entityMap = sortByType(filterByCategory(entityList, entityCategory));
            entityMap.forEach((entityType, integer) -> {
                entities.append(entityType.getName().getString() + ": ").formatted(Formatting.YELLOW).append(new LiteralText(String.valueOf(integer))).formatted(Formatting.GOLD).append("\n");

            });
            if(entityMap.size() > 0){
                message.append(new LiteralText(entityCategory.getName()).styled(style -> {
                    return style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, entities)).withFormatting(Formatting.GOLD);
                }).append(" "));
            }
        }
        source.sendFeedback(message, false);
        return 1;
    }

}
