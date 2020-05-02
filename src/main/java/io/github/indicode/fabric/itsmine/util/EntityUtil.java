package io.github.indicode.fabric.itsmine.util;

import io.github.indicode.fabric.itsmine.claim.Claim;
import io.github.indicode.fabric.itsmine.Functions;
import io.github.indicode.fabric.itsmine.MonitorableWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.github.indicode.fabric.itsmine.util.WorldUtil.getServerWorld;

public class EntityUtil {

    public static boolean canAttack(UUID player, Claim claim, Entity entity) {
        return claim.hasPermission(player, Claim.Permission.DAMAGE_ENTITY) ||
                (isPassive(entity) && claim.hasPermission(player, Claim.Permission.DAMAGE_ENTITY_PASSIVE) ||
                        (isHostile(entity) && claim.hasPermission(player, Claim.Permission.DAMAGE_ENTITY_HOSTILE)));
    }

    public static boolean isPassive(Entity entity) {
        return entity.getType().getCategory().isPeaceful();
    }

    public static boolean isHostile(Entity entity) {
        return !isPassive(entity);
    }

    public static ArrayList<Entity> getEntities(Claim claim){
        ArrayList<Entity> entityList = new ArrayList<>();
        ServerWorld world = getServerWorld(claim.dimension);
        MonitorableWorld monitorableWorld = (MonitorableWorld) world;
        monitorableWorld.EntityList().forEach((uuid, entity) -> {
            if(claim.includesPosition(entity.getBlockPos())) entityList.add(entity);
        });
        return entityList;
    }

    public static ArrayList<Entity> filterByCategory(ArrayList<Entity> entityList, EntityCategory entityCategory){
        ArrayList<Entity> filteredEntityList = new ArrayList<>();
        for(Entity entity : entityList) if(entity.getType().getCategory() == entityCategory) filteredEntityList.add(entity);
        return filteredEntityList;
    }

    public static Map<EntityType, Integer> sortByType(ArrayList<Entity> entityList){
        Map<EntityType, Integer> entityMap = new HashMap<>();
        for(Entity entity : entityList) {
            EntityType entityType = entity.getType();
            if (entityMap.containsKey(entityType)) {
                entityMap.put(entityType, entityMap.get(entityType)+1);
            } else {
                entityMap.put(entityType, 1);
            }
        }
        return Functions.sortByValue(entityMap);
    }



}
