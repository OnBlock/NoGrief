package io.github.indicode.fabric.itsmine.util;

import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.ClaimShower;
import io.github.indicode.fabric.itsmine.mixin.BlockUpdatePacketMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class ShowerUtil {


    public static void update(Claim claim, ServerWorld world, boolean hide){
        world.getPlayers().forEach(playerEntity -> {
            ClaimUtil.getParentClaim(claim);
            if (((ClaimShower)playerEntity).getShownClaim() != null && ((ClaimShower)playerEntity).getShownClaim().name.equals(claim.name)) silentHideShow(playerEntity, claim, hide, false, ((ClaimShower) playerEntity).getMode());
        });
    }

    public static void silentHideShow(ServerPlayerEntity player, Claim claim, boolean hide, boolean updateStatus, String mode) {
        if (updateStatus) {
            if (!hide) {
                ((ClaimShower) player).setShownClaim(claim);
                ((ClaimShower) player).setShowMode(mode);
            }
            else ((ClaimShower) player).setShownClaim(null);
        }
        BlockState block = hide ? null : Blocks.GOLD_BLOCK.getDefaultState();
        show(player, claim, hide, block, mode);
        if(!claim.isChild){
            block = hide ? null : Blocks.DIAMOND_BLOCK.getDefaultState();
            for(Claim subzone : claim.children){
                show(player, subzone, hide, block, mode);
            }
        }

    }

    private static void show(ServerPlayerEntity player, Claim claim, boolean hide, BlockState state, String mode){
        state = hide ? null : state;
        if(mode == null) mode = ((ClaimShower)player).getMode() == null ? "outline" : ((ClaimShower)player).getMode();

        switch (mode){
            case "outline": {
                showOutline(player, claim, state);break;
            }
            case "corner": {
                showCorner(player, claim, state);break;
            }
            default: {
                player.sendSystemMessage(new LiteralText("Unknown mode").formatted(Formatting.RED), player.getUuid());
            }
        }
    }

    private static void showOutline(ServerPlayerEntity player, Claim claim, BlockState state){
        int y = player.getBlockPos().getY();
        for(int i = claim.min.getX(); i < claim.max.getX(); i++){
            sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(i, y, claim.min.getZ()), player.getEntityWorld())).down(), state);
        }
        for(int i = claim.min.getZ(); i < claim.max.getZ(); i++){
            sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), y, i), player.getEntityWorld())).down(), state);
        }
        for(int i = claim.max.getX(); i > claim.min.getX(); i--){
            sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(i, y, claim.max.getZ()), player.getEntityWorld())).down(), state);
        }
        for(int i = claim.max.getZ(); i > claim.min.getZ(); i--){
            sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), y, i), player.getEntityWorld())).down(), state);
        }
    }

    private static void showCorner(ServerPlayerEntity player, Claim claim, BlockState state){
        int y = player.getBlockPos().getY();
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), y, claim.min.getZ()), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), y, claim.max.getZ()), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), y, claim.min.getZ()), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), y, claim.max.getZ()), player.getEntityWorld())).down(), state);

        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), y, claim.min.getZ()).south(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), y, claim.max.getZ()).east(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), y, claim.min.getZ()).south(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), y, claim.max.getZ()).north(), player.getEntityWorld())).down(), state);

        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), y, claim.min.getZ()).east(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.min.getX(), y, claim.max.getZ()).north(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), y, claim.min.getZ()).west(), player.getEntityWorld())).down(), state);
        sendBlockPacket(player, new BlockPos(ClaimUtil.getPosOnGround(new BlockPos(claim.max.getX(), y, claim.max.getZ()).west(), player.getEntityWorld())).down(), state);
    }

    private static void sendBlockPacket(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        BlockUpdateS2CPacket packet =  new BlockUpdateS2CPacket(player.world, pos);
        if (state != null) ((BlockUpdatePacketMixin)packet).setState(state);
        player.networkHandler.sendPacket(packet);
    }

}
