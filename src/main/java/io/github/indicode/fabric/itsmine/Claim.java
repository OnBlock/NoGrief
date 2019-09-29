package io.github.indicode.fabric.itsmine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;

/**
 * @author Indigo Amann
 */
public class Claim {
    public String name;
    public BlockPos min, max;
    public DimensionType dimension;
    public Map<UUID, ClaimPermissions> permssionsMap = new HashMap();
    public List<Claim> children = new ArrayList<>();
    public ClaimSettings settings = new ClaimSettings();
    public UUID owner;
    public Claim() {

    }
    public Claim(CompoundTag tag) {
        fromTag(tag);
    }
    public Claim(String name, UUID owner, BlockPos min, BlockPos max, DimensionType dimension) {
        this.min = min;
        this.max = max;
        this.owner = owner;
        this.name = name;
        this.dimension = dimension;
    }
    public boolean includesPosition(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getY() >= min.getY() && pos.getZ() >= min.getZ() &&
                pos.getX() <= max.getX() && pos.getY() <= max.getY() && pos.getZ() <= max.getZ();
    }
    public boolean intersects(Claim claim) {
        return intersects(claim, true);
    }
    public boolean intersects(Claim claim, boolean checkOther) {
        if (claim == null) return false;
        if (!claim.dimension.equals(dimension)) return false;
        BlockPos a = min,
                b = max,
                c = new BlockPos(max.getX(), min.getY(), min.getZ()),
                d = new BlockPos(min.getX(), max.getY(), min.getZ()),
                e = new BlockPos(min.getX(), min.getY(), max.getZ()),
                f = new BlockPos(max.getX(), max.getY(), min.getZ()),
                g = new BlockPos(max.getX(), min.getY(), max.getZ()),
                h = new BlockPos(min.getX(), max.getY(), max.getZ());
        if (
                claim.includesPosition(a) ||
                claim.includesPosition(b) ||
                claim.includesPosition(c) ||
                claim.includesPosition(d) ||
                claim.includesPosition(e) ||
                claim.includesPosition(f) ||
                claim.includesPosition(g) ||
                claim.includesPosition(h)
        ) return true;
        else return checkOther && claim.intersects(this, false);
    }
    public Claim getZoneCovering(BlockPos pos) {
        if (includesPosition(pos)) {
            for (Claim child : children) {
                Claim value = child.getZoneCovering(pos);
                if (value != null) return value;
            }
            return this;
        } else return null;
    }
    public ClaimSettings getSettingsAt(BlockPos pos) {
        Claim at = getZoneCovering(pos);
        if (at != null) {
            return at.settings;
        } else return null;
    }
    public ClaimPermissions getPermissionsAt(UUID player, BlockPos pos) {
        Claim at = getZoneCovering(pos);
        if (at != null) {
            return at.getPlayerPermissions(player);
        } else return settings;
    }
    public ClaimPermissions initializePermissions() {
        ClaimPermissions permissions = new ClaimPermissions();
        permissions.fromTag(settings.toTag().getCompound("permissions"));
        return permissions;
    }
    public ClaimPermissions getPlayerPermissions(UUID player) {
        if (permssionsMap.containsKey(player)) return permssionsMap.get(player);
        else return settings;
    }
    public boolean hasPermission(UUID player, ClaimPermissions.Permission permission) {
        return player.equals(owner) || ClaimManager.INSTANCE.ignoringClaims.contains(player) || getPlayerPermissions(player).hasPermission(permission);
    }
    public boolean hasPermissionAt(UUID player, ClaimPermissions.Permission permission, BlockPos pos) {
        return player.equals(owner) || ClaimManager.INSTANCE.ignoringClaims.contains(player) || getPermissionsAt(player, pos).hasPermission(permission);
    }
    public void addSubzone(Claim claim) {
        if (claim != null && claim.dimension == dimension && includesPosition(claim.min) && includesPosition(claim.max)) {
            children.add(claim);
        } else throw new IllegalArgumentException("Subzone must be inside the original claim, in the same dimension, and not null");
    }
    public void expand(BlockPos min, BlockPos max) {
        this.min = this.min.add(min);
        this.max = this.max.add(max);
    }
    public BlockPos getSize() {
        return max.subtract(min);
    }
    public void expand(BlockPos modifier) {
        (modifier.getX() > 0 ? max : min).add(modifier.getX(), 0, 0);
        (modifier.getY() > 0 ? max : min).add(0, modifier.getY(), 0);
        (modifier.getZ() > 0 ? max : min).add(0, 0, modifier.getZ());
    }
    public void expand(Direction direction, int distance) {
        expand(new BlockPos(direction.getOffsetX() * distance, direction.getOffsetY() * distance, direction.getOffsetZ() * distance));
    }
    public int getArea() {
        return getSize().getX() * getSize().getY() * getSize().getZ();
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
            pos.putString("dimension", DimensionType.getId(dimension).toString());
            tag.put("position", pos);
        }
        {
            ListTag subzoneList = new ListTag();
            children.forEach(it -> subzoneList.add(it.toTag()));
            tag.put("subzones", subzoneList);
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
            this.dimension = DimensionType.byId(new Identifier(pos.getString("dimension")));
        }
        {
            children = new ArrayList<>();
            ListTag subzoneList = (ListTag) tag.getTag("subzones");
            if (subzoneList != null) {
                subzoneList.forEach(it -> children.add(new Claim((CompoundTag) it)));
            }
        }
        {
            this.settings = new ClaimSettings(tag.getCompound("settings"));
            this.permssionsMap = new HashMap<>();
            CompoundTag permissions = tag.getCompound("permissions");
            permissions.getKeys().forEach(key -> permssionsMap.put(UUID.fromString(key), new ClaimPermissions(permissions.getCompound(key))));
            this.owner = tag.getUuid("owner");
        }
        name = tag.getString("name");
    }

    public static class ClaimPermissions {
        public enum Permission {
            MODIFY_WORLD("modify_world", "Modify Blocks");
            String id, name;
            Permission(String id, String name) {
                this.id = id;
                this.name =  name;
            }
            public static Permission byId(String id) {
                for (Permission permission: values()) {
                    if (permission.id.equals(id)) return permission;
                }
                return null;
            }
        }
        protected  List<Permission> perms = new ArrayList<>();
        public ClaimPermissions(CompoundTag tag) {
            fromTag(tag);
        }
        public ClaimPermissions() {
        }
        public CompoundTag toTag() {
            CompoundTag tag =  new CompoundTag();
            ListTag listTag =  new ListTag();
            CompoundTag tvargetter = new CompoundTag();
            perms.forEach(perm -> {
                tvargetter.putString("it", perm.id);
                listTag.add(tvargetter.getTag("it"));
            });
            tag.put("permissions", listTag);
            return tag;
        }
        public void fromTag(CompoundTag tag) {
            perms.clear();
            ((ListTag)tag.getTag("permissions")).forEach(key -> {
                perms.add(Permission.byId(key.asString()));
            });
        }
        public boolean hasPermission(Permission permission) {
            return perms.contains(permission);
        }
        public void setPermission(Permission permission, boolean value) {
            if (value && !hasPermission(permission)) {
                perms.add(permission);
                return;
            }
            if (!value) perms.remove(permission);
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
