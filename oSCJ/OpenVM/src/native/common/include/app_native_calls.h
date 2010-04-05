/*
 * This file is intentionally left empty.
 * Execution engines #include the file app_native_calls.h when
 * compiling the native call dispatcher.  If an application programmer
 * wishes to call C code using Ovm's LibraryGlue interface, he or she
 * should declare all the called C functions in a file called
 * app_native_calls.h, and place that file in the directory where
 * gen-ovm is run.
 *
 * If the application programmer does not use the LibraryGlue native
 * interface, this file empty will be included in place of the
 * application-specific one.
 */
