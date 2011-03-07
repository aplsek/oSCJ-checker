package scope.miniCDx;

import javax.safetycritical.annotate.SCJAllowed;

@SCJAllowed(members = true)
public class Callsign {
    public Callsign(byte[] cs2) {
    }

    byte[] cs;
    public int length;
}
