/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1998     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
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
*******************************************************************************
*/

#include "resbund.h"
#include "ures.h"
#include "locid.h"
#include "uloc.h"

/**
 * Functions to create and destroy resource bundles.
 */

CAPI UResourceBundle* ures_open(    const char* myPath,
                    const char* localeID,
                    UErrorCode* status)
{
  UnicodeString uPath;
  Locale myLocale;


  if (myPath != 0) uPath = myPath;
  else uPath = uloc_getDataDirectory();

  if (localeID == 0) localeID = uloc_getDefault();


  return (UResourceBundle*) new ResourceBundle(uPath, myLocale.init(localeID), *status);
}

CAPI UResourceBundle* ures_openW(    const wchar_t* myPath,
                    const char* localeID,
                    UErrorCode* status)
{
  Locale myLocale;
  if (localeID == 0) localeID = uloc_getDefault();

  return (UResourceBundle*) new ResourceBundle(myPath, myLocale.init(localeID), *status);
}


/**
 * Functions to retrieve data from resource bundles.
 */

CAPI const UChar* ures_get(    const UResourceBundle*    resourceBundle,
                const char*              resourceTag,
                UErrorCode*               status)
{
  if (FAILURE(*status)) return NULL;
  if (!resourceBundle || !resourceTag)
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }

  const UnicodeString* tmp =  ((ResourceBundle*)resourceBundle)->getString(resourceTag, *status);

  if (SUCCESS(*status)) return tmp->getUChars();
  else return NULL;
}

CAPI const UChar* ures_getArrayItem(const UResourceBundle*     resourceBundle,
                    const char*               resourceTag,
                    int32_t                   resourceIndex,
                    UErrorCode*                status)
{
  if (FAILURE(*status)) return NULL;
  if (!resourceBundle || !resourceTag || (resourceIndex < 0))
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }

  const UnicodeString* tmp =  ((ResourceBundle*)resourceBundle)->getArrayItem(resourceTag, resourceIndex, *status);

  if (SUCCESS(*status)) return tmp->getUChars();
  else return NULL;
}



CAPI const UChar* ures_get2dArrayItem(const UResourceBundle*   resourceBundle,
                      const char*             resourceTag,
                      int32_t                 rowIndex,
                      int32_t                 columnIndex,
                      UErrorCode*              status)
{
  if (FAILURE(*status)) return NULL;
  if (!resourceBundle || !resourceTag || (rowIndex < 0) || (columnIndex < 0))
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }

  const UnicodeString* tmp = ((ResourceBundle*)resourceBundle)->get2dArrayItem(resourceTag,
                                rowIndex,
                                columnIndex,
                                *status);
  if (SUCCESS(*status)) return tmp->getUChars();
  else return NULL;
}

CAPI const UChar* ures_getTaggedArrayItem(const UResourceBundle*   resourceBundle,
                      const char*             resourceTag,
                      const char*             itemTag,
                      UErrorCode*              status)
{
  if (FAILURE(*status)) return NULL;
  if (!resourceBundle || !resourceTag || !itemTag)
    {
      *status = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }

  const UnicodeString* tmp =  ((ResourceBundle*)resourceBundle)->getTaggedArrayItem(resourceTag,
                               itemTag,
                               *status);
  if (SUCCESS(*status)) return tmp->getUChars();
  else return NULL;
}


CAPI const char* ures_getVersionNumber(const UResourceBundle*   resourceBundle)
{
  if (!resourceBundle) return NULL;
  return ((ResourceBundle*)resourceBundle)->getVersionNumber();
}

extern "C" int32_t T_ResourceBundle_countArrayItems(const ResourceBundle* rb,
                             const char* resourceKey,
                             UErrorCode* err);

CAPI int32_t ures_countArrayItems(const UResourceBundle* resourceBundle,
                  const char* resourceKey,
                  UErrorCode* err)
{
  return T_ResourceBundle_countArrayItems((ResourceBundle*)resourceBundle,
                      resourceKey,
                      err);
}

CAPI void ures_close(    UResourceBundle*    resourceBundle)
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
extern "C" void T_ResourceBundle_getTaggedArrayUChars(const ResourceBundle*   UResourceBundle,
                            const UnicodeString&    resourceTag,
                            UChar const**         itemTags,
                            UChar const**         items,
                            int32_t                    maxItems,
                            int32_t*                numItems,
                            UErrorCode*              err);

//eof
