INSTALL_DATA=$(srcdir)/Compile.mk		\
	     $(srcdir)/gdbinit.in		\
	     $(wildcard $(srcdir)/*.c)		\
	     $(wildcard $(srcdir)/include/*.h)
DESTDIR=ovm/simplejit

all:

install: install-default

clean:
