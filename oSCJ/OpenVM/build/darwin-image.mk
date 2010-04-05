# Link the bootimage memory dump inot the ovm executable for Mach-O
# systems with Apple ld.
CFLAGS += -DBOOTBASE=$(BOOTBASE)
LDFLAGS += -Wl,-sectcreate,image,image,img,-segaddr,image,$(BOOTBASE)
IMAGE_LINK_MAGIC_FILE=
IMAGE_LINK=true
