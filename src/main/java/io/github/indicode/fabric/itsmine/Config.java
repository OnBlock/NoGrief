package io.github.indicode.fabric.itsmine;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonPrimitive;
import io.github.indicode.fabric.tinyconfig.DefaultedJsonArray;
import io.github.indicode.fabric.tinyconfig.ModConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Indigo Amann
 */
public class Config {
    public static List<Integer> claimCountPerms = Arrays.asList(1, 3, 5, 10);
    private static ModConfig modConfig = new ModConfig("itsmine");
    static void sync(boolean overwrite) {
        modConfig.configure(overwrite, config -> {
            config.accessChild("count_based_claims", cbc -> {
                DefaultedJsonArray countPermsArray = cbc.getArray("home_count_perms", () -> {
                    DefaultedJsonArray def = new DefaultedJsonArray();
                    claimCountPerms.forEach(it -> def.add(new JsonPrimitive(it)));
                    return def;
                }, "Requires Restart");
                claimCountPerms = new ArrayList<>();
                for (int i = 0; i < countPermsArray.size(); i++) {
                    claimCountPerms.add(countPermsArray.getInt(i));
                }
            });
        });
    }
}
