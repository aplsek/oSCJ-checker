//scope/TestNonExistentScope.java:11: (Class scope.TestNonExistentScope has a scope annotation with no matching @DefineScope)
//public class TestNonExistentScope {
//       ^
//1 error

package scope;

import javax.safetycritical.annotate.Scope;

@Scope("a")
public class TestNonExistentScope {
}
