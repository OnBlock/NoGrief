package io.github.indicode.fabric.itsmine.config;

import io.github.indicode.fabric.itsmine.config.sections.DefaultClaimBlockSection;
import io.github.indicode.fabric.itsmine.config.sections.MessageSection;
import io.github.indicode.fabric.itsmine.config.sections.RentSection;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;


@ConfigSerializable
public class Config {

    public static final String HEADER = "ItsMine! Main Configuration File";

    @Setting(value = "debug")
    public boolean debug = false;

    @Setting(value = "prefix")
    public String prefix = "&f[&aClaims&f] ";

    @Setting(value = "permissionManager", comment = "Values: thimble, luckperms, vanilla")
    public String permissionManager = "luckperms";

    @Setting(value = "claims2d", comment = "If this is enabled, claims reach from 0 to 256 (claim blocks ignore the y-axis)")
    public boolean claims2d = true;

    @Setting(value = "defaultClaimBlocks", comment = "Adjust the amount of claimblocks players get upon joining")
    public DefaultClaimBlockSection defaultClaimBlockSection = new DefaultClaimBlockSection();

    @Setting(value = "messages")
    private MessageSection messageSection = new MessageSection();

    @Setting(value = "rent")
    private RentSection rentSection = new RentSection();


    public MessageSection message(){
        return messageSection;
    }

    public DefaultClaimBlockSection claimBlock(){
        return defaultClaimBlockSection;
    }

    public RentSection rent(){
        return rentSection;
    }

}
