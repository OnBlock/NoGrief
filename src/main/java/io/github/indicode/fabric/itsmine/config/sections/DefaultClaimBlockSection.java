package io.github.indicode.fabric.itsmine.config.sections;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class DefaultClaimBlockSection {

    @Setting(value = "default2D", comment = "2D base claim blocks")
    public int default2D = 15625;

    @Setting(value = "default3D", comment = "3D base claim blocks")
    public int default3D = 2500;


}
