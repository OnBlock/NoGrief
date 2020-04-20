package io.github.indicode.fabric.itsmine.util;

import io.github.indicode.fabric.itsmine.Claim;
import io.github.indicode.fabric.itsmine.ClaimShower;
import io.github.indicode.fabric.itsmine.mixin.BlockUpdatePacketMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ShowerUtil {

    public static void update(Claim claim, ServerWorld world){
        update(claim, world, false);
        update(claim, world, true);
    }
    public static void update(Claim claim, ServerWorld world, boolean hide){
        world.getPlayers().forEach(playerEntity -> {
            ClaimUtil.getParentClaim(claim);
            if (((ClaimShower)playerEntity).getShownClaim() != null && ((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, hide, false);
        });
    }

    public static void silentHideShow(ServerPlayerEntity player, Claim claim, boolean hide, boolean updateStatus) {
        if (updateStatus) {
            if (!hide) ((ClaimShower) player).setShownClaim(claim);
            else ((ClaimShower) player).setShownClaim(null);
        }
        BlockState block = hide ? null : Blocks.GOLD_BLOCK.getDefaultState();
        showCorners(player, claim, hide, block);
        if(!claim.isChild){
            block = hide ? null : Blocks.DIAMOND_BLOCK.getDefaultState();
            for(Claim subzone : claim.children){
                showCorners(player, subzone, hide, block);
            }
        }

    }

    private static void showCorners(ServerPlayerEntity player, Claim claim, boolean hide, BlockState state){
        state = hide ? null : state;
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), 256, claim.min.getZ()), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), 256, claim.max.getZ()), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), 256, claim.min.getZ()), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), 256, claim.max.getZ()), player.getEntityWorld())).down(), state);

        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), 256, claim.min.getZ()).south(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), 256, claim.max.getZ()).east(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), 256, claim.min.getZ()).south(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), 256, claim.max.getZ()).north(), player.getEntityWorld())).down(), state);

        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), 256, claim.min.getZ()).east(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), 256, claim.max.getZ()).north(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), 256, claim.min.getZ()).west(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), 256, claim.max.getZ()).west(), player.getEntityWorld())).down(), state);
    }

    private static void sendBlockPacket(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        BlockUpdateS2CPacket packet =  new BlockUpdateS2CPacket(player.world, pos);
        if (state != null) ((BlockUpdatePacketMixin)packet).setState(state);
        player.networkHandler.sendPacket(packet);
    }

}
