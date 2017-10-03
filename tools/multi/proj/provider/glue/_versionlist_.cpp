// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#include <icuglue/icuglue.h>
#include <unicode/coll.h>


#if defined ( ICUGLUE_VER )
//#error DEFINED!
#else

#define GLUE_VER(x) puts("Version " # x );

#include <stdio.h>

// generate list of versions
#include <icuglue/fe_verlist.h>

int main(int argc, const char *argv[]) {
printf("I'm on " U_ICU_VERSION " but i have modules for: \n");
  for(int i=0;fe_verlist[i];i++) {
      printf("%d: %s\n", i, fe_verlist[i]);
  }
}

#endif




