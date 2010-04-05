package ovm.util.logging;

import java.io.Serializable;

import ovm.core.OVMBase;

public class Level extends OVMBase implements Serializable {

    final String name;
    final int value;
    public Level(String name, int value) {
	this.name = name;
	this.value = value;
    }

    public static Level parse(String aName) {
	String s = aName.toUpperCase();
	if (s.equals("ALL")) {
	    return ALL;
	} else if (s.equals("FINEST")) {
	    return FINEST;
	} else if (s.equals("FINER")) {
	    return FINER;
	} else if (s.equals("FINE")) {
	    return FINE;
	} else if (s.equals("CONFIG")) {
	    return CONFIG;
	} else if (s.equals("INFO")) {
	    return INFO;
	} else if (s.equals("WARNING")) {
	    return WARNING;
	} else if (s.equals("SEVERE")) {
	    return SEVERE;
	} else if (s.equals("OFF")) {
	    return OFF;
	} else {
	    return null;
	}
    }


    public static Level ALL = new Level("ALL", 0);

    public static Level FINEST = new Level("FINEST", 1);

    public static Level FINER = new Level("FINER", 10);

    public static Level FINE = new Level("FINE", 100);

    public static Level CONFIG = new Level("CONFIG", 1000);

    public static Level INFO = new Level("INFO", 10000);

    public static Level WARNING = new Level("WARNING", 100000);

    public static Level SEVERE = new Level("SEVERE", 1000000);

    public static Level OFF = new Level("OFF", Integer.MAX_VALUE);

    public int intValue() {
	return value;
    }

    public String toString() {
	return name;
    }
	  
    public boolean equals(Object o) {
	if (o instanceof Level) {
	    Level other = (Level)o;
	    return other.intValue() == value && other.name.equals(name);
	}
	return false;
    }

}
