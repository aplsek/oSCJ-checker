INCS += -I. -I./include -I$(srcdir)/include

all: libovmnative.a objectsCollection.dat.phony

GEN_FILES=signalmapper.c include/signalmapper.h

$(GEN_FILES): $(CONSTGEN)/OvmSignalMapper.class
	$(JAVA) -cp $(CONSTGEN) OvmSignalMapper	\
		-cp .				\
		-cn signalmapper

SRCS=	clock.c \
	mem.c \
	native_helpers.c  \
	nativeScheduling.c \
	timer.c \
	sigiomanager.c \
	blockingio.c \
	eventmanager.c \
	systemproperties.c \
	signalmanager.c \
	signalmapper.c \
        signalmonitor.c \
        intmonitor.c \
	atomicops.c \
        fdutils.c \
        doselect.c \
        waitmanager.c\
        nanosleep_timer.c\
	smp_timer.c\
	rtems_support.c

OBJS=signalmapper.o $(SRCS:.c=.o) 

signalmapper.o:  signalmapper.c
singalmanager.c: include/signalmapper.h

thunkTester: thunkTester.o
	$(CXX) -o $@ $<

libovmnative.a: $(OBJS)

objectsCollection.dat.phony: $(OBJS)
	rm -f objectsCollection.dat
	for x in $(OBJS); do echo $$PWD/$$x >> objectsCollection.dat; done

clean: native-clean
	-rm $(GEN_FILES) 

OVM_LIB=$(prefix)/ovm/lib/$(MULTI_OS_DIRECTORY)

#hmm, now we have a source file called %.gen.
INSTALL_DATA=include/signalmapper.h		 \
	     include/autodefs.h			 \
	     $(wildcard $(srcdir)/include/*.h)	 \
	     $(wildcard $(srcdir)/include/*.inc)
DESTDIR=ovm/include

install: libovmnative.a install-default
	$(INSTALL) -d $(OVM_LIB)
	$(INSTALL) libovmnative.a $(OVM_LIB)
	$(RANLIB) $(OVM_LIB)/libovmnative.a
