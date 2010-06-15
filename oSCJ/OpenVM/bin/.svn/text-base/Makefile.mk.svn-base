all: gen-spec-ovm 
	chmod a+x gen-ovm ovm-config record-eclipse-launch

.in.installed:
	sed -e 's,@JAVA@,$(JAVA),g'			     \
	    -e 's,@MAKE@,$(MAKE),g'			     \
	    -e 's,@STITCHER_PATH@,$(prefix)/ovm/config,g'    \
	    -e 's,@OVM_JAR@,$(prefix)/ovm/classes/ovm.jar,g' \
	    < $< > $@

# Re-expand gen-ovm.in for install paths.  Performing this step here 
# ensures that `make prefix=/some/weird/dir install' works
# FIXME: is running sed by hand really the only option?
install: gen-ovm.installed ovm-config.installed
	$(INSTALL) -d $(prefix)/bin
	$(INSTALL) gen-ovm.installed $(prefix)/bin/gen-ovm
	$(INSTALL) ovm-config.installed $(prefix)/bin/ovm-config
	$(INSTALL) gen-spec-ovm $(prefix)/bin
	$(INSTALL) record-eclipse-launch $(prefix)/bin

clean:
	-rm gen-ovm.installed ovm-config.installed gen-spec-ovm

