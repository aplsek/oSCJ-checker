package java.net;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.Scope;

public class Proxy {
    @Scope(IMMORTAL)
    static enum Type { }
}
