package io.github.indicode.fabric.itsmine.claim;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class ClaimSettings{
    public enum Setting {
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

        Setting(String id, String name, boolean defaultValue) {
            this.id = id;
            this.name =  name;
            this.defaultValue = defaultValue;
        }
        public static Setting byId(String id) {
            for (Setting permission: values()) {
                if (permission.id.equals(id)) return permission;
            }
            return null;
        }
    }
    public Map<Setting, Boolean> settings = new HashMap<>();
    public ClaimSettings(CompoundTag tag) {
        fromTag(tag);
    }
    public ClaimSettings() {
    }
    public boolean getSetting(Setting setting) {
        return settings.getOrDefault(setting, setting.defaultValue);
    }
    public CompoundTag toTag() {
        CompoundTag tag =  new CompoundTag();
        this.settings.forEach((setting, data) -> {
            tag.putBoolean(setting.id, data);
        });
        return tag;
    }
    public void fromTag(CompoundTag tag) {
        settings.clear();
        tag.getKeys().forEach(key -> {
            Setting setting = Setting.byId(key);
            if (setting == null) return;
            this.settings.put(setting, tag.getBoolean(key));
        });
    }
}
