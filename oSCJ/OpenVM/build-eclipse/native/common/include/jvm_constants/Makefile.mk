# Convert member interfaces in JVMConstants to C header files.  Each file 
# has a name derived from the interface name, and defines constants 
# corresponding to each interface field.  The constants are all given
# a prefix to avoid name conflicts.

# The naming conventions predate DumpInterface, and don't track very
# closely between C and Java.
HEADERS=opcodes.h				\
	invoke_system_arguments.h		\
	word_ops.h				\
	dereference_ops.h			\
	throwables.h

JVMConstants=ovm.services.bytecode.JVMConstants
JVMConstants_java=$(top_srcdir)/src/$(subst .,/,$(JVMConstants)).java
DumpInterface=s3.services.buildtools.DumpInterface
DumpInterface_java=$(top_srcdir)/src/$(subst .,/,$(DumpInterface)).java

DUMP=$(JAVA) -classpath $(top_builddir)/src/ovm.jar $(DumpInterface)

all: $(HEADERS)

$(HEADERS): $(JVMContants_java) $(DumpInterface_java)

opcodes.h:
	$(DUMP) -output=$@ -prefix=OPCODE '$(JVMConstants)$$Opcodes'

invoke_system_arguments.h:
	$(DUMP) -output=$@ -prefix=SYSTEM '$(JVMConstants)$$InvokeSystemArguments'

word_ops.h:
	$(DUMP) -output=$@ -prefix=WORD_OP '$(JVMConstants)$$WordOps'

dereference_ops.h:
	$(DUMP) -output=$@ -prefix=DEREFERENCE '$(JVMConstants)$$DereferenceOps'

throwables.h:
	$(DUMP) -output=$@ '$(JVMConstants)$$Throwables'

INSTALL_DATA=$(HEADERS)
DESTDIR=ovm/include/jvm_constants
install: install-default

clean:
	-rm $(HEADERS)
