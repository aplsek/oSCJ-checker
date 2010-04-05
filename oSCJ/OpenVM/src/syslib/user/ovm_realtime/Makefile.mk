# Build the real-time runtime classes. Some classes here will "override" those
# in the normal runtime library. The resulting jar file must be placed ahead
# of the normal jar, on the bootclasspath when building the image. For
# compiling, we allow the normal classes to be seen.
JAVA_BOOTCLASSPATH=..
JAVA_CLASSPATH=../ovm_platform/ovm_platform.jar
JAVA_JARFILE=ovm_rt_user_realtime.jar

JAVA_EXCLUDES=

JAVADOC_SUBPACKAGES=javax.realtime

VPATH=$(srcdir)

all: $(JAVA_JARFILE)

# The default jar rule excludes too much.  java-clean actually suffers
# from a similar problem.  The recursion trick used in clean does not
# work here, because $(JAVA_JARFILE) depends on compile.
$(JAVA_JARFILE): java-compile
	(echo META-INF/MANIFEST.MF;				    \
         $(FIND_AND_EXCLUDE) . '' "*.class") \
		| sed 's,^./,,' | zip -q@ ovm_rt_user_realtime.zip
	mv ovm_rt_user_realtime.zip $@

clean:
	$(MAKE) java-clean

install: java-install
