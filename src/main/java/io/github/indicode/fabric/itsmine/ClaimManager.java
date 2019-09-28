package io.github.indicode.fabric.itsmine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.math.BlockPos;

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
    public int getClaimBlocks(UUID id) {
        return blocksLeft.getOrDefault(id, Config.baseClaimBlocks);
    }
    public boolean useClaimBlocks(UUID player, int amount) {
        int blocks = getClaimBlocks(player) - amount;
        if (blocks < 0) return false;
        blocksLeft.put(player, blocks);
        return true;
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
        return tag;
    }
    public Claim getClaimAt(BlockPos pos) {
        for (Claim claim : claimsByName.values()) {
            if (claim.includesPosition(pos)) return claim;
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
    }
}
