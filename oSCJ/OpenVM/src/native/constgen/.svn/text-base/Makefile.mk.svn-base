INCS += -I../common/include -I$(srcdir)/../common/include

SRCS=constantGenerator.c
OBJS=$(SRCS:.c=.o)

ifeq "$(RTEMS_BUILD)" "1"

# RTEMS requires command line arguments to be compiled in

all: OvmSignalMapper.class constGen_noctorg.ovmj.java.exe constGen_default.exe

constGen_default.exe: constantGenerator.c
	$(CC) $(ALL_CFLAGS) -o $@ $^ $(LIBS)

constGen_noctorg.ovmj.java.exe: constantGenerator.c
	$(CC) $(ALL_CFLAGS) -DCG_DEFAULT_NOCT=1 -DCG_DEFAULT_PKG=\"org.ovmj.java\" $(LDFLAGS) -o $@ $^ $(LIBS)

else # non-RTEMS

all: OvmSignalMapper.class constGen

constGen: $(OBJS)
	$(LD) $(LDFLAGS) -o $@ $^ $(LIBS)
#	$(LD) -o $@ $^

endif

clean: native-clean
	-rm constGen OvmSignalMapper.class

install:
