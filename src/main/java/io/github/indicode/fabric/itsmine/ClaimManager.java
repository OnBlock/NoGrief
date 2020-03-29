package io.github.indicode.fabric.itsmine;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import javax.naming.Name;
import java.util.*;

/**
 * @author Indigo Amann
 */
public class ClaimManager {
    public Map<PlayerEntity, Pair<BlockPos, BlockPos>> stickPositions = new HashMap<>();
    public static ClaimManager INSTANCE = null;
    private HashMap<UUID, Integer> blocksLeft = new HashMap<>();
    public List<UUID> ignoringClaims = new ArrayList<>();
    public List<UUID> flyers = new ArrayList<>();
    public int getClaimBlocks(UUID id) {
        return blocksLeft.getOrDefault(id, Config.claims2d ? Config.baseClaimBlocks2d : Config.baseClaimBlocks3d);
    }
    public boolean useClaimBlocks(UUID player, int amount) {
        int blocks = getClaimBlocks(player) - amount;
        if (blocks < 0) return false;
        blocksLeft.put(player, blocks);
        return true;
    }
    public void addClaimBlocks(UUID player, int amount) {
        useBlocksUntil0(player, -amount);
    }
    public void addClaimBlocks(Collection<ServerPlayerEntity> players, int amount) {
        players.forEach(player -> useBlocksUntil0(player.getGameProfile().getId(), -amount));
    }
    public void useBlocksUntil0(UUID player, int amount) {
        if (!useClaimBlocks(player, amount)) blocksLeft.put(player, 0);
    }
    public void setClaimBlocks(Collection<ServerPlayerEntity> players, int amount) {
        players.forEach(player -> setClaimBlocks(player.getGameProfile().getId(), -amount));
    }

    public Claim getClaim(String name){
        return claimsByName.get(name);
    }

    public void setClaimBlocks(UUID player, int amount) {
        blocksLeft.put(player, Math.max(amount, 0));
    }
    public void releaseBlocksToOwner(Claim claim) {
        if (claim.claimBlockOwner != null) addClaimBlocks(claim.claimBlockOwner, claim.getArea());
    }
    public HashMap<String, Claim> claimsByName = new HashMap<>();
    public List<Claim> getPlayerClaims(UUID id) {
        List<Claim> list = new ArrayList<>();
        claimsByName.values().forEach(claim -> {
            if (claim.claimBlockOwner != null && claim.claimBlockOwner.equals(id)) list.add(claim);
        });
        return list;
    }
    public boolean addClaim(Claim claim) {
        if (claimsByName.containsKey(claim.name)) return false;
        if (wouldIntersect(claim) && !claim.isChild) return false;
        claimsByName.put(claim.name, claim);
        return true;
    }
    public boolean wouldIntersect(Claim claim) {
        for (Claim value : claimsByName.values()) {
            if(!claim.name.equals(value.name) && claim.intersects(value)) return true;
        }
        return false;
    }

    public boolean wouldSubzoneIntersect(Claim claim) {
        for (Claim value : claimsByName.values()) {
                if(!claim.name.equals(value.name) && claim.intersects(value, true, true)){
                    return true;
                }

        }
        return false;
    }
    public CompoundTag toNBT() {
        CompoundTag tag =  new CompoundTag();
        ListTag list = new ListTag();
        claimsByName.values().forEach(claim -> {
            if(!claim.isChild){
                list.add(claim.toTag());
            }
        });
        tag.put("claims", list);
        CompoundTag blocksLeftTag = new CompoundTag();
        blocksLeft.forEach((id, amount) -> {if (id != null) blocksLeftTag.putInt(id.toString(), amount);});
        tag.put("blocksLeft", blocksLeftTag);
        ListTag ignoring = new ListTag();
        CompoundTag tvargetter = new CompoundTag();
        ignoringClaims.forEach(id -> {
            tvargetter.putString("id", id.toString());
            ignoring.add(tvargetter.get("id"));
        });
        ListTag listTag = new ListTag();
        for (UUID flyer : flyers) {
            CompoundTag tag1 = new CompoundTag();
            tag1.putUuidNew("uuid", flyer);
            listTag.add(tag1);
        }
        tag.put("flyers", listTag);
        tag.put("ignoring", ignoring);
        return tag;
    }

    public Claim getMainClaimAt(BlockPos pos, DimensionType dimension){
        for (Claim claim : claimsByName.values()) {
            if(claim.dimension.equals(dimension) && claim.includesPosition(pos)){
                return claim.getZoneCovering(pos);
            }
        }
        return null;
    }

    public Claim getSubzoneClaimAt(BlockPos pos, DimensionType dimension) {
        for (Claim claim : claimsByName.values()) {
            if (claim.dimension.equals(dimension) && claim.includesPosition(pos)) {
                for(Claim subzone : claim.children){
                    if(subzone.dimension.equals(dimension) && subzone.includesPosition(pos)){
                        return subzone.getZoneCovering(pos);
                    }
                }
                return null;
            }
        }
        return null;
    }

    @Nullable
    public Claim getClaimAt(BlockPos pos, DimensionType dimension) {
        for (Claim claim : claimsByName.values()) {
            if (claim.dimension.equals(dimension) && claim.includesPosition(pos)) {
                for(Claim subzone : claim.children){
                    if(subzone.dimension.equals(dimension) && subzone.includesPosition(pos)){
                        return subzone.getZoneCovering(pos);
                    }
                }
                return claim.getZoneCovering(pos);
            }
        }

        return null;
    }
    public void fromNBT(CompoundTag tag) {
        ListTag list = (ListTag) tag.get("claims");
        claimsByName.clear();
        list.forEach(it -> {
            Claim claim = new Claim();
            claim.fromTag((CompoundTag) it);
            claimsByName.put(claim.name, claim);
            for(Claim subzone : claim.children){
                claimsByName.put(subzone.name, subzone);
            }
        });
        CompoundTag blocksLeftTag = tag.getCompound("blocksLeft");
        blocksLeft.clear();
        blocksLeftTag.getKeys().forEach(key -> blocksLeft.put(UUID.fromString(key), blocksLeftTag.getInt(key)));
        ListTag ignoringTag = (ListTag) tag.get("ignoring");
        ignoringTag.forEach(it -> ignoringClaims.add(UUID.fromString(it.asString())));
        ListTag listTag = tag.getList("flyers", 11);
        flyers.clear();
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag tag1 = listTag.getCompound(i);
            flyers.add(tag1.getUuidNew("uuid"));
        }
    }
}
