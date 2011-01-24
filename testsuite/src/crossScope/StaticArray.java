package crossScope;


class StaticArray {
    static Object array[] = new Object[1];

    public void method (Object array[]) {
        array[0] = new Object();                   // ERROR if the array is IMMORTAL!
    } 

    public static void main() {
        StaticArray st = new StaticArray();
        st.method(array);
    }

}