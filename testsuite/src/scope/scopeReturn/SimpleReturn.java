package scope.scopeReturn;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@DefineScope(name="Mission",parent=IMMORTAL)
@Scope("Mission")
public abstract class SimpleReturn extends Mission {

   // @Scope("Mission") Foo foo;

    @Scope("Mission") Bar bar;
    Bar bar2;

    public void bar2() {
        this.bar = method3();
        bar.method();
    }
    /*
    @Scope("Mission")
    public Foo method2() {
        Foo foo = new Foo();
        return foo;
    }*/

    @Scope("Mission")
    public Bar method3() {
        Bar bar = new Bar();
        return bar;
    }


    @Scope("Mission")
    static class Bar extends Mission {
       public void method() {}

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Override
    protected void initialize() {}
    }


}



/*
@Scope("Mission") @RunsIn("Mission")
public Foo method() {
    Foo foo = new Foo();
    return foo;
}

public void bar() {
    this.foo = method();
}*/