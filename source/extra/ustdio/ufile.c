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



UFILE*
u_fopen(const char    *filename,
    const char    *perm,
    const char    *locale,
    const char    *codepage)
{
  UErrorCode     status = U_ZERO_ERROR;
  UBool     useSysCP = (UBool)(locale == 0 && codepage == 0);
  UFILE     *result = (UFILE*) malloc(sizeof(UFILE));
  if(result == 0)
    return 0;

  result->fFile = fopen(filename, perm);
  if(result->fFile == 0) {
    free(result);
    return 0;
  }

  result->fOwnFile = TRUE;

  /* if locale is 0, use the default */
  if(locale == 0)
    locale = uloc_getDefault();

  result->fBundle = u_loccache_get(locale);
  if(result->fBundle == 0) {
    fclose(result->fFile);
    free(result);
    return 0;
  }

  result->fOwnBundle     = FALSE;
  result->fUCPos     = result->fUCBuffer;
  result->fUCLimit     = result->fUCBuffer;

  /* if the codepage is 0, use the default for the locale */
  if(codepage == 0) {
    codepage = uprv_defaultCodePageForLocale(locale);
  
    /* if the codepage is still 0, the default codepage will be used */
  }
  
  /* if both locale and codepage are 0, use the system default codepage */
  else if(useSysCP)
    codepage = 0;

  result->fConverter = ucnv_open(codepage, &status);
  if(U_FAILURE(status) || result->fConverter == 0) {
    fclose(result->fFile);
    free(result);
    return 0;
  }

  return result;
}

UFILE*
u_finit(FILE        *f,
    const char    *locale,
    const char    *codepage)
{
  UErrorCode     status         = U_ZERO_ERROR;
  UBool     useSysCP     = (UBool)(locale == NULL && codepage == NULL);
  UFILE     *result     = (UFILE*) malloc(sizeof(UFILE));
  if(result == 0)
    return 0;


#ifdef WIN32
  result->fFile = &_iob[_fileno(f)];
#else
  result->fFile = f;
#endif
  result->fOwnFile = FALSE;
  result->fOwnBundle     = FALSE;
  result->fUCPos     = result->fUCBuffer;
  result->fUCLimit     = result->fUCBuffer;
  result->fConverter = NULL;
  result->fBundle = NULL;

  if(hasICUData(codepage)) {
      /* if locale is 0, use the default */
      if(locale == 0)
        locale = uloc_getDefault();

      result->fBundle = u_loccache_get(locale);
      if(result->fBundle == 0) {
        /* DO NOT FCLOSE HERE! */
        free(result);
        return 0;
      }
  } else {
      /* bootstrap mode */
      return result;
  }

  /* if the codepage is 0, use the default for the locale */
  if(codepage == 0) {
    codepage = uprv_defaultCodePageForLocale(locale); 



    /* if the codepage is still 0, the default codepage will be used */
    if(codepage == 0) {
        result->fConverter = ucnv_open(0, &status);
        if(U_FAILURE(status) || result->fConverter == 0) {
        /* DO NOT fclose here!!!!!! */
            free(result);
            return 0;
        }
    }
  } else if (*codepage != '\0') {
      result->fConverter = ucnv_open(codepage, &status);
      if(U_FAILURE(status) || result->fConverter == 0) {
        /* DO NOT fclose here!!!!!! */
        free(result);
        return 0;
      }
  } else if(useSysCP) { /* if both locale and codepage are 0, use the system default codepage */
    codepage = 0;
  }
  return result;
}

void
u_fclose(UFILE *file)
{
  fflush(file->fFile);

  if(file->fOwnFile)
    fclose(file->fFile);

  if(file->fOwnBundle)
    u_locbund_delete(file->fBundle);

  ucnv_close(file->fConverter);

  free(file);
}

FILE*
u_fgetfile(    UFILE         *f)
{
  return f->fFile;
}

const char*
u_fgetlocale(    UFILE        *file)
{
  return file->fBundle->fLocale;
}

int32_t
u_fsetlocale(const char        *locale,
         UFILE        *file)
{
  if(file->fOwnBundle)
    u_locbund_delete(file->fBundle);

  file->fBundle     = u_loccache_get(locale);
  file->fOwnBundle     = FALSE;

  return file->fBundle == 0 ? -1 : 0;
}

const char*
u_fgetcodepage(UFILE        *file)
{
  UErrorCode     status = U_ZERO_ERROR;
  const char     *codepage;

  codepage = ucnv_getName(file->fConverter, &status); 
  if(U_FAILURE(status)) return 0;
  return codepage;
}

int32_t
u_fsetcodepage(    const char    *codepage,
        UFILE        *file)
{
  UErrorCode status = U_ZERO_ERROR;

  /* if the codepage is 0, use the default for the locale */
  if(codepage == 0) {
    codepage = uprv_defaultCodePageForLocale(file->fBundle->fLocale);
  
    /* if the codepage is still 0, fall back on the default codepage */
  }

  ucnv_close(file->fConverter);
  file->fConverter = ucnv_open(codepage, &status);
  if(U_FAILURE(status))
    return -1;
  return 0;
}


UConverter * u_fgetConverter(UFILE *file)
{
  return file->fConverter;
}
