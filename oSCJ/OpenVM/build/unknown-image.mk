# Disable linking of bootimage into executable
CFLAGS += -UBOOTBASE
IMAGE_LINK_MAGIC_FILE=
IMAGE_LINK=false
