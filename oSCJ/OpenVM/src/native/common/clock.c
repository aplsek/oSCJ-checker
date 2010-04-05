#include "config.h"
#include "jtypes.h"
#include "clock.h"
#include <stdio.h>
#include <string.h>
#include <assert.h>

jlong getClockTickCount() {
#if defined(OVM_PPC) || defined(OVM_ARM) || defined(OVM_SPARC)

    static jlong counter = 0;
    if (counter == 0) /* only print warning once */
        fprintf(stderr, "WARNING: getClockTickCount not supported - using fake monotonic counter\n"); 
    return counter++; /* montonically increasing value */

#elif defined(OVM_X86)

    unsigned long long int rdtsc = 0;  
    __asm__ volatile (".byte 0x0f, 0x31" : "=A" (rdtsc));
    /* returning a jlong which is signed should not be a problem provided
       the tsc is always set to zero on system boot. In that case on a
       1GHz machine we have 292 years to worry about the overflow.
    */
    return (jlong)rdtsc;

#else
# error "Unsupported architecture"
#endif
}

jfloat getClockFrequency() {
#if !defined(HAS_PROCCPUINFO)

    /* there looks like there is something similar to /proc/cpuinfo for
       darwin but we don't have the details yet
    */
     fprintf(stderr, "WARNING: getClockFrequency not supported on this platform\n");    
     return 1.0;  /* returning 1 is safer than 0 */

#elif defined(OVM_X86)

    const int buf_size = 8192;
    const int freqstring_size = 32;  /* should be ample - and is checked */

    char *start, *end;  /* pointers to the line we want */
    char buffer[buf_size];
    char freqstring[freqstring_size];
    char* label = "cpu MHz\t\t: ";
    jfloat freq;
    int needBiggerBuffer = 0, needBiggerArray = 0;
    FILE* cpuinfo = fopen("/proc/cpuinfo", "r");

    fread(buffer, sizeof(char), buf_size, cpuinfo);
    if (fread(buffer, sizeof(char), buf_size, cpuinfo) == buf_size 
        && !feof(cpuinfo) ){
        /* we haven't read the whole cpuinfo but we may have read
           what we need.
        */
        needBiggerBuffer = 1;
    }
    fclose(cpuinfo);

    start = strstr(buffer, label);
    if (start != NULL) {
        end = strchr(start, '\n');
        if (end != NULL) {
            int length = 0;
            start = start + strlen(label); /* skip over label */
            length = end - start;
            assert(length > 0);
            if (length >= freqstring_size) {
                needBiggerArray = 1;
            }
            else {
                strncpy(freqstring, start, length);
                freqstring[length+1] = 0;
                /* Note: the value read is not exact. When we turn it into 
                   a float it may not be exactly representable so it's value 
                   will change again. Using this value to convert ticks 
                   into seconds is a bad idea. -DH
                */
                if (sscanf(freqstring, "%f", &freq) == 1) {
                    /* check if there is another CPU listed */
                    if (strstr(start, label) != NULL) {
                        fprintf(stderr,
                                "# WARNING: more than one CPU found, but only the first frequency is reported\n");
                    }
                    return freq;
                }
            }
        }
    }
    /* if we get here something went wrong */
    fprintf(stderr, "ERROR: reading cpu clock frequency");
    if (needBiggerBuffer) {
        fprintf(stderr," - unable to read all of /proc/cpuinfo: increase buffer size\n");
    }
    else if (needBiggerArray) {
        fprintf(stderr, " - temporary string array too small\n");
    }
    else {
        fputc('\n',stderr);
    }
    return 1.0;
    
#endif
}


