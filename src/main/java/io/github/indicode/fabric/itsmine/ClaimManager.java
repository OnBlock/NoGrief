package io.github.indicode.fabric.itsmine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.math.BlockPos;

import javax.naming.Name;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class ClaimManager {
    public static ClaimManager INSTANCE = null;
    public HashMap<String, Claim> claimsByName = new HashMap<>();
    public List<Claim> getPlayerClaims(UUID id) {
        List<Claim> list = new ArrayList<>();
        claimsByName.values().forEach(claim -> {
            if (claim.owner.equals(id)) list.add(claim);
        });
        return list;
    }
    public void addClaim(Claim claim) {
        claimsByName.put(claim.name, claim);
    }
    public ListTag toNBT() {
        ListTag tag = new ListTag();
        claimsByName.values().forEach(claim -> tag.add(claim.toTag()));
        return tag;
    }
    public void fromNBT(ListTag tag) {
        claimsByName.clear();
        tag.forEach(it -> {
            Claim claim = new Claim();
            claim.fromTag((CompoundTag) it);
            claimsByName.put(claim.name, claim);
        });
    }
}
