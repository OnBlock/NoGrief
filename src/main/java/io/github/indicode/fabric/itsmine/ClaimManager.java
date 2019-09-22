package io.github.indicode.fabric.itsmine;

import net.minecraft.util.math.BlockPos;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class ClaimManager {
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
}
