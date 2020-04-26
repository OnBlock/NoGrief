package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.*;
import io.github.indicode.fabric.itsmine.util.BlockUtil;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

/**
 * @author Indigo Amann
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow public ServerPlayerEntity player;

    @Shadow public ServerWorld world;

    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onUse(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult interactIfPossible(BlockState blockState, World world, PlayerEntity player, Hand hand, BlockHitResult hit, PlayerEntity player1, World world1, ItemStack itemStack, Hand hand1, BlockHitResult hitResult1) {
        BlockPos pos = hit.getBlockPos();
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, player.world.getDimension().getType());
        if (claim != null) {
            if (!Functions.canInteractWith(claim, blockState.getBlock(), player.getUuid())) {
                if (!itemStack.isEmpty() && !(itemStack.getItem() instanceof BlockItem)) {
                    player.sendSystemMessage(Messages.MSG_INTERACT_BLOCK);
                } else if (BlockUtil.isContainer(blockState.getBlock())) {
                    player.sendSystemMessage(Messages.MSG_OPEN_CONTAINER);
                }

                return ActionResult.FAIL;
            }
//            else {
//                if (blockState.getBlock() instanceof DoorBlock && player instanceof ServerPlayerEntity) {
//                    DoubleBlockHalf blockHalf = blockState.get(DoorBlock.HALF);
//                    ((ServerPlayerEntity) player).networkHandler.sendPacket(new BlockUpdateS2CPacket(world, blockHalf == DoubleBlockHalf.LOWER ? pos.up() : pos.down(1)));
//                }
//            }
        }

        return blockState.onUse(world, player, hand, hit);
    }

    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 2))
    private boolean interactWithItemIfPossible(ItemStack stack, PlayerEntity player, World world, ItemStack itemStack, Hand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, world.getDimension().getType());
        if (claim != null && !stack.isEmpty()) {
            if (Functions.canInteractUsingItem(claim, stack.getItem(), player.getUuid())) {
                return false;
            }

            if (stack.getItem() instanceof BlockItem) {
                player.sendSystemMessage(Messages.MSG_PLACE_BLOCK);
            }

            return true;
        }

        return stack.isEmpty();
    }

    @Redirect(method = "processBlockBreakingAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;canPlayerModifyAt(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;)Z"))
    public boolean canBreak(ServerWorld world, PlayerEntity player, BlockPos pos) {
        if (player.inventory.getMainHandStack().getItem() == Items.STICK) {
            if (!player.isSneaking()) {
                Pair<BlockPos, BlockPos> posPair = ClaimManager.INSTANCE.stickPositions.get(player);
                if (posPair != null) {
                    posPair = new Pair<>(posPair.getLeft(), pos);
                    ClaimManager.INSTANCE.stickPositions.put(player, posPair);
                    player.sendSystemMessage(new LiteralText("Position #2 set: " + pos.getX() + (Config.claims2d ? "" : " " + pos.getY()) + " " + pos.getZ()).formatted(Formatting.GREEN));
                    if (posPair.getLeft() != null) {
                        player.sendSystemMessage(new LiteralText("Area Selected. Type /claim create <name> to create your claim!").formatted(Formatting.GOLD));
                        if (!Config.claims2d)
                            player.sendSystemMessage(new LiteralText("Remember that claims are three dimensional. Don't forget to expand up/down or select a big enough area...").formatted(Formatting.LIGHT_PURPLE).formatted(Formatting.ITALIC));
                    }
                    return false;
                }
            }
        }
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, player.world.getDimension().getType());
        if (claim != null) {
            UUID uuid = player.getGameProfile().getId();
            if (!claim.hasPermission(uuid, Claim.Permission.BUILD)) {
                player.sendSystemMessage(Messages.MSG_BREAK_BLOCK);
                return false;
            }

        }

        return world.canPlayerModifyAt(player, pos);
    }
}
