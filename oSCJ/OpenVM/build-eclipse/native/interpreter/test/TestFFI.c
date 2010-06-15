#include "point.h"
#include <math.h>
#include "framework.h" /* unless there's a good reason not to */

int OVM_ovm_test_TestFFI (struct Point *p, struct Point *q) {

  int xsqr = p->x * q->x;
  int ysqr = p->y * q->y;
  return sqrt (xsqr + ysqr);
}
