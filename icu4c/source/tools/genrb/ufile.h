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
*   12/01/98    stephen	    Creation.
*   03/12/99    stephen     Modified for new C API.
*   05/26/99    stephen     Created in tools/genrb from extra/ustdio.
*                           Modified to use FileStream, removed LocaleBundle.
*******************************************************************************
*/

#ifndef UFILE_H
#define UFILE_H 1

#include "utypes.h"
#include "ucnv.h"
#include "filestrm.h"

/* The buffer size for fromUnicode calls */
#define UFILE_CHARBUFFER_SIZE 1024

/* The buffer size for toUnicode calls */
#define UFILE_UCHARBUFFER_SIZE 1024

/* A UFILE */
struct UFILE {
  FileStream		*fFile;		/* the actual fs interface */
  bool_t		fOwnFile;	/* TRUE if fFile should be closed */

  UConverter		*fConverter; 	/* for codeset conversion */

  					/* buffer used for fromUnicode */
  char			fCharBuffer 	[UFILE_CHARBUFFER_SIZE];

  					/* buffer used for toUnicode */
  UChar			fUCBuffer 	[UFILE_UCHARBUFFER_SIZE];

  UChar			*fUCLimit; 	/* data limit in fUCBuffer */
  UChar 		*fUCPos; 	/* current pos in fUCBuffer */
};
typedef struct UFILE UFILE;

/**
 * Fill a UFILE's buffer with converted codepage data.
 * @param f The UFILE containing the buffer to fill.
 */
void
ufile_fill_uchar_buffer(UFILE *f,
			UErrorCode *status);

#endif
