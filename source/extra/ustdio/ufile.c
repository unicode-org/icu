/*
******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File ufile.c
*
* Modification History:
*
*   Date        Name        Description
*   11/19/98    stephen     Creation.
*   03/12/99    stephen     Modified for new C API.
*   06/16/99    stephen     Changed T_LocaleBundle to u_locbund
*   07/19/99    stephen     Fixed to use ucnv's default codepage.
******************************************************************************
*/

#include "locmap.h"
#include "unicode/ustdio.h"
#include "ufile.h"
#include "unicode/uloc.h"
#include "loccache.h"
#include "unicode/ures.h"
#include "unicode/ucnv.h"
#include "cstring.h"
#include "cmemory.h"


static UBool hasICUData(const char *cp) {
    UErrorCode status = U_ZERO_ERROR;
    UConverter *cnv = NULL;
#if 0
    UResourceBundle *r = NULL;

    r = ures_open(NULL, NULL, &status);
    if(U_FAILURE(status)) {
        return FALSE;
    } else {
        ures_close(r);
    }
#endif
    cnv = ucnv_open(cp, &status);
    if(cnv == NULL) {
        return FALSE;
    } else {
        ucnv_close(cnv);
    }

    return TRUE;
}



U_CAPI UFILE* U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fopen(const char    *filename,
        const char    *perm,
        const char    *locale,
        const char    *codepage)
{
    UFILE     *result;
    FILE     *systemFile = fopen(filename, perm);
    if(systemFile == 0) {
        return 0;
    }

    result = u_finit(systemFile, locale, codepage);

    if (result) {
        result->fOwnFile = TRUE;
    }

    return result;
}

U_CAPI UFILE* U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_finit(FILE        *f,
        const char    *locale,
        const char    *codepage)
{
    UErrorCode status     = U_ZERO_ERROR;
    UBool     useSysCP    = (UBool)(locale == NULL && codepage == NULL);
    UFILE     *result     = (UFILE*) uprv_malloc(sizeof(UFILE));
    if(result == NULL || f == NULL) {
        return 0;
    }

    uprv_memset(result, 0, sizeof(UFILE));

#ifdef WIN32
    result->fFile = &_iob[_fileno(f)];
#else
    result->fFile = f;
#endif
    result->fUCPos     = result->fUCBuffer;
    result->fUCLimit     = result->fUCBuffer;

    if(hasICUData(codepage)) {
#if !UCONFIG_NO_FORMATTING
        /* if locale is 0, use the default */
        if(locale == 0) {
            locale = uloc_getDefault();
        }

        result->fBundle = u_loccache_get(locale);
        if(result->fBundle == 0) {
            /* DO NOT FCLOSE HERE! */
            uprv_free(result);
            return 0;
        }
#endif
    } else {
        /* bootstrap mode */
        return result;
    }

    /* if the codepage is NULL, use the default for the locale */
    if(codepage == NULL) {
        if(!useSysCP) { /* if both locale and codepage are NULL, use the system default codepage */
            codepage = uprv_defaultCodePageForLocale(locale);
        }

        /* if the codepage is still NULL, the default codepage will be used */
        result->fConverter = ucnv_open(codepage, &status);
        if(U_FAILURE(status) || result->fConverter == NULL) {
            /* DO NOT fclose here!!!!!! */
            uprv_free(result);
            return 0;
        }
    } else if (*codepage != '\0') {
        result->fConverter = ucnv_open(codepage, &status);
        if(U_FAILURE(status) || result->fConverter == NULL) {
            /* DO NOT fclose here!!!!!! */
            uprv_free(result);
            return 0;
        }
    }
    return result;
}


U_CAPI void U_EXPORT2
u_fflush(UFILE *file)
{
  ufile_flush_translit(file);
  fflush(file->fFile);
  /* TODO: flush input */
}

U_CAPI void U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fclose(UFILE *file)
{
    u_fflush(file);
    ufile_close_translit(file);

    if(file->fOwnFile)
        fclose(file->fFile);

#if !UCONFIG_NO_FORMATTING
    if(file->fOwnBundle)
        u_locbund_delete(file->fBundle);
#endif

    ucnv_close(file->fConverter);
    uprv_free(file);
}

U_CAPI FILE* U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fgetfile(    UFILE         *f)
{
    return f->fFile;
}

#if !UCONFIG_NO_FORMATTING

U_CAPI const char*  U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fgetlocale(    UFILE        *file)
{
    return file->fBundle->fLocale;
}

U_CAPI int32_t U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fsetlocale(const char        *locale,
             UFILE        *file)
{
    if(file->fOwnBundle)
        u_locbund_delete(file->fBundle);

    file->fBundle     = u_loccache_get(locale);
    file->fOwnBundle     = FALSE;

    return file->fBundle == 0 ? -1 : 0;
}

#endif

U_CAPI const char* U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fgetcodepage(UFILE        *file)
{
    UErrorCode     status = U_ZERO_ERROR;
    const char     *codepage = NULL;

    if (file->fConverter) {
        codepage = ucnv_getName(file->fConverter, &status);
        if(U_FAILURE(status))
            return 0;
    }
    return codepage;
}

U_CAPI int32_t U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fsetcodepage(    const char    *codepage,
               UFILE        *file)
{
    UErrorCode status = U_ZERO_ERROR;

#if !UCONFIG_NO_FORMATTING
    /* if the codepage is 0, use the default for the locale */
    if(codepage == 0) {
        codepage = uprv_defaultCodePageForLocale(file->fBundle->fLocale);

        /* if the codepage is still 0, fall back on the default codepage */
    }
#endif

    ucnv_close(file->fConverter);
    file->fConverter = ucnv_open(codepage, &status);
    if(U_FAILURE(status))
        return -1;
    return 0;
}


U_CAPI UConverter * U_EXPORT2 /* U_CAPI ... U_EXPORT2 added by Peter Kirk 17 Nov 2001 */
u_fgetConverter(UFILE *file)
{
    return file->fConverter;
}
