package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.network.packet.BlockUpdateS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.swing.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author Indigo Amann
 */
@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;activate(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Z"))
    public boolean activateIfPossible(BlockState state, World world, PlayerEntity playerEntity_1, Hand hand_1, BlockHitResult blockHitResult_1) {
        BlockPos pos =  blockHitResult_1.getBlockPos();
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, world.getDimension().getType());
        if (claim != null) {
            UUID uuid =  playerEntity_1.getGameProfile().getId();
            if (
                    claim.hasPermissionAt(uuid, Claim.ClaimPermissions.Permission.ACTIVATE_BLOCKS, pos) ||
                            (state.getBlock() instanceof AbstractButtonBlock && claim.hasPermissionAt(uuid, Claim.ClaimPermissions.Permission.PRESS_BUTTONS, pos)) ||
                            (state.getBlock() instanceof LeverBlock && claim.hasPermissionAt(uuid, Claim.ClaimPermissions.Permission.USE_LEVERS, pos)) ||
                            (state.getBlock() instanceof DoorBlock && claim.hasPermissionAt(uuid, Claim.ClaimPermissions.Permission.OPEN_DOORS, pos))
            ) return state.activate(world, playerEntity_1, hand_1, blockHitResult_1);
            else {
                if (state.getBlock() instanceof DoorBlock && playerEntity_1 instanceof ServerPlayerEntity) {
                    DoubleBlockHalf half = state.get(DoorBlock.HALF);
                    ((ServerPlayerEntity) playerEntity_1).networkHandler.sendPacket(new BlockUpdateS2CPacket(world, half == DoubleBlockHalf.LOWER ? pos.up() : pos.down()));
                }
                //playerEntity_1.sendMessage(new LiteralText("").append(new LiteralText("You are in a claim that does not allow you to use that").formatted(Formatting.RED)).append(new LiteralText("(Use /claim show to see an outline)").formatted(Formatting.YELLOW)));
                return false;
            }
        }
        return state.activate(world, playerEntity_1, hand_1, blockHitResult_1);
    }
    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 2))
    public boolean allowItemUse(ItemStack stack, PlayerEntity playerEntity_1, World world_1, ItemStack itemStack_1, Hand hand_1, BlockHitResult blockHitResult_1) {
        BlockPos pos =  blockHitResult_1.getBlockPos();
        Claim claim = ClaimManager.INSTANCE.getClaimAt(pos, playerEntity_1.world.getDimension().getType());
        if (claim != null && !stack.isEmpty()) {
            UUID uuid =  playerEntity_1.getGameProfile().getId();
            if (
                    claim.hasPermissionAt(uuid, Claim.ClaimPermissions.Permission.USE_ITEMS_ON_BLOCKS, pos) ||
                            (stack.getItem() instanceof BlockItem && claim.hasPermissionAt(uuid, Claim.ClaimPermissions.Permission.PLACE_BREAK, pos))
            ) return false;
            return true;
        }
        return stack.isEmpty();
    }
}
