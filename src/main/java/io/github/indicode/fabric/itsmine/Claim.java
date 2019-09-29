package io.github.indicode.fabric.itsmine;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import jdk.nashorn.internal.objects.annotations.Getter;
import net.minecraft.nbt.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;

import javax.xml.crypto.Data;
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
            SPAWN_PROTECT("modify_world", "Spawn Protection Bypass"),
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
                Permission perm = Permission.byId(key.asString());
                if (perm != null) perms.add(perm);
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
        private static class SettingData { // Wdym overcomplicated...
            private static BiConsumer<Object, AtomicReference<Tag>> BOOL_WRITER = (data, ref) -> {
                CompoundTag compat = new CompoundTag();
                compat.putBoolean("it", (Boolean) data);
                ref.set(compat.getTag("it"));
            };
            private static BiConsumer<Tag, AtomicReference> BOOL_READER = (data, ref) -> ref.set(((ByteTag) data).getByte() != 0);
            private static Consumer<AtomicReference<ArgumentType>> BOOL_ARGUMENT = ref -> ref.set(BoolArgumentType.bool());
            private static BiConsumer<CommandContext<ServerCommandSource>, AtomicReference> BOOL_PARSER = (context, ref) -> ref.set(BoolArgumentType.getBool(context, (String)ref.get()));
            private static BiConsumer<Object, AtomicReference<Tag>> STRING_WRITER = (data, ref) -> {
                CompoundTag compat = new CompoundTag();
                compat.putString("it", (String) data);
                ref.set(compat.getTag("it"));
            };
            private static BiConsumer<Tag, AtomicReference> STRING_READER = (data, ref) -> ref.set(data.asString());
            private static Consumer<AtomicReference<ArgumentType>> GREEDY_STRING_ARGUMENT = ref -> ref.set(StringArgumentType.greedyString());
            private static BiConsumer<CommandContext<ServerCommandSource>, AtomicReference> STRING_PARSER = (context, ref) -> ref.set(StringArgumentType.getString(context, (String)ref.get()));
            private static BiConsumer<Object, AtomicReference<String>> TOSTRING_STRINGIFIER = (data, ref) -> ref.set(data.toString());
        }
        public enum Setting {

            EXPLOSIONS("explosion_destruction", "Explosions Destroy Blocks", SettingData.BOOL_WRITER, SettingData.BOOL_READER, SettingData.BOOL_ARGUMENT, SettingData.BOOL_PARSER, SettingData.TOSTRING_STRINGIFIER, false),
            FLUID_CROSSES_BORDERS("fluid_across_borders", "Fluid Crosses Borders", SettingData.BOOL_WRITER, SettingData.BOOL_READER, SettingData.BOOL_ARGUMENT, SettingData.BOOL_PARSER, SettingData.TOSTRING_STRINGIFIER, false);

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
            tag.put("permissions", super.toTag());
            CompoundTag settings = new CompoundTag();
            this.settings.forEach((setting, data) -> {
                AtomicReference<Tag> writer = new AtomicReference<>();
                setting.writer.accept(data, writer);
                settings.put(setting.id, writer.get());
            });
            tag.put("settings", settings);
            return tag;
        }
        public void fromTag(CompoundTag tag) {
            super.fromTag(tag.getCompound("permissions"));
            settings.clear();
            CompoundTag settings = tag.getCompound("settings");
            settings.getKeys().forEach(key -> {
                Setting setting = Setting.byId(key);
                if (setting == null) return;
                Tag value = settings.getTag(key);
                AtomicReference data = new AtomicReference();
                setting.reader.accept(value, data);
                this.settings.put(setting, data.get());
            });
        }
    }
}
