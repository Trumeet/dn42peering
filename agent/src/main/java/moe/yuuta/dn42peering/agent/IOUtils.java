package moe.yuuta.dn42peering.agent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public class IOUtils {
    @Nonnull
    public static String read(@Nonnull InputStream in) throws IOException {
        StringBuilder data = new StringBuilder();
        while(true) {
            int i = in.read();
            if(i == -1) break;
            data.append((char)i);
        }
        in.close();
        return data.toString();
    }

    @Nonnull
    public static String readFromResource(@Nonnull ClassLoader cl, @Nonnull String name) throws IOException {
        final InputStream in = cl.getResourceAsStream(name);
        final String data = read(in);
        in.close();
        return data;
    }
}
