#include "atomicops.h"
#include <stdio.h>



jint target = 50;
jlong target_l = 50;

int main(int argc, char* argv[]) {

    if (CAS32(&target, 50, 25)) {
        printf("Success: CAS32 succeeded target = %d (25)\n", target);
        if (CAS32(&target, -1, -50)) {
            printf("Fail: CAS32 succeeded - target = %d\n", target);
        }
        else {
            printf("Success: CAS32 failed target = %d (25))\n", target);
        }
    }
    else {
        printf("Fail: CAS32 failed - target = %d\n", target);
    }

    if (CAS64(&target_l, 50, 25)) {
        printf("Success: CAS64 succeeded target_l = %d (25)\n", target_l);
        if (CAS64(&target_l, -1, -50)) {
            printf("Fail: CAS64 succeeded - target_l = %d\n", target_l);
        }
        else {
            printf("Success: CAS64 failed target_l = %d (25))\n", target_l);
        }
    }
    else {
        printf("Fail: CAS64 failed - target_l = %d\n", target_l);
    }
}

