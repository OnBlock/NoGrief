package io.github.indicode.fabric.itsmine;

import com.google.common.reflect.TypeToken;
import io.github.indicode.fabric.itsmine.config.Config;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.DefaultObjectMapperFactory;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.File;
import java.io.IOException;

public class ItsMineConfig {
    private static Config config;
    private static ConfigurationNode mainNode;

    public ItsMineConfig() {
        try {
            File CONFIG_FILE = new File(ItsMine.getDirectory() + "/config/itsmine.hocon");

            ConfigurationLoader<CommentedConfigurationNode> mainLoader = HoconConfigurationLoader.builder()
                    .setFile(CONFIG_FILE).build();


            CONFIG_FILE.createNewFile();

            mainNode = mainLoader.load(configurationOptions());

            config = mainNode.getValue(TypeToken.of(Config.class), new Config());

            mainLoader.save(mainNode);
        } catch (IOException | ObjectMappingException e) {
            System.out.println("Exception loading config file");
            e.printStackTrace();
        }
    }

    public static Config main() {
        return config;
    }

    public static ConfigurationNode getMainNode() {
        return mainNode;
    }


    public static void reload() {
        new ItsMineConfig();
    }

    public static ConfigurationOptions configurationOptions() {
        return ConfigurationOptions.defaults()
                .setHeader(Config.HEADER)
                .setObjectMapperFactory(DefaultObjectMapperFactory.getInstance())
                .setShouldCopyDefaults(true);
    }
}
