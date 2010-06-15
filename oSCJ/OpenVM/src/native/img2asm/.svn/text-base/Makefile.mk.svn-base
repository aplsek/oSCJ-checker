INCS += -I../common/include -I$(srcdir)/../common/include

all: img2asm

img2asm: img2asm.c
	cc $(INCS) -O2 -g -o $@ $^

clean:
	-rm -f img2asm

install:
