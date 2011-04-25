package java.util;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed
public class StringTokenizer {

    @SCJAllowed
    public StringTokenizer(String str) {
    }

    @SCJAllowed
    public String nextToken() {
        return null;
    }

    @SCJAllowed
    public boolean hasMoreTokens() {
        return false;
    }
}