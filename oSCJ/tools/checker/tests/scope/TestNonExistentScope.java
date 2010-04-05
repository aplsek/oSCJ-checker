//scope/TestNonExistentScope.java:6: Scope a does not exist.
//public class TestNonExistentScope {
//       ^
//1 error

package scope;

import javax.safetycritical.annotate.Scope;

@Scope("a")
public class TestNonExistentScope {

}
