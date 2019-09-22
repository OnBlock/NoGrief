package io.github.indicode.fabric.itsmine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Indigo Amann
 */
public class Claim {
    public String name;
    public BlockPos min, max;
    public Map<UUID, ClaimPermissions> permssionsMap = new HashMap();
    public ClaimSettings settings = new ClaimSettings();
    public UUID owner;
    public void expand(BlockPos min, BlockPos max) {
        this.min = this.min.add(min);
        this.max = this.max.add(max);
    }
    public BlockPos getSize() {
        return min.subtract(max);
    }
    public void expand(BlockPos modifier) {
        (modifier.getX() > 0 ? max : min).add(modifier.getX(), 0, 0);
        (modifier.getY() > 0 ? max : min).add(0, modifier.getY(), 0);
        (modifier.getZ() > 0 ? max : min).add(0, 0, modifier.getZ());
    }
    public void expand(Direction direction, int distance) {
        expand(new BlockPos(direction.getOffsetX() * distance, direction.getOffsetY() * distance, direction.getOffsetZ() * distance));
    }
    public CompoundTag toTag() {
        CompoundTag tag =  new CompoundTag();
        {
            CompoundTag pos = new CompoundTag();
            pos.putInt("minX", min.getX());
            pos.putInt("minY", min.getY());
            pos.putInt("minZ", min.getZ());
            pos.putInt("maxX", max.getX());
            pos.putInt("maxY", max.getY());
            pos.putInt("maxZ", max.getZ());
            tag.put("position", pos);
        }
        {
            tag.put("settings", settings.toTag());
            CompoundTag permissions = new CompoundTag();
            permssionsMap.forEach((id, perms) -> permissions.put(id.toString(), perms.toTag()));
            tag.put("permissions", permissions);
            tag.putUuid("owner", owner);
        }
        tag.putString("name", name);
        return tag;
    }
    public void fromTag(CompoundTag tag) {
        {
            CompoundTag pos = tag.getCompound("position");
            int minX = pos.getInt("minX");
            int minY = pos.getInt("minY");
            int minZ = pos.getInt("minZ");
            int maxX = pos.getInt("maxX");
            int maxY = pos.getInt("maxY");
            int maxZ = pos.getInt("maxZ");
            this.min = new BlockPos(minX, minY, minZ);
            this.max = new BlockPos(maxX, maxY, maxZ);
        }
        {
            this.settings = new ClaimSettings(tag.getCompound("settings"));
            this.permssionsMap = new HashMap<>();
            CompoundTag permissions = new CompoundTag();
            permissions.getKeys().forEach(key -> permssionsMap.put(UUID.fromString(key), new ClaimPermissions(permissions.getCompound(key))));
            this.owner = tag.getUuid("owner");
        }
        name = tag.getString("name");
    }

    public static class ClaimPermissions {
        public ClaimPermissions(CompoundTag tag) {
            fromTag(tag);
        }
        public ClaimPermissions() {
        }
        public CompoundTag toTag() {
            CompoundTag tag =  new CompoundTag();
            return tag;
        }
        public void fromTag(CompoundTag tag) {

        }
    }
    public static class ClaimSettings extends ClaimPermissions{
        public ClaimSettings(CompoundTag tag) {
            fromTag(tag);
        }
        public ClaimSettings() {
        }
        public CompoundTag toTag() {
            CompoundTag tag =  new CompoundTag();
            tag.put("permissions", super.toTag());
            return tag;
        }
        public void fromTag(CompoundTag tag) {
            super.fromTag(tag.getCompound("permissions"));
        }
    }
}
