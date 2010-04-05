# classpath is simple
JAVA_CLASSPATH=.:$(top_builddir)/src/ovm.jar

JAVA_BOOTCLASSPATH=.:$(srcdir)

JAVA_JARFILE=ovm_rt.jar

all: $(JAVA_JARFILE)

clean: java-clean

install: java-install
