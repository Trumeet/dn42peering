package moe.yuuta.dn42peering.portal;

public class HTTPException extends Exception {
    public final int code;

    public HTTPException(int code) {
        this.code = code;
    }
}
