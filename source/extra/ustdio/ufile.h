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
#include "unicode/utrans.h"
#include "locbund.h"

/* The buffer size for fromUnicode calls */
#define UFILE_CHARBUFFER_SIZE 1024

/* The buffer size for toUnicode calls */
#define UFILE_UCHARBUFFER_SIZE 1024

/* A UFILE */

#if !UCONFIG_NO_TRANSLITERATION

typedef struct {
  UChar  *buffer;             /* Beginning of buffer */
  int32_t capacity;           /* Capacity of buffer */
  int32_t pos;                /* Beginning of untranslitted data */
  int32_t length;             /* Length *from beginning of buffer* of untranslitted data */
  UTransliterator *translit;
} UFILETranslitBuffer;

#endif

struct UFILE {
  FILE            *fFile;        /* the actual fs interface */
  UBool        fOwnFile;    /* TRUE if fFile should be closed */

#if !UCONFIG_NO_FORMATTING
  ULocaleBundle        *fBundle;     /* formatters */
  UBool        fOwnBundle;     /* TRUE if fBundle should be deleted */
#endif

  UConverter        *fConverter;     /* for codeset conversion */

                      /* buffer used for fromUnicode */
  char            fCharBuffer     [UFILE_CHARBUFFER_SIZE];

                      /* buffer used for toUnicode */
  UChar            fUCBuffer     [UFILE_UCHARBUFFER_SIZE];

  UChar            *fUCLimit;     /* data limit in fUCBuffer */
  UChar         *fUCPos;     /* current pos in fUCBuffer */

#if !UCONFIG_NO_TRANSLITERATION
  UFILETranslitBuffer *fTranslit;
#endif
};

/**
 * Like u_file_write but takes a flush parameter
 */
U_CAPI int32_t U_EXPORT2
u_file_write_flush(    const UChar     *chars, 
        int32_t        count, 
        UFILE         *f,
        UBool         flush);

/**
 * Fill a UFILE's buffer with converted codepage data.
 * @param f The UFILE containing the buffer to fill.
 */
void
ufile_fill_uchar_buffer(UFILE *f);

/**
 * Close out the transliterator and flush any data therein.
 * @param f flu
 */
void 
ufile_close_translit(UFILE *f);

/**
 * Flush the buffer in the transliterator 
 * @param f UFile to flush
 */
void 
ufile_flush_translit(UFILE *f);


#endif
