/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File CRESBUND.CPP
*
* Modification History:
*
*   Date        Name        Description
*   04/01/97    aliu        Creation.
*   06/14/99    stephen     Removed functions taking a filename suffix.
*   07/20/99    stephen     Changed for UResourceBundle typedef'd to void*
*   11/09/99	weiv		Added ures_getLocale()
*******************************************************************************
*/

#include "unicode/resbund.h"
#include "unicode/ures.h"
#include "unicode/locid.h"
#include "unicode/uloc.h"

/**
 * Functions to create and destroy resource bundles.
 */

U_CAPI UResourceBundle* ures_open(    const char* myPath,
                    const char* localeID,
                    UErrorCode* status)
{
  UnicodeString uPath;
  Locale myLocale(localeID); // Handles NULL properly.


  if (myPath != 0) uPath = myPath;
  else uPath = u_getDataDirectory();

  return (UResourceBundle*) new ResourceBundle(uPath, myLocale, *status);
}

U_CAPI UResourceBundle* ures_openW(    const wchar_t* myPath,
                    const char* localeID,
                    UErrorCode* status)
{
  Locale myLocale(localeID);
  if (localeID == 0) localeID = uloc_getDefault();

  return (UResourceBundle*) new ResourceBundle(myPath, myLocale, *status);
}


/**
 * Functions to retrieve data from resource bundles.
 */

U_CAPI const UChar* ures_get(    const UResourceBundle*    resourceBundle,
                const char*              resourceTag,
                UErrorCode*               status)
{
  if (status==NULL || U_FAILURE(*status)) return NULL;
  if (!resourceBundle || !resourceTag)
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }

  const UnicodeString* tmp =  ((ResourceBundle*)resourceBundle)->getString(resourceTag, *status);

  if (U_SUCCESS(*status)) return tmp->getUChars();
  else return NULL;
}

U_CAPI const UChar* ures_getArrayItem(const UResourceBundle*     resourceBundle,
                    const char*               resourceTag,
                    int32_t                   resourceIndex,
                    UErrorCode*                status)
{
  if (status==NULL || U_FAILURE(*status)) return NULL;
  if (!resourceBundle || !resourceTag || (resourceIndex < 0))
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }

  const UnicodeString* tmp =  ((ResourceBundle*)resourceBundle)->getArrayItem(resourceTag, resourceIndex, *status);

  if (U_SUCCESS(*status)) return tmp->getUChars();
  else return NULL;
}



U_CAPI const UChar* ures_get2dArrayItem(const UResourceBundle*   resourceBundle,
                      const char*             resourceTag,
                      int32_t                 rowIndex,
                      int32_t                 columnIndex,
                      UErrorCode*              status)
{
  if (status==NULL || U_FAILURE(*status)) return NULL;
  if (!resourceBundle || !resourceTag || (rowIndex < 0) || (columnIndex < 0))
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }

  const UnicodeString* tmp = ((ResourceBundle*)resourceBundle)->get2dArrayItem(resourceTag,
                                rowIndex,
                                columnIndex,
                                *status);
  if (U_SUCCESS(*status)) return tmp->getUChars();
  else return NULL;
}

U_CAPI const char* ures_getLocale(const UResourceBundle* resourceBundle, UErrorCode* status)
{
  if (status==NULL || U_FAILURE(*status)) return NULL;
  if (!resourceBundle)
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }
  return ((ResourceBundle*)resourceBundle)->getLocale().getName();
}

U_CAPI const UChar* ures_getTaggedArrayItem(const UResourceBundle*   resourceBundle,
                      const char*             resourceTag,
                      const char*             itemTag,
                      UErrorCode*              status)
{
  if (status==NULL || U_FAILURE(*status)) return NULL;
  if (!resourceBundle || !resourceTag || !itemTag)
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }

  const UnicodeString* tmp =  ((ResourceBundle*)resourceBundle)->getTaggedArrayItem(resourceTag,
                               itemTag,
                               *status);
  if (U_SUCCESS(*status)) return tmp->getUChars();
  else return NULL;
}


U_CAPI const char* ures_getVersionNumber(const UResourceBundle*   resourceBundle)
{
  if (!resourceBundle) return NULL;
  return ((ResourceBundle*)resourceBundle)->getVersionNumber();
}

extern "C" int32_t T_ResourceBundle_countArrayItems(const ResourceBundle* rb,
                             const char* resourceKey,
                             UErrorCode* err);

U_CAPI int32_t ures_countArrayItems(const UResourceBundle* resourceBundle,
                  const char* resourceKey,
                  UErrorCode* err)
{
  return T_ResourceBundle_countArrayItems((ResourceBundle*)resourceBundle,
                      resourceKey,
                      err);
}

U_CAPI void ures_close(    UResourceBundle*    resourceBundle)
{
    delete resourceBundle;
    return;
}

/**
 * Returns a list of all available ULocales.  The return value is a pointer to
 * an array of pointers to ULocale objects.  Both this array and the pointers
 * it contains are owned by ICU and should not be deleted or written through
 * by the caller.  The array is terminated by a null pointer.
 */
extern "C" void T_ResourceBundle_getTaggedArrayUChars(const ResourceBundle*   resourceBundle,
                            const UnicodeString&    resourceTag,
                            UChar const**         itemTags,
                            UChar const**         items,
                            int32_t                    maxItems,
                            int32_t*                numItems,
                            UErrorCode*              err);

//eof
