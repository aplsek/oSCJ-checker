#ifndef _NATIVE_H_
#define _NATIVE_H_

#include "stack.h"

typedef struct NativeMethod_ {
  char *name;
  void *address;

} NativeMethod;

void invokeNative(NativeMethod *m, struct StackFrame *f);

#endif
