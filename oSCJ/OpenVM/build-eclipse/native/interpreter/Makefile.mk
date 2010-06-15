INSTALL_DATA=$(srcdir)/Compile.mk		\
	     $(srcdir)/gdbinit.in		\
	     $(wildcard $(srcdir)/*.c)		\
	     $(wildcard $(srcdir)/*.gen)	\
	     $(wildcard $(srcdir)/include/*.h)
DESTDIR=ovm/interpreter

all:

install: install-default

clean:
