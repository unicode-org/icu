/*
******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File ustdio.c
*
* Modification History:
*
*   Date        Name        Description
*   11/18/98    stephen     Creation.
*   03/12/99    stephen     Modified for new C API.
*   07/19/99    stephen     Fixed read() and gets()
******************************************************************************
*/

#include "unicode/ustdio.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "ufile.h"
#include "ufmt_cmn.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"

#include <string.h>

#define DELIM_CR 0x000D
#define DELIM_LF 0x000A

/* Leave this copyright notice here! */
static const char copyright[] = U_COPYRIGHT_STRING;

#ifdef WIN32
static const UChar DELIMITERS [] = { DELIM_CR, DELIM_LF, 0x0000 };
static const uint32_t DELIMITERS_LEN = 2;
#else
static const UChar DELIMITERS [] = { DELIM_LF, 0x0000 };
static const uint32_t DELIMITERS_LEN = 1;
#endif

#define IS_STRING_DELIMITER(s)    (UBool)(    (s) == DELIM_CR || \
(s) == DELIM_LF    )

#if !UCONFIG_NO_TRANSLITERATION

U_CAPI UTransliterator* U_EXPORT2
u_fsettransliterator(UFILE *file, UFileDirection direction,
                     UTransliterator *adopt, UErrorCode *status)
{
    UTransliterator *old = NULL;

    if(file==NULL || U_FAILURE(*status))
    {
        return adopt;
    }

    if(!file)
    {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return adopt;
    }

    if(direction & U_READ)
    {
        /** TODO: implement */
        *status = U_UNSUPPORTED_ERROR;
        return adopt;
    }

    if(adopt == NULL) /* they are clearing it */
    {
        if(file->fTranslit != NULL)
        {
            /* TODO: Check side */
            old = file->fTranslit->translit;
            uprv_free(file->fTranslit->buffer);
            file->fTranslit->buffer=NULL;
            uprv_free(file->fTranslit);
            file->fTranslit=NULL;
        }
    }
    else
    {
        if(file->fTranslit == NULL)
        {
            file->fTranslit = (UFILETranslitBuffer*) uprv_malloc(sizeof(UFILETranslitBuffer));
            if(!file->fTranslit)
            {
                *status = U_MEMORY_ALLOCATION_ERROR;
                return adopt;
            }
            file->fTranslit->capacity = 0;
            file->fTranslit->length = 0;
            file->fTranslit->pos = 0;
            file->fTranslit->buffer = NULL;
        }
        else
        {
            old = file->fTranslit->translit;
            ufile_flush_translit(file);
        }

        file->fTranslit->translit = adopt;
    }

    return old;
}

static const UChar * u_file_translit(UFILE *f, const UChar *src, int32_t *count, UBool flush)
{
    int32_t newlen;
    int32_t junkCount = 0;
    int32_t textLength;
    int32_t textLimit;
    UTransPosition pos;
    UErrorCode status = U_ZERO_ERROR;

    if(count == NULL)
    {
        count = &junkCount;
    }

    if ((!f)||(!f->fTranslit)||(!f->fTranslit->translit))
    {
        /* fast path */
        return src;
    }

    /* First: slide over everything */
    if(f->fTranslit->length > f->fTranslit->pos)
    {
        memmove(f->fTranslit->buffer, f->fTranslit->buffer + f->fTranslit->pos,
            (f->fTranslit->length - f->fTranslit->pos)*sizeof(UChar));
    }
    f->fTranslit->length -= f->fTranslit->pos; /* always */
    f->fTranslit->pos = 0;

    /* Calculate new buffer size needed */
    newlen = (*count + f->fTranslit->length) * 4;

    if(newlen > f->fTranslit->capacity)
    {
        if(f->fTranslit->buffer == NULL)
        {
            f->fTranslit->buffer = (UChar*)uprv_malloc(newlen * sizeof(UChar));
        }
        else
        {
            f->fTranslit->buffer = (UChar*)uprv_realloc(f->fTranslit->buffer, newlen * sizeof(UChar));
        }
        f->fTranslit->capacity = newlen;
    }

    /* Now, copy any data over */
    u_strncpy(f->fTranslit->buffer + f->fTranslit->length,
        src,
        *count);
    f->fTranslit->length += *count;

    /* Now, translit in place as much as we can  */
    if(flush == FALSE)
    {
        textLength = f->fTranslit->length;
        pos.contextStart = 0;
        pos.contextLimit = textLength;
        pos.start        = 0;
        pos.limit        = textLength;

        utrans_transIncrementalUChars(f->fTranslit->translit,
            f->fTranslit->buffer, /* because we shifted */
            &textLength,
            f->fTranslit->capacity,
            &pos,
            &status);

#ifdef _DEBUG
        if(U_FAILURE(status))
        {
            fprintf(stderr, " Gack. Translit blew up with a %s\n", u_errorName(status));
            return src;
        }
#endif

        /* now: start/limit point to the transliterated text */
        /* Transliterated is [buffer..pos.start) */
        *count            = pos.start;
        f->fTranslit->pos = pos.start;
        f->fTranslit->length = pos.limit;

        return f->fTranslit->buffer;
    }
    else
    {
        textLength = f->fTranslit->length;
        textLimit = f->fTranslit->length;

        utrans_transUChars(f->fTranslit->translit,
            f->fTranslit->buffer,
            &textLength,
            f->fTranslit->capacity,
            0,
            &textLimit,
            &status);

#ifdef _DEBUG
        if(U_FAILURE(status))
        {
            fprintf(stderr, " Gack. Translit(flush) blew up with a %s\n", u_errorName(status));
            return src;
        }
#endif

        /* out: converted len */
        *count = textLimit;

        /* Set pointers to 0 */
        f->fTranslit->pos = 0;
        f->fTranslit->length = 0;

        return f->fTranslit->buffer;
    }
}

#endif

void
ufile_flush_translit(UFILE *f)
{
#if !UCONFIG_NO_TRANSLITERATION
    if((!f)||(!f->fTranslit))
        return;
#endif

    u_file_write_flush(NULL, 0, f, TRUE);
}


void
ufile_close_translit(UFILE *f)
{
#if !UCONFIG_NO_TRANSLITERATION
    if((!f)||(!f->fTranslit))
        return;
#endif

    ufile_flush_translit(f);

#if !UCONFIG_NO_TRANSLITERATION
    if(f->fTranslit->translit)
        utrans_close(f->fTranslit->translit);

    if(f->fTranslit->buffer)
    {
        uprv_free(f->fTranslit->buffer);
    }

    uprv_free(f->fTranslit);
    f->fTranslit = NULL;
#endif
}


/* Input/output */

U_CAPI int32_t U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fputs(const UChar    *s,
        UFILE        *f)
{
    int32_t count = u_file_write(s, u_strlen(s), f);
    count += u_file_write(DELIMITERS, DELIMITERS_LEN, f);
    return count;
}

U_CAPI int32_t U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fputc(UChar        uc,
        UFILE        *f)
{
    return u_file_write(&uc, 1, f) == 1 ? uc : EOF;
}


U_CAPI int32_t U_EXPORT2
u_file_write_flush(    const UChar     *chars,
                   int32_t        count,
                   UFILE         *f,
                   UBool         flush)
{
    /* Set up conversion parameters */
    UErrorCode         status        = U_ZERO_ERROR;
    const UChar        *mySource       = chars;
    const UChar        *sourceAlias       = chars;
    const UChar        *mySourceEnd     = chars + count;
    char            *myTarget     = f->fCharBuffer;
    int32_t        bufferSize    = UFILE_CHARBUFFER_SIZE;
    int32_t        written        = 0;
    int32_t        numConverted   = 0;

#if !UCONFIG_NO_TRANSLITERATION
    if((f->fTranslit) && (f->fTranslit->translit))
    {
        /* Do the transliteration */
        mySource = u_file_translit(f, chars, &count, flush);
        sourceAlias = mySource;
        mySourceEnd = mySource + count;
    }
#endif

    /* Perform the conversion in a loop */
    do {
        status     = U_ZERO_ERROR;
        sourceAlias = mySource;
        if(f->fConverter != NULL) { /* We have a valid converter */
            ucnv_fromUnicode(f->fConverter,
                &myTarget,
                f->fCharBuffer + bufferSize,
                &mySource,
                mySourceEnd,
                NULL,
                flush,
                &status);
        } else { /*weiv: do the invariant conversion */
            u_UCharsToChars(mySource, myTarget, count);
            myTarget += count;
        }
        numConverted = (int32_t)(myTarget - f->fCharBuffer);

        if (numConverted > 0) {
            /* write the converted bytes */
            fwrite(f->fCharBuffer,
                sizeof(char),
                numConverted,
                f->fFile);

            written     += numConverted;
        }
        myTarget     = f->fCharBuffer;
    }
    while(status == U_BUFFER_OVERFLOW_ERROR);

    /* return # of chars written */
    return written;
}

U_CAPI int32_t U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_file_write(    const UChar     *chars,
             int32_t        count,
             UFILE         *f)
{
    return u_file_write_flush(chars,count,f,FALSE);
}


/* private function used for buffering input */
void
ufile_fill_uchar_buffer(UFILE *f)
{
    UErrorCode         status;
    const char        *mySource;
    const char        *mySourceEnd;
    UChar            *myTarget;
    int32_t        bufferSize;
    int32_t        maxCPBytes;
    int32_t        bytesRead;
    int32_t        availLength;
    int32_t        dataSize;


    /* shift the buffer if it isn't empty */
    dataSize = (int32_t)(f->fUCLimit - f->fUCPos);
    if(dataSize != 0) {
        memmove(f->fUCBuffer,
            f->fUCPos,
            dataSize * sizeof(UChar));
    }


    /* record how much buffer space is available */
    availLength = UFILE_UCHARBUFFER_SIZE - dataSize;

    /* Determine the # of codepage bytes needed to fill our UChar buffer */
    /* weiv: if converter is NULL, we use invariant converter with charwidth = 1)*/
    maxCPBytes = availLength / (f->fConverter!=NULL?(2*ucnv_getMinCharSize(f->fConverter)):1);

    /* Read in the data to convert */
    bytesRead = (int32_t)fread(f->fCharBuffer,
        sizeof(char),
        ufmt_min(maxCPBytes, UFILE_CHARBUFFER_SIZE),
        f->fFile);

    /* Set up conversion parameters */
    status      = U_ZERO_ERROR;
    mySource    = f->fCharBuffer;
    mySourceEnd = f->fCharBuffer + bytesRead;
    myTarget    = f->fUCBuffer + dataSize;
    bufferSize  = UFILE_UCHARBUFFER_SIZE;

    if(f->fConverter != NULL) { /* We have a valid converter */
        /* Perform the conversion */
        ucnv_toUnicode(f->fConverter,
            &myTarget,
            f->fUCBuffer + bufferSize,
            &mySource,
            mySourceEnd,
            NULL,
            (UBool)(feof(f->fFile) != 0),
            &status);

    } else { /*weiv: do the invariant conversion */
        u_charsToUChars(mySource, myTarget, bytesRead);
        myTarget += bytesRead;
    }

    /* update the pointers into our array */
    f->fUCPos    = f->fUCBuffer;
    f->fUCLimit     = myTarget;
}

U_CAPI UChar* U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fgets(UFILE        *f,
        int32_t        n,
        UChar        *s)
{
    int32_t dataSize;
    int32_t count;
    UChar *alias;
    UChar *limit;
    UChar *sItr;

    if (n <= 0) {
        /* Caller screwed up. We need to write the null terminatior. */
        return NULL;
    }

    /* fill the buffer if needed */
    if (f->fUCPos >= f->fUCLimit) {
        ufile_fill_uchar_buffer(f);
    }

    /* subtract 1 from n to compensate for the terminator */
    --n;

    /* determine the amount of data in the buffer */
    dataSize = (int32_t)(f->fUCLimit - f->fUCPos);

    /* if 0 characters were left, return 0 */
    if (dataSize == 0)
        return NULL;

    /* otherwise, iteratively fill the buffer and copy */
    count = 0;
    sItr = s;
    while (dataSize > 0 && count < n) {
        alias = f->fUCPos;

        /* Find how much to copy */
        if (dataSize < n) {
            limit = f->fUCLimit;
        }
        else {
            limit = alias + n;
        }

        /* Copy UChars until we find the first occurrence of a delimiter character */
        while (alias < limit && !IS_STRING_DELIMITER(*alias)) {
            count++;
            *(sItr++) = *(alias++);
        }
        /* Preserve the newline */
        if (alias < limit && IS_STRING_DELIMITER(*alias)) {
            count++;
            *(sItr++) = *(alias++);
        }

        /* update the current buffer position */
        f->fUCPos = alias;

        /* if we found a delimiter */
        if (alias < f->fUCLimit) {

            /* break out */
            break;
        }

        /* refill the buffer */
        ufile_fill_uchar_buffer(f);

        /* determine the amount of data in the buffer */
        dataSize = (int32_t)(f->fUCLimit - f->fUCPos);
    }

    /* add the terminator and return s */
    *sItr = 0x0000;
    return s;
}

U_CAPI UChar U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fgetc(UFILE        *f)
{
    /* if we have an available character in the buffer, return it */
    if(f->fUCPos < f->fUCLimit)
        return *(f->fUCPos)++;
    /* otherwise, fill the buffer and return the next character */
    else {
        ufile_fill_uchar_buffer(f);
        if(f->fUCPos < f->fUCLimit)
            return *(f->fUCPos)++;
        else
            return 0xFFFF;
    }
}

/* u_unescapeAt() callback to return a UChar from a UFILE */
static UChar U_CALLCONV
_charAt(int32_t offset, void *context) {
    return ((UFILE*) context)->fUCPos[offset];
}

/* Read a UChar from a UFILE and process escape sequences */
U_CAPI UChar32 U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fgetcx(UFILE        *f)
{
    int32_t length;
    int32_t offset;
    UChar32 c32;
    UChar c16;

    /* Fill the buffer if it is empty */
    if (f->fUCPos >= f->fUCLimit) {
        ufile_fill_uchar_buffer(f);
    }

    /* Get the next character in the buffer */
    if (f->fUCPos < f->fUCLimit) {
        c16 = *(f->fUCPos)++;
    } else {
        c16 = U_EOF;
    }

    /* If it isn't a backslash, return it */
    if (c16 != 0x005C /*'\\'*/) {
        return c16;
    }

    /* Determine the amount of data in the buffer */
    length = (int32_t)(f->fUCLimit - f->fUCPos);

    /* The longest escape sequence is \Uhhhhhhhh; make sure
    we have at least that many characters */
    if (length < 10) {
        /* fill the buffer */
        ufile_fill_uchar_buffer(f);
        length = (int32_t)(f->fUCLimit - f->fUCPos);
    }

    /* Process the escape */
    offset = 0;
    c32 = u_unescapeAt(_charAt, &offset, length, (void*)f);

    /* Update the current buffer position */
    f->fUCPos += offset;

    return c32;
}

U_CAPI UChar U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fungetc(UChar        c,
    UFILE        *f)
{
    /* if we're at the beginning of the buffer, sorry! */
    if(f->fUCPos == f->fUCBuffer)
        return 0xFFFF;
    /* otherwise, put the character back */
    else {
        *--(f->fUCPos) = c;
        return c;
    }
}

U_CAPI int32_t U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_file_read(    UChar        *chars,
    int32_t        count,
    UFILE         *f)
{
    int32_t dataSize;
    int32_t read;

    /* fill the buffer */
    ufile_fill_uchar_buffer(f);

    /* determine the amount of data in the buffer */
    dataSize = (int32_t)(f->fUCLimit - f->fUCPos);

    /* if the buffer contains the amount requested, just copy */
    if(dataSize > count) {
        memcpy(chars, f->fUCPos, count * sizeof(UChar));

        /* update the current buffer position */
        f->fUCPos += count;

        /* return # of chars read */
        return count;
    }

    /* otherwise, iteratively fill the buffer and copy */
    read = 0;
    do {

        /* determine the amount of data in the buffer */
        dataSize = (int32_t)(f->fUCLimit - f->fUCPos);

        /* copy the current data in the buffer */
        memcpy(chars + read, f->fUCPos, dataSize * sizeof(UChar));

        /* update number of items read */
        read += dataSize;

        /* update the current buffer position */
        f->fUCPos += dataSize;

        /* refill the buffer */
        ufile_fill_uchar_buffer(f);

    } while(dataSize != 0 && read < count);

    return read;
}
