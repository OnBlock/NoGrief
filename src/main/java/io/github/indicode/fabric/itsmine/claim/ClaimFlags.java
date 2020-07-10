package io.github.indicode.fabric.itsmine.claim;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class ClaimFlags{
    public enum Flag {
        FLIGHT_ALLOWED("flight_allowed", "Flying Allowed", true),
        EXPLOSION_DESTRUCTION("explosion_destruction", "Explosion Destroys Blocks", false),
        EXPLOSION_DAMAGE("explosion_damage", "Explosion Damages Entities", false),
        FLUID_CROSSES_BORDERS("fluid_crosses_borders", "Fluid Crosses Borders", false),
        FIRE_CROSSES_BORDERS("fire_crosses_borders", "Fire Crosses Borders", false),
        FIRE_DAMAGE("fire_damage", "Fire Damages Entities", false),
        PISTON_FROM_INSIDE("pistons_inside_border", "Pistons Cross border from Inside", true),
        PISTON_FROM_OUTSIDE("pistons_outside_border", "Pistons Cross border from Outside", false),
        MOB_SPAWNING("mob_spawn", "Natural mob spawning", true),
        //            KEEP_INVENTORY("keep_inventory", "Keep Inventory", true),
        ENTER_SOUND("enter_sound", "Enter Sound", false),
        BREAK_FARMLANDS("break_farmlands", "Break Farmlands", false);

        public String id, name;
        boolean defaultValue;

        Flag(String id, String name, boolean defaultValue) {
            this.id = id;
            this.name =  name;
            this.defaultValue = defaultValue;
        }
        public static Flag byId(String id) {
            for (Flag permission: values()) {
                if (permission.id.equals(id)) return permission;
            }
            return null;
        }
    }
    public Map<Flag, Boolean> flags = new HashMap<>();
    public ClaimFlags(CompoundTag tag) {
        fromTag(tag);
    }
    public ClaimFlags() {
    }
    public boolean getFlag(Flag flag) {
        return flags.getOrDefault(flag, flag.defaultValue);
    }
    public CompoundTag toTag() {
        CompoundTag tag =  new CompoundTag();
        this.flags.forEach((flag, data) -> {
            tag.putBoolean(flag.id, data);
        });
        return tag;
    }
    public void fromTag(CompoundTag tag) {
        flags.clear();
        tag.getKeys().forEach(key -> {
            Flag flag = Flag.byId(key);
            if (flag == null) return;
            this.flags.put(flag, tag.getBoolean(key));
        });
    }
}
