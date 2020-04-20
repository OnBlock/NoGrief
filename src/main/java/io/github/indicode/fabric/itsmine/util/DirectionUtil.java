package io.github.indicode.fabric.itsmine.util;

import net.minecraft.util.math.Direction;

public class DirectionUtil {
    public static Direction directionByName(String name) {
        for (Direction direction : Direction.values()) {
            if (name.equals(direction.getName())) return direction;
        }
        return null;
    }
}
