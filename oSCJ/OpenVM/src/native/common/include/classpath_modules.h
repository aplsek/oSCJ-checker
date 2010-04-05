#ifndef _CLASSPATH_MODULES_H
#define _CLASSPATH_MODULES_H

/*
 * The user-domain native interface consists of a set of function
 * pointers defined in the common section.  If no classpath modules
 * are loaded, all pointers remain null, and attempts to call native
 * methods will fail.
 *
 * We implement a native mehtod by redefining the common symbol for
 * its function pointer in a "classpath_module".  Each classpath
 * module exports one symbol, which is used to pull it into the final
 * executable, thus filling in the function pointers for native
 * methods that it defines.
 *
 * See ../../interpreter/main.c for an example of how the modules can
 * be used.
 */

extern char classpath_io[];
#endif /* _CLASSPATH_MODULES_H */
