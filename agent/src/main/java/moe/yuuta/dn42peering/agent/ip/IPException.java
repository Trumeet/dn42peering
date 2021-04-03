package moe.yuuta.dn42peering.agent.ip;

public class IPException extends Exception {
    public final int returnCode;
    public final String stderr;

    public IPException(int returnCode, String stderr) {
        super(String.format("Failed to execute ip: %s", stderr));
        this.returnCode = returnCode;
        this.stderr = stderr;
    }
}
