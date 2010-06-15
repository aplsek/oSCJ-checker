/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This file contains an implementation of the image loader.
 *
 * @file include/boot.h 
 * @author Christian Grothoff
 * @author James Liang
 * @author Ben L. Titzer
 * @author Hiroshi Yamauchi
 **/

#ifndef BOOT_H
#define BOOT_H

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/mman.h>
#include <stdlib.h>
#include <netinet/in.h>

#include "mem.h"
#include "boot.h"
#include "debugging.h"
#include "types.h"
#include "interpreter_defs.h"

/**
 * Internal macros that make code simpler and more consistent.
 **/
#define IMAGE_MAGIC_NUMBER 0x494E2086

/**
 * Load bootimage from file (should only be called once).
 * @param filename the name of the bootimage-file
 * @param mainObject this is set to the address of the main object
 * @param mainMethod this is set to the ByteCode that is executed first
 * @return 0 on success, non-zero on error
 **/
int load_image(char * filename,
	       void ** mainObject,
	       ByteCode ** mainMethod,
	       struct ovm_core_execution_CoreServicesAccess ** execCSA,
	       struct arr_jbyte ** utf8Array,
	       ByteCode ** bottomMethod,
	       jref *bootContext);

/* ************************** end of boot.h ******************** */
#endif
