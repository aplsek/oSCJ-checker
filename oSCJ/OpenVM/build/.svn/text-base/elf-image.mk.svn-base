# Link the bootimage memory dump into the ovm executable for elf
# systems with GNU ld and objcopy.
IMAGE_LINK=true
CFLAGS += -DBOOTBASE=$(BOOTBASE)
LDFLAGS += -Wl,-T,ld.script
IMAGE_LINK_MAGIC_FILE=img.o
DRIVER_GENERATED += empty.s empty.o img.o ld.script

# glibc OS-specific program header types
# quick link to .eh_frame_hdr section
PT_GNU_EH_FRAME=0x6474e550
# GNU_STACK is unused on x86
PT_GNU_STACK=   0x6474e551

# Use default linker emulation unless we are on x86/amd64
ifeq ($(shell uname -m),x86_64)
  EMULATION=-melf_i386
endif

# Derive a new elf linker script from ld's builtin script.  
# ld -verbose will print the script (which varies slightly between
# linux platforms).  Our job is to add a third loadable segment to the
# executable called bootimage, force the .bootimage section to be
# allocated in this segment.
#
# The builtin linker script never sets up elf segments, so it is up to
# us to define the segments (using PHDRS), map all the standard
# sections into text or data, and map our new section into our new
# segment.  For dynamically linked programs, the path to ld.so must be
# in an interp segment, but statically linked programs must not define
# this segment.
#
# It helps to examine the output of `readelf -e'.
# 
# gcc 3.2 dynamically linked objects also have a PHDR segment, INTERP
# comes before .note.ABI-tag, and .eh_frame_hdr is defined as a
# subsegment of text.  I don't think relocations are part of the
# interp segment.
# 
# Maybe a more portable option would be to extract segment mappings
# from readelf output, parse the builtin linker script more carefully,
# and generate segment placement specifiers for every segment we
# actually see in programs
ld.script:
	ld $(EMULATION) -verbose | gawk -v in_leading_junk=1 -v in_preinit_array=0 '		  \
		function static_p() { 					  \
		   return "$(STATIC_LINK)" == "yes"; 			  \
		}							  \
		/}/ { if (in_preinit_array) {                             \
			print $$0 ":data";                                \
			in_preinit_array = 0;                             \
			next;                                             \
		      }                                                   \
			in_preinit_array = 0;                             \
	         }                                                        \
                /^=========/ { in_leading_junk = 0; next }		  \
                in_leading_junk { next }				  \
		/^SECTIONS/ {						  \
		    print "PHDRS {";					  \
		    if (!static_p()) {					  \
                      print "  phdr\t\tPT_PHDR PHDRS;";			  \
		      print "  interp\tPT_INTERP;";			  \
                    }							  \
		    print "  text\t\tPT_LOAD FILEHDR PHDRS;";	  \
		    print "  data\t\tPT_LOAD;";			  \
		    if (!static_p())					  \
                      print "  dynamic\tPT_DYNAMIC;";			  \
		    print "  abi-tag\tPT_NOTE;";			  \
		    if (!static_p())					  \
                      print "  eh_frame\t$(PT_GNU_EH_FRAME);";		  \
		    if (strtonum($(HEAPBASE)) && (strtonum($(HEAPBASE)) < strtonum($(BOOTBASE))))	  \
                       print "  java-heap\tPT_LOAD AT($(HEAPBASE));";	  \
		    print "  bootimage\tPT_LOAD AT($(BOOTBASE));";	  \
		    if (strtonum($(HEAPBASE)) && (strtonum($(HEAPBASE)) > strtonum($(BOOTBASE))))	  \
                       print "  java-heap\tPT_LOAD AT($(HEAPBASE));";	  \
		    print "}";						  \
		    print "";						  \
		}							  \
		reset_segment && /}$$/ {				  \
		  print $$0 " " reset_segment;				  \
		  reset_segment = 0;					  \
		  next;							  \
		}							  \
		/.interp/ {						  \
		   print $$0 " :text" (static_p() ? "" : " :interp");	  \
		   print "  .note.ABI-tag : "				  \
			 "{ *(.note.ABI-tag) }:abi-tag :text";		  \
		   next;						  \
		}							  \
		!static_p() && /\.dynamic.*}/ {				  \
		   print $$0 " :data :dynamic";				  \
		   reset_segment = ":data";				  \
		   next;						  \
                }							  \
		!static_p() && /\.eh_frame_hdr/ {			  \
		   print $$0 " :text :eh_frame";			  \
		   reset_segment = ":text";				  \
		   next;						  \
		}							  \
		/.hash/ { print $$0 ":text"; next; }			  \
		/^  .preinit_array/ { print $$0 ; in_preinit_array = 1; next; }	  \
		/\*\(COMMON\)/ {					  \
		  print "   *(EXCLUDE_FILE(empty.o) COMMON)"; next;	  \
                }							  \
		/.stab / { print $$0 ":NONE"; next; }			  \
		/^}/ {							  \
		  if ($(HEAPBASE))					  \
                    print "  .java-heap $(HEAPBASE): { empty.o(COMMON) }" \
	                  " :java-heap";				  \
		  print "  .bootimage $(BOOTBASE) : "			  \
		        "{ *(.bootimage) } :bootimage";			  \
                }							  \
		{ print }' > $@

#		/dynamic.*}$$/ { 					  \
#		  print $$0 " :data :dynamic";				  \
#		  after_dynamic = 1;					  \
#		  next;							  \
#		}							  \
#		/}/ && after_dynamic {					  \
#		  print $$0 ":data";					  \
#		  after_dynamic = 0;					  \
#		  next;							  \
#		}							  \

#                    print "  .java-heap $(HEAPBASE) : {";
#	            printf("    . += 0x%x;\n", $(HEAPSIZE));
#                    print "  } :java-heap";

ifeq ($(HEAPBASE), 0x0)
empty.s: 
	echo > $@
else
empty.s: 
	echo .comm _java_heap, $(HEAPSIZE) > $@
endif

img.o: empty.o img ld.script
	objcopy --add-section .bootimage=img		        \
		--change-section-address .bootimage=$(BOOTBASE) \
		--set-section-flags .bootimage=alloc,load       \
		empty.o $@
