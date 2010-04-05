INSTALL_DATA=$(srcdir)/Compile.mk		\
	     $(srcdir)/gdbinit.in		\
	     $(srcdir)/make-method-table.awk	\
	     $(srcdir)/barrier_filt.rb		\
	     $(wildcard $(srcdir)/*.c)		\
	     $(wildcard $(srcdir)/*.h)
DESTDIR=ovm/j2c

all:

install: install-default

clean:
