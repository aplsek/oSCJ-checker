//crossScope/TestInference.java:64: error message.
//        foo.methodErr(bar); 
//                      ^
//2 errors

package crossScope;


class StaticArray {
    static Object array[] = new Object[1];

    public void method (Object array[]) {
        array[0] = new Object();                  //OK
    } 
    
    public void method () {
        this.array[0] = new Object();                  //ERROR
    } 

    public static void main() {
        StaticArray st = new StaticArray();
        st.method(array);                           // ERROR
    }

}