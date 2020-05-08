package io.github.indicode.fabric.itsmine;

public interface ClaimPlayerEntity {
    void tickMessageCooldown();
    int getMessageCooldown();
    boolean shouldMessage();
    void setMessageCooldown();
    void setMessageCooldown(int cooldown);
}
