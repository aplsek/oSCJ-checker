#include <stdio.h>
#include <stdint.h>

#include "autodefs.h"

int main(int argc, char **argv) {
    int i,j; 
    unsigned char buffer[4];
    
    unsigned size=0;
    unsigned pad_amount;

    if (argc!=2 ||
	sscanf(argv[1],"%u",&pad_amount)!=1) {
	fprintf(stderr,"Usage: img2asm <pad amount>\n");
	return 1;
    }

    printf(
	"\t .section \".text\" \n"
	"\t .global OVMImage \n"
#if defined(OVM_SPARC)	
	"\t .type OVMImage, #function \n"
#elif defined(OVM_X86)
	"\t .type OVMImage, @function \n"	
#else
  #error "Unsupported processor in img2asm"
#endif
	"OVMImage: \n"
	);
  
    for(;;) {
  
	// read 4 bytes
  
	for(i=0;i<4;i++) {
    
	    int c = getchar();
      
	    if (c == EOF) {
      
		int j;
		for(j=0;j<i;j++) {
		    printf("\t .byte 0x%x\n", buffer[j]);
		}
		goto done;
	    }
	    
	    size++;
	    
	    buffer[i] = c;
	}
   
	printf("\t .long 0x%x\n", 
#if defined(OVM_SPARC)	
/* any big endian CPU here */
	       (buffer[0]<<24) + (buffer[1]<<16) + (buffer[2]<<8) + (buffer[3]) );    
#elif defined(OVM_X86)
/* any little endian CPU here */
               (buffer[3]<<24) + (buffer[2]<<16) + (buffer[1]<<8) + (buffer[0]) );    
#else
  #error "Unsupported processor in img2asm"
#endif	       
    }
done:
    
    if (size<pad_amount) {
	fprintf(stderr,"padding by %u bytes\n",pad_amount-size);
	printf("\t .skip %u\n",pad_amount-size);
    } else {
        if (size>pad_amount) {
          fprintf(stderr,"!!! FATAL ERROR !!!\n");
          fprintf(stderr,"!!! The image is too large. Please update -pad-image-size.\n");
          fprintf(stderr,"!!! The image size is %u, the limit is %u\n",
            size, pad_amount);                    
          fprintf(stderr,"!!! FATAL ERROR !!!\n");          
         
          return 1; 
        }
    }
    
    return 0;
}
