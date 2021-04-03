package moe.yuuta.dn42peering.agent.ip;

public class IPSyntaxException extends IPException {
    public IPSyntaxException(String stderr) {
        super(1, stderr);
    }
}
