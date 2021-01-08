package moe.yuuta.dn42peering.jaba;

/**
 * Oh Jesus how can't Jaba have out parameters ?!
 *
 * The out parameters in C# is such a beautiful innovation,
 *
 * as I can get rid of exceptions and write C style functions,
 *
 * just like <pre>int func(class *out);</pre>.
 */
public final class OutParm<E> {
    public E out;

    public OutParm() {
        this(null);
    }

    public OutParm(E out) {
        this.out = out;
    }
}
