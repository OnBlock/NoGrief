package io.github.indicode.fabric.itsmine;

import io.github.indicode.fabric.itsmine.mixin.BlockActionPacketMixin;
import io.github.indicode.fabric.itsmine.mixin.BucketItemMixin;
import io.github.indicode.fabric.itsmine.mixin.projectile.OwnedProjectile;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockActionS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;

/**
 * @author Indigo Amann
 */
public class Functions {
    public static void doPistonUpdate(ServerWorld world, Packet packet) {
        if (packet instanceof BlockActionS2CPacket) {
            BlockActionPacketMixin accessor = (BlockActionPacketMixin) packet;
            if ((accessor.getBlock() == Blocks.PISTON || accessor.getBlock() == Blocks.PISTON_HEAD || accessor.getBlock() == Blocks.STICKY_PISTON)) {
                Direction direction = Direction.byId(accessor.getData());
                BlockPos pos = BlockPos.fromLong(accessor.getPos().offset(2, direction));
                world.getChunkManager().markForUpdate(pos);
            }
        }
    }
    public static boolean canPlayerActuallyModifyAt(ServerWorld world, PlayerEntity playerEntity_1, BlockPos blockPos_1) {
        return !world.getServer().isSpawnProtected(world, blockPos_1, playerEntity_1) && !world.getWorldBorder().contains(blockPos_1);
    };
    public static boolean isBucketEmpty(BucketItem item) {
        return ((BucketItemMixin)item).getFluid() != Fluids.EMPTY;
    }
    private static Map<UUID, Boolean> flyers = new HashMap<>();
    public static boolean canClaimFly(ServerPlayerEntity player) {
        if (flyers.containsKey(player.getGameProfile().getId())) {
            return flyers.get(player.getGameProfile().getId());
        } else {
            refreshFly(player);
            return canClaimFly(player);
        }
    }
    public static void refreshFly(ServerPlayerEntity player) {
        flyers.put(player.getGameProfile().getId(), player.world.getServer().getPlayerManager().isOperator(player.getGameProfile()) || ItsMine.permissions().hasPermission(player.getUuid(), "itsmine.claim_fly"));
    }
    public static boolean canFly(ServerPlayerEntity player) {
        boolean op = false;
        if (player.world.getServer() != null) {
            op = player.world.getServer().getPlayerManager().isOperator(player.getGameProfile());
        }

        return op || ItsMine.permissions().hasPermission(player.getUuid(), "itsmine.claim_fly");
    }
    private static List<UUID> claimFlyNow = new ArrayList<>();
    public static boolean isClaimFlying(UUID player) {
        return claimFlyNow.contains(player);
    }
    public static void setClaimFlying(UUID player, boolean flying) {
        if (flying && !claimFlyNow.contains(player)) {
            claimFlyNow.add(player);
        } else if (!flying) {
            claimFlyNow.remove(player);
        }
    }

    public static boolean canInteractWith(Claim claim, Block block, UUID player) {
        return claim.hasPermission(player, Claim.Permission.INTERACT_BLOCKS) ||
                (BlockUtils.isButton(block) && claim.hasPermission(player, Claim.Permission.USE_BUTTONS)) ||
                (BlockUtils.isLever(block) && claim.hasPermission(player, Claim.Permission.USE_LEVERS)) ||
                (BlockUtils.isDoor(block) && claim.hasPermission(player, Claim.Permission.INTERACT_DOORS)) ||
                (BlockUtils.isContainer(block) && claim.hasPermission(player, Claim.Permission.CONTAINER)) ||
                (BlockUtils.isChest(block) && claim.hasPermission(player, Claim.Permission.CONTAINER_CHEST)) ||
                (BlockUtils.isEnderchest(block) && claim.hasPermission(player, Claim.Permission.CONTAINER_ENDERCHEST)) ||
                (BlockUtils.isShulkerBox(block) && claim.hasPermission(player, Claim.Permission.CONTAINER_SHULKERBOX));
    }

    public static boolean canInteractUsingItem(Claim claim, Item item, UUID player) {
        return claim.hasPermission(player, Claim.Permission.USE_ITEMS_ON_BLOCKS) ||
                (item instanceof BlockItem && claim.hasPermission(player, Claim.Permission.BUILD)) ||
                (item instanceof BucketItem && claim.hasPermission(player, Claim.Permission.BUILD));
    }


    public static boolean canDamageWithProjectile(ThrownEntity thrownEntity, Entity entity) {
        if (checkCanDamageWithProjectile(entity, thrownEntity.getServer(), ((OwnedProjectile) thrownEntity).getOwner())) {
            return true;
        }

        thrownEntity.kill();
        return false;
    }

    public static boolean canDamageWithProjectile(ProjectileEntity projectile, Entity entity) {
        if (checkCanDamageWithProjectile(entity, projectile.getServer(), ((OwnedProjectile) projectile).getOwner())) {
            projectile.kill();
            return true;
        }

        return false;
    }

    public static boolean checkCanDamageWithProjectile(Entity entity, MinecraftServer server, UUID uuid) {
        if (entity.world.isClient)
            return true;

        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(uuid);
        Claim claim = ClaimManager.INSTANCE.getClaimAt(entity.getBlockPos(), entity.world.getDimension().getType());

        if (claim != null && owner != null && !claim.hasPermission(owner.getUuid(), Claim.Permission.DAMAGE_ENTITY)) {
            owner.sendMessage(Messages.MSG_DAMAGE_ENTITY);
            return false;
        }

        return true;
    }

    public static BlockPos getPosOnGround(BlockPos pos, World world) {
        BlockPos blockPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());

        do {
            blockPos = blockPos.down();
        } while (world.getBlockState(blockPos).isAir());

        return blockPos.up();
    }


}
