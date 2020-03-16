package io.github.indicode.fabric.itsmine;

import net.minecraft.entity.Entity;
import net.minecraft.util.registry.Registry;

import java.util.UUID;

public class EntityUtils {

    public static boolean canAttack(UUID player, Claim claim, Entity entity) {
        return claim.hasPermission(player, Claim.Permission.DAMAGE_ENTITY) ||
                (isPassive(entity) && claim.hasPermission(player, Claim.Permission.DAMAGE_ENTITY_PASSIVE) ||
                        (isHostile(entity) && claim.hasPermission(player, Claim.Permission.DAMAGE_ENTITY_HOSTILE)));
    }

    public static boolean isPassive(Entity entity) {
        System.out.println("Is Passive " + Registry.ENTITY_TYPE.getId(entity.getType()).toString() + " " + entity.getType().getCategory().isPeaceful());
        return entity.getType().getCategory().isPeaceful();
    }

    public static boolean isHostile(Entity entity) {
        return !isPassive(entity);
    }
}
