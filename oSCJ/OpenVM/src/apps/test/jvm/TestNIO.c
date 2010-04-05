/* do the same as TestNIO.java, just in C */

#include <sys/time.h>
#include <time.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <errno.h>

#define VERBOSE 0

static void testTCPChain(int mlen,
			 int clen,
			 int mcnt) {
  struct timeval start;
  struct timeval end;
  struct sockaddr_in addr;
  fd_set readSet;
  fd_set errorSet;
  fd_set writeSet;
  int lenOfIncomingAddr;
  int * serverSockets;
  int * clientSockets;
  int * acceptSockets;
  char ** buffers;  
  int i;
  int sendPending;
  int recvPending;
  int ret;
  int max;
  int flags;

  gettimeofday(&start, NULL);
  serverSockets = malloc(sizeof(int) * clen);
  clientSockets = malloc(sizeof(int) * clen);
  acceptSockets = malloc(sizeof(int) * clen);
  buffers       = malloc(sizeof(char*) * (1+clen));
  for (i=0;i<clen;i++) {
    buffers[i] = malloc(mlen+sizeof(int));
    *(int*)&buffers[i][mlen] = mlen; /* mark as unused */
    serverSockets[i] = socket(PF_INET,
			      SOCK_STREAM,
			      0);
    memset(&addr, 0, sizeof(struct sockaddr_in));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_ANY);
    addr.sin_port = htons(10000+i);
    if (0 != bind(serverSockets[i],
		  (struct sockaddr*) &addr,
		  sizeof(struct sockaddr_in))) {
      printf("bind failed: %s\n",
	     strerror(errno));
      abort();
    }
    listen(serverSockets[i], 1);
    clientSockets[i] = socket(PF_INET, SOCK_STREAM, 6);
    flags = fcntl(clientSockets[i], F_GETFL, 0);
    fcntl(clientSockets[i], F_SETFL, flags | O_NONBLOCK);
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    addr.sin_port = htons(10000+i);
    if (-1 == connect(clientSockets[i],
		      (struct sockaddr*) &addr,
		      sizeof(addr)))
      if (errno != EINPROGRESS) {
	printf("connect failed: %s\n",
	       strerror(errno));
	abort();
      }
    lenOfIncomingAddr = sizeof(addr);
    acceptSockets[i] = accept(serverSockets[i],
			      (struct sockaddr*) &addr,
			      &lenOfIncomingAddr);
  }
  buffers[clen] = malloc(mlen+sizeof(int));
  *(int*)&buffers[clen][mlen] = mlen; /* mark as unused */

  for (i=0;i<mlen;i++)
    buffers[0][i] = i;
  *(int*)&buffers[0][mlen] = 0;
  sendPending = mcnt-1;
  recvPending = mcnt;

  while (recvPending > 0) {
    FD_ZERO(&readSet);
    FD_ZERO(&writeSet);
    FD_ZERO(&errorSet);

    max = 0;
    for (i=0;i<clen;i++) {
      if (*(int*)&buffers[i+1][mlen] >= mlen) {
	FD_SET(acceptSockets[i],
	       &readSet);
#if VERBOSE
	printf("Consumer %d ready...\n", i);
#endif
	if (acceptSockets[i] > max)
	  max = acceptSockets[i];
      }
      if (*(int*)&buffers[i][mlen] < mlen) {
	FD_SET(clientSockets[i],
	       &writeSet);     
#if VERBOSE
	printf("Producer %d pending...\n", i);
#endif
	if (clientSockets[i] > max)
	  max = clientSockets[i];	
      }   
    }
    ret = select(max+1,
		 &readSet, 
		 &writeSet,
		 &errorSet,
		 NULL);
    if (ret < 0)
      continue;
    for (i=0;i<clen;i++) {
      if (FD_ISSET(acceptSockets[i], &readSet)) {
	ret = read(acceptSockets[i],
		   &buffers[i+1][*(int*)&buffers[i+1][mlen]-mlen],
		   2 * mlen - *(int*)&buffers[i+1][mlen]);
	*(int*)&buffers[i+1][mlen] += ret;
	if (*(int*)&buffers[i+1][mlen] == 2*mlen) {
	  if (i < clen-1) {
#if VERBOSE
	    printf("Consumer %d switches to writer\n", i);
#endif
	    /* switch to writer */
	    *(int*)&buffers[i+1][mlen] = 0;
	  } else {
	    /* final consumer */
#if VERBOSE
	    printf("Final consumption!\n");
#endif
	    recvPending--;
	    *(int*)&buffers[i+1][mlen] = mlen;
	  }	  
	}
      }
      if (FD_ISSET(clientSockets[i], &writeSet)) {
	ret = write(clientSockets[i],
		    &buffers[i][*(int*)&buffers[i][mlen]],
		    mlen - *(int*)&buffers[i][mlen]);
	*(int*)&buffers[i][mlen] += ret;
#if VERBOSE
	if ( (*(int*)&buffers[0][mlen] == mlen) ) {
	  printf("Producer %d completed write!\n", i);
	}
#endif
	if ( (i == 0) &&
	     (*(int*)&buffers[0][mlen] == mlen) &&
	     (sendPending > 0) ) {
#if VERBSOE
	  printf("Completed initial write!\n");
#endif
	  sendPending--;
	  *(int*)&buffers[0][mlen] = 0;
	}
      }  
    }    
  }
  /* shutdown */
  for (i=0;i<clen;i++) {
    free(buffers[i]);
    close(serverSockets[i]);
    close(clientSockets[i]);
    close(acceptSockets[i]);
  }
  free(buffers[clen]);
  free(buffers);
  free(serverSockets);
  free(clientSockets);
  free(acceptSockets);
  gettimeofday(&end, NULL);
  printf("Took %llu ms.\n",
	 ((unsigned long long) (end.tv_sec - start.tv_sec) * 1000000 +
	  (unsigned long long) (end.tv_usec - start.tv_usec)) / 1000);
    
}


int main(int argc,
	 char * argv[]) {
  int mlen;
  int clen;
  int mcnt;

  if (argc != 4) {
    printf("Call with MLEN CLEN MCNT\n");
    return -1;
  }
  mlen = atoi(argv[1]);
  clen = atoi(argv[2]);
  mcnt = atoi(argv[3]);
  testTCPChain(mlen, clen, mcnt);
}
