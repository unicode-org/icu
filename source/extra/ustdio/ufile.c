/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
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
*******************************************************************************
*/

#include "ustdio.h"
#include "loccache.h"
#include "ufile.h"
#include "uloc.h"

#include <string.h>
#include <stdlib.h>


/* the data in the following two functions should REALLY be somewhere else */
/* convert from a country code (only the first 2 chars are significant)  */
/* to an IBM codepage */

/* thanks to http://czyborra.com/charsets/iso8859.htm for most of this info */
static const char ufile_locale2codepage[][256] = {
  "af", "latin-1",  /* Afrikaans */
  "ar", "ibm-1256", /* arabic */

  "be", "ibm-915"  , /*  Byelorussian */
  "bg", "ibm-915"  , /* Bulgarian */
    
  "ca", "latin-1"  , /* catalan */
  "cs", "ibm-912"  , /* Czech */
  
  "da", "latin-1"  ,  /* danish */
  "de", "latin-1"  ,  /* german */
  
  "el", "ibm-813"  , /* Greek */
  "en", "latin-1"  ,  /* English */
  "eo", "ibm-913"  , /* Esperanto */
  "es", "latin-1"  ,  /* Spanish */
  "eu", "latin-1"  , /* basque */
  "et", "ibm-914"  , /* Estonian  */
  
  "fi", "latin-1"  ,  /* Finnish */
  "fo", "latin-1"  ,  /* faroese */
  "fr", "latin-1"  ,  /* French */
  
  "ga", "latin-1"  ,  /* Irish (Gaelic) */
  "gd", "latin-1"  ,  /* Scottish */
  
  "hr", "ibm-912"  , /* Croatian */
  "hu", "ibm-912"  , /* Hungarian */
  
  "in", "latin-1"  , /* Indonesian */
  "is", "latin-1"  ,  /* Icelandic */
  "it", "latin-1"  ,  /* Italian  */
  "iw", "ibm-916", /* hebrew */
  
  "ja", "ibm-943", /* Japanese */
  "ji", "ibm-916", /* Yiddish */
  
  "kl", "ibm-914", /* Greenlandic */
  "ko", "ibm-949", /* korean  */
  
  "lt", "ibm-914", /* Lithuanian */
  "lv", "ibm-914", /* latvian (lettish) */
  
  "mk", "ibm-915"  , /* Macedonian */
  "mt", "ibm-1208"  , /* Maltese [UTF8] */
  
  "nl", "latin-1"  ,  /* dutch */
  "no", "latin-1"  ,  /* Norwegian */
  
  "pl", "ibm-912"  , /* Polish */
  "pt", "latin-1"  ,  /* Portugese */
  
  "rm", "latin-1"  ,  /* Rhaeto-romanic (??) */
  "ro", "ibm-912"  , /* Romanian */
  "ru", "ibm-878"  , /* Russian */
  
  "sk", "ibm-912"  , /* Slovak */

  "sl", "ibm-912"  , /* Slovenian */
  "sq", "latin-1"  ,  /* albanian */
  "sr", "ibm-915"  , /* Serbian */
  "sv", "latin-1"  ,  /* Swedish */
  "sw", "latin-1"  ,  /* Swahili */
  
  "th", "ibm-1208" , /* Thai - UTF8 */
  
  "tr", "ibm-920",  /* Turkish */
  
  "uk", "ibm-915"  , /* pre 1990 Ukranian (?) */
  
  "zh", "Big-5",  /* Chinese */
  0,    0 
};

static const char* 
ufile_lookup_codepage(const char *locale)
{ 
  int32_t i;
  for(i = 0; ufile_locale2codepage[i][0]; i+= 2)
    if( ! strncmp(ufile_locale2codepage[i], locale, 2))
      return ufile_locale2codepage[i + 1];
  return 0;
}


UFILE*
u_fopen(const char    *filename,
    const char    *perm,
    const char    *locale,
    const char    *codepage)
{
  UErrorCode     status = ZERO_ERROR;
  bool_t     useSysCP = (locale == 0 && codepage == 0);
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
    codepage = ufile_lookup_codepage(locale);
  
    /* if the codepage is still 0, the default codepage will be used */
  }
  
  /* if both locale and codepage are 0, use the system default codepage */
  else if(useSysCP)
    codepage = 0;

  result->fConverter = ucnv_open(codepage, &status);
  if(FAILURE(status) || result->fConverter == 0) {
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
  UErrorCode     status         = ZERO_ERROR;
  bool_t     useSysCP     = (locale == 0 && codepage == 0);
  UFILE     *result     = (UFILE*) malloc(sizeof(UFILE));
  if(result == 0)
    return 0;

  result->fFile = f;
  result->fOwnFile = FALSE;

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
    codepage = ufile_lookup_codepage(locale);
  
    /* if the codepage is still 0, the default codepage will be used */
  }
  
  /* if both locale and codepage are 0, use the system default codepage */
  else if(useSysCP)
    codepage = 0;

  result->fConverter = ucnv_open(codepage, &status);
  if(FAILURE(status) || result->fConverter == 0) {
    fclose(result->fFile);
    free(result);
    return 0;
  }

  return result;
}

void
u_fclose(UFILE *file)
{
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
  UErrorCode     status = ZERO_ERROR;
  const char     *codepage;

  codepage = ucnv_getName(file->fConverter, &status); 
  if(FAILURE(status)) return 0;
  return codepage;
}

int32_t
u_fsetcodepage(    const char    *codepage,
        UFILE        *file)
{
  UErrorCode status = ZERO_ERROR;

  /* if the codepage is 0, use the default for the locale */
  if(codepage == 0) {
    codepage = ufile_lookup_codepage(file->fBundle->fLocale);
  
    /* if the codepage is still 0, fall back on the default codepage */
  }

  ucnv_close(file->fConverter);
  file->fConverter = ucnv_open(codepage, &status);
  if(FAILURE(status))
    return -1;
  return 0;
}
