# This build the main runtime libraries, but excludes the platform specific
# files, and excludes the real-time library classes
JAVA_BOOTCLASSPATH=.:$(srcdir)/ovm_classpath:$(top_srcdir)/gnu-classpath:./gnu-classpath
JAVA_CLASSPATH=ovm_platform/ovm_platform.jar
JAVA_JARFILE=ovm_rt_user.jar

# These directories get shoved into vpath, and aren't packages
OVM_EXCLUDE_SUBDIRS=ovm_classpath/**,		\
		    ovm_platform/**,		\
		    ovm_test/**,                \
		    ovm_realtime/**,		\
		    ovm_scj/**,		\
		    gnu-classpath/**


# This material from classpath should never be included in an OVM
# build.  Because all excludes apply to all source directories, we
# must be careful to include test/** in ovm_test
#
# We exclude the swing gtk peer, not because we don't want java code
# to link against it, but because it has somehow developed compilation
# errors.
GCP_EXCLUDE_SUBDIRS=testsuite/**,		\
		    test/base/**,		\
		    test/native/**,		\
		    test/gnu.*/**,		\
		    test/java.*/**,		\
		    vm/**,			\
		    compat/**,			\
		    tools/**,			\
		    examples/**,		\
		    external/**,                \
                    gnu/test/**,                \
                    **/swing/**/gtk/**

# We also want to exclude packages that we do not support to speed up
# build time, and prevent inadvernent linkage (I guess)
JAVA_EXCLUDES=$(OVM_EXCLUDE_SUBDIRS),		\
	      $(GCP_EXCLUDE_SUBDIRS)

VPATH=.:$(srcdir):$(srcdir)/ovm_classpath:$(srcdir)/ovm_test:$(top_srcdir)/gnu-classpath:$(top_srcdir)/gnu-classpath/external/w3c_dom:$(top_srcdir)/gnu-classpath/external/sax:$(top_srcdir)/gnu-classpath/external/relaxngDatatype:./gnu-classpath

all: $(JAVA_JARFILE)

# java-compile: java/util/LocaleData.java

# # FIXME: this makefile should definitely move to ovm_classpath
# java/util/LocaleData.java:
# 	mkdir -p `dirname $@`
# 	sh -c $(top_srcdir)/gnu-classpath/scripts/generate-locale-list.sh > $@

# RESOURCES=lib/awt/font.properties		\
# 	  lib/security/classpath.security	\
# 	  $(BUNDLES)

# BUNDLES=gnu/regexp/MessagesBundle.properties	\
# 	gnu/regexp/MessagesBundle_fr.properties	\
# 	java/util/iso3166-a3.properties		\
# 	java/util/iso3166.properties		\
# 	java/util/iso3166_de.properties		\
# 	java/util/iso639-a2-old.properties	\
# 	java/util/iso639-a3.properties		\
# 	java/util/iso639.properties		\
# 	java/util/iso639_de.properties		\
# 	java/util/iso639_fr.properties		\
# 	java/util/iso639_ga.properties

# rscdir=$(top_srcdir)/gnu-classpath/resource

# resources:
# 	$(MKTREE) lib/awt
# 	cp $(rscdir)/gnu/java/awt/peer/gtk/font.properties lib/awt
# 	$(MKTREE) lib/security
# 	cp $(rscdir)/java/security/classpath.security lib/security
# 	for b in $(BUNDLES); do cp $(rscdir)/$$b $$b; done

# The default jar rule excludes too much.  java-clean actually suffers
# from a similar problem.  The recursion trick used in clean does not
# work here, because $(JAVA_JARFILE) depends on compile.
$(JAVA_JARFILE): java-compile 
	$(FIND_AND_EXCLUDE) . '$(OVM_EXCLUDE_SUBDIRS)' "*.class" 	\
		| sed 's,^./,,' | zip -q@ ovm_rt_user.zip
	(cd gnu-classpath/lib; zip -qru ../../ovm_rt_user.zip *)
	mv ovm_rt_user.zip $@

clean:
	$(MAKE) JAVA_EXCLUDES='$(OVM_EXCLUDE_SUBDIRS)' java-clean

#	-rm java/util/LocaleData.java

install: java-install
