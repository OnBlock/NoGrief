package io.github.indicode.fabric.itsmine.util;

import com.google.common.collect.Lists;
import io.github.indicode.fabric.itsmine.ClaimManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

public class WorldUtil {

    private static MinecraftServer server(){
        return ClaimManager.INSTANCE.server;
    }
    private static final List<RegistryKey<DimensionType>> registryKeys = Lists.newArrayList();

    private static RegistryKey<DimensionType> DEFAULT = DimensionType.OVERWORLD_REGISTRY_KEY;
    private static DimensionType DEFAULT_DIMENSION = DimensionType.getDefaultDimensionType();

    public static ServerWorld getServerWorld(DimensionType dimensionType){
        return server().getWorld(getRegistry(dimensionType));
    }


    public static String getDimensionName(DimensionType dimensionType){
        return getRegistry(dimensionType).getValue().getPath();
    }

    public static String getDimensionNameWithNameSpace(DimensionType dimensionType){
        return getRegistry(dimensionType).getValue().getNamespace() + ":" + getDimensionName(dimensionType);
    }

    public static String[] getNameSpaceAndName(String string){
        return string.split(":");
    }


    public static DimensionType getDimensionType(String dimension){
/*        if(dimension.matches("[a-z_]+:[a-z_]+")){
            System.out.println("Trying to convert " + dimension);
            return getDimensionType(getNameSpaceAndName(dimension)[1]);
        } else {*/
            for(RegistryKey<DimensionType> registryKey : getDimensionKeys()){
                System.out.println(registryKey);
                if(dimension.equalsIgnoreCase(registryKey.getValue().getNamespace() + ":" + registryKey.getValue().getPath())){
                    DimensionType dimensionType = server().getWorld(registryKey).getDimension();
                    return dimensionType;
                } else {
                }
            }
        /*}*/
        return DEFAULT_DIMENSION;
    }

    public static RegistryKey<DimensionType> getRegistry(DimensionType dimensionType){
        for(RegistryKey<DimensionType> registryKey : getDimensionKeys()){
            DimensionType dimension = server().getWorld(registryKey).getDimension();
            if(dimension.equals(dimensionType)){
                return registryKey;
            }
        }
        return DEFAULT;
    }

/*    public static ArrayList<RegistryKey<DimensionType>> getRegistries(){
        ArrayList<RegistryKey<DimensionType>> registryKeys = new ArrayList<>();
        System.out.println("getRegistries");
        server().method_29174().getRegistry().getIds().forEach(identifier -> {
            System.out.println("Identfier " + identifier);
            registryKeys.add(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, identifier));
        });
        return registryKeys;
    }*/

    public static List<RegistryKey<DimensionType>> getDimensionKeys() {
        return registryKeys;
    }

    static {
        for (Identifier id : server().method_29174().getRegistry().getIds()) {
            registryKeys.add(RegistryKey.of(Registry.DIMENSION_TYPE_KEY, id));
        }
    }

}
