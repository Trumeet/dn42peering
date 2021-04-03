package moe.yuuta.dn42peering;

public final class RPC {
    public static final int AGENT_PORT = 49200;

    // Agents before v1.11
    public static final int VERSION_LEGACY = -1;
    // v1.11: Adds GetVersion() support.
    public static final int VERSION_11 = 11;
}
