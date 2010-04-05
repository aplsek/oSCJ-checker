/*
 * fdutils.h -- useful stuff for dealing with file descriptors
 * by Filip Pizlo, 2004
 */

#ifndef FDUTILS_H
#define FDUTILS_H

#ifdef __cplusplus
extern "C" {
#endif

#if 0
} /* fool emacs */
#endif

int get_flags(int fd);
int set_flags(int fd, int flags);

int get_fl(int fd,int flags);
int set_fl(int fd,int flags);
int clr_fl(int fd,int flags);

int makeNonBlocking(int fd);
int makeBlocking(int fd);

/* set blocking enabled */
int SBE(int fd,int enabled);
int fSBE(FILE *file,int enabled);

/* printf after unblocking */
void ubprintf(const char *msg,...);

#ifdef __cplusplus
}
#endif

#endif



