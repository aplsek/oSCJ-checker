/**
 * OVM Interpreter (c) 2001-2002 S3 Lab, Purdue University
 *
 * This file contains an implementation of the image loader.
 *
 * @file boot.c 
 * @author Christian Grothoff
 * @author James Liang
 * @author Ben L. Titzer
 * @author Hiroshi Yamauchi
 **/

#include "boot.h"

/**
 * Debug boot.c?
 **/
#define DEBUG_BOOT_C YES

/* ************************** helper methods ******************** */

#ifndef BOOTBASE
/**
 * Check the magic value given as a parameter. Print an error message if it does not
 * match and exit.
 * @param value the value to check
 * @param message message to print
 **/
static void check_magic(int value, char * message) {
  if (IMAGE_MAGIC == value)  return;

  else if (ntohl(IMAGE_MAGIC) == (unsigned int)value) 
    errexit("Image is in wrong endianness (MAGIC error: %s)\n", message);
  else 
    errexit("0x%08X does not match expected IMAGE_MAGIC 0x%08X (%s)\n",
	    value, IMAGE_MAGIC, message);
}

/**
 * Compute the total size of an image based on the used memory reported in the
 * header, the size of the header, and the computed size of the reference map.
 **/
static size_t compute_image_size(int usedMem) {
/*    int t6 = usedMem / 4; */
/*    int t3 = ( t6 / 8) +  1; */
  // int t = t2 + sizeof(ImageFormat) + 8;
    ASSERT(usedMem > 0, "negative value for usedMem");
    return (size_t)usedMem + sizeof(ImageFormat) ;// t;

}
#endif

#if defined(SOLARIS_BUILD)
// on solaris, mmap totally ignores the supplied address,
// unless MAP_FIXED is used. OTOH, MAP_FIXED destroys
// previous existing mappings. The very convoluted code
// below detects whether the address space requested is
// actually free, recycling the code at
// http://www.winehq.com/hypermail/wine-devel/2000/11/0202.html

#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <alloca.h>
#include <stdio.h>
#include <string.h>
#include <signal.h>
#include <procfs.h>

static int is_mapped_test (uintptr_t vaddr, size_t size,
			   const prmap_t *asmap, int n)
{
  int i = 0, j = n;

  while (i < j)
  {
    int m = (i + j) / 2;
    const prmap_t *o = &asmap[m];

    if ((uintptr_t) o->pr_vaddr >= vaddr + size)
      j = m;
    else if ((uintptr_t) o->pr_vaddr + o->pr_size <= vaddr)
      i = m + 1;
    else
      return 1;
  }

  return 0;
}


static void *safe_mmap (void *addr, size_t len, int prot, int flags,
			int fildes, off_t off)
{
  if (flags & MAP_FIXED)
    return mmap (addr, len, prot, flags, fildes, off);
  else
  {
    int stat = 0;
    pid_t pid;
    int fd;
    struct stat sb;
    prmap_t *asmap;
    void *actual_addr;

    fd = open ("/proc/self/rmap", O_RDONLY);
    assert (fd != -1);
    if ((pid = vfork ()) == -1)
    {
      perror ("is_mapped: vfork");
      abort ();
    }
    else if (pid == 0)
    {
      fstat (fd, &sb);
      asmap = (prmap_t *) alloca (sb.st_size);
      read (fd, asmap, sb.st_size);
      if (is_mapped_test ((uintptr_t) addr, len, asmap,
			  sb.st_size / sizeof (prmap_t)))
	_exit (EADDRINUSE);
      else if ((actual_addr = mmap (addr, len, prot, flags | MAP_FIXED,
				    fildes, off)) == (void *) -1)
	_exit (errno);
      else if (actual_addr != addr)
      {
	munmap (actual_addr, len);
	kill (getpid (), SIGKILL);
      }
      else
      {
	_exit (0);
      }
    }
    else if (waitpid (pid,  &stat, WNOHANG) != pid)
    {
      perror ("is_mapped: waitpid");
      abort ();
    }
    close (fd);
    if (!WIFEXITED (stat))
      return mmap (addr, len, prot, flags, fildes, off);
    else if (WEXITSTATUS (stat) == 0)
      return addr;
    else if (WEXITSTATUS (stat) == EADDRINUSE)
      return mmap (addr, len, prot, flags, fildes, off);
    else
    {
      errno = WEXITSTATUS (stat);
      return (void *) -1;
    }
  }
}
#endif

/* ************************** public methods ******************** */

static ImageFormat *image;

void *getImageBaseAddress() {
  return image->data + 4;	   /* first word is null */
}

void *getImageEndAddress() {
  int off = image->usedMemory - 4; /* last word is magic number */
  return image->data + off;
}

/**
 * Load bootimage from file (should only be called once).
 * @param filename the name of the bootimage-file
 * @param mainObject this is set to the address of the main object
 * @param mainMethod this is set to the ByteCode that is executed first
 * @return OK on success, SYSERR on error (some errors also call errexit)
 **/
int load_image(char * filename,
	       void ** mainObject,
	       ByteCode ** mainMethod,
	       struct ovm_core_execution_CoreServicesAccess ** execCSA,
	       struct arr_jbyte ** utf8Array,
	       ByteCode ** bottomMethod,
	       jref *bootContext) {
#ifndef BOOTBASE
  int fileDescriptor;
  size_t length;
  ImageFormat imageHeader;
  void* baseAddress;
  int pgsize;

#if DEBUG_BOOT_C == YES
  print("Loading image from file %s...\n",filename);
#endif

  pgsize = getpagesize();
#define ROUND(X) ({ int _x = X; (_x + pgsize - 1) & ~(pgsize - 1); })
  /** try to open the file **/
  fileDescriptor = open(filename, O_NDELAY|O_RDONLY);
  ASSERT(fileDescriptor >= 0,"Could not open file with the bootimage.\n");

  /** read in the header of the image **/
  length = read(fileDescriptor, &imageHeader, sizeof(ImageFormat));
  ASSERT(sizeof(ImageFormat) == length, "Could not read full image header!");

  /** check magic number **/
  check_magic(imageHeader.OVM_MAGIC, 
	      "Magic number of image does not match (1)");

  /** look at the base address it expects **/
  baseAddress = (void*) imageHeader.baseAddress;

  /** find out the size of the entire image **/
  length = lseek(fileDescriptor,0,SEEK_END);
  ASSERT((off_t)length > 0, "Couldn't determine image size");

  /* Round up request to a multiple of page size.  The remainder of
   * the last page will be zero-filled.  Not * rounding up may cause
   * the OS to round down.
   */
  length = ROUND(length);

#if defined(SOLARIS_BUILD)
  image = safe_mmap(baseAddress,
	       length,
	       PROT_READ|PROT_WRITE,
	       MAP_PRIVATE,
	       fileDescriptor,
	       0);
#else
  image = mmap(baseAddress,
	       length,
	       PROT_READ|PROT_WRITE,
	       MAP_PRIVATE,
	       fileDescriptor,
	       0);
#endif

  close(fileDescriptor);

  ASSERT(image == baseAddress,
	 "Could not mmap bootimage at base address");

  registerMem(image, length);

#if DEBUG_BOOT_C == YES
  printf(" Size of image       : %d bytes\n",image->usedMemory);
  printf(" Image Base Address  : 0x%0X\n",(int) baseAddress);
  printf(" Main object location: 0x%0X\n",(int) image->mainObject);
  printf(" Main method location: 0x%0X\n",(int) image->mainMethod);
#endif

  if (length != ROUND(compute_image_size(image->usedMemory)))
      /* On OSX size_t is not unsigned int but unsigned long int - hence the casts */
    fprintf(stderr, "computed image size and actual size mismatch! %u != %u\n",
	   (unsigned)length, (unsigned)compute_image_size(image->usedMemory));      
#else
  image = (ImageFormat *) BOOTBASE;
#endif

  *mainObject = (void *)image->mainObject;
  *mainMethod = (ByteCode *)image->mainMethod;
  *execCSA = image->coreServicesAccess; 
  // *utf8Array = ((struct ovm_core_repository_Repository*)image->repository)->utf8s_->utf8s_;
  *bootContext = (jref)image->bootContext;
  *bottomMethod = (ByteCode* ) image->bottomFrameCode;
  return OK;

} // end of boot
