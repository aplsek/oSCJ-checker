#include "image.h"

#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdarg.h>
#include <netinet/in.h>

//#define IMAGE_MAGIC 0x494E2086  -- already defined somewhere else?

/**
 * errexit - log an error message and exit.
 */
void errexit(const char *format, ...) {
  va_list args;
  va_start(args, format);
  vfprintf(stderr, format, args);
  va_end(args);
  exit(1);
}

/**
 * Check the magic value given as a parameter. Print an error message
 * if it does not match and exit.
 **/
static void checkHeader(ImageFormat * image) {
  if (IMAGE_MAGIC == image->OVM_MAGIC) {
    if (0x00010000 != image->OVM_VERSION)
      errexit("Image has the wrong version number (%d).\n",
	      image->OVM_VERSION);
    return;
  } else if (IMAGE_MAGIC == ntohl(image->OVM_MAGIC)) 
    errexit("Image is in wrong endianness.\n");
  else 
    errexit("0x08X does not match expected IMAGE_MAGIC 0x%08X\n",
	    image->OVM_MAGIC, IMAGE_MAGIC);
}

extern void** runtimeFunctionTable;

/**
 * Load the bootimage from a file.
 * @return the image on success, NULL on error 
 **/
ImageFormat * loadImage(char* imageName) {
#ifndef BOOTBASE
    ImageFormat * ret = malloc(sizeof(ImageFormat));
    int fileDescriptor;
    unsigned int length;
    int pgsize;

    pgsize = getpagesize();
    
    fileDescriptor = open(imageName, O_NDELAY|O_RDONLY);
    if (fileDescriptor < 0) 
	errexit("Could not open bootimage file %s.\n",
		imageName);

    /** read in the header of the image **/
    length = read(fileDescriptor, 
		  ret, 
		  sizeof(ImageFormat));
    printf("base address = 0x%x\n", ret->baseAddress);
    if (sizeof(ImageFormat) != length)
	errexit("Image %s is corrupt, its size is smaller than the image header!\n",
		imageName);
    checkHeader(ret);

    length = lseek(fileDescriptor, 0, SEEK_END) - sizeof(ImageFormat);
/*     if (length != ret->usedMemory) */
/* 	errexit("Image %s is corrupt, the size of the file is incorrect (%d, header says %d).\n", */
/* 		imageName, */
/* 		length, */
/* 		ret->usedMemory); */

    /* Appearently, usedMemory does not include the image header at
     * all.  be sure to factor in the header size before rounding.
     */
    length += sizeof(ImageFormat);

    /* Round up request to a multiple of page size.  The remainder of
     * the last page will be zero-filled.  Not rounding up may cause
     * the OS to round down.
     */
    length = (length + pgsize - 1) & ~(pgsize - 1),

    ret = mmap((void*) ret->baseAddress /*- sizeof(ImageFormat)*/,
	       length,
	       PROT_READ|PROT_WRITE,
	       MAP_PRIVATE,
	       fileDescriptor,
	       0);
    if (ret == NULL)
	errexit("Could not mmap image %s: %s\n",
		imageName, 
		strerror(errno));
    close(fileDescriptor);  
  
    if (ret != (void*) (ret->baseAddress))
	errexit("Could not mmap %s at address: 0x%08X, got 0x%08X\n",
		imageName,
		ret->baseAddress,
		(unsigned int) ret);
#else
    ImageFormat *ret = (ImageFormat *) BOOTBASE;
#endif
    
    defineRuntimeFunctionTable();
    ((JITHeader*)ret->simpleJITBootImageHeader)->runtimeFunctionTableHandle
      = (void *) &runtimeFunctionTable;

    return ret;
}


