/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ufile.h
*
* Modification History:
*
*   Date        Name        Description
*   12/01/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef UFILE_H
#define UFILE_H

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "locbund.h"

/* The buffer size for fromUnicode calls */
#define UFILE_CHARBUFFER_SIZE 1024

/* The buffer size for toUnicode calls */
#define UFILE_UCHARBUFFER_SIZE 1024

/* A UFILE */
struct UFILE {
  FILE            *fFile;        /* the actual fs interface */
  UBool        fOwnFile;    /* TRUE if fFile should be closed */
  
  ULocaleBundle        *fBundle;     /* formatters */
  UBool        fOwnBundle;     /* TRUE if fBundle should be deleted */

  UConverter        *fConverter;     /* for codeset conversion */

                      /* buffer used for fromUnicode */
  char            fCharBuffer     [UFILE_CHARBUFFER_SIZE];

                      /* buffer used for toUnicode */
  UChar            fUCBuffer     [UFILE_UCHARBUFFER_SIZE];

  UChar            *fUCLimit;     /* data limit in fUCBuffer */
  UChar         *fUCPos;     /* current pos in fUCBuffer */
};

/**
 * Fill a UFILE's buffer with converted codepage data.
 * @param f The UFILE containing the buffer to fill.
 */
void
ufile_fill_uchar_buffer(UFILE *f);

#endif
