package scope;

import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.ScopeDef;

@Scope("a")
public class TestStringLiteralAssignment {
    @ScopeDef(name="a", parent="immortal")
    int x;
    public void run() {
        String x = "test";
    }
}
