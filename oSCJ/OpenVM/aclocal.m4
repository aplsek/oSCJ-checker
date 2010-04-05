# Some configure macros for configuring Java programs
# Copyright (c) 2001 University of Utah 
# Copyright (c) 2003 Purdue University

dnl utility macros
AC_DEFUN(JV_ENSURE_VAR, [
  if test -z "[$]$1"; then
    AC_MSG_ERROR([$2 not found])
  fi
])

AC_DEFUN(JV_REQUIRE_PROG, [
  AC_CHECK_PROG($1, $2, $2)
  JV_ENSURE_VAR($1, $2)
])

AC_DEFUN(JV_REQUIRE_PROGS, [
  AC_CHECK_PROGS($1, $2) 
  if test -z "[$]$1"; then
    AC_MSG_ERROR([$3 not found])
  fi
])

dnl standard arguments
AC_DEFUN(JV_ARG_WITH_CLASS_DIR, [
  AC_ARG_WITH(classdir, 
[  --with-classdir=DIR     Java class file in DIR [DATADIR/java/classes]],
              [classdir=$withval], [classdir='${datadir}/java/classes'])
  if test $classdir = yes || test $classdir = no; then
    AC_MSG_ERROR(class directory must be specified)
  fi
  AC_SUBST(classdir)
])

dnl program checks
AC_DEFUN(JV_PROG_JAVA, [JV_REQUIRE_PROG(JAVA, java)])

AC_DEFUN(JV_TRY_JAVAC, [
  AC_MSG_CHECKING(for $1)
  JAVAC_FLAVOR="$1"
  JAVAC="$2"
  JAVAC_EXTRAS="$3"
  JV_TRY_COMPILE(conftest, [[
    public class conftest {
      static public void main(String[] args) {
        System.exit(0);
      }
   }]])
  if AC_TRY_COMMAND([CLASSPATH=. $JAVA conftest]); then
    AC_MSG_RESULT(yes)
    JAVAC="$2"
  else
    AC_MSG_RESULT(no)
    $4
  fi
])

dnl First: look for javamake.jar on classpath
dnl Second: look for working jikes on path
dnl Third: fall back to javac
dnl I had a test for jikes here, but it caused problems when jikes
dnl was present but not set up properly
dnl
dnl This code is not being used in the ovm because javamake doesn't like 
dnl managing Object.java and jikes doesn't like certain source-level
dnl constructs used in the OVM.  (Appearently the eclipse compiler can
dnl compile the OVM, but the OVM may not understand the bytecode that
dnl eclipse generates.)
AC_DEFUN(JV_CHOOSE_BEST_JAVAC, [
  AC_REQUIRE([JV_PROG_JAVA])
  JV_TRY_JAVAC([javamake], [java com.sun.tools.javamake.Main -C-g],
               [-pdb conftest.pdb], [
    AC_REQUIRE([JV_JAVA_BOOT_PATH])
    JV_TRY_JAVAC([jikes], [jikes -g -depend],
                 [-bootclasspath $JAVA_BOOT_PATH], [
      JV_TRY_JAVAC([javac], [javac -g], [], [
        AC_MSG_ERROR(No working java compiler found)
      ])
    ])
  ])
  AC_SUBST([JAVAC])
  AC_SUBST([JAVAC_FLAVOR])
])

AC_DEFUN(JV_PROG_ANT, [AC_CHECK_PROG(ANT, ant, ant)])
AC_DEFUN(JV_PROG_JAVAC, [JV_REQUIRE_PROG(JAVAC, javac)])
AC_DEFUN(JV_PROG_JAVADOC, [JV_REQUIRE_PROG(JAVADOC, javadoc)])

AC_DEFUN(JV_PROG_JAVAH, [
  JV_REQUIRE_PROGS(JAVAH, [javah kaffeh], javah)
])

AC_DEFUN(JV_GNU_MAKE, [
  JV_REQUIRE_PROGS(MAKE, [gmake make], [GNU Make])
  $MAKE -v 2>&1 | grep -i "GNU Make" 2>&1 > /dev/null
  if test $? != 0 ; then
    AC_MSG_ERROR(GNU Make required)
  fi
])

AC_DEFUN(JV_PROG_ZIP, [JV_REQUIRE_PROG(ZIP, zip)])

AC_DEFUN(JV_TRY_COMPILE, [
  cat - > $1.java <<EOF
$2
EOF
  AC_TRY_COMMAND(${JAVAC:-javac} $JAVAC_EXTRAS $1.java 2>& AC_FD_CC)
dnl   if test $? != 0 || test ! -s $1.class
dnl   then
dnl      AC_MSG_ERROR([Can't compile java programs with ${JAVAC:-javac}])
dnl   fi
])

dnl java environment checks
AC_DEFUN(JV_CONFTEST_JAVA, [
  AC_REQUIRE([JV_PROG_JAVA])
  JV_TRY_COMPILE(conftestJava, [[
public class conftestJava {
  static String metaChars = ";(){}\`'\\"\\\\";
  //" stupid fucking font-lock
  public static void main(String[] args)
  {
    for (int i = 0; i < args.length; i++)
     {
        String val = System.getProperty(args[i]);
        if (val == null) {
          System.err.println("could not find" + args[i]);
          System.exit(1);
        }
        char[] c = val.toCharArray();
        for (int j = 0; j < c.length; j++)
          {
             if (metaChars.indexOf(c[j]) != -1)
                System.out.print('\\\\');
             System.out.print(c[j]);
          }
        System.out.println("");
      }
  }
}]])
])

AC_DEFUN(JV_JAVA_PROP, [
  AC_CACHE_CHECK([Java property $2], jv_cv_$1, [
     AC_REQUIRE([JV_CONFTEST_JAVA])
     if test "x$$1" == x; then
       jv_cv_$1=`AC_TRY_COMMAND(CLASSPATH=. $JAVA conftestJava $2)`
       if test $? != 0 || test -z "$jv_cv_$1" ; then
          AC_MSG_ERROR([Can't execute java programs with $JAVA])
       fi
     else
       jv_cv_$1=$$1
     fi
  ])
  $1="$jv_cv_$1"
  AC_SUBST($1)
])

AC_DEFUN(JV_JDIRSEP, [JV_JAVA_PROP(D, file.separator)])
AC_DEFUN(JV_JPATHSEP, [JV_JAVA_PROP(P, path.separator)])
AC_DEFUN(JV_JVM_NAME, [JV_JAVA_PROP(JVM_NAME, java.vm.name)])
AC_DEFUN(JV_JVM_VERSION, [JV_JAVA_PROP(JVM_VERSION, java.vm.version)])
AC_DEFUN(JV_JAVA_HOME, [
  JV_JAVA_PROP(JAVA_HOME, java.home)
  dnl Not sure what to do here.  Ant expects JAVA_HOME to point to a jdk 
  dnl install directory: $JAVA_HOME/lib/tools.jar should contain javac and 
  dnl friends.  However, some versions of jdk set java.home to a jre 
  dnl subdirectory.  Apple's bundled jdk is set up even more strangely.
  dnl It seems there is no escape from the plague of ants.
  JAVA_HOME=`echo $JAVA_HOME| sed 's,/jre$,,'`
])
AC_DEFUN(JV_JAVA_OS, [JV_JAVA_PROP(JAVA_OS, os.name)])
AC_DEFUN(JV_JAVA_VENDOR, [JV_JAVA_PROP(JAVA_VENDOR, java.vendor)])
AC_DEFUN(JV_JAVA_BOOT_PATH, [
  JV_JAVA_PROP(JAVA_BOOT_PATH, sun.boot.class.path)
])

dnl JAVA_CMD and JAVA_HOME are important parameters for ant.
AC_DEFUN(JV_JAVACMD, [
   AC_REQUIRE([JV_PROG_JAVA])
   AC_PATH_PROG(JAVACMD, $JAVA)
   if test -z "$JAVACMD"; then
      AC_MSG_ERROR("absolute path to java not found")
   fi
])

AC_DEFUN(JV_REQUIRE_JAVA_VERSION, [
  JV_JAVA_VERSION
  if echo $JAVA_VERSION | egrep -q '$1'; then
    AC_MSG_ERROR("unsupported java version $JAVA_VERSION: $2 required")
  fi
])

AC_DEFUN(JV_REQUIRE_JAVA_VENDOR, [
  JV_JAVA_VENDOR
  if echo $JAVA_VENDOR | egrep -q '$1'; then
    AC_MSG_ERROR("unsupported java version $JAVA_VENDOR: $2 required")
  fi
])

dnl convert native slashes to forward slashes.  Can be used to
dnl translate win32 paths from Java to cygwin paths for gcc
AC_DEFUN(JV_SHELL_PATH, [AC_REQUIRE([JV_JDIRSEP]) echo $1 | tr "$D" /])
AC_DEFUN(JV_JAVA_PATH,  [AC_REQUIRE([JV_JDIRSEP]) echo $1 | tr / "$D"])

AC_DEFUN(JV_MAKE_PATHS, [
  AC_REQUIRE([JV_JDIRSEP])
  java2shell="subst /,$D,"
  shell2java="subst $D,/,"
  AC_SUBST(java2shell)
  AC_SUBST(shell2java)
])

dnl Kaffe sets java.version to the Kaffe version number, rather than
dnl a jdk version number.  I can't see any way to determine kaffe's
dnl level of library compliance.
dnl
dnl AC_DEFUN(AC_JAVA_VERSION, [AC_JAVA_PROP(JAVA_VERSION, java.version)])

dnl Test whether getDeclaredClasses works.  Doesn't work in
dnl Kaffe-1.0.6, but  does work in current.
AC_DEFUN(JV_JAVA_REFLECTS_INNER, [
  AC_CACHE_CHECK([whether Class.getDeclaredClasses() works],
                 jv_cv_JAVA_REFLECTS_INNER, [
    AC_REQUIRE([JV_PROG_JAVA])
    JV_TRY_COMPILE(conftest, [[
public class conftest{
  static class Inner { }

  public static void main(String[] args)
  {
    System.exit(conftest.class.getDeclaredClasses().length == 1 ? 0 : 1);
  }
}]])
    if AC_TRY_COMMAND([CLASSPATH=. $JAVA conftest]) ; then
      jv_cv_JAVA_REFLECTS_INNER=yes
    else
      jv_cv_JAVA_REFLECTS_INNER=no
    fi
  ])
  JAVA_REFLECTS_INNER="$jv_cv_JAVA_REFLECTS_INNER"
  AC_SUBST(JAVA_REFLECTS_INNER)
])

AC_DEFUN(JV_DEBUG, [ echo 'exec $@'; $@ ])

dnl FIXME: this only applies to versions of kaffe before 1.0.7
AC_DEFUN(JV_JAVA_INCLUDES, [
  if test -z "$JAVA_INCLUDES" ; then
    AC_CACHE_CHECK([Java header file directories], jv_cv_JAVA_INCLUDES, [
      AC_REQUIRE([JV_JVM_NAME])
      AC_REQUIRE([JV_JAVA_HOME])
      AC_REQUIRE([JV_JAVA_OS])
      AC_REQUIRE([JV_JDIRSEP])

      if test "$JVM_NAME" = Kaffe; then
        # java.home is typically /usr/local, look for include there
        jv_cv_JAVA_INCLUDES="-I`JV_SHELL_PATH($JAVA_HOME)`/include/kaffe"
      elif echo $JAVA_HOME | grep "$Djre\$" 2>&1 > /dev/null; then
        # For jdk, java.home is the jre directory, include should be a 
        # sibling.  I can only hope that the md directory name and os.name 
        # property have some relationship.
        base=`echo $JAVA_HOME | sed "s,${D}[[jJ][rR][eE]]\$,,"`
        base=`JV_SHELL_PATH($base)`
        md=`echo $JAVA_OS | tr "[[A-Z]]" "[[a-z]]"`
	case "$md" in 
          win*) md=win32;; 
	esac
        jv_cv_JAVA_INCLUDES="-I$base/include -I$base/include/$md"
      else
        AC_MSG_ERROR("Cannot infer JAVA_INCLUDES for $JVM_NAME set to -I... manually")
      fi
    ])
    JAVA_INCLUDES=$jv_cv_JAVA_INCLUDES
  fi
  AC_SUBST(JAVA_INCLUDES)
])

AC_DEFUN(JV_JSRCDIRS, [
  AC_REQUIRE([JV_JDIRSEP])
  jsrcdir=`echo $srcdir | sed "s,/,$D,g"`
  jtop_srcdir=`echo $top_srcdir | sed "s,/,$D,g"`
  AC_SUBST(jsrcdir)
  AC_SUBST(jtop_srcdir) 
])

dnl automatically generate an extra directory level as needed.  We
dnl should be able to assume that the last component of objdir is our
dnl root package, $1.  If not, add a subdir in objdir.
AC_DEFUN(JV_MUNGE_OBJDIR, [
  AC_MSG_CHECKING(whether objdir matches package)
  objdir=`pwd`
  [base_objdir=`echo $objdir | sed -e 's,/*$,,' -e 's,.*/\([^/][^/]*\)$,\1,'`]
  if test "$base_objdir" != "$1" ; then
    mkdir $objdir/$1
    cat - > $objdir/Makefile <<EOF
# THIS FILE WAS GENERATED BY CONFIGURE!
# 
# The real Makefile in $1 assumes that the current directory and the
# root package share the same name.
all:
	cd $1; \$(MAKE) \[$]@

default:
	cd $1; \$(MAKE) \[$]@
EOF
    AC_MSG_RESULT(fixed)
    AC_CACHE_SAVE
    cd $1
  else
    AC_MSG_RESULT(yes)
  fi
])