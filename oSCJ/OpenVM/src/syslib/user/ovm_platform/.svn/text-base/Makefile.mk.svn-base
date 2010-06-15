JAVA_CLASSPATH=.
JAVA_BOOTCLASSPATH=$(JAVA_BOOT_PATH)
JAVA_JARFILE=ovm_platform.jar

GEN_PKG=org.ovmj.java
GEN_DIR=$(subst .,/,$(GEN_PKG))

GEN_FILES=$(GEN_DIR)/NativeConstants.java $(GEN_DIR)/OVMSignals.java

all: $(JAVA_JARFILE)

clean: java-clean
	-rm $(GEN_FILES)

install: java-install

java-compile: $(GEN_FILES)
java-file-list: $(GEN_FILES)

ifeq "$(RTEMS_BUILD)" "1"

# WARNING: GEN_PKG is compiled in constGen

$(GEN_DIR)/NativeConstants.java: $(CONSTGEN)/constGen_noct$(GEN_PKG).exe
	$(MKTREE) $(GEN_DIR)
	$(RTEMS_SIMULATOR) $(CONSTGEN)/constGen_noct$(GEN_PKG).exe > $@

else # not RTEMS

$(GEN_DIR)/NativeConstants.java: $(CONSTGEN)/constGen
	$(MKTREE) $(GEN_DIR)
	$(QEMU) $(CONSTGEN)/constGen -noct $(GEN_PKG) $@

endif # constgen

$(GEN_DIR)/OVMSignals.java: $(CONSTGEN)/OvmSignalMapper.class
	$(MKTREE) $(GEN_DIR)
	$(JAVA) -cp $(CONSTGEN) OvmSignalMapper			\
		-jp $(GEN_DIR) -pkg $(GEN_PKG) -jn OVMSignals

