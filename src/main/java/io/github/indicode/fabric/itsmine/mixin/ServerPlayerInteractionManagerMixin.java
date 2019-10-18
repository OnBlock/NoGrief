package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import io.github.indicode.fabric.itsmine.Functions;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.network.packet.BlockUpdateS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

/**
 * @author Indigo Amann
 */
@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onUse(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    public ActionResult activateIfPossible(BlockState state, World world, PlayerEntity playerEntity_1, Hand hand_1, BlockHitResult blockHitResult_1) {
        BlockPos pos =  blockHitResult_1.getBlockPos();
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, world.getDimension().getType());
        if (claim != null) {
            UUID uuid =  playerEntity_1.getGameProfile().getId();
            if (
                    claim.hasPermission(uuid, Claim.Permission.ACTIVATE_BLOCKS) ||
                            (state.getBlock() instanceof AbstractButtonBlock && claim.hasPermission(uuid, Claim.Permission.PRESS_BUTTONS)) ||
                            (state.getBlock() instanceof LeverBlock && claim.hasPermission(uuid, Claim.Permission.USE_LEVERS)) ||
                            (state.getBlock() instanceof DoorBlock && claim.hasPermission(uuid, Claim.Permission.OPEN_DOORS))
            ) return state.onUse(world, playerEntity_1, hand_1, blockHitResult_1);
            else {
                if (state.getBlock() instanceof DoorBlock && playerEntity_1 instanceof ServerPlayerEntity) {
                    DoubleBlockHalf half = state.get(DoorBlock.HALF);
                    ((ServerPlayerEntity) playerEntity_1).networkHandler.sendPacket(new BlockUpdateS2CPacket(world, half == DoubleBlockHalf.LOWER ? pos.up() : pos.down(1)));
                }
                //playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You are in a claim that does not allow you to use that").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                return ActionResult.FAIL;
            }
        }
        return state.onUse(world, playerEntity_1, hand_1, blockHitResult_1);
    }
    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 2))
    public boolean allowItemUse(ItemStack stack, PlayerEntity playerEntity_1, World world_1, ItemStack itemStack_1, Hand hand_1, BlockHitResult blockHitResult_1) {
        BlockPos pos =  blockHitResult_1.getBlockPos().offset(blockHitResult_1.getSide());
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, playerEntity_1.world.getDimension().getType());
        if (claim != null && !stack.isEmpty()) {
            UUID uuid =  playerEntity_1.getGameProfile().getId();
            if (
                    claim.hasPermission(uuid, Claim.Permission.USE_ITEMS_ON_BLOCKS) ||
                            (stack.getItem() instanceof BlockItem && claim.hasPermission(uuid, Claim.Permission.PLACE_BREAK)) ||
                            (stack.getItem() instanceof BucketItem && claim.hasPermission(uuid, Claim.Permission.PLACE_BREAK))
            ) return false;
            if (stack.getItem() instanceof BlockItem) {
                playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You cannot place blocks in this claim").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
            }
            if (stack.getItem() instanceof BucketItem) {
                if (!Functions.isBucketEmpty((BucketItem) stack.getItem())) {
                    playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You cannot pick up fluids in this claim").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                } else {
                    playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You cannot place fluids in this claim").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                }
            }
            return true;
        }
        return stack.isEmpty();
    }
    @Redirect(method = "processBlockBreakingAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;canPlayerModifyAt(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;)Z"))
    public boolean canBreak(ServerWorld world, PlayerEntity playerEntity_1, BlockPos pos) {
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, playerEntity_1.world.getDimension().getType());
        if (claim != null) {
            UUID uuid =  playerEntity_1.getGameProfile().getId();
            if (
                    claim.hasPermission(uuid, Claim.Permission.PLACE_BREAK)
            ) return Functions.canPlayerActuallyModifyAt(world, playerEntity_1, pos);
            playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You cannot break blocks in this claim").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
            return false;
        }
        return world.canPlayerModifyAt(playerEntity_1, pos);
    }
}
