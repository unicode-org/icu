// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2009-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#ifndef _ICUGLUE_H
#define _ICUGLUE_H

/* Get utypes.h from whatever ICU we are under */
#include <unicode/utypes.h>

#define GLUE_SYM_V(x, v) glue ## x ## v

/**
 * how to parse a version string.
 * old:  3_6_X, 3_8_X, 4_4_X, 4_8_X
 * new:  49_X, 50_X, 51_X, ...
 *
 * example use:
 *  char *str = "50_0_0", str1="49_1_2",str2="4_8_1_1";
 *  if(IS_OLD_VERSION(str)) {
 *    maj = str[OLD_VERSTR_MAJ];
 *    min = str[OLD_VERSTR_MIN];
 *  } else {
 *    maj = str[NEW_VERSTR_MAJ];
 *    min = str[NEW_VERSTR_MIN];
 *  }
 */
#define IS_OLD_VERSTR(x) ((x[0]<'4') || ((x[0]=='4') && (x[2]<'9') && (x[2]>='0')))
#define OLD_VERSTR_MAJ 0 
#define OLD_VERSTR_MIN 2
#define NEW_VERSTR_MAJ 0
#define NEW_VERSTR_MIN 1

/**
 * copy version into dst[0] and dst[1]
 * does not modify dst ptr
 */
#define CPY_VERSTR(dst,ver) if(IS_OLD_VERSTR(ver)) \
    { \
      (dst)[0]=ver[OLD_VERSTR_MAJ];              \
      (dst)[1]=ver[OLD_VERSTR_MIN];              \
    } else {                      \
      (dst)[0]=ver[NEW_VERSTR_MAJ];       \
      (dst)[1]=ver[NEW_VERSTR_MIN];             \
    }

/**
 * compare a verstr to a string
 * @param str a 2 char string such as "50", "44", "49"
 * @param ver a verstr such as "50_0_2", "4_8_1_1", etc
 * @return true or false
 */

#define CMP_VERSTR(str,ver) \
  ( (IS_OLD_VERSTR(ver)) ? \
     ( \
      (str)[0]==ver[OLD_VERSTR_MAJ] &&              \
      (str)[1]==ver[OLD_VERSTR_MIN] \
     ):(                                              \
      (str)[0]==ver[NEW_VERSTR_MAJ]&&       \
      (str)[1]==ver[NEW_VERSTR_MIN]             \
     ) \
   )

#endif
