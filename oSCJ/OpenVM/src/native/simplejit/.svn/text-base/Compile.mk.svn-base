INCS += -I. -I$(srcdir)/include $(include_path)
LDFLAGS += $(lib_path)
LIBS := -lovmnative $(LIBS)
SRCS=gen-ovm.c					\
     globals.c					\
     image.c					\
     invoke_native.c				\
     main.c					\
     mthread.c					\
     runtime.c					\
     runtime_functions.c

OBJS=$(SRCS:.c=.o) $(IMAGE_LINK_MAGIC_FILE)
DRIVER_GENERATED+=runtime_functions.h

ovm: $(OBJS)
	$(LD) $(APP_LDFLAGS) $(LDFLAGS) -o $@ $^ $(LIBS) $(APP_LIBS)

clean: native-clean
	-rm $(DRIVER_GENERATED) ovm

