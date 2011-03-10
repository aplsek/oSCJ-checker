package scope.scope.simple;

import static javax.safetycritical.annotate.Scope.UNKNOWN;

import javax.realtime.MemoryArea;
import javax.safetycritical.annotate.Scope;

public class TestBadGetMemoryArea {
    void foo(@Scope(UNKNOWN) Object o) {
        //## checkers.scope.ScopeChecker.ERR_BAD_GET_MEMORY_AREA
        MemoryArea.getMemoryArea(o);
    }
}
