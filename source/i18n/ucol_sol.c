/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucol_sol.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created 06/27/2001
*   created by: Vladimir Weinstein
*
* Just trying to help Paul Grinberg compile on Solaris 8 using Workshop 6 compiler
* 
*/

#include "ucol_sol.h"
#include "ucol_tok.h"
#include "cmemory.h"

const UChar *rulesToParse = 0;

int32_t
uhash_hashTokens(const UHashKey k) {
  int32_t hash = 0;
  uint32_t key = (uint32_t)k.integer;
  if (key != 0) {
      int32_t len = (key & 0xFF000000)>>24;
      int32_t inc = ((len - 32) / 32) + 1;

      const UChar *p = (key & 0x00FFFFFF) + rulesToParse;
      const UChar *limit = p + len;    

      while (p<limit) {
          hash = (hash * 37) + *p;
          p += inc;
      }
  }
  return hash;
}

UBool uhash_compareTokens(const UHashKey key1, const UHashKey key2) {
    uint32_t p1 = (uint32_t) key1.integer;
    uint32_t p2 = (uint32_t) key2.integer;
    const UChar *s1 = (p1 & 0x00FFFFFF) + rulesToParse;
    const UChar *s2 = (p2 & 0x00FFFFFF) + rulesToParse;
    uint32_t s1L = ((p1 & 0xFF000000) >> 24);
    uint32_t s2L = ((p2 & 0xFF000000) >> 24);
    const UChar *end = s1+s1L-1;

    if (p1 == p2) {
        return TRUE;
    }
    if (p1 == 0 || p2 == 0) {
        return FALSE;
    }
    if(s1L != s2L) {
      return FALSE;
    }
    if(p1 == p2) {
      return TRUE;
    }
    while((s1 < end) && *s1 == *s2) {
      ++s1;
      ++s2;
    }
    if(*s1 == *s2) {
      return TRUE;
    } else {
      return FALSE;
    }
}

void deleteToken(void *token) {
    UColToken *tok = (UColToken *)token;
    uprv_free(tok);
}
