/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998, 1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File ustr.c
*
* Modification History:
*
*   Date        Name        Description
*   05/28/99    stephen     Creation.
*******************************************************************************
*/

#include "ustr.h"
#include "cmemory.h"

/* Protos */
static void ustr_resize(struct UString *s, int32_t len, UErrorCode *status);

/* Macros */
#define ALLOCATION(minSize) (minSize < 0x80 ? 0x80 : (2 * minSize + 0x80) & ~(0x80 - 1))

void
ustr_init(struct UString *s)
{
  s->fChars = 0;
  s->fLength = s->fCapacity = 0;
}

void
ustr_deinit(struct UString *s)
{
  icu_free(s->fChars);
  s->fChars = 0;
  s->fLength = s->fCapacity = 0;
}
		       
void
ustr_cpy(struct UString *dst, 
	 const struct UString *src, 
	 UErrorCode *status)
{
  if(FAILURE(*status) || dst == src) return;
  
  if(dst->fCapacity < src->fLength) {
    ustr_resize(dst, ALLOCATION(src->fLength), status);
    if(FAILURE(*status)) return;
  }

  icu_memcpy(dst->fChars, src->fChars, sizeof(UChar) * src->fLength);
  dst->fLength = src->fLength;
  dst->fChars[dst->fLength] = 0x0000;
}

void
ustr_setlen(struct UString *s, 
	    int32_t len,
	    UErrorCode *status)
{
  if(FAILURE(*status)) return;

  if(s->fCapacity < (len + 1)) {
    ustr_resize(s, ALLOCATION(len), status);
    if(FAILURE(*status)) return;
  }

  s->fLength = len;
  s->fChars[len] = 0x0000;
}

void
ustr_cat(struct UString *dst, 
	 const struct UString *src, 
	 UErrorCode *status)
{
  ustr_ncat(dst, src, src->fLength, status);
}

void
ustr_ncat(struct UString *dst, 
	  const struct UString *src, 
	  int32_t n, 
	  UErrorCode *status)
{
  if(FAILURE(*status) || dst == src) return;
  
  if(dst->fCapacity < (dst->fLength + n)) {
    ustr_resize(dst, ALLOCATION(dst->fLength + n), status);
    if(FAILURE(*status)) return;
  }
  
  icu_memcpy(dst->fChars + dst->fLength, src->fChars, 
	     sizeof(UChar) * n);
  dst->fLength += src->fLength;
  dst->fChars[dst->fLength] = 0x0000;
}

void
ustr_ucat(struct UString *dst, 
	  UChar c, 
	  UErrorCode *status)
{
  if(FAILURE(*status)) return;

  if(dst->fCapacity < (dst->fLength + 1)) {
    ustr_resize(dst, ALLOCATION(dst->fLength + 1), status);
    if(FAILURE(*status)) return;
  }
  
  icu_memcpy(dst->fChars + dst->fLength, &c, 
	     sizeof(UChar) * 1);
  dst->fLength += 1;
  dst->fChars[dst->fLength] = 0x0000;
}

/* Destroys data in the string */
static void
ustr_resize(struct UString *s, 
	    int32_t len, 
	    UErrorCode *status)
{
  if(FAILURE(*status)) return;

  /* +1 for trailing 0x0000 */
  s->fChars = (UChar*) icu_realloc(s->fChars, sizeof(UChar) * (len + 1));
  if(s->fChars == 0) {
    *status = MEMORY_ALLOCATION_ERROR;
    s->fChars = 0;
    s->fLength = s->fCapacity = 0;
    return;
  }

  s->fCapacity = len;
}
