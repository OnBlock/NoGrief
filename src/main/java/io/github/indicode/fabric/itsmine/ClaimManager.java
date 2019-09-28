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
    public HashMap<UUID, Integer> usedClaims = new HashMap<>();
    public HashMap<String, Claim> claimsByName = new HashMap<>();
    public List<Claim> getPlayerClaims(UUID id) {
        List<Claim> list = new ArrayList<>();
        claimsByName.values().forEach(claim -> {
            if (claim.owner.equals(id)) list.add(claim);
        });
        return list;
    }
    public boolean addClaim(Claim claim) {
        for (Claim value : claimsByName.values()) {
            if(claim.intersects(value)) return false;
        }
        claimsByName.put(claim.name, claim);
        return true;
    }
    public CompoundTag toNBT() {
        CompoundTag tag =  new CompoundTag();
        ListTag list = new ListTag();
        claimsByName.values().forEach(claim -> list.add(claim.toTag()));
        tag.put("claims", list);
        CompoundTag usedClaimsTag = new CompoundTag();
        usedClaims.forEach((id, amount) -> usedClaimsTag.putInt(id.toString(), amount));
        tag.put("usedClaims", usedClaimsTag);
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
        CompoundTag usedClaimsTag = tag.getCompound("usedClaims");
        usedClaims.clear();
        usedClaimsTag.getKeys().forEach(key -> usedClaims.put(UUID.fromString(key), usedClaimsTag.getInt(key)));
    }
}
