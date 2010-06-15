/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/include/atomicops.h,v 1.15 2006/04/08 21:08:05 baker29 Exp $
 */
#ifndef ATOMICOPS_H
#define ATOMICOPS_H

#include "config.h"
#include "jtypes.h"
#include "md.h"

/* the other atomic functions */

/**
 * Atomically retrieve the current value of the target
 */
jint atomic_get_int(volatile jint* target);

/**
 * Atomically set the target to newVal
 */
void atomic_set_int(volatile jint* target, jint newVal);

/**
 * Atomically retrieve the current value of the target and set it to newVal
 */
jint atomic_get_and_set_int(volatile jint* target, jint newVal);

/**
 * Atomically increment by one, the target, and return the previous value.
 */
jint atomic_get_and_inc_int(volatile jint* target);


/**
 * Atomically decrement by one, the target, and return the previous value.
 */
jint atomic_get_and_dec_int(volatile jint* target);

/**
 * Atomically add the given val to the target, and return the previous value
 */
jint atomic_get_and_add_int(volatile jint* target, jint val);


/**
 * Atomically increment by one, the target, and return the new value.
 */
jint atomic_inc_and_get_int(volatile jint* target);


/**
 * Atomically decrement by one, the target, and return the new value.
 */
jint atomic_dec_and_get_int(volatile jint* target);

/**
 * Atomically add the given val to the target, and return the new value
 */
jint atomic_add_and_get_int(volatile jint* target, jint val);



#endif /* ATOMICOPS_H */




