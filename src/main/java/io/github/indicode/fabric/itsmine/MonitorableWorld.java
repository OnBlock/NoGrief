package io.github.indicode.fabric.itsmine;

import net.minecraft.entity.Entity;

import java.util.Map;
import java.util.UUID;

public interface MonitorableWorld {

    int loadedEntities();

    Map<UUID, Entity> EntityList();
}
