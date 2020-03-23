package io.github.indicode.fabric.itsmine;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SubzoneCommand {
    private static final String SUB_ZONE_NAME_FORMAT = "%s.%s";

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command, boolean admin) {
        LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("subzone")
                .requires(src -> ItsMine.permissions().hasPermission(src, Permissions.Command.COMMAND, 2));

        {
            LiteralArgumentBuilder<ServerCommandSource> add = CommandManager.literal("add");

            RequiredArgumentBuilder<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> addZone(ctx.getSource(), StringArgumentType.getString(ctx, "name"), null, admin));

            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word())
                    .suggests(ClaimCommand.CLAIM_PROVIDER)
                    .executes(ctx -> addZone(ctx.getSource(), StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "claim"), admin));

            name.then(claim);
            builder.then(name);
            builder.then(add);
        }

        command.then(builder);
    }

    private static int addZone(ServerCommandSource source, String name, @Nullable String claimName, boolean admin) throws CommandSyntaxException {
        if (name.length() > ClaimCommand.MAX_NAME_LENGTH) {
            source.sendError(Messages.MSG_LONG_NAME);
            return -1;
        }

        ServerPlayerEntity player = source.getPlayer();
        Claim claim = validateAndGet(source, claimName);
        Claim subZone = null;

        Pair<BlockPos, BlockPos> selectedPositions = ClaimManager.INSTANCE.stickPositions.get(player);
        if (selectedPositions == null) {
            source.sendFeedback(new LiteralText("You need to specify block positions or select them with a stick.").formatted(Formatting.RED), false);
        } else if (selectedPositions.getLeft() == null) {
            source.sendFeedback(new LiteralText("You need to specify block positions or select position #1(Right Click) with a stick.").formatted(Formatting.RED), false);
        } else if (selectedPositions.getRight() == null) {
            source.sendFeedback(new LiteralText("You need to specify block positions or select position #2(Left Click) with a stick.").formatted(Formatting.RED), false);
        } else {
            subZone = createSubzone(source, name, selectedPositions.getLeft(), selectedPositions.getRight(), admin);
            if (subZone != null) {
                ClaimManager.INSTANCE.stickPositions.remove(player);


                return 1;
            }
        }

        return 0;
    }

    private static Claim createSubzone(ServerCommandSource source, String name, BlockPos posA, BlockPos posB, boolean admin) throws CommandSyntaxException {
        UUID ownerID = source.getPlayer().getGameProfile().getId();
        int x, y = 0, z, mx, my = 255, mz;
        if (posA.getX() > posB.getX()) {
            x = posB.getX();
            mx = posA.getX();
        } else {
            x =  posA.getX();
            mx = posB.getX();
        }
        if (!Config.claims2d) {
            if (posA.getY() > posB.getY()) {
                y = posB.getY();
                my = posA.getY();
            } else {
                y = posA.getY();
                my = posB.getY();
            }
        }
        if (posA.getZ() > posB.getZ()) {
            z = posB.getZ();
            mz = posA.getZ();
        } else {
            z =  posA.getZ();
            mz = posB.getZ();
        }
        BlockPos min = new BlockPos(x, y, z);
        BlockPos max = new BlockPos(mx, my, mz);
        BlockPos sub = max.subtract(min);
        sub = sub.add(1, Config.claims2d ? 0 : 1,1);

        source.sendFeedback(new LiteralText("").append(new LiteralText("Your claim was created").formatted(Formatting.GREEN)).append(new LiteralText("(Area: " + sub.getX() + "x" + sub.getY() + "x" + sub.getZ() + ")").setStyle(new Style().setColor(Formatting.GREEN))), false);
        return new Claim(name, admin ? null : ownerID, min, max, source.getWorld().getDimension().getType(), source.getPlayer().getBlockPos());
    }

    private static Claim validateAndGet(ServerCommandSource source, @Nullable String  claimName) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        Claim claim = claimName == null ? ClaimManager.INSTANCE.getClaimAt(player.getBlockPos(), player.dimension) :
                ClaimManager.INSTANCE.claimsByName.get(claimName);

        if (claim == null) {
            throw new SimpleCommandExceptionType(Messages.INVALID_CLAIM).create();
        }

        return claim;
    }

}
