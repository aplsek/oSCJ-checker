# Basic make rules for OVM.  Some work is delegated to ant, and some
# is just too hard to generalize

.SUFFIXES: .cc .c  .h .hh .ii .i .s .o .java .class 	\
	   .gen  .sh .script .texi .info .html .pdf .ps

# This is what automake does.  The reasoning is described in
# http://make.paulandlesley.org/autodep.html#norule
FIX_DEPEND=@cp $*.d $*.P; \
            sed -e 's/^[^:]*: *//' -e 's/ *\\$$//' \
                -e '/^$$/ d' -e 's/$$/ :/' < $*.d >> $*.P; \
            rm -f $*.d

.c.o:
	$(CC) -MMD $(ALL_CFLAGS) -c $< -o $@
	$(FIX_DEPEND)

# Macroexpand a C file:  A convient rule for debugging macros, but
# probably not useful in a standard build:
.c.i:
#	$(CC) $(ALL_CFLAGS) -E $< > $@
	$(CC) -MMD -MT $@ $(ALL_CFLAGS) -E $< > $@
	$(FIX_DEPEND)

.c.s:
	$(CC) $(ALL_CFLAGS) -S $< -o $@
	$(FIX_DEPEND)

.s.o:
	$(CC) $(CFLAGS) -c $< -o $@

.cc.o:
	$(CXX) -MMD $(ALL_CXXFLAGS) -c $< -o $@
	$(FIX_DEPEND)

# Macroexpand before compilation:  g++ -E; g++ -c doesn't have the
# same strange effects effects on line numbers as g++ -c on a .cc
# file. 
.cc.ii:
	$(CXX) -MMD -MT $@ $(ALL_CXXFLAGS) -E $< > $@
	$(FIX_DEPEND)

.cc.s:
	$(CXX) $(ALL_CXXFLAGS) -S $< -o $@
	$(FIX_DEPEND)

.ii.o:
	$(CXX) $(ALL_CXXFLAGS) -fpreprocessed -c $< -o $@

%: %.sh
	cp $< $@
	chmod a+x $@

-include *.P

# rule for simple java programs such as constgen/OvmSignalMapper
# If we invoked $(JAVAC) from make normally and $JAVAC == jikes, we
# should make sure that the right depend flags are passed in.
# We also need some notion of the root package's directory.
.java.class:
	$(JAVAC) -nowarn -g -d . $<

# A generic target for executing a target in all subdirectories before
# performing some actions locally:
# usage:
#
# all: all-recursive
# 	...
#
# clean: clean-recusrive
#	...

# expect target name of form %-recursive # .DEFAULT:
#	 for d in $(SUBDIRS) ; do				\
#	   $(MAKE) -w -C $$d $(@:-recursive=) ;			\
#	   if [ $$? != 0 ] ; then exit 1 ; fi ;			\
#	 done

%-recursive:
	for d in $(SUBDIRS) ; do				\
	  $(MAKE) -w -C $$d $*;					\
	  if [ $$? != 0 ] ; then exit 1 ; fi ;			\
	done

# If there is no known extension, try to linking it as a program.  If 
# no dependencies have been specified, it must be a typeo.
# .DEFAULT:
# 	@if [ "$^" = "" ] ; then 		\
# 		echo "no rule to make $@";	\
# 		exit 1;				\
# 	fi
# 	$(LD) $(LDFLAGS) -o $@ $^ $(LIBS)

%.a:
	rm -f $@
	$(AR) cq $@ $^
	$(RANLIB) $@

native-clean:
	rm -f *.o *.ii *.P *.a

prog-clean: native-clean
	rm -f $(PROG)


ifeq ($(ANT),)
java-file-list: force
	$(FIND_AND_EXCLUDE) .:$(VPATH) '$(JAVA_EXCLUDES)' "*.java" > $@

aj-file-list: force
	$(FIND_AND_EXCLUDE) .:$(VPATH) '$(JAVA_EXCLUDES)' "*.aj" > $@

# Note this does not perform weaving, but simply statically check aspects against the Java code
ajc-check: aj-file-list
	$(AJC) -bootclasspath $(JAVA_BOOTCLASSPATH) \
		 -sourcepath .:$(VPATH)		      \
		 -classpath $(JAVA_CLASSPATH):$(top_builddir)/build/aspectjrt.jar \
		 -target 1.4                          \
		 -source 1.4                          \
		 -g                                   \
		 -d .                                 \
		 -XterminateAfterCompilation \
		 @aj-file-list

