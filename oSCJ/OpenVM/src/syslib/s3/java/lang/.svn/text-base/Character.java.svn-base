package java.lang;

public class Character {

    public static final char MIN_VALUE = '\u0000';
    public static final char MAX_VALUE = '\uFFFF';
    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;

    public static char toUpperCase(char c) {
	if ( (c >= 'a') &&
	     (c <= 'z') )
	    return (char) (c - ('a' - 'A'));
	else
	    return c;
    }

    public static int digit(char x, 
			    int radix) {
	if ( (radix < MIN_RADIX) || 
	     (radix > MAX_RADIX) )
	    return -1;				    
				   
	int value = -1;
	if ( (x >= '0') && (x <= '9'))
	    value = (int) (x - '0');
	if ( (x >= 'A') && (x <= 'Z'))
	    value = 10 + x - 'A';
	if ( (x >= 'a') && (x <= 'z'))
	    value = 10 + x - 'a';
	if (value >= radix)
	    return -1;
	else
	    return value;
    }

    private char value;
    public Character(char c) {
    	value = c;
    }
    public char charValue() { return value; }
    
} // end of Character
