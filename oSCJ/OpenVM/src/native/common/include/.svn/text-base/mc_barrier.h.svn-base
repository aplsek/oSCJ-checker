
/* this code is NOT USED anymore, since we have a SW barrier */

/*
 * Support for hardware write barriers in MostlyCopying collectors.
 * This header file defines a non-static non-inlined function.  It is
 * intened to be included exactly once in the ovm executable, and it
 * takes several macro inputs
 *
 * OVM_PAGE_SIZE          Is the granularity of dirty and continued
 *                        block maps.  It should be a power of two and
 *                        a multiple of the hardware page size.
 * HEAP_START             Is the first address indexed by the dirty bitmap
 * IMAGE_CONTINUED_RANGES is a list of { start, end } of multi-page
 * 		          allocations within the bootimage
 *
 * This file exports one symbol: mc_barrier_init 
 */

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/mman.h>
#include "signalmanager.h"

/* J2c needs to create virtually identical text segments both before
 * and after the img file is generated.  We fix the size of
 * image_continued_ranges so that gen-ovm.o's rodata size the same
 * size both before and after IMAGE_CONTINUED_RANGES is fully defined
 *
 * When statically compiling dacapo fop with simpeljit we get around
 * 600 continued ranges, and pmd has 450.  Other dacapo benchmarks
 * have 200 or less.
 */
#define MAX_CONTINUED_RANGES 1200

typedef struct {
  size_t start;
  size_t end;
} continued_range_t;

static const continued_range_t image_continued_ranges[MAX_CONTINUED_RANGES] = {
  IMAGE_CONTINUED_RANGES
};

#define N_CONTINUED_RANGES \
        (sizeof(image_continued_ranges)/sizeof(continued_range_t))

static int *dirty;
static size_t dirty_size;

typedef void handler_t();
static handler_t *previous_handler[32];

#define SET_BIT(V, I) (V[I>>5] |= (1 << ((I) & 0x1f)))

static void segv_handler(int sig_no, siginfo_t *info, void *_ctx) {
  size_t fault_addr = (size_t) FAULT_ADDR(info, _ctx);
  size_t delta = fault_addr - HEAP_START;

#if 0
  fprintf(stderr, "segv_handler(%d, %p, %p) [addr = %p]\n",
	  sig_no, info, _ctx, fault_addr);
#endif
  if (delta < dirty_size) {	/* note unsigned comparison */
    int idx = delta / OVM_PAGE_SIZE;
    SET_BIT(dirty, idx);
    int r =  mprotect((void *) (fault_addr & ~(OVM_PAGE_SIZE-1)), OVM_PAGE_SIZE,
		      PROT_READ|PROT_WRITE);
    if (r < 0)
      perror("mprotect");
#if 0
    fprintf(stderr, "mprotect(%p, %d, %x) for %p => %d\n",
	    (void *) (fault_addr & ~(OVM_PAGE_SIZE-1)), OVM_PAGE_SIZE,
	    PROT_READ|PROT_WRITE, (void *) fault_addr, r);
#endif
  } else {
    previous_handler[sig_no](sig_no, info, _ctx);
  }
}

/*
 * Initialize the write barrier
 * heap_end   the first address for which dirty bits are NOT maintained
 * continued  a bit-map filled from IMAGE_CONTINUED_BLOCKS
 * _dirty     a bit-map set by the barrier
 *
 * This function initializes continued_blocks, and establishes the
 * write barrier.  It is the responsibility of java code to
 * write-protected pages and clear bits within dirty_blocks.  The
 * write barrier simply sets bits.
 *
 * The signal handler will run on the signal stack iff a previous
 * SIGSEGV handler was defined with SA_ONSTACK, and will call through
 * the previously installed handler on faults for addresses outside
 * the heap range.
 */
void mc_barrier_init(size_t heap_end,
		     int *continued,
		     int *_dirty) {
  int i;
  struct sigaction act, old;
  
  // OVM_PAGE_SIZE may be a multiple of the hardware pagesize, but it
  // should always be a power of two.
  assert((OVM_PAGE_SIZE % getpagesize()) == 0);
  dirty_size = (heap_end - HEAP_START);
  dirty = _dirty;

  for (i = 0; i < N_CONTINUED_RANGES; i++) {
    const continued_range_t *p = image_continued_ranges + i;
    if (p->start == 0)
      break;
    int nb = (p->end - p->start)/OVM_PAGE_SIZE;
    int idx = (p->start + OVM_PAGE_SIZE - HEAP_START)/OVM_PAGE_SIZE;
    while (nb--) {
      SET_BIT(continued, idx);
      idx++;
    }
  }

  sigaction(SIGSEGV, 0, &act);
  act.sa_flags = SA_RESTART | SA_SIGINFO;
  sigfillset(&act.sa_mask);
  act.sa_sigaction = segv_handler;
  if (sigaction(SIGSEGV, &act, &old)) {
    perror("install handler");
    exit(-1);
  }
  previous_handler[SIGSEGV] = (old.sa_handler == SIG_DFL
			       ? (handler_t *) abort
			       : (handler_t *) old.sa_handler);

  sigaction(SIGBUS, 0, &act);
  act.sa_flags = SA_RESTART | SA_SIGINFO;
  sigfillset(&act.sa_mask);
  act.sa_sigaction = segv_handler;
  if (sigaction(SIGBUS, &act, &old)) {
    perror("install handler");
    exit(-1);
  }
  previous_handler[SIGBUS] = (old.sa_handler == SIG_DFL
			      ? (handler_t *) abort
			      : (handler_t *) old.sa_handler);
}
