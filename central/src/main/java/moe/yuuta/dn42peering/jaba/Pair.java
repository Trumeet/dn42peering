package moe.yuuta.dn42peering.jaba;

public class Pair<A, B> {
    public final A a;
    public final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("Pair { a = %s, b = %s }", a, b);
    }
}
