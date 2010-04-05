TOP_FILES=README TUTORIAL FAQ

ifeq ($(MAKEINFO),)
all install:
	echo "nothing to do: texinfo not installed"
else
all: ovm.info

ovm.info stamp-html ovm.pdf: $(wildcard $(srcdir)/*.texi)

# Convert a chapter of the manual to a standalone file
%-wrapper.texi:
	echo "\\input texinfo" > $@
	echo @setfilename $*.info >> $@
	echo @settitle $* >> $@
	echo @node Top >> $@
	echo @top $* >> $@
	echo @include $*.texi >> $@
	echo @bye

%.info: %.texi
	$(MAKEINFO) -I $(srcdir) $<

%.pdf: %.texi
	$(TEXI2DVI) -I $(srcdir) --pdf $<

%.ps: %.texi
	$(TEXI2DVI) -I $(srcdir) $<
	dvips -o $@ $*

%: %-wrapper.texi %.texi
	-$(MAKEINFO) -I $(srcdir) --force --no-headers -o $@ $*-wrapper.texi

FAQ.html: FAQ-wrapper.texi FAQ.texi
	-$(MAKEINFO) -I $(srcdir) --html --no-split --force \
		     -o $@.raw $*-wrapper.texi
	awk -v in_link=0 '/<div class="node">/ { in_link=1 }	   \
			  in_link && /<a name=.*>/ {		   \
				match($$0, /<a name=[^>]*>/);	   \
			        link=substr($$0, RSTART, RLENGTH); \
				print link "</a>"		   \
			  }					   \
			  !in_link { print }			   \
		          /<\/div>/ { in_link = 0 }' < $@.raw > $@

stamp-html:
	$(MAKEINFO) -I $(srcdir) --html $(srcdir)/ovm.texi
	touch $@

# install generated files to the toplevel source directory
distinstall: distall
	cp $(TOP_FILES) $(top_srcdir)

distall: $(TOP_FILES)

weball: stamp-html ovm.pdf FAQ.html

# install generated files to www.ovmj.org.  Must be run as ovm user
WEBDATA=ovm ovm.pdf FAQ.html 
WEBDIR=doc
webinstall:  weball webinstall-default

# Once the .info file grows past 300k, we will have a small file named
# ovm.info, and some 300k files named ovm.info-N.  I don't know
# whether the below definition works with `make all install'.  If
# $(wildcard) is expanded as this file is read, it will fail.
DESTDIR=$(subst $(prefix)/,,$(infodir))
INSTALL_DATA=ovm.info $(wildcard ovm.info-*)

DIR_ENTRY='* ovm: (ovm).	A configurable JVM generator'

ifeq ($(INSTALL_INFO),)
install: ovm.info install-default
	echo "$(DIR_ENTRY)" >> $(infodir)/dir
else
install: ovm.info install-default
	$(INSTALL_INFO) --defentry=$(DIR_ENTRY) --section=Programming --info-dir=$(infodir) $< \
	|| $(INSTALL_INFO) --entry=$(DIR_ENTRY) --section=Programming --info-dir=$(infodir) $< \
	|| echo "$(DIR_ENTRY)" >> $(infodir)/dir
endif
endif

clean:
	-rm *.info *.ps *.dvi $(TOP_FILES) *-wrapper.texi FAQ.html*
	-rm -r ovm
