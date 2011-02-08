//scope/scopeReturn/MyMission.java:47: Cannot assign expression in scope MyHandler to variable in scope IMMORTAL.
//        return new Bar();           // ERR
//        ^
//scope/scopeReturn/MyMission.java:52: A method that returns a primitive type cannot have @Scope annotation.
//    public int getPrimitive() {
//               ^
//scope/scopeReturn/MyMission.java:57: A method that returns void cannot have @Scope annotation.
//    public void getVoid() {
//                ^
//scope/scopeReturn/MyMission.java:63: A method that returns a primitive type cannot have @Scope annotation.
//    public int[] getPrimitive2() {
//                 ^
//4 errors

package scope.scopeReturn;

import static javax.safetycritical.annotate.Scope.IMMORTAL;

import javax.safetycritical.Mission;
import javax.safetycritical.annotate.DefineScope;
import javax.safetycritical.annotate.RunsIn;
import javax.safetycritical.annotate.Scope;


@DefineScope(name="MyMission",parent=IMMORTAL)
@Scope("MyMission") 
public class ScopeReturn extends Mission {

    //@Scope(IMMORTAL) static Foo foo = new Foo();
    
    protected void initialize() { 
    }

    @Override
    public long missionMemorySize() {
        return 0;
    }

    @Scope(IMMORTAL) @RunsIn("MyHandler")
    public BarBar getFooErr() {
        return new BarBar();           // ERR
    }
    
    
    @Scope(IMMORTAL)                    // ERROR
    public int getPrimitive() {
        return 1;           
    }

    @Scope(IMMORTAL)                    // ERROR
    public void getVoid() {
        return;           
    }
    
    @Scope(IMMORTAL)                    // ERROR
    public int[] getPrimitive2() {
        return new int[]{1};   
        }        
}

class BarBar {}



@Scope("MyMission")
@DefineScope(name="MyHandler",parent="MyMission")
class MyHandlerrrr {}