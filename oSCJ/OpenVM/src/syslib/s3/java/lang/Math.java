package java.lang;

public class Math {

    public static int min(int a, int b) {
	if (a > b)
	    return b;
	else
	    return a;
    }

    public static int max(int a, int b) {
	if (a > b)
	    return a;
	else
	    return b;
    }

    public static int abs(int i) {
	if (i > 0)
	    return i;
	else
	    return -i;
    }

} // end of Math
