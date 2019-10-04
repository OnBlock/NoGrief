package io.github.indicode.fabric.itsmine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class ClaimManager {
    public static ClaimManager INSTANCE = null;
    private HashMap<UUID, Integer> blocksLeft = new HashMap<>();
    public List<UUID> ignoringClaims = new ArrayList<>();
    public int getClaimBlocks(UUID id) {
        return blocksLeft.getOrDefault(id, Config.baseClaimBlocks);
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
    public void useBlocksUntil0(UUID player, int amount) {
        if (!useClaimBlocks(player, amount)) blocksLeft.put(player, 0);
    }
    public void setClaimBlocks(UUID player, int amount) {
        blocksLeft.put(player, amount < 0 ? 0 : amount);
    }
    public void releaseBlocksToOwner(Claim claim) {
        addClaimBlocks(claim.owner, claim.getArea());
    }
    public HashMap<String, Claim> claimsByName = new HashMap<>();
    public List<Claim> getPlayerClaims(UUID id) {
        List<Claim> list = new ArrayList<>();
        claimsByName.values().forEach(claim -> {
            if (claim.owner.equals(id)) list.add(claim);
        });
        return list;
    }
    public boolean addClaim(Claim claim) {
        if (wouldIntersect(claim)) return false;
        claimsByName.put(claim.name, claim);
        return true;
    }
    public boolean wouldIntersect(Claim claim) {
        for (Claim value : claimsByName.values()) {
            if(claim.intersects(value)) return true;
        }
        return false;
    }
    public CompoundTag toNBT() {
        CompoundTag tag =  new CompoundTag();
        ListTag list = new ListTag();
        claimsByName.values().forEach(claim -> list.add(claim.toTag()));
        tag.put("claims", list);
        CompoundTag blocksLeftTag = new CompoundTag();
        blocksLeft.forEach((id, amount) -> blocksLeftTag.putInt(id.toString(), amount));
        tag.put("blocksLeft", blocksLeftTag);
        ListTag ignoring = new ListTag();
        CompoundTag tvargetter = new CompoundTag();
        ignoringClaims.forEach(id -> {
            tvargetter.putString("id", id.toString());
            ignoring.add(tvargetter.getTag("id"));
        });
        tag.put("ignoring", ignoring);
        return tag;
    }
    public Claim getClaimAt(BlockPos pos, DimensionType dimension) {
        for (Claim claim : claimsByName.values()) {
            if (claim.dimension.equals(dimension) && claim.includesPosition(pos)) return claim.getZoneCovering(pos);
        }
        return null;
    }
    public void fromNBT(CompoundTag tag) {
        ListTag list = (ListTag) tag.getTag("claims");
        claimsByName.clear();
        list.forEach(it -> {
            Claim claim = new Claim();
            claim.fromTag((CompoundTag) it);
            claimsByName.put(claim.name, claim);
        });
        CompoundTag blocksLeftTag = tag.getCompound("blocksLeft");
        blocksLeft.clear();
        blocksLeftTag.getKeys().forEach(key -> blocksLeft.put(UUID.fromString(key), blocksLeftTag.getInt(key)));
        ListTag ignoringTag = (ListTag) tag.getTag("ignoring");
        ignoringTag.forEach(it -> ignoringClaims.add(UUID.fromString(it.asString())));
    }
}
