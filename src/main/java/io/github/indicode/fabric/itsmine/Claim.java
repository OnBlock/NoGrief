package io.github.indicode.fabric.itsmine;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.nbt.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Indigo Amann
 */
public class Claim {
    public String name;
    public BlockPos min, max;
    public DimensionType dimension;
    public List<Claim> children = new ArrayList<>();
    public ClaimSettings settings = new ClaimSettings();
    public PermissionManager permissionManager = new PermissionManager();
    public UUID claimBlockOwner;
    public Claim() {

    }
    public Claim(CompoundTag tag) {
        fromTag(tag);
    }
    public Claim(String name, UUID claimBlockOwner, BlockPos min, BlockPos max, DimensionType dimension) {
        this.claimBlockOwner = claimBlockOwner;
        this.min = min;
        this.max = max;
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
    /*public ClaimSettings getSettingsAt(BlockPos pos) {
        Claim at = getZoneCovering(pos);
        if (at != null) {
            return at.settings;
        } else return settings;
    }
    public ClaimPermissions getPermissionsAt(UUID player, BlockPos pos) {
        Claim at = getZoneCovering(pos);
        if (at != null) {
            return at.getPlayerPermissions(player);
        } else return settings;
    }*/
    public boolean hasPermission(UUID player, Permission permission) {
        return ClaimManager.INSTANCE.ignoringClaims.contains(player) || permissionManager.hasPermission(player, permission);
    }
    //public boolean hasPermissionAt(UUID player, ClaimPermissions.Permission permission, BlockPos pos) {
    //    return player.equals(owner) || ClaimManager.INSTANCE.ignoringClaims.contains(player) || getPermissionsAt(player, pos).hasPermission(permission);
    //}
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
        if (modifier.getX() > 0) max = max.add(modifier.getX(), 0, 0);
        else min = min.add(modifier.getX(), 0, 0);
        if (modifier.getY() > 0) max = max.add(0, modifier.getY(), 0);
        else min = min.add(0, modifier.getY(), 0);
        if (modifier.getZ() > 0) max = max.add(0, 0, modifier.getZ());
        else min = min.add(0, 0, modifier.getZ());
    }
    public boolean shrink(BlockPos modifier) {
        if (modifier.getX() < 0) {
            if (min.getX() - modifier.getX() > max.getX()) return false;
            min = min.add(-modifier.getX(), 0, 0);
        } else {
            if (max.getX() - modifier.getX() < min.getX()) return false;
            max = max.add(-modifier.getX(), 0, 0);
        }
        if (modifier.getY() < 0) {
            if (min.getY() - modifier.getY() > max.getY()) return false;
            min = min.add(0, -modifier.getY(), 0);
        } else {
            if (max.getY() - modifier.getY() < min.getY()) return false;
            max = max.add(0, -modifier.getY(), 0);
        }
        if (modifier.getZ() < 0) {
            if (min.getZ() - modifier.getZ() > max.getZ()) return false;
            min = min.add(0, 0, -modifier.getZ());
        } else {
            if (max.getZ() - modifier.getZ() < min.getZ()) return false;
            max = max.add(0, 0, -modifier.getZ());
        }
        return true;
    }
    public void expand(Direction direction, int distance) {
        expand(new BlockPos(direction.getOffsetX() * distance, direction.getOffsetY() * distance, direction.getOffsetZ() * distance));
    }
    public boolean shrink(Direction direction, int distance) {
        return shrink(new BlockPos(direction.getOffsetX() * distance, direction.getOffsetY() * distance, direction.getOffsetZ() * distance));
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
            tag.put("permissions", permissionManager.toNBT());
            tag.putUuid("top_owner", claimBlockOwner);
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
            ListTag subzoneList = (ListTag) tag.get("subzones");
            if (subzoneList != null) {
                subzoneList.forEach(it -> children.add(new Claim((CompoundTag) it)));
            }
        }
        {
            this.settings = new ClaimSettings(tag.getCompound("settings"));
            permissionManager = new PermissionManager();
            permissionManager.fromNBT(tag.getCompound("permissions"));
            claimBlockOwner = tag.getUuid("top_owner");
        }
        name = tag.getString("name");
    }

    public enum Permission {
        //Admin
        DELETE_CLAIM("delete_claim", "Delete Claim"),
        MODIFY_SIZE("modify_size", "Modify Claim Size"),
        CHANGE_FLAGS("modify_flags", "Change Claim Flags"),
        CHANGE_PERMISSIONS("modify_permissions", "Change Permissions"),
        //Normal
        SPAWN_PROTECT("spawn_protect", "Spawn Protection Bypass"),
        PLACE_BREAK("place_break", "Place/Break Blocks"),
        ACTIVATE_BLOCKS("block_interact", "Right click Blocks"),
        USE_ITEMS_ON_BLOCKS("use_block_modifier_items", "Use Block Modifying items"),
        PRESS_BUTTONS("press_buttons", "Press Buttons"),
        USE_LEVERS("use_levers", "Use Levers"),
        OPEN_DOORS("open_doors", "Use Doors"),
        ENTITY_INTERACT("entity_interact", "Entity Interaction"),
        ENTITY_DAMAGE("entity_damage", "Hurt Entities");
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
    public static class PermissionManager {
        public ClaimPermissionMap defaults = new DefaultPermissionMap();
        protected Map<UUID, ClaimPermissionMap> playerPermissions = new HashMap<>();
        protected Map<String, ClaimPermissionMap> groupPermissions = new HashMap<>();
        public boolean isPermissionSet(UUID player, Permission permission) {
            return playerPermissions.get(player) != null && playerPermissions.get(player).isPermissionSet(permission);
        }
        public boolean hasPermission(UUID player, Permission permission) {
            if (isPermissionSet(player, permission)) return playerPermissions.get(player).hasPermission(permission);
            for (Map.Entry<String, ClaimPermissionMap> entry : groupPermissions.entrySet()) {
                if (Thimble.PERMISSIONS.hasPermission(entry.getKey(), player) && entry.getValue().hasPermission(permission)) return true;
            }
            return defaults.hasPermission(permission);
        }
        public void setPermission(UUID player, Permission permission, boolean enabled) {
            if (!playerPermissions.containsKey(player)) playerPermissions.put(player, new DefaultPermissionMap());
            playerPermissions.get(player).setPermission(permission, enabled);
        }
        public void clearPermission(UUID player, Permission permission) {
            if (!playerPermissions.containsKey(player)) playerPermissions.put(player, new DefaultPermissionMap());
            playerPermissions.get(player).clearPermission(permission);
        }
        public void resetPermissions(UUID player) {
            playerPermissions.remove(player);
        }
        public boolean isPermissionSet(String group, Permission permission) {
            return groupPermissions.get(group) != null && groupPermissions.get(group).isPermissionSet(permission);
        }
        public boolean hasPermission(String group, Permission permission) {
            if (isPermissionSet(group, permission)) return groupPermissions.get(group).hasPermission(permission);
            return defaults.hasPermission(permission);
        }
        public void setPermission(String group, Permission permission, boolean enabled) {
            if (!groupPermissions.containsKey(group)) groupPermissions.put(group, new DefaultPermissionMap());
            groupPermissions.get(group).setPermission(permission, enabled);
        }
        public void clearPermission(String group, Permission permission) {
            if (!groupPermissions.containsKey(group)) groupPermissions.put(group, new DefaultPermissionMap());
            groupPermissions.get(group).clearPermission(permission);
        }
        public void resetPermissions(String group) {
            groupPermissions.remove(group);
        }
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put("defaults", defaults.toRegisteredNBT());
            {
                CompoundTag players = new CompoundTag();
                playerPermissions.forEach((player, map) -> players.put(player.toString(), map.toRegisteredNBT()));
                tag.put("players", players);
            }
            {
                CompoundTag groups = new CompoundTag();
                groupPermissions.forEach((group, map) -> groups.put(group, map.toRegisteredNBT()));
                tag.put("groups", groups);
            }
            return tag;
        }
        public void fromNBT(CompoundTag tag) {
            defaults = ClaimPermissionMap.fromRegisteredNBT(tag.getCompound("defaults"));
            {
                CompoundTag players = tag.getCompound("players");
                playerPermissions.clear();
                players.getKeys().forEach(player -> playerPermissions.put(UUID.fromString(player), ClaimPermissionMap.fromRegisteredNBT(players.getCompound(player))));
            }
            {
                CompoundTag groups = tag.getCompound("groups");
                groupPermissions.clear();
                groups.getKeys().forEach(group -> groupPermissions.put(group, ClaimPermissionMap.fromRegisteredNBT(groups.getCompound(group))));
            }
        }
    }
    public static abstract class ClaimPermissionMap {
        protected static HashMap<String, Class<? extends ClaimPermissionMap>> mapTypes = new HashMap<>();
        protected static HashMap<Class<? extends ClaimPermissionMap>, String> reverseMapTypes = new HashMap<>();
        static {
            mapTypes.put("default", DefaultPermissionMap.class);
            reverseMapTypes.put(DefaultPermissionMap.class, "default");
            mapTypes.put("inverted", InvertedPermissionMap.class);
            reverseMapTypes.put(InvertedPermissionMap.class, "inverted");
        }
        public abstract boolean isPermissionSet(Permission permission);
        public abstract boolean hasPermission(Permission permission);
        public abstract void setPermission(Permission permission, boolean has);
        public abstract void clearPermission(Permission permission);
        public abstract void fromNBT(CompoundTag tag);
        public abstract CompoundTag toNBT();
        public CompoundTag toRegisteredNBT() {
            CompoundTag tag = toNBT();
            tag.putString("type", reverseMapTypes.get(this.getClass()));
            return tag;
        }
        public static ClaimPermissionMap fromRegisteredNBT(CompoundTag tag) {
            String type = tag.getString("type");
            tag.remove("type");
            Class<? extends ClaimPermissionMap> clazz = mapTypes.get(type);
            if (clazz == null) return new DefaultPermissionMap();
            try {
                ClaimPermissionMap map = clazz.newInstance();
                map.fromNBT(tag);
                return map;
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    public static class DefaultPermissionMap extends ClaimPermissionMap {
        private HashMap<Permission, Boolean> permissions = new HashMap<>();
        @Override
        public boolean isPermissionSet(Permission permission) {
            return permissions.containsKey(permission);
        }

        @Override
        public boolean hasPermission(Permission permission) {
            return isPermissionSet(permission) && permissions.get(permission);
        }

        @Override
        public void setPermission(Permission permission, boolean has) {
            permissions.put(permission, has);
        }

        @Override
        public void clearPermission(Permission permission) {
            permissions.remove(permission);
        }

        @Override
        public void fromNBT(CompoundTag tag) {
            permissions.clear();
            for (String permissionString : tag.getKeys()) {
                Permission permission = Permission.byId(permissionString);
                if (permission == null) continue;
                boolean allowed = tag.getBoolean(permissionString);
                permissions.put(permission, allowed);
            }
        }

        @Override
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            permissions.forEach((permission, allowed) -> {
                if (allowed != null) tag.putBoolean(permission.id, allowed);
            });
            return tag;
        }
    }
    public static class InvertedPermissionMap extends ClaimPermissionMap {

        private HashMap<Permission, Boolean> permissions = new HashMap<>();
        @Override
        public boolean isPermissionSet(Permission permission) {
            return true;
        }

        @Override
        public boolean hasPermission(Permission permission) {
            return !permissions.containsKey(permission) || permissions.get(permission);
        }

        @Override
        public void setPermission(Permission permission, boolean has) {
            permissions.put(permission, has);
        }

        @Override
        public void clearPermission(Permission permission) {
            permissions.remove(permission);
        }

        @Override
        public void fromNBT(CompoundTag tag) {
            permissions.clear();
            for (String permissionString : tag.getKeys()) {
                Permission permission = Permission.byId(permissionString);
                if (permission == null) continue;
                boolean allowed = tag.getBoolean(permissionString);
                permissions.put(permission, allowed);
            }
        }

        @Override
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            permissions.forEach((permission, allowed) -> {
                if (allowed != null) tag.putBoolean(permission.id, allowed);
            });
            return tag;
        }
    }
    public static class ClaimSettings{
        private static class SettingData { // Wdym overcomplicated...
            private static BiConsumer<Object, AtomicReference<Tag>> BOOL_WRITER = (data, ref) -> {
                CompoundTag compat = new CompoundTag();
                compat.putBoolean("it", (Boolean) data);
                ref.set(compat.get("it"));
            };
            private static BiConsumer<Tag, AtomicReference> BOOL_READER = (data, ref) -> ref.set(((ByteTag) data).getByte() != 0);
            private static Consumer<AtomicReference<ArgumentType>> BOOL_ARGUMENT = ref -> ref.set(BoolArgumentType.bool());
            private static BiConsumer<CommandContext<ServerCommandSource>, AtomicReference> BOOL_PARSER = (context, ref) -> ref.set(BoolArgumentType.getBool(context, (String)ref.get()));
            private static BiConsumer<Object, AtomicReference<Tag>> STRING_WRITER = (data, ref) -> {
                CompoundTag compat = new CompoundTag();
                compat.putString("it", (String) data);
                ref.set(compat.get("it"));
            };
            private static BiConsumer<Tag, AtomicReference> STRING_READER = (data, ref) -> ref.set(data.asString());
            private static Consumer<AtomicReference<ArgumentType>> GREEDY_STRING_ARGUMENT = ref -> ref.set(StringArgumentType.greedyString());
            private static BiConsumer<CommandContext<ServerCommandSource>, AtomicReference> STRING_PARSER = (context, ref) -> ref.set(StringArgumentType.getString(context, (String)ref.get()));
            private static BiConsumer<Object, AtomicReference<String>> TOSTRING_STRINGIFIER = (data, ref) -> ref.set(data.toString());
        }
        public enum Setting {

            EXPLOSIONS("explosion_destruction", "Explosions Destroy Blocks", SettingData.BOOL_WRITER, SettingData.BOOL_READER, SettingData.BOOL_ARGUMENT, SettingData.BOOL_PARSER, SettingData.TOSTRING_STRINGIFIER, false),
            FLUID_CROSSES_BORDERS("fluid_crosses_borders", "Fluid Crosses Borders", SettingData.BOOL_WRITER, SettingData.BOOL_READER, SettingData.BOOL_ARGUMENT, SettingData.BOOL_PARSER, SettingData.TOSTRING_STRINGIFIER, false),
            FIRE_CROSSES_BORDERS("fire_crosses_borders", "Fire Crosses Borders", SettingData.BOOL_WRITER, SettingData.BOOL_READER, SettingData.BOOL_ARGUMENT, SettingData.BOOL_PARSER, SettingData.TOSTRING_STRINGIFIER, false),
            PISTON_FROM_INSIDE("pistons_inside_border", "Pistons Cross border from Inside", SettingData.BOOL_WRITER, SettingData.BOOL_READER, SettingData.BOOL_ARGUMENT, SettingData.BOOL_PARSER, SettingData.TOSTRING_STRINGIFIER, true),
            PISTON_FROM_OUTSIDE("pistons_outside_border", "Pistons Cross border from Outside", SettingData.BOOL_WRITER, SettingData.BOOL_READER, SettingData.BOOL_ARGUMENT, SettingData.BOOL_PARSER, SettingData.TOSTRING_STRINGIFIER, false);

            String id, name;
            BiConsumer<Object, AtomicReference<Tag>> writer;
            BiConsumer<Tag, AtomicReference> reader;
            Consumer<AtomicReference<ArgumentType>> argumentType;
            BiConsumer<CommandContext<ServerCommandSource>, AtomicReference> parser;
            BiConsumer<Object, AtomicReference<String>> stringifier;
            Object defaultValue;
            Setting(String id, String name, BiConsumer<Object, AtomicReference<Tag>> writer,
                    BiConsumer<Tag, AtomicReference> reader,
                    Consumer<AtomicReference<ArgumentType>> argumentType,
                    BiConsumer<CommandContext<ServerCommandSource>, AtomicReference> parser,
                    BiConsumer<Object, AtomicReference<String>> stringifer,
                    Object defaultValue) {
                this.id = id;
                this.name =  name;
                this.argumentType = argumentType;
                this.parser = parser;
                this.writer = writer;
                this.reader = reader;
                this.stringifier = stringifer;
                this.defaultValue = defaultValue;
            }
            public static ClaimSettings.Setting byId(String id) {
                for (ClaimSettings.Setting permission: values()) {
                    if (permission.id.equals(id)) return permission;
                }
                return null;
            }
        }
        public  Map<Setting, Object> settings = new HashMap<>();
        public ClaimSettings(CompoundTag tag) {
            fromTag(tag);
        }
        public ClaimSettings() {
        }
        public Object getSetting(Setting setting) {
            return settings.getOrDefault(setting, setting.defaultValue);
        }
        public CompoundTag toTag() {
            CompoundTag tag =  new CompoundTag();
            this.settings.forEach((setting, data) -> {
                AtomicReference<Tag> writer = new AtomicReference<>();
                setting.writer.accept(data, writer);
                tag.put(setting.id, writer.get());
            });
            return tag;
        }
        public void fromTag(CompoundTag tag) {
            settings.clear();
            tag.getKeys().forEach(key -> {
                Setting setting = Setting.byId(key);
                if (setting == null) return;
                Tag value = tag.get(key);
                AtomicReference data = new AtomicReference();
                setting.reader.accept(value, data);
                this.settings.put(setting, data.get());
            });
        }
    }
}
