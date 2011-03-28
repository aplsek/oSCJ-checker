package java.net;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.Scope;

public class InetAddress {
    static class Cache {
        @Scope(IMMORTAL)
        static enum Type { }
    }
}
