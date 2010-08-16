package scjAllowed;

import javax.safetycritical.annotate.SuppressSCJ;

public class TestSuppress302 {

    // Here we are suppressing the checks for the whole statement
    @SuppressSCJ
    final Runnable runner_ = new Runnable() {
                               public final void run() {
                                   // here is the ERROR but it is suppressed
                                   Runnable rune = new Runnable() {
                                       public void run() {
                                       }
                                   };
                               }
                           };
}
