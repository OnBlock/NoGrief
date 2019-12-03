package io.github.indicode.fabric.itsmine;

/**
 * @author Indigo Amann
 */
public interface ClaimShower {
    void setLast2dHeight(int height);
    void setShownClaim(Claim claim);
    Claim getShownClaim();
    int getLast2dHeight();
}
