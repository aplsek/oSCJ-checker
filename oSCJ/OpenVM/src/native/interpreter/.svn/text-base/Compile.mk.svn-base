SRCS=boot.c					\
     debugging.c				\
     frame.c					\
     gen-ovm.c					\
     globals.c					\
     instruction_names.c			\
     interpreter.c				\
     interpreter_defs.c				\
     main.c					\
     print.c

INCS+= -I. -I$(srcdir) -I$(srcdir)/include $(include_path) 
LDFLAGS += $(lib_path)
LIBS := -lovmnative $(LIBS)
OBJS=$(SRCS:.c=.o) $(IMAGE_LINK_MAGIC_FILE)
DRIVER_GENERATED+=java_instructions_threaded.gen \
		  instruction_dispatch.gen	 \
		  instruction_names.c

ovm: $(OBJS)
	$(LD) $(APP_LDFLAGS) $(LDFLAGS) -o $@ $^ $(LIBS) $(APP_LIBS)

clean: native-clean
	-rm $(DRIVER_GENERATED) ovm
