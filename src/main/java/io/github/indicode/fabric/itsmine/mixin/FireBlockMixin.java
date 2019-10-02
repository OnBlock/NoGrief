package io.github.indicode.fabric.itsmine.mixin;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * @author Indigo Amann
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Shadow protected abstract void trySpreadingFire(World world_1, BlockPos blockPos_1, int int_1, Random random_1, int int_2);

    @Redirect(method = "onScheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FireBlock;trySpreadingFire(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;ILjava/util/Random;I)V"))
    private void dontBurnClaims(FireBlock fireBlock, World world, BlockPos newPos, int int_1, Random random_1, int int_2, BlockState blockState_1, ServerWorld serverWorld_1, BlockPos oldPos, Random random_1_) {
        Claim oldClaim = ClaimManager.INSTANCE.getClaimAt(oldPos, world.getDimension().getType());
        Claim newClaim = ClaimManager.INSTANCE.getClaimAt(newPos, world.getDimension().getType());
        if (oldClaim != newClaim) {
            if (oldClaim == null) {
                if (!(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) return;
            }
            else if (newClaim == null) {
                if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) return;
            } else {
                if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS) ||
                        !(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) return;
            }
        }
        trySpreadingFire(world, newPos, int_1, random_1, int_2);
    }
    @Redirect(method = "onScheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", ordinal = 1))
    private boolean dontCauseFire2(ServerWorld world, BlockPos newPos, BlockState blockState_1, int int_1, BlockState blockState_1_, ServerWorld serverWorld_1, BlockPos oldPos, Random random_1_) {
        {
            Claim oldClaim = ClaimManager.INSTANCE.getClaimAt(oldPos, world.getDimension().getType());
            Claim newClaim = ClaimManager.INSTANCE.getClaimAt(newPos, world.getDimension().getType());
            System.out.println("Spread - " + oldPos + " -> " + newPos);
            if (oldClaim != newClaim) {
                if (oldClaim == null) {
                    if (!(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS))
                        return false;
                } else if (newClaim == null) {
                    if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS))
                        return false;
                } else {
                    if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS) ||
                            !(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS))
                        return false;
                }
            }
        }
        {
            List<Direction> directions = new ArrayList<>();
            if (blockState_1.get(FireBlock.UP)) directions.add(Direction.UP);
            if (blockState_1.get(FireBlock.NORTH)) directions.add(Direction.NORTH);
            if (blockState_1.get(FireBlock.SOUTH)) directions.add(Direction.SOUTH);
            if (blockState_1.get(FireBlock.WEST)) directions.add(Direction.WEST);
            if (blockState_1.get(FireBlock.EAST)) directions.add(Direction.EAST);
            System.out.println("TrySpread - " + directions + " - " + blockState_1);
            if (directions.isEmpty()) {
                return world.setBlockState(newPos, blockState_1, int_1);
            }
            Claim oldClaim = ClaimManager.INSTANCE.getClaimAt(newPos, world.getDimension().getType());
            Iterator<Direction> iterator = directions.iterator();
            for (Direction direction = null; iterator.hasNext(); direction = iterator.next()) {
                BlockPos newPos2 = newPos.offset(direction);
                Claim newClaim = ClaimManager.INSTANCE.getClaimAt(newPos2, world.getDimension().getType());
                if (oldClaim != newClaim) {
                    if (oldClaim == null) {
                        if (!(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) iterator.remove();
                    }
                    else if (newClaim == null) {
                        if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) iterator.remove();
                    } else {
                        if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS) ||
                                !(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) iterator.remove();
                    }
                }
            }
            System.out.println(directions);
            if (directions.isEmpty()) {
                return world.setBlockState(newPos, Blocks.FIRE.getDefaultState(), int_1);
            } else {
                if (!directions.contains(Direction.UP)) blockState_1 = blockState_1.with(FireBlock.UP, false);
                if (!directions.contains(Direction.NORTH)) blockState_1 = blockState_1.with(FireBlock.NORTH, false);
                if (!directions.contains(Direction.SOUTH)) blockState_1 = blockState_1.with(FireBlock.SOUTH, false);
                if (!directions.contains(Direction.EAST)) blockState_1 = blockState_1.with(FireBlock.EAST, false);
                if (!directions.contains(Direction.WEST)) blockState_1 = blockState_1.with(FireBlock.WEST, false);
                System.out.println(blockState_1);
            }
        }
        return world.setBlockState(newPos, blockState_1, int_1);
    }
    @Redirect(method = "trySpreadingFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private boolean dontCauseFire(World world, BlockPos oldPos, BlockState blockState_1, int int_1) {
        List<Direction> directions = new ArrayList<>();
        if (blockState_1.get(FireBlock.UP)) directions.add(Direction.UP);
        if (blockState_1.get(FireBlock.NORTH)) directions.add(Direction.NORTH);
        if (blockState_1.get(FireBlock.SOUTH)) directions.add(Direction.SOUTH);
        if (blockState_1.get(FireBlock.WEST)) directions.add(Direction.WEST);
        if (blockState_1.get(FireBlock.EAST)) directions.add(Direction.EAST);
        System.out.println("TrySpread - " + directions + " - " + blockState_1);
        if (directions.isEmpty()) {
            return world.setBlockState(oldPos, blockState_1, int_1);
        }
        Claim oldClaim = ClaimManager.INSTANCE.getClaimAt(oldPos, world.getDimension().getType());
        Iterator<Direction> iterator = directions.iterator();
        for (Direction direction = null; iterator.hasNext(); direction = iterator.next()) {
            BlockPos newPos = oldPos.offset(direction);
            Claim newClaim = ClaimManager.INSTANCE.getClaimAt(newPos, world.getDimension().getType());
            if (oldClaim != newClaim) {
                if (oldClaim == null) {
                    if (!(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) iterator.remove();
                }
                else if (newClaim == null) {
                    if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) iterator.remove();
                } else {
                    if (!(Boolean) oldClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS) ||
                            !(Boolean) newClaim.settings.getSetting(Claim.ClaimSettings.Setting.FIRE_CROSSES_BORDERS)) iterator.remove();
                }
            }
        }
        System.out.println(directions);
        if (directions.isEmpty()) {
            return world.setBlockState(oldPos, Blocks.FIRE.getDefaultState(), int_1);
        } else {
            if (!directions.contains(Direction.UP)) blockState_1 = blockState_1.with(FireBlock.UP, false);
            if (!directions.contains(Direction.NORTH)) blockState_1 = blockState_1.with(FireBlock.NORTH, false);
            if (!directions.contains(Direction.SOUTH)) blockState_1 = blockState_1.with(FireBlock.SOUTH, false);
            if (!directions.contains(Direction.EAST)) blockState_1 = blockState_1.with(FireBlock.EAST, false);
            if (!directions.contains(Direction.WEST)) blockState_1 = blockState_1.with(FireBlock.WEST, false);
            System.out.println(blockState_1);
            return world.setBlockState(oldPos, blockState_1, int_1);
        }
    }
}
