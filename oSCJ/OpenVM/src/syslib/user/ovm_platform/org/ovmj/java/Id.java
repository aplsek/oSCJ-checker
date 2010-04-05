package org.ovmj.java;

/**
 * This code lives only in the user domain.  It is used by
 * NativeConstants to construct static final fields that are not
 * compile-time constants.
 *
 * @author <a href=mailto://baker29@cs.purdue.edu> Jason Baker </a>
 **/
class Id {
    static int id(int x) { return x; }
    static boolean id(boolean x) { return x; }
}
