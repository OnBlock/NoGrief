package io.github.indicode.fabric.itsmine.config.sections;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class RentSection {

    @Setting(value = "maxRentTime", comment = "Default: 7776000 (90d)")
    public int maxRentTime = 7776000;

}
