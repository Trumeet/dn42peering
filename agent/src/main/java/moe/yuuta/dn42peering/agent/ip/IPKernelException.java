package moe.yuuta.dn42peering.agent.ip;

public class IPKernelException extends IPException {
    public IPKernelException(String stderr) {
        super(2, stderr);
    }
}
