import checkers.nullness.quals.*;

import java.util.*;
import java.io.*;

public class NonNullMapValue {

  // Discussion:
  //
  // It can be useful to indicate that all the values in a map are non-null.
  // ("@NonNull" is redundant in this declaration, but I've written it
  // explicitly because it is the annotation we are talking about.)
  //   HashMap<String,@NonNull String> myMap;
  //
  // However, the get method's declaration is misleading (in the context of
  // the nullness type system), since it can always return null no matter
  // whether the map values are non-null:
  //   V get(Object key) { ... return null; }
  //
  // Here are potential solutions:
  //  * Forbid declaring values as non-null.  This is the wrong approach.
  //    (It would also be hard to express syntactically.)
  //  * The checker could recognize a special new annotation on the return
  //    value of get, indicating that its return type isn't merely inferred
  //    from the generic type, but is always nullable.  (This special new
  //    annotations could even be "@Nullable".  A different annotation may
  //    be better, becase in general we would like to issue an error
  //    message when someone applies an annotation to a generic type
  //    parameter.)
  // Additionally, to reduce the number of false positive warnings caused
  // by the fact that get's return value is nullable:
  //  * Build a more specialized sophisticated flow analysis that checks
  //    that the passed key to Map.containsKey() is either checked against
  //    Map.containsKey() or Map.keySet().


  Map<String,@NonNull String> myMap;

  void testMyMap(String key) {
    @NonNull String value;
    value = myMap.get(key);    // should issue warning
    if (myMap.containsKey(key)) {
      value = myMap.get(key);
    }
    for (String keyInMap : myMap.keySet()) {
      value = myMap.get(key); // should issue warning
    }
    for (String keyInMap : myMap.keySet()) {
        value = myMap.get(keyInMap);
    }
    for (Map.Entry<String,@NonNull String> entry : myMap.entrySet()) {
      String keyInMap = entry.getKey();
      value = entry.getValue();
    }
    for (Iterator<String> iter = myMap.keySet().iterator(); iter.hasNext();) {
      String keyInMap = iter.next();
      //value = myMap.get(keyInMap);
    }
    value = myMap.containsKey(key) ? myMap.get(key) : "hello";
  }

  public static
  <T> void print(Map<T,List<T>> graph, PrintStream ps, int indent) {
    for (T node : graph.keySet()) {
      for (T child : graph.get(node)) {
        ps.printf("  %s%n", child);
      }
      @NonNull List<T> children = graph.get(node);
      for (T child : children) {
        ps.printf("  %s%n", child);
      }
    }
  }

  public static <T> void testAssertFlow(Map<T,List<T>> preds, T node) {
    assert preds.containsKey(node);
    for (T pred : preds.get(node)) {
    }
  }

  public static <T> void testContainsKey1(Map<T,List<T>> dom, T pred) {
    assert dom.containsKey(pred);
    // Both of the next two lines should type-check.  The second one won't
    // unless the checker knows that pred is a key in the map.
    List<T> dom_of_pred1 = dom.get(pred);
    @NonNull List<T> dom_of_pred2 = dom.get(pred);
  }

  public static <T> void testContainsKey2(Map<T,List<T>> dom, T pred) {
    if (! dom.containsKey(pred)) {
      throw new Error();
    }
    // Both of the next two lines should type-check.  The second one won't
    // unless the checker knows that pred is a key in the map.
    List<T> dom_of_pred1 = dom.get(pred);
    @NonNull List<T> dom_of_pred2 = dom.get(pred);
  }

  // Too ambitious for now
  // public static void process_unmatched_procedure_entries() {
  //   HashMap<Integer,Date> call_hashmap = new HashMap<Integer,Date>();
  //   for (Integer i : call_hashmap.keySet()) {
  //     @NonNull Date d = call_hashmap.get(i);
  //   }
  //   Set<Integer> keys = call_hashmap.keySet();
  //   for (Integer i : keys) {
  //     @NonNull Date d = call_hashmap.get(i);
  //   }
  //   Set<Integer> keys_sorted = new TreeSet<Integer>(call_hashmap.keySet());
  //   for (Integer i : keys_sorted) {
  //     @NonNull Date d = call_hashmap.get(i);
  //   }
  // }

  public static Object testPut(Map<Object,Object> map, Object key) {
    if (!map.containsKey(key)) {
      map.put(key, new Object());
    }
    return map.get(key);
  }

  public static Object testAssertGet(Map<Object,Object> map, Object key) {
    assert map.get(key) != null;
    return map.get(key);
  }

}