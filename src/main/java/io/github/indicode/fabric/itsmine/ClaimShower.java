package io.github.indicode.fabric.itsmine;

import io.github.indicode.fabric.itsmine.claim.Claim;
import net.minecraft.util.math.BlockPos;

/**
 * @author Indigo Amann
 */
public interface ClaimShower {
    void setLastShowPos(BlockPos pos);
    void setShownClaim(Claim claim);
    void setShowMode(String mode);
    Claim getShownClaim();
    String getMode();
    BlockPos getLastShowPos();
}
