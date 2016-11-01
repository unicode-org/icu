#include <stdio.h>
/* Copyright (c) 2014 IBM Corporation and Others. All Rights Reserved. */
#include <unicode/putil.h>
#include <unicode/uclean.h>

int main() {
  UErrorCode status = U_ZERO_ERROR;
  u_init(&status);
  puts(u_errorName(status));
  u_cleanup();
  return U_FAILURE(status)?1:0;
}
