package scope.scopeReturn;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.Scope;
import javax.safetycritical.annotate.RunsIn;

@DefineScope(name="Mission",parent=IMMORTAL)
@Scope("Mission")
public class SimpleReturn {

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
    
}


@Scope("Mission")
class Bar{
   public void method() {}
    
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