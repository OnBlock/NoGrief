package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.*;
import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.util.BlockUtil;
import io.github.indicode.fabric.itsmine.util.MessageUtil;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;


/**
 * @author Indigo Amann
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow public ServerPlayerEntity player;

    @Shadow public ServerWorld world;

    public BlockPos blockPos;

    @Shadow private boolean mining;

    @Inject(method = "interactBlock", at = @At(value = "HEAD"))
    private void getBlock(ServerPlayerEntity serverPlayerEntity, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir){
        blockPos = hitResult.getBlockPos();
    }

    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onUse(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
//    private ActionResult interactIfPossible(BlockState blockState, World world, PlayerEntity player, Hand hand, BlockHitResult hit, PlayerEntity player1, World world1, ItemStack itemStack, Hand hand1, BlockHitResult hitResult1) {
        private ActionResult interactIfPossible(BlockState blockState, World world, PlayerEntity player, Hand hand, BlockHitResult hit){
        BlockPos pos = hit.getBlockPos();
        ItemStack itemStack = player.getMainHandStack();
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, player.world.getDimension());
        if (claim != null) {
            if (!Functions.canInteractWith(claim, blockState.getBlock(), player.getUuid())) {
                if (!itemStack.isEmpty() && !(itemStack.getItem() instanceof BlockItem)) {
                    player.sendSystemMessage(Messages.MSG_INTERACT_BLOCK, player.getUuid());
                } else if (BlockUtil.isContainer(blockState.getBlock())) {
                    player.sendSystemMessage(Messages.MSG_OPEN_CONTAINER, player.getUuid());
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
    private boolean interactWithItemIfPossible(ItemStack stack) {
//        BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
//        System.out.println(blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ());
        Claim claim = ClaimManager.INSTANCE.getClaimAt(blockPos, world.getDimension());
        if (claim != null && !stack.isEmpty()) {
            if (Functions.canInteractUsingItem(claim, stack.getItem(), player.getUuid())) {
                return false;
            }
            if (stack.getItem() instanceof BlockItem) {
                player.sendSystemMessage(Messages.MSG_PLACE_BLOCK, player.getUuid());
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
                    player.sendSystemMessage(new LiteralText("Position #2 set: " + pos.getX() + (ItsMineConfig.main().claims2d ? "" : " " + pos.getY()) + " " + pos.getZ()).formatted(Formatting.GREEN), player.getUuid());
                    if (posPair.getLeft() != null) {
                        player.sendSystemMessage(new LiteralText("Area Selected. Type /claim create <name> to create your claim!").formatted(Formatting.GOLD), player.getUuid());
                        if (!ItsMineConfig.main().claims2d)
                            player.sendSystemMessage(new LiteralText("Remember that claims are three dimensional. Don't forget to expand up/down or select a big enough area...").formatted(Formatting.LIGHT_PURPLE).formatted(Formatting.ITALIC), player.getUuid());
                    }
                    return false;
                }
            }
        }
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, player.world.getDimension());
        if (claim != null) {
            UUID uuid = player.getGameProfile().getId();
            if (!claim.hasPermission(uuid, Claim.Permission.BUILD)) {
//                MessageUtil.sendText(ItsMineConfig.main().message().breakBlock);
                player.sendSystemMessage(Messages.MSG_BREAK_BLOCK, player.getUuid());
                return false;
            }

        }

        return world.canPlayerModifyAt(player, pos);
    }
}
