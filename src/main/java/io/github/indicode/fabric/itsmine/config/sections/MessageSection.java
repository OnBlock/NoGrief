package io.github.indicode.fabric.itsmine.config.sections;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MessageSection {
    @Setting(value = "messageCooldown")
    public int messageCooldown = 20;

    @Setting(value = "eventStayTicks", comment = "Sets how many ticks an event message will stay on action bar, Default: -1")
    public int eventStayTicks = -1;

    @Setting(value = "noPermission")
    public String noPermission = "&cSorry but you don't have permission to do that";

    @Setting(value = "interactEntity")
    public String interactEntity = "&cSorry but you can't interact with Entities here!";

    @Setting(value = "interactBlock")
    public String interactBlock = "&cSorry but you can't interact with Blocks here!";

    @Setting(value = "openContainer")
    public String openContainer = "&cSorry but you can't open containers here!";

    @Setting(value = "breakBlock")
    public String breakBlock = "&cSorry but you can't Break Blocks here!";

    @Setting(value = "placeBlock")
    public String placeBlock = "&cSorry but you can't Place Blocks here!";

    @Setting(value = "attackEntity")
    public String attackEntity = "&cSorry but you can't Attack Entities here!";

    @Setting(value = "enterDefault", comment = "Variables: %claim% %player%")
    public String enterDefault = "&eNow entering claim &6%claim%";

    @Setting(value = "leaveDefault", comment = "Variables: %claim% %player%")
    public String leaveDefault = "&eNow leaving claim &6%claim%";

    @Setting(value = "cantEnter")
    public String cantEnter = "&cSorry but you don't have permission to enter this claim!";

    @Setting(value = "cantUse")
    public String cantUse = "&cSorry but you can't to use that here!";

    @Setting(value = "longName")
    public String longName = "&cThe name of the claim must be less than 30 characters!";

    @Setting(value = "cantDo")
    public String cantDo ="&cSorry but you can't do that!";
}
