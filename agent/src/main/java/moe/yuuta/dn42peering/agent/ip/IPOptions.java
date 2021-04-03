package moe.yuuta.dn42peering.agent.ip;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IPOptions {
    public enum Family {
        INET,
        INET6,
        BRIDGE,
        MPLS,
        LINK
    }

    private boolean details;
    private Family family;
    private String netns;
    private boolean force;

    public boolean isDetails() {
        return details;
    }

    @Nonnull
    public IPOptions setDetails(boolean details) {
        this.details = details;
        return this;
    }

    public Family getFamily() {
        return family;
    }

    @Nonnull
    public IPOptions setFamily(Family family) {
        this.family = family;
        return this;
    }

    public String getNetns() {
        return netns;
    }

    @Nonnull
    public IPOptions setNetns(String netns) {
        this.netns = netns;
        return this;
    }

    public boolean isForce() {
        return force;
    }

    @Nonnull
    public IPOptions setForce(boolean force) {
        this.force = force;
        return this;
    }

    @Nonnull
    List<String> toCommand() {
        final List<String> cmds = new ArrayList<>();
        if(details)
            cmds.add("-details");
        if(family != null) {
            cmds.add("-family");
            cmds.add(family.toString().toLowerCase(Locale.ROOT));
        }
        if(netns != null) {
            cmds.add("-netns");
            cmds.add(netns);
        }
        if(force) {
            cmds.add("-force");
        }
        cmds.add("-json");
        cmds.add("-c=never");
        return cmds;
    }
}
