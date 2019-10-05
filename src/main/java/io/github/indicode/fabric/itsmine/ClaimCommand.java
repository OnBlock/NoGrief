package io.github.indicode.fabric.itsmine;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.indicode.fabric.itsmine.mixin.BlockUpdatePacketMixin;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.packet.BlockUpdateS2CPacket;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.arguments.BlockPosArgumentType;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.PosArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Indigo Amann
 */
public class ClaimCommand {
    public static final SuggestionProvider DIRECTION_SUGGESTION_BUILDER = (source, builder) -> {
        for (Direction direction: Direction.values()) {
            builder.suggest(direction.getName());
        };
        return builder.buildFuture();
    };
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> command = CommandManager.literal("claim");
        {
            LiteralArgumentBuilder<ServerCommandSource> create = CommandManager.literal("create");
            ArgumentBuilder name = CommandManager.argument("name", StringArgumentType.word());
            ArgumentBuilder min = CommandManager.argument("min", BlockPosArgumentType.blockPos());
            RequiredArgumentBuilder<ServerCommandSource, PosArgument> max = CommandManager.argument("max", BlockPosArgumentType.blockPos());
            max.executes(context -> createClaim(
                    StringArgumentType.getString(context, "name"),
                    context.getSource(),
                    BlockPosArgumentType.getBlockPos(context, "min"),
                    BlockPosArgumentType.getBlockPos(context, "max"),
                    false
            ));
            min.then(max);
            name.then(min);
            create.then(name);
            command.then(create);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> show = CommandManager.literal("show");
            show.executes(context -> showClaim(context.getSource(), ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().dimension.getType()), false));
            RequiredArgumentBuilder<ServerCommandSource, String> name = CommandManager.argument("name", StringArgumentType.word());
            name.executes(context -> showClaim(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "name")), false));
            show.then(name);
            command.then(show);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> hide = CommandManager.literal("hide");
            hide.executes(context -> showClaim(context.getSource(), null, true));
            command.then(hide);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> check = CommandManager.literal("check_blocks");
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> other = CommandManager.argument("player", EntityArgumentType.player());
            other.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.checkothers", 2));
            other.executes(ctx -> checkPlayer(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player").getGameProfile().getId()));
            check.then(other);
            check.executes(ctx -> checkPlayer(ctx.getSource(), ctx.getSource().getPlayer().getGameProfile().getId()));
            command.then(check);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> delete = CommandManager.literal("destroy");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
            confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.then(confirm);
            delete.then(claim);
            delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), false));
            command.then(delete);
        }
        {
            {
                LiteralArgumentBuilder<ServerCommandSource> expand = CommandManager.literal("expand");
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE));
                RequiredArgumentBuilder<ServerCommandSource, String> direction = CommandManager.argument("direction", StringArgumentType.word());
                direction.suggests(DIRECTION_SUGGESTION_BUILDER);

                direction.executes(context -> expand(
                        ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                        IntegerArgumentType.getInteger(context, "distance"),
                        directionByName(StringArgumentType.getString(context, "direction")),
                        context.getSource()
                ));

                amount.executes(context -> expand(
                        ClaimManager.INSTANCE.getClaimAt(context.getSource().getPlayer().getBlockPos(), context.getSource().getWorld().getDimension().getType()),
                        IntegerArgumentType.getInteger(context, "distance"),
                        Direction.getEntityFacingOrder(context.getSource().getPlayer())[0],
                        context.getSource()
                ));

                amount.then(direction);
                expand.then(amount);
                command.then(expand);
            }
            LiteralArgumentBuilder<ServerCommandSource> delete = CommandManager.literal("destroy");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
            confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), false));
            claim.then(confirm);
            delete.then(claim);
            delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), false));
            command.then(delete);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> info = CommandManager.literal("info");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());

            //TODO
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> exceptions = CommandManager.literal("exceptions");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
            LiteralArgumentBuilder<ServerCommandSource> remove = CommandManager.literal("remove");
            remove.executes(context -> {
                Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                if (claim1 == null) {
                    context.getSource().sendFeedback(new LiteralText("That claim does not exist.").formatted(Formatting.RED), false);
                    return 0;
                }
                if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                    context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim.").formatted(Formatting.RED), false);
                    return 0;
                }
                ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                claim1.permssionsMap.remove(player1.getGameProfile().getId());
                context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + " no longer has an exception in the claim").formatted(Formatting.YELLOW), false);
                return 0;
            });
            player.then(remove);
            for (Claim.ClaimPermissions.Permission value : Claim.ClaimPermissions.Permission.values()) {
                LiteralArgumentBuilder<ServerCommandSource> permNode = CommandManager.literal(value.id);
                RequiredArgumentBuilder<ServerCommandSource, Boolean> allow = CommandManager.argument("allow", BoolArgumentType.bool());
                allow.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                        context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                    boolean permission = BoolArgumentType.getBool(context, "allow");
                    modifyException(claim1, player1, value, permission);
                    context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " now" : " no longer") + " has the permission " + value.name).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                permNode.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                        context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    ServerPlayerEntity player1 = EntityArgumentType.getPlayer(context, "player");
                    boolean permission = hasPermission(claim1, player1, value);
                    context.getSource().sendFeedback(new LiteralText(player1.getGameProfile().getName() + (permission ? " does" : " does not") + " have the permission " + value.name).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                permNode.then(allow);
                player.then(permNode);
            }
            claim.then(player);
            exceptions.then(claim);
            command.then(exceptions);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> settings = CommandManager.literal("settings");
            RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
            for (Claim.ClaimSettings.Setting setting: Claim.ClaimSettings.Setting.values()) {
                LiteralArgumentBuilder<ServerCommandSource> arg = CommandManager.literal(setting.id);
                arg.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                        context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    AtomicReference<String> stringify = new AtomicReference<>();
                    setting.stringifier.accept(claim1.settings.getSetting(setting), stringify);
                    context.getSource().sendFeedback(new LiteralText(setting.name + " is equal to " + stringify.get()).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                AtomicReference<ArgumentType> ref = new AtomicReference<>();
                setting.argumentType.accept(ref);
                RequiredArgumentBuilder<ServerCommandSource, ?> setter = CommandManager.argument("value", ref.get());
                setter.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                        context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    AtomicReference data = new AtomicReference();
                    data.set("value");
                    setting.parser.accept(context, data);
                    claim1.settings.settings.put(setting, data.get());
                    AtomicReference<String> stringify = new AtomicReference<>();
                    setting.stringifier.accept(data.get(), stringify);
                    context.getSource().sendFeedback(new LiteralText(setting.name + " is now equal to " + stringify.get()).formatted(Formatting.GREEN), false);
                    return 0;
                });
                arg.then(setter);
                claim.then(arg);
            }
            for (Claim.ClaimPermissions.Permission value : Claim.ClaimPermissions.Permission.values()) {
                LiteralArgumentBuilder<ServerCommandSource> permNode = CommandManager.literal(value.id);
                RequiredArgumentBuilder<ServerCommandSource, Boolean> allow = CommandManager.argument("allow", BoolArgumentType.bool());
                allow.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                        context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    boolean permission = BoolArgumentType.getBool(context, "allow");
                    claim1.settings.setPermission(value, permission);
                    context.getSource().sendFeedback(new LiteralText("Players" + (permission ? " now" : " no longer") + " have the permission " + value.name).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                permNode.executes(context -> {
                    Claim claim1 = ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim"));
                    if (claim1 == null) {
                        context.getSource().sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
                        return 0;
                    }
                    if (!context.getSource().getPlayer().getGameProfile().getId().equals(claim1.owner)) {
                        context.getSource().sendFeedback(new LiteralText("You are not the owner of this claim").formatted(Formatting.RED), false);
                        return 0;
                    }
                    boolean permission = claim1.settings.hasPermission(value);
                    context.getSource().sendFeedback(new LiteralText("Players" + (permission ? " do" : " does not") + " have the permission " + value.name).formatted(Formatting.YELLOW), false);
                    return 0;
                });
                permNode.then(allow);
                claim.then(permNode);
            }
            settings.then(claim);
            command.then(settings);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> admin = CommandManager.literal("admin");
            //admin.requires(source -> Thimble.hasPermissionChildOrOp(source, "itsmine.admin", 4));
            {
                LiteralArgumentBuilder<ServerCommandSource> add = CommandManager.literal("add_blocks");
                add.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 4));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.addClaimBlocks(EntityArgumentType.getPlayer(context, "player").getGameProfile().getId(), IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Gave " + IntegerArgumentType.getInteger(context, "amount") + " claim blocks to " + EntityArgumentType.getPlayer(context, "player").getGameProfile().getName()).formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                add.then(player);
                admin.then(add);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> remove = CommandManager.literal("remove_blocks");
                remove.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 4));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.addClaimBlocks(EntityArgumentType.getPlayer(context, "player").getGameProfile().getId(), -IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Took " + IntegerArgumentType.getInteger(context, "amount") + " claim blocks from " + EntityArgumentType.getPlayer(context, "player").getGameProfile().getName()).formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                remove.then(player);
                admin.then(remove);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set_blocks");
                set.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.modify_balance", 4));
                RequiredArgumentBuilder<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player());
                RequiredArgumentBuilder<ServerCommandSource, Integer> amount = CommandManager.argument("amount", IntegerArgumentType.integer());
                amount.executes(context -> {
                    ClaimManager.INSTANCE.setClaimBlocks(EntityArgumentType.getPlayer(context, "player").getGameProfile().getId(), IntegerArgumentType.getInteger(context, "amount"));
                    context.getSource().sendFeedback(new LiteralText("Set " + EntityArgumentType.getPlayer(context, "player").getGameProfile().getName() + "'s claim block amount to " + IntegerArgumentType.getInteger(context, "amount")).formatted(Formatting.GREEN), true);
                    return 0;
                });
                player.then(amount);
                set.then(player);
                admin.then(set);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> delete = CommandManager.literal("destroy");
                delete.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.destroy", 4));
                RequiredArgumentBuilder<ServerCommandSource, String> claim = CommandManager.argument("claim", StringArgumentType.word());
                LiteralArgumentBuilder<ServerCommandSource> confirm = CommandManager.literal("confirm");
                confirm.executes(context -> delete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), true));
                claim.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.claimsByName.get(StringArgumentType.getString(context, "claim")), true));
                claim.then(confirm);
                delete.then(claim);
                delete.executes(context -> requestDelete(context.getSource(), ClaimManager.INSTANCE.getClaimAt(new BlockPos(context.getSource().getPosition()), context.getSource().getWorld().getDimension().getType()), true));
                admin.then(delete);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> create = CommandManager.literal("create_free");
                create.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.infinite_claim", 4));
                ArgumentBuilder name = CommandManager.argument("name", StringArgumentType.word());
                ArgumentBuilder min = CommandManager.argument("min", BlockPosArgumentType.blockPos());
                RequiredArgumentBuilder<ServerCommandSource, PosArgument> max = CommandManager.argument("max", BlockPosArgumentType.blockPos());
                max.executes(context -> createClaim(
                        StringArgumentType.getString(context, "name"),
                        context.getSource(),
                        BlockPosArgumentType.getBlockPos(context, "min"),
                        BlockPosArgumentType.getBlockPos(context, "max"),
                        true
                ));
                min.then(max);
                name.then(min);
                create.then(name);
                admin.then(create);
            }
            {
                LiteralArgumentBuilder<ServerCommandSource> ignore = CommandManager.literal("ignore_claims");
                ignore.requires(source -> Thimble.hasPermissionOrOp(source, "itsmine.admin.ignore_claims", 4));
                ignore.executes(context -> {
                    UUID id = context.getSource().getPlayer().getGameProfile().getId();
                    boolean isIgnoring = ClaimManager.INSTANCE.ignoringClaims.contains(id);
                    if (isIgnoring) ClaimManager.INSTANCE.ignoringClaims.remove(id);
                    else ClaimManager.INSTANCE.ignoringClaims.add(id);
                    context.getSource().sendFeedback(new LiteralText("You are " + (isIgnoring ? "no longer" : "now") + " ignoring claims.").formatted(Formatting.GREEN), false);
                    return 0;
                });
                admin.then(ignore);
            }
            command.then(admin);
        }
        dispatcher.register(command);
    }

    private static int showClaim(ServerCommandSource source, Claim claim, boolean reset) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        if (!reset && ((ClaimShower)player).getShownClaim() != null && ((ClaimShower)player).getShownClaim() != claim) showClaim(source, ((ClaimShower)player).getShownClaim(), true);
        if (reset && ((ClaimShower)player).getShownClaim() != null) claim = ((ClaimShower)player).getShownClaim();
        if (claim != null) {
            if (!claim.dimension.equals(source.getWorld().getDimension().getType())) {
                if (claim == ((ClaimShower)player).getShownClaim()) ((ClaimShower)player).setShownClaim(null); // just so we dont have extra packets on this
                source.sendFeedback(new LiteralText("That claim is not in this dimension").formatted(Formatting.RED), false);
                return 0;
            }
            source.sendFeedback(new LiteralText((!reset ? "Showing" : "Hiding") + " claim: " + claim.name).formatted(Formatting.GREEN), false);
            silentHideShow(player, claim, reset, true);

        } else {
            source.sendFeedback(new LiteralText("That is not a valid claim").formatted(Formatting.RED), false);
        }
        return 0;
    }
    private static void silentHideShow(ServerPlayerEntity player, Claim claim, boolean hide, boolean updateStatus) {
        BlockState block = hide ? null : Blocks.LAPIS_BLOCK.getDefaultState();
        for (int x = claim.min.getX(); x < claim.max.getX(); x++) {
            sendBlockPacket(player, new BlockPos(x, claim.min.getY(), claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.max.getY(), claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.min.getY(), claim.max.getZ()), block);
            sendBlockPacket(player, new BlockPos(x, claim.max.getY(), claim.max.getZ()), block);
        }
        for (int y = claim.min.getY(); y < claim.max.getY(); y++) {
            sendBlockPacket(player, new BlockPos(claim.min.getX(), y, claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), y, claim.min.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.min.getX(), y, claim.max.getZ()), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), y, claim.max.getZ()), block);
        }
        for (int z = claim.min.getZ(); z < claim.max.getZ(); z++) {
            sendBlockPacket(player, new BlockPos(claim.min.getX(), claim.min.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), claim.min.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.min.getX(), claim.max.getY(), z), block);
            sendBlockPacket(player, new BlockPos(claim.max.getX(), claim.max.getY(), z), block);
        }
        if (updateStatus) {
            if (!hide) ((ClaimShower) player).setShownClaim(claim);
            else ((ClaimShower) player).setShownClaim(null);
        }
    }
    private static void sendBlockPacket(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        BlockUpdateS2CPacket packet =  new BlockUpdateS2CPacket(player.world, pos);
        if (state != null) ((BlockUpdatePacketMixin)packet).setState(state);
        player.networkHandler.sendPacket(packet);
    }
    private static int createClaim(String name, ServerCommandSource owner, BlockPos posA, BlockPos posB, boolean ignoreLimits) throws CommandSyntaxException {
        UUID ownerID = owner.getPlayer().getGameProfile().getId();
        int x, y, z, mx, my, mz;
        if (posA.getX() > posB.getX()) {
            x = posB.getX();
            mx = posA.getX();
        } else {
            x =  posA.getX();
            mx = posB.getX();
        }
        if (posA.getY() > posB.getY()) {
            y = posB.getY();
            my = posA.getY();
        } else {
            y =  posA.getY();
            my = posB.getY();
        }
        if (posA.getZ() > posB.getZ()) {
            z = posB.getZ();
            mz = posA.getZ();
        } else {
            z =  posA.getZ();
            mz = posB.getZ();
        }
        BlockPos min = new BlockPos(x,y, z);
        BlockPos max = new BlockPos(mx, my, mz);
        BlockPos sub = max.subtract(min);
        int subInt = sub.getX() * sub.getY() * sub.getZ();

        Claim claim = new Claim(name, ownerID, min, max, owner.getWorld().getDimension().getType());
        if (!ClaimManager.INSTANCE.claimsByName.containsKey(name)) {
            if (!ClaimManager.INSTANCE.wouldIntersect(claim)) {
                // works because only the first statemet is evaluated if true
                if ((ignoreLimits && Thimble.hasPermissionOrOp(owner, "itsmine.admin.infinite_blocks", 4)) || ClaimManager.INSTANCE.useClaimBlocks(ownerID, subInt)) {
                    ClaimManager.INSTANCE.addClaim(claim);
                    owner.sendFeedback(new LiteralText("").append(new LiteralText("Your claim was created").formatted(Formatting.GREEN)).append(new LiteralText("(Area: " + sub.getX() + "x" + sub.getY() + "x" + sub.getZ() + ")").setStyle(new Style()
                            .setColor(Formatting.GREEN).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(subInt + " blocks").formatted(Formatting.YELLOW))))), false);
                    checkPlayer(owner, owner.getPlayer().getGameProfile().getId());
                    showClaim(owner, claim, false);
                    if (ignoreLimits)
                        owner.getMinecraftServer().sendMessage(new LiteralText(owner.getPlayer().getGameProfile().getName() + " Has created a new claim(" + claim.name + ") using the admin command."));
                } else {
                    owner.sendFeedback(new LiteralText("You don't have enough claim blocks. You have " + ClaimManager.INSTANCE.getClaimBlocks(ownerID) + ", you need " + subInt + "(" + (subInt - ClaimManager.INSTANCE.getClaimBlocks(ownerID)) + " more)").formatted(Formatting.RED), false);
                }
            } else {
                owner.sendFeedback(new LiteralText("Your claim would overlap with another claim").formatted(Formatting.RED), false);
            }
        } else {
            owner.sendFeedback(new LiteralText("The name \"" + name + "\" is already taken.").formatted(Formatting.RED), false);
        }
        return 0;
    }
    private static int checkPlayer(ServerCommandSource ret, UUID player) throws CommandSyntaxException {
        int blocks = ClaimManager.INSTANCE.getClaimBlocks(player);
        ret.sendFeedback(new LiteralText((ret.getPlayer().getGameProfile().getId().equals(player) ? "You have " : "They have ") + ClaimManager.INSTANCE.getClaimBlocks(player) + " blocks left").setStyle(new Style()
                .setColor(Formatting.YELLOW).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Area of " + ItsMine.blocksToAreaString(ClaimManager.INSTANCE.getClaimBlocks(player))).formatted(Formatting.YELLOW)))), false);
        return 0;
    }
    private static int requestDelete(ServerCommandSource sender, Claim claim, boolean admin) throws CommandSyntaxException {
        if (claim == null) {
            sender.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.owner.equals(sender.getPlayer().getGameProfile().getId())) {
            if (admin && Thimble.hasPermissionOrOp(sender, "itsmine.admin.delete_others", 4)) {
                sender.sendFeedback(new LiteralText("WARNING: This is not your claim...").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("That is not your claim.").formatted(Formatting.RED), false);
                return 0;
            }
        }
        sender.sendFeedback(new LiteralText("").append(new LiteralText("Are you sure you want to delete the claim \"" + claim.name + "\"? ").formatted(Formatting.GOLD))
                .append(new LiteralText("[I'M SURE]").setStyle(new Style()
                        .setColor(Formatting.DARK_RED)
                        .setBold(true)
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, (admin ? "/claim admin" : "/claim") + " destroy " + claim.name + " confirm")))), false);
        return 0;
    }
    private static int delete(ServerCommandSource sender, Claim claim, boolean admin) throws CommandSyntaxException {
        if (claim == null) {
            sender.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.owner.equals(sender.getPlayer().getGameProfile().getId())) {
            if (admin && Thimble.hasPermissionOrOp(sender, "itsmine.admin.destroy", 4)) {
                sender.sendFeedback(new LiteralText("Deleting a claim belonging to somebody else").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            } else {
                sender.sendFeedback(new LiteralText("That is not your claim").formatted(Formatting.RED), false);
                return 0;
            }
        }
        ClaimManager.INSTANCE.releaseBlocksToOwner(claim);
        ClaimManager.INSTANCE.claimsByName.remove(claim.name);
        sender.getWorld().getPlayers().forEach(playerEntity -> {
            if (((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, true, true);
        });
        sender.sendFeedback(new LiteralText("Deleted the claim \"" + claim.name + "\"").formatted(Formatting.GREEN), claim.owner != sender.getPlayer().getGameProfile().getId());
        return 0;
    }
    private static int showClaimInfo(ServerCommandSource sender, Claim claim) {
        sender.sendFeedback(new LiteralText("Claim Name: " + claim.name), false);
        //sender.sendFeedback(new LiteralText("Owner")); // how to do this...
        return 0;
    }
    private static int modifyException(Claim claim, ServerPlayerEntity exception, Claim.ClaimPermissions.Permission permission, boolean allowed) {
        UUID exceptionID = exception.getGameProfile().getId();
        Claim.ClaimPermissions permissions;
        if(claim.permssionsMap.containsKey(exceptionID)) {
            permissions = claim.permssionsMap.get(exceptionID);
        } else {
            permissions = claim.initializePermissions();
            claim.permssionsMap.put(exceptionID, permissions);
        }
        permissions.setPermission(permission, allowed);
        return 0;
    }
    private static boolean hasPermission(Claim claim, ServerPlayerEntity exception, Claim.ClaimPermissions.Permission permission) {
        UUID exceptionID = exception.getGameProfile().getId();
        Claim.ClaimPermissions permissions;
        if(claim.permssionsMap.containsKey(exceptionID)) {
            permissions = claim.permssionsMap.get(exceptionID);
        } else {
            permissions = claim.initializePermissions();
            claim.permssionsMap.put(exceptionID, permissions);
        }
        return permissions.hasPermission(permission);
    }
    private static Direction directionByName(String name) {
        for (Direction direction : Direction.values()) {
            if (name.equals(direction.getName())) return direction;
        }
        return null;
    }
    private static int expand(Claim claim, int amount, Direction direction, ServerCommandSource source) throws CommandSyntaxException {
        UUID ownerID = source.getPlayer().getGameProfile().getId();
        if (claim == null) {
            source.sendFeedback(new LiteralText("That claim does not exist").formatted(Formatting.RED), false);
            return 0;
        }
        if (direction == null) {
            source.sendFeedback(new LiteralText("That is not a valid direction").formatted(Formatting.RED), false);
            return 0;
        }
        if (!claim.owner.equals(ownerID)) {
            source.sendFeedback(new LiteralText("You do not own that claim").formatted(Formatting.RED), false);
            return 0;
        }
        int oldArea = claim.getArea();
        claim.expand(direction, amount);
        if (ClaimManager.INSTANCE.wouldIntersect(claim)) {
            claim.expand(direction, -amount);
            source.sendFeedback(new LiteralText("Expansion would result in hitting another claim").formatted(Formatting.RED), false);
            return 0;
        }
        int newArea = claim.getArea() - oldArea;
        if (ClaimManager.INSTANCE.getClaimBlocks(ownerID) < newArea) {
            source.sendFeedback(new LiteralText("You don't have enough claim blocks. You have " + ClaimManager.INSTANCE.getClaimBlocks(ownerID) + ", you need " + newArea + "(" + (newArea - ClaimManager.INSTANCE.getClaimBlocks(ownerID)) + " more)").formatted(Formatting.RED), false);
            checkPlayer(source, ownerID);
            return 0;
        } else {
            ClaimManager.INSTANCE.useClaimBlocks(ownerID, newArea);
            source.sendFeedback(new LiteralText("Your claim was expanded by " + amount + " blocks " + direction.getName()).formatted(Formatting.GREEN), false);
            checkPlayer(source, ownerID);
            claim.shrink(direction, amount);
            source.getWorld().getPlayers().forEach(playerEntity -> {
                if (((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, true, false);
            });
            claim.expand(direction, amount);
            source.getWorld().getPlayers().forEach(playerEntity -> {
                if (((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, false, false);
            });
        }
        return 0;
    }
}