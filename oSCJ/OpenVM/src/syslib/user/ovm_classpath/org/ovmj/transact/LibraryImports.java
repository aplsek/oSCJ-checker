package org.ovmj.transact;
public class LibraryImports {
      public static native void start(int size, boolean commitOnOverflow);
      public static native void start();
      public static native void commit();
      public static native void undo();
      public static native void retry();
      public static native int logSize();
}