java-compile: java-file-list
	$(JAVAC) -J-Xmx700m \
		 -bootclasspath $(JAVA_BOOTCLASSPATH) \
		 -sourcepath .:$(VPATH)		      \
		 -classpath $(JAVA_CLASSPATH)         \
		 -target 1.4                          \
		 -source 1.4                          \
		 -g                                   \
		 -nowarn                              \
		 -d .                                 \
		 @java-file-list

else
ANT_PROPS=-Dbasedir=`pwd`				\
	  -Dsrcdir=.:$(VPATH)				\
	  -Dtop_srcdir=$(top_srcdir)			\
	  -Dexcludes="$(JAVA_EXCLUDES)"			\
	  -Dclasspath="$(JAVA_CLASSPATH)"		\
	  -Dbootclasspath="$(JAVA_BOOTCLASSPATH)"	\
	  -emacs

java-compile:
	$(ANT) -buildfile $(top_srcdir)/build/javac.xml $(ANT_PROPS)

endif

force:

META-INF/MANIFEST.MF: force
	$(MKTREE) META-INF
	echo "Manifest-Version: 1.0"            > $@
	echo "Version: $(OVM_VERSION)"         >> $@
	echo "Build-Date: `date '+%d %B %Y'`"  >> $@
	echo ""                                >> $@

OVM_VERSION=0.1.0

 %.jar: META-INF/MANIFEST.MF java-compile
	(echo $<; $(FIND_AND_EXCLUDE) . '$(JAVA_EXCLUDES)' "*.class") 	\
		| sed 's,^./,,' | zip -q@ $*.zip
	mv $*.zip $@

# @modified seems to be an RCS id that we add when we change a method
# or class in the ovm.util collections code.  The rcs ids attached to
# methods are meaningless, so they are not included in the docs.
# 
# @require was being included in the docs, but that looked wrong
#
# @specnote appears to be specific to the classpath-based ovm.util
# collections code, and other classpath code
#
# @spec is used in the RTSJ portion of the user library.
java-doc:
	$(JAVADOC) -J-Xmx384m					 \
		   -sourcepath $(VPATH)			 \
		   -classpath '$(JAVA_CLASSPATH)'		 \
		   -bootclasspath '$(JAVA_BOOTCLASSPATH)'	 \
		   -private					 \
		   -source 1.4                                   \
		   -tag param					 \
		   -tag return  				 \
                   -tag since                                    \
		   -tag date:X:"MMTk rcs date"  		 \
		   -tag require:X:"classpath preconditions"	 \
		   -tag specnote:a:"Specnote: " 		 \
		   -tag modified:t:"Modified: " 		 \
		   -tag spec:a:"Specified by: "		 \
	           -tag specbug:a:"Specification issue to be resolved: " \
		   -tag assert:X:"OVM preconditions"		 \
		   -tag status:X:"classpath compatibility notes" \
		   -breakiterator				 \
		   -d javadoc					 \
		   -subpackages $(JAVADOC_SUBPACKAGES)

java-doc-clean:
	rm -fr javadoc

java-tags:
	-rm $(srcdir)/TAGS
	$(FIND_AND_EXCLUDE) .:$(VPATH) '$(JAVA_EXCLUDES)' "*.java" | \
		xargs etags --append --output=$(srcdir)/TAGS

java-clean:
	rm -f $(JAVA_JARFILE) *.pdb java-file-list aj-file-list
	rm -fr META-INF
	-$(FIND_AND_EXCLUDE) . '$(JAVA_EXCLUDES)' "*.class" | xargs rm -f

java-install:
	$(INSTALL) -d $(prefix)/ovm/classes
	$(INSTALL_DATA) $(JAVA_JARFILE) $(prefix)/ovm/classes 

# double-dollar is used because at least one version of bash cannot parse
# a for loop with an empty `in' list.  If this version of bash looks
# up an empty environment variable, all will be well.
install-default: $(INSTALL_PROGS) $(INSTALL_DATA)
	$(INSTALL) -d  $(prefix)/$(DESTDIR)
	ee="$(INSTALL_PROGS)"; for e in $$ee; do	\
	  $(INSTALL) $$e $(prefix)/$(DESTDIR);		\
	done
	dd="$(INSTALL_DATA)"; for d in $$dd; do		\
	  $(INSTALL) -m 444 $$d $(prefix)/$(DESTDIR);	\
	done

# Use rsync to install data onto the web server.  The default value of
# WEBROOT works when this target is run as ovm on an S3 lab computer.
#
# Also note that --delete is a bit dangerous.
webinstall-default: $(WEBDATA)
	rsync -rltz -e ssh --delete $(WEBDATA) $(WEBROOT)/$(WEBDIR)
