/*
 * $Header: /p/sss/cvs/OpenVM/src/native/common/atomicops.c,v 1.8 2004/05/18 21:22:07 baker29 Exp $
 */
#include "atomicops.h"
/**
 * A suite of atomic operations for use by either C code, or OVM code.
 *
 * The basic operations are defined in terms of CAS32. This has the advantage
 * that you can always return the current value if desired - in contrast 
 * machine specific "optimisations" typically don't allow this.
 *
 * It might seem like overkill to use CAS for atomic_get and atomic_set but
 * this ensures consistent memory synchronization semantics on all operations.
 * It may be that explicit memory barriers are also needed on some 
 * architectures. For X86 the JSR-166 effort reached the consensus that 
 * additional memory barriers were not needed.
 *
 * This method set is based on the JSR-166 API.
 * 
 * Because there is no CAS64 on PPC we do not currently support operations on
 * Java long values. We do however name the method _int in readyiness for 
 * _long versions.
 *
 * @author David Holmes
 */

/**
 * Atomically retrieve the current value of the target
 */
jint atomic_get_int(volatile jint* target) {
    while(1) {
        jint oldVal = *target;
        jint newVal = oldVal;
        if (CAS32(target, oldVal, newVal))
            return oldVal;
    }
}


/**
 * Atomically set the target to newVal
 */
void atomic_set_int(volatile jint* target, jint newVal) {
    while(1) {
        jint oldVal = *target;
        if (CAS32(target, oldVal, newVal))
            return;
    }
}

/**
 * Atomically retrieve the current value of the target and set it to newVal
 */
jint atomic_get_and_set_int(volatile jint* target, jint newVal) {
    while(1) {
        jint oldVal = *target;
        if (CAS32(target, oldVal, newVal))
            return oldVal;
    }
}

/**
 * Atomically increment by one, the target, and return the previous value.
 */
jint atomic_get_and_inc_int(volatile jint* target) {
    while(1) {
        jint oldVal = *target;
        jint newVal = oldVal + 1;
        if (CAS32(target, oldVal, newVal))
            return oldVal;
    }
}


/**
 * Atomically decrement by one, the target, and return the previous value.
 */
jint atomic_get_and_dec_int(volatile jint* target) {
    while(1) {
        jint oldVal = *target;
        jint newVal = oldVal - 1;
        if (CAS32(target, oldVal, newVal))
            return oldVal;
    }
}

/**
 * Atomically add the given val to the target, and return the previous value
 */
jint atomic_get_and_add_int(volatile jint* target, jint val) {
    while(1) {
        jint oldVal = *target;
        jint newVal = oldVal + val;
        if (CAS32(target, oldVal, newVal))
            return oldVal;
    }
}


/**
 * Atomically increment by one, the target, and return the new value.
 */
jint atomic_inc_and_get_int(volatile jint* target) {
    while(1) {
        jint oldVal = *target;
        jint newVal = oldVal + 1;
        if (CAS32(target, oldVal, newVal))
            return newVal;
    }
}


/**
 * Atomically decrement by one, the target, and return the new value.
 */
jint atomic_dec_and_get_int(volatile jint* target) {
    while(1) {
        jint oldVal = *target;
        jint newVal = oldVal - 1;
        if (CAS32(target, oldVal, newVal))
            return newVal;
    }
}

/**
 * Atomically add the given val to the target, and return the new value
 */
jint atomic_add_and_get_int(volatile jint* target, jint val) {
    while(1) {
        jint oldVal = *target;
        jint newVal = oldVal + val;
        if (CAS32(target, oldVal, newVal))
            return newVal;
    }
}




