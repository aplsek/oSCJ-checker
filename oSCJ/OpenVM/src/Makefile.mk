# Hmm.  We can't quite abstract this away to an include file, because 
# we invoke constGen different ways depending on whether we are in the 
# executive domain.
GEN_PKG=ovm.core.execution
GEN_DIR=$(subst .,/,$(GEN_PKG))
GEN_FILES=$(GEN_DIR)/NativeConstants.java $(GEN_DIR)/OVMSignals.java

JAVA_JARFILE=ovm.jar

all: $(JAVA_JARFILE)

ifeq "$(RTEMS_BUILD)" "1"

# WARNING: command line arguments are compiled in this version of constGen

$(GEN_DIR)/NativeConstants.java: $(CONSTGEN)/constGen_default.exe
	$(MKTREE) $(GEN_DIR)
	$(RTEMS_SIMULATOR) $(CONSTGEN)/constGen_default.exe > $@

else

$(GEN_DIR)/NativeConstants.java: $(CONSTGEN)/constGen
	$(MKTREE) $(GEN_DIR)
	$(QEMU) $(CONSTGEN)/constGen $@

endif

$(GEN_DIR)/OVMSignals.java: $(CONSTGEN)/OvmSignalMapper.class
	$(MKTREE) $(GEN_DIR)
	$(JAVA) -cp $(CONSTGEN) OvmSignalMapper			\
		-jp $(GEN_DIR) -pkg $(GEN_PKG) -jn OVMSignals

JAVA_EXCLUDES=native/**,syslib/**,apps/**,test/tools/**

JAVADOC_SUBPACKAGES=ovm:s3

JAVA_LIBS=$(srcdir)/runabout.jar
JAVA_CLASSPATH=.:$(srcdir):$(JAVA_LIBS)

# we don't need to redefine bootclasspath, but can't very well leave the
# entry empty...
#
# ${java.home}/lib/classes.zip doesn't work as expected on OSX, but
# the ant wrapper script is careful to set the following
# (sun.boot.class.path also suites our needs, since we require 
# Sun JDK)
JAVA_BOOTCLASSPATH=$(JAVA_BOOT_PATH)

RT_JAR=$(word 1,$(subst :, ,$(JAVA_BOOT_PATH)))

# ovm_original.jar is used to avoid double-application of aspects in gen-ovm
remove_ovm_original_jar:
	-rm ovm_original.jar

#
# if $(RO) is not set (equal to the empty string)
# then do nothing but expanding runabout.jar so that the Runabout classes will be in ovm.jar
# else run RO.
#
ifeq ($(RO),)

ro:
	jar xf $(srcdir)/runabout.jar

ovm.jar: ro remove_ovm_original_jar

else

# we make ovm_ro.jar, as input to RO, which does not include Runabout classes in org/**.

ro:
	$(MAKE) 'JAVA_EXCLUDES=$(JAVA_EXCLUDES),org/**' ovm_ro.jar
	java -Xmx800m -classpath $(top_srcdir)/build/xtc.jar org.ovmj.tools.ro.RO -library=$(RT_JAR) -dump=. $(srcdir)/runabout.jar ovm_ro.jar

ovm.jar: ro
endif

clean: java-clean
	-rm ovm_original.jar
	-rm ovm_ro.jar
	-rm -r org
	-rm $(GEN_FILES)

java-compile: $(GEN_FILES)
java-file-list: $(GEN_FILES)

WEBDIR=doc/
WEBDATA=javadoc
weball: java-doc
webinstall: webinstall-default

install: java-install


# Eclipse support.  The following rule will correctly generate eclipse
# .classpath and .project files for using the main source tree in the
# toplevel directory.  These rules ensure that Eclipse exclude
# expressions are in close agreement with the excludes we use during
# normal compilation, and take seperate build directories into account.
ifeq ($(srcdir),.)
eclipse-dotfiles:
	echo -n > $(top_srcdir)/.project
	for line in $(PROJECT_PROLOGUE) $(PROJECT_EPILOGUE); \
	do echo "$$line" >> $(top_srcdir)/.project;	     \
	done
	echo -n > $(top_srcdir)/.classpath
	for line in $(CLASSPATH_PROLOGUE) $(CLASSPATH_OUTPUT_SRC) \
		    $(CLASSPATH_EPILOGUE);			  \
	do echo "$$line" >> $(top_srcdir)/.classpath;		  \
	done
else
eclipse-dotfiles:
	echo -n > $(top_srcdir)/.project
	for line in $(PROJECT_PROLOGUE) $(PROJECT_LINKS) 		      \
		    $(PROJECT_EPILOGUE);				      \
	do echo "$$line" >> $(top_srcdir)/.project;			      \
	done
	echo -n > $(top_srcdir)/.classpath
	for line in $(CLASSPATH_PROLOGUE) $(CLASSPATH_BUILDDIR)	\
		    $(CLASSPATH_EPILOGUE);			\
	do echo "$$line" >> $(top_srcdir)/.classpath;		\
	done
endif

ECLIPSE_EXCLUDES=$(shell echo '$(JAVA_EXCLUDES)' | sed -e 's/ *, */|/g')

