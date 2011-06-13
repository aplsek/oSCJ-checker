package scope.scope.simple;

import static javax.safetycritical.annotate.Phase.INITIALIZATION;
import static javax.safetycritical.annotate.Scope.IMMORTAL;



import static javax.safetycritical.annotate.Level.SUPPORT;
import javax.safetycritical.MissionSequencer;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJRestricted;
import javax.safetycritical.annotate.Scope;

@Scope(IMMORTAL)
@DefineScope(name="A", parent=IMMORTAL)
@SCJAllowed(members = true)
public abstract class TestUpcast5 extends MissionSequencer {

    @SCJRestricted(INITIALIZATION)
    public TestUpcast5() { super(null, null); }

    @Scope("A")
    @DefineScope(name="B", parent="A")
    @SCJAllowed(members = true)
    abstract class MS extends MissionSequencer {
        @SCJRestricted(INITIALIZATION)
        public MS() { super(null, null); }
    }

    public void bar() {
        Bar b  = new Bar();
        //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
        Foo f = b;

        BarBar bar = new BarBar();
        //## checkers.scope.ScopeChecker.ERR_BAD_UPCAST
        FooFoo foo = bar;
    }


    class Foo {
        void method() {}
    }

    class Bar extends Foo {

        @Override
        @RunsIn("A")
        void method() {}
    }

    class FooFoo {
        @RunsIn("A")
        void method() {}
    }

    class BarBar extends FooFoo {

        @Override
        @RunsIn("B")
        void method() {}
    }

}
