/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File locbund.c
*
* Modification History:
*
*   Date        Name        Description
*   11/18/98    stephen        Creation.
*   12/10/1999  bobbyr@optiosoftware.com       Fix for memory leak + string allocation bugs
*******************************************************************************
*/

#include <stdlib.h>

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "locbund.h"

#include "cmemory.h"
#include "unicode/ustring.h"
#include "unicode/uloc.h"

ULocaleBundle*        
u_locbund_new(const char *loc)
{
  ULocaleBundle *result = (ULocaleBundle*) uprv_malloc(sizeof(ULocaleBundle));
  int32_t len;

  if(result == 0)
    return 0;

  len = (int32_t)(loc == 0 ? strlen(uloc_getDefault()) : strlen(loc));
  result->fLocale = (char*) uprv_malloc(len + 1);
  if(result->fLocale == 0) {
    uprv_free(result);
    return 0;
  }
  
  strcpy(result->fLocale, (loc == 0 ? uloc_getDefault() : loc) );
  
  result->fNumberFormat     = 0;
  result->fPercentFormat     = 0;
  result->fCurrencyFormat     = 0;
  result->fScientificFormat     = 0;
  result->fSpelloutFormat     = 0;
  result->fDateFormat         = 0;
  result->fTimeFormat         = 0;

  return result;
}

ULocaleBundle*
u_locbund_clone(const ULocaleBundle *bundle)
{
  ULocaleBundle *result = (ULocaleBundle*)uprv_malloc(sizeof(ULocaleBundle));
  UErrorCode status = U_ZERO_ERROR;

  if(result == 0)
    return 0;
  
  result->fLocale = (char*) uprv_malloc(strlen(bundle->fLocale) + 1);
  if(result->fLocale == 0) {
    uprv_free(result);
    return 0;
  }
  
  strcpy(result->fLocale, bundle->fLocale );
  
  result->fNumberFormat     = (bundle->fNumberFormat == 0 ? 0 :
                   unum_clone(bundle->fNumberFormat, &status));
  result->fPercentFormat     = (bundle->fPercentFormat == 0 ? 0 :
                   unum_clone(bundle->fPercentFormat, 
                          &status));
  result->fCurrencyFormat     = (bundle->fCurrencyFormat == 0 ? 0 :
                   unum_clone(bundle->fCurrencyFormat, 
                          &status));
  result->fScientificFormat     = (bundle->fScientificFormat == 0 ? 0 :
                   unum_clone(bundle->fScientificFormat, 
                          &status));
  result->fSpelloutFormat     = (bundle->fSpelloutFormat == 0 ? 0 :
                   unum_clone(bundle->fSpelloutFormat, 
                          &status));
  result->fDateFormat         = (bundle->fDateFormat == 0 ? 0 :
                   udat_clone(bundle->fDateFormat, &status));
  result->fTimeFormat         = (bundle->fTimeFormat == 0 ? 0 :
                   udat_clone(bundle->fTimeFormat, &status));

  return result;
}

void
u_locbund_delete(ULocaleBundle *bundle)
{
  uprv_free(bundle->fLocale);

  if(bundle->fNumberFormat != 0)
    unum_close(bundle->fNumberFormat);
  if(bundle->fPercentFormat != 0)
    unum_close(bundle->fPercentFormat);
  if(bundle->fCurrencyFormat != 0)
    unum_close(bundle->fCurrencyFormat);
  if(bundle->fScientificFormat != 0)
    unum_close(bundle->fScientificFormat);
  if(bundle->fSpelloutFormat != 0)
    unum_close(bundle->fSpelloutFormat);
  if(bundle->fDateFormat != 0)
    udat_close(bundle->fDateFormat);
  if(bundle->fTimeFormat != 0)
    udat_close(bundle->fTimeFormat);

  uprv_free(bundle);
}

UNumberFormat*        
u_locbund_getNumberFormat(ULocaleBundle *bundle)
{
  UErrorCode status = U_ZERO_ERROR;
  
  if(bundle->fNumberFormat == 0) {
    bundle->fNumberFormat = unum_open(UNUM_DEFAULT, NULL,0,bundle->fLocale,NULL, &status);
    if(U_FAILURE(status))
      return 0;
  }
  
  return bundle->fNumberFormat;
}

UNumberFormat*    
u_locbund_getPercentFormat(ULocaleBundle *bundle)
{
  UErrorCode status = U_ZERO_ERROR;
  
  if(bundle->fPercentFormat == 0) {
    bundle->fPercentFormat = unum_open(UNUM_PERCENT,NULL,0, bundle->fLocale,NULL, &status);
    if(U_FAILURE(status))
      return 0;
  }
  
  return bundle->fPercentFormat;
}

UNumberFormat*    
u_locbund_getCurrencyFormat(ULocaleBundle *bundle)
{
  UErrorCode status = U_ZERO_ERROR;
  
  if(bundle->fCurrencyFormat == 0) {
    bundle->fCurrencyFormat = unum_open(UNUM_CURRENCY,NULL,0, bundle->fLocale, NULL,&status);
    if(U_FAILURE(status))
      return 0;
  }
  
  return bundle->fCurrencyFormat;
}

#define PAT_SIZE 512

UNumberFormat*    
u_locbund_getScientificFormat(ULocaleBundle *bundle)
{
  UErrorCode status = U_ZERO_ERROR;
/*  UChar pattern [PAT_SIZE];*/

  if(bundle->fScientificFormat == 0) {
    /* create the pattern for the locale */
/*    u_uastrcpy(pattern, "0.000000E000");*/
    
    bundle->fScientificFormat = unum_open(UNUM_SCIENTIFIC, NULL, 0,
                         bundle->fLocale, NULL,&status);
    
    if(U_FAILURE(status))
      return 0;
  }
  
  return bundle->fScientificFormat;
}

UNumberFormat*
u_locbund_getSpelloutFormat(ULocaleBundle *bundle)
{
  UErrorCode status = U_ZERO_ERROR;
  
  if(bundle->fSpelloutFormat == 0) {
    bundle->fSpelloutFormat = unum_open(UNUM_SPELLOUT,NULL,0 ,bundle->fLocale, NULL,
                    &status);
  }
  
  return bundle->fSpelloutFormat;
}

UDateFormat*
u_locbund_getDateFormat(ULocaleBundle *bundle)
{
  UErrorCode status = U_ZERO_ERROR;
  
  if(bundle->fDateFormat == 0) {
    bundle->fDateFormat = udat_open(UDAT_NONE, UDAT_DEFAULT, 
                    bundle->fLocale, 0, 0,NULL,0, &status);
  }
  
  return bundle->fDateFormat;
}

UDateFormat*
u_locbund_getTimeFormat(ULocaleBundle *bundle)
{
  UErrorCode status = U_ZERO_ERROR;
  
  if(bundle->fTimeFormat == 0) {
    bundle->fTimeFormat = udat_open(UDAT_DEFAULT, UDAT_NONE, 
                    bundle->fLocale, 0, 0,NULL,0, &status);
  }

  return bundle->fTimeFormat;
}

#endif /* #if !UCONFIG_NO_FORMATTING */