PROJECT_PROLOGUE=							       \
	'<?xml version="1.0" encoding="UTF-8"?>'			       \
	'<projectDescription>'						       \
	'	<name>'`basename $(top_srcdir)`'</name>'		       \
	'	<comment></comment>'					       \
	'	<projects>'						       \
	'	</projects>'						       \
	'	<buildSpec>'						       \
	'		<buildCommand>'					       \
	'			<name>org.eclipse.jdt.core.javabuilder</name>' \
	'			<arguments>'				       \
	'			</arguments>'				       \
	'		</buildCommand>'				       \
	'	</buildSpec>'						       \
	'	<natures>'						       \
	'		<nature>org.eclipse.jdt.core.javanature</nature>'      \
	'	</natures>'
PROJECT_LINKS=						      \
	'	<linkedResources>'			      \
	'		<link>'				      \
	'			<name>build directory</name>' \
	'			<type>2</type>'		      \
	'			<location>'`pwd`'</location>' \
	'		</link>'			      \
	'	</linkedResources>'
PROJECT_EPILOGUE='</projectDescription>'
CLASSPATH_PROLOGUE=				 \
	'<?xml version="1.0" encoding="UTF-8"?>' \
	'<classpath>'				 \
	'    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>' \
	'    <classpathentry kind="lib" path="$(subst $(srcdir)/,src/,$(JAVA_LIBS))"/>' \
	'    <classpathentry excluding="$(ECLIPSE_EXCLUDES)" ' \
        '        kind="src" path="src"/>'
CLASSPATH_OUTPUT_SRC=					\
	'    <classpathentry kind="output" path="src"/>'
CLASSPATH_BUILDDIR=						    \
	'    <classpathentry excluding="$(ECLIPSE_EXCLUDES)" ' 	    \
        '        kind="src" path="build directory"/>' 		    \
	'    <classpathentry kind="output" path="build directory"/>'
CLASSPATH_EPILOGUE='</classpath>'

prj.el: Makefile.mk
	(echo '(setq jde-project-name "OpenVM")';			     \
	 echo '(setq jde-global-classpath';				     \
	 echo '      (list "$(shell pwd)"';				     \
	 echo '             "$(subst :," ",$(JAVA_CLASSPATH))"))';	     \
	 echo "(setq jde-compile-option-directory \"`pwd`\")";		     \
	 echo "(setq jde-sourcepath '(\"$(srcdir)\" \"`pwd`\"))";	     \
	 echo '(setq jdb-compile-option-target "1.4")';			     \
	 echo "(setq jde-build-function '(jde-make))";			     \
	 echo '(setq jde-make-args "-w -C $(shell pwd)")';		     \
	 echo '(setq jde-run-application-class';			     \
	 echo '      "s3.services.bootimage.Driver")';			     \
	 echo '(setq jde-run-option-properties';			     \
	 echo "      '((\"ovm.stitcher.path\" .";			     \
	 echo '         "$(top_srcdir)/config:$(shell cd ../config; pwd)")'; \
	 echo '        ("ovm.stitcher.file" . "stitchery")))';		     \
	 echo "(setq jde-run-option-vm-args '(\"-Xmx700m\"))";		     \
	 echo "(setq jde-db-option-properties jde-run-option-properties";    \
	 echo "	     jde-db-option-vm-args  jde-run-option-vm-args)";	     \
	 echo "(setq jde-run-read-app-args t)";				     \
	 echo "(setq jde-help-docsets";					     \
	 echo "      (cons '(\"User (javadoc)\"";			     \
	 echo '              "http://ovmj.org/doc/javadoc"';		     \
	 echo '              nil)';					     \
	 echo '      jde-help-docsets))';				     \
	 echo '(setq jde-xref-store-prefixes (list "ovm" "s3"))';	     \
	 echo '(setq jde-xref-db-base-directory "$(shell pwd)")';	     \
	 echo '(setq jde-xref-class-path (list "$(shell pwd)"))';	     \
	 echo '(setq jde-built-class-path jde-xref-class-path)';	     \
	 echo '(setq c-basic-offset 4)';				     \
	 ) > $(srcdir)/prj.el

aspectcheck: ajc-check
