package scope.defineScope.simple;

import javax.safetycritical.ManagedMemory;
import javax.safetycritical.SCJRunnable;

public class TestEnterPrivateMemoryNoDefineScope {
    static class X {
        void foo(ManagedMemory mem) {
            mem.enterPrivateMemory(0, new Y());
            SCJRunnable y = new Y();
            //## checkers.scope.DefineScopeChecker.ERR_ENTER_PRIVATE_MEMORY_NO_DEFINE_SCOPE
            mem.enterPrivateMemory(0, y);
        }
    }
    //## checkers.scope.DefineScopeChecker.ERR_ENTER_PRIVATE_MEMORY_NO_DEFINE_SCOPE
    static class Y implements SCJRunnable {
        @Override
        public void run() { }
    }
    static class Z implements SCJRunnable {
        @Override
        public void run() { }
    }
}
