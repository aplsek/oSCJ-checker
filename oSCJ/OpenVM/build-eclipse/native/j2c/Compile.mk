# Rules for compiling a j2c-based virual machine
INCS +=  -I. -I$(srcdir) $(include_path)
# I had src/.libs here initially, but it didn't work!
LDFLAGS += $(lib_path)
LIBS:=-lovmnative -lffi $(LIBS)

DRIVER_GENERATED+=ovm_inline.c 		\
		  ovm_inline.h			\
		  ovm_heap_exports.s		\
		  ovm_heap_exports.h

# Turn off warnings in this directory, and be VERY sure to compile C code
# with exception support
CFLAGS:=$(patsubst -W%,,$(CFLAGS)) -fno-strict-aliasing

CXX_SRC=jrt.c
C_SRC=invoke_native.c gen-ovm.c
ASM_SRC=ovm_heap_exports.s

include GenOvm.mk

OBJS=$(CXX_SRC:.c=.o) $(C_SRC:.c=.o) $(ASM_SRC:.s=.o) \
     ovm_inline_tables.o $(IMAGE_LINK_MAGIC_FILE)

ovm_inline.s: ovm_inline.i
	$(CC) $(ALL_CXXFLAGS) -fpreprocessed -S -o $@ $<

# This target implicitly generates the file code_indexes.
# code_indexes contains one line per J2cCodeFragment.frags entry,
# giving the method UID for this code-fragment index.
ovm_inline_tables.s: ovm_inline.s make-method-table.awk
	$(FILTER_OVM_INLINE) |\
	$(AWK) -v header_skip_bytes=$(HEADER_SKIP_BYTES) \
	       -v self_forwarding_at_offset=$(SELF_FORWARDING_OFFSET) \
	       -v underscore=$(UNDERSCORE) \
	       -f $(srcdir)/make-method-table.awk  > $@

# print exported data addresss in the format XXXXXXXX S*
ovm_heap_imports: ovm
	$(NM) $< | $(AWK) '/^[0-9a-fA-F]/ && $$2 ~ /^D$$/ {		\
	    sub("^00000000", "");					\
            printf("%s %s\n", $$1, $$3);				\
         }' > $@

ifeq "$(RTEMS_BUILD)" "1"

# FIXME: make this platform independent using Autoconf macros

OVM_BINCPU:=unsupported

ifeq "$(OVM_SPARC)" "1"
	OVM_BINCPU:=sparc
endif

ifeq "$(OVM_X86)" "1"
	OVM_BINCPU:=i386
endif


# disable - this is way too slow

# it in fact depends on FilesystemImage.h, which is generated together with
# FilesystemImage.c

# ifneq "$(EXTRA_OVM_OBJS)" ""
# jrt.o:	FilesystemImage.c
# endif 

# FilesystemImage.c:	FilesystemImage
#	$(RTEMS)/tools/build/rtems-bin2c FilesystemImage FilesystemImage

FilesystemImage:
	( cd $(INITFS) && tar cf - . ) > $@

FilesystemImage.o:	FilesystemImage
	$(OBJCOPY) -I binary -O elf32-$(OVM_BINCPU) -B $(OVM_BINCPU) FilesystemImage FilesystemImage.o

imgimg.o: img
	$(top_builddir)/src/native/img2asm/img2asm $(PAD_IMAGE_SIZE) < img > imgimg.s
	$(CC) $(ALL_CXXFLAGS) -c -o imgimg.o imgimg.s

ovm: $(OBJS) imgimg.o img $(EXTRA_OVM_OBJS) 
	$(CC) $(ALL_CXXFLAGS) $(APP_LDFLAGS) $(LDFLAGS) $(OPT) $(ADDL_LDFLAGS) -o $@ \
	       imgimg.o $(OBJS) $(LIBS) $(APP_LIBS) $(EXTRA_OVM_OBJS)

else # RTEMS_BUILD
ovm: $(OBJS) img
	$(CC) $(APP_LDFLAGS) $(LDFLAGS) $(OPT) -o $@ \
	       $(OBJS) $(LIBS) $(APP_LIBS)
endif # RTEMS_BUILD

clean: native-clean
	-rm $(DRIVER_GENERATED) ovm ovm_heap_imports

