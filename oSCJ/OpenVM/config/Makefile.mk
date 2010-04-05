all:

clean:
	-rm -f install-paths

# don't install the build-tree path file, any templates, or any Makefile bits
NOINSTALL=paths *.in Makefile.mk Makefile
INSTALLABLE=$(patsubst %,! -name "%",$(NOINSALL)) ! -path "*/\.svn/*" -type f
DESTDIR=$(prefix)/ovm/config

install: install-paths.in
	$(INSTALL) -d $(DESTDIR)
	cd $(srcdir);					       \
	   for d in `find . $(INSTALLABLE) -print`;	       \
	   do						       \
	     $(INSTALL) -d $(prefix)/ovm/config/`dirname $$d`; \
	     $(INSTALL_DATA) $$d $(DESTDIR)/`dirname $$d`;     \
	   done
	sed -e 's,@prefix@,$(prefix),g' 	    \
	    -e 's,@ABS_TOP_SRCDIR@,$(top_srcdir),g' \
	    -e 's,@IMAGE_ARGUMENT@,$(IMAGE_ARGUMENT),g'
	    < $< > install-paths
	$(INSTALL) install-paths $(DESTDIR)/paths

