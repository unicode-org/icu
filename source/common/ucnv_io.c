/*
   ********************************************************************************
   *                                                                              *
   * COPYRIGHT:                                                                   *
   *   (C) Copyright International Business Machines Corporation, 1998            *
   *   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
   *   US Government Users Restricted Rights - Use, duplication, or disclosure    *
   *   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
   *                                                                              *
   ********************************************************************************
   *
   *
   *  uconv_io.c:
   *  initializes global variables and defines functions pertaining to file access,
   *  and name resolution aspect of the library.
   ********************************************************************************
 */
#include "utypes.h"
#include "umutex.h"
#include "filestrm.h"
#include "cstring.h"
#include "cmemory.h"
#include "uhash.h"
#include "ucmp8.h"
#include "ucmp16.h"
#include "ucnv_bld.h"
#include "ucnv_io.h"
#include "uloc.h"

static void doSetupAliasTableAndAvailableConverters (FileStream * converterFile,
						     UErrorCode * err);

static char *_convertDataDirectory = NULL;

/*Initializes Global Variables */
static UHashtable *ALIASNAMES_HASHTABLE = NULL;
char **AVAILABLE_CONVERTERS_NAMES = NULL;
int32_t AVAILABLE_CONVERTERS = 0;

/* Remove all characters followed by '#'
 */
char *
  removeComments (char *line)
{
  char *pound = icu_strchr (line, '#');

  if (pound != NULL)
    *pound = '\0';
  return line;
}

/*Returns uppercased string */
char *
  strtoupper (char *name)
{
  int32_t i = 0;

  while (name[i] = icu_toupper (name[i]))
    i++;

  return name;
}

/* Returns true in c is a in set 'setOfChars', false otherwise
 */
bool_t 
  isInSet (char c, const char *setOfChars)
{
  uint8_t i = 0;

  while (setOfChars[i] != '\0')
    {
      if (c == setOfChars[i++])
	return TRUE;
    }

  return FALSE;
}

/* Returns pointer to the next non-whitespace (or non-separator)
 */
int32_t 
  nextTokenOffset (const char *line, const char *separators)
{
  int32_t i = 0;

  while (line[i] && isInSet (line[i], separators))
    i++;

  return i;
}

/* Returns pointer to the next token based on the set of separators
 */
char *
  getToken (char *token, char *line, const char *separators)
{
  int32_t i = nextTokenOffset (line, separators);
  int8_t j = 0;

  while (line[i] && (!isInSet (line[i], separators)))
    token[j++] = line[i++];
  token[j] = '\0';

  return line + i;
}

/* this function is called only   if ((ALIASNAMES_HASHTABLE == NULL) ||
 * (AVAILABLE_CONVERTERS_NAMES == NULL)) it builds a hashtable containing
 * all the "real" table names (filenames), keyed-off of the aliases and
 * the real-names themselves.
 * Also builds an array of char **, that point to the allocated memory
 * for each actual names in the Hashtable.
 * That array is used in T_UnicodeConverter_getAvailableNames.
 */
void 
  setupAliasTableAndAvailableConverters (UErrorCode * err)
{
  char fullFileName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
  FileStream *converterFile = NULL;

  if (U_FAILURE (*err))
    return;

  icu_strcpy (fullFileName, uloc_getDataDirectory ());
  icu_strcat (fullFileName, CONVERTER_FILE_NAME);

  converterFile = T_FileStream_open (fullFileName, "r");
  if (converterFile == NULL)
    {
      *err = U_FILE_ACCESS_ERROR;
    }
  else
    {
      doSetupAliasTableAndAvailableConverters (converterFile, err);
      T_FileStream_close (converterFile);
    }

  return;
}

/* this function is only to be called by setupAliasTableAndAvailableConverters
 */
void 
  doSetupAliasTableAndAvailableConverters (FileStream * converterFile, UErrorCode * err)
{
  char myLine[UCNV_MAX_LINE_TEXT];
  char *line = myLine;
  char actualNameToken[UCNV_MAX_CONVERTER_NAME_LENGTH];
  char aliasNameToken[UCNV_MAX_CONVERTER_NAME_LENGTH];
  char *toBeHashed = NULL;
  UHashtable *myALIASNAMES_HASHTABLE = NULL;
  char **myAVAILABLE_CONVERTERS_NAMES = NULL;
  int32_t myAVAILABLE_CONVERTERS = 0;

  /*We need to do the initial work of setting everything */
  myALIASNAMES_HASHTABLE = uhash_open ((UHashFunction)uhash_hashIString, err);
  if (U_FAILURE (*err))
    return;

  if (myALIASNAMES_HASHTABLE == NULL)
    return;

  while (T_FileStream_readLine (converterFile, line, UCNV_MAX_LINE_TEXT))
    {
      removeComments (line);
      if (line[nextTokenOffset (line, SPACE_SEPARATORS)] != '\0')	/*Skips Blank lines */
	{
	  line = getToken (actualNameToken, line, SPACE_SEPARATORS);
	  toBeHashed = (char *) icu_malloc ((icu_strlen (actualNameToken) + 1) * sizeof (char));
	  if (toBeHashed == NULL)
	    {
	      *err = U_MEMORY_ALLOCATION_ERROR;
	      return;
	    }
	  icu_strcpy (toBeHashed, actualNameToken);
	  myAVAILABLE_CONVERTERS_NAMES = (char **) icu_realloc (myAVAILABLE_CONVERTERS_NAMES,
			    (myAVAILABLE_CONVERTERS + 1) * sizeof (char *));
	  if (myAVAILABLE_CONVERTERS_NAMES == NULL)
	    {
	      *err = U_MEMORY_ALLOCATION_ERROR;
	      return;
	    }
	  myAVAILABLE_CONVERTERS_NAMES[myAVAILABLE_CONVERTERS++] = toBeHashed;

	  uhash_put (myALIASNAMES_HASHTABLE, toBeHashed, err);
	  while (line[nextTokenOffset (line, SPACE_SEPARATORS)] != '\0')
	    {
	      line = getToken (aliasNameToken, line, SPACE_SEPARATORS);
	      uhash_putKey (myALIASNAMES_HASHTABLE,
			    uhash_hashIString (aliasNameToken),
			    toBeHashed,
			    err);
	    }
	  if (U_FAILURE (*err))
	    return;
	}

    }

  /*If another thread has already created the hashtable and array, we need to free */
  if ((ALIASNAMES_HASHTABLE != NULL) || (AVAILABLE_CONVERTERS_NAMES != NULL))
    {
      while (myAVAILABLE_CONVERTERS > 0)
	{
	  icu_free (myAVAILABLE_CONVERTERS_NAMES[--myAVAILABLE_CONVERTERS]);
	}
      icu_free (myAVAILABLE_CONVERTERS_NAMES);
      uhash_close (myALIASNAMES_HASHTABLE);
    }
  else
    {
      umtx_lock (NULL);
      ALIASNAMES_HASHTABLE = myALIASNAMES_HASHTABLE;
      AVAILABLE_CONVERTERS_NAMES = myAVAILABLE_CONVERTERS_NAMES;
      AVAILABLE_CONVERTERS = myAVAILABLE_CONVERTERS;
      umtx_unlock (NULL);
    }

  return;
}

/* resolveName takes a table alias name and fills in the actual name used internally.
 * it returns a TRUE if the name was found (table supported) returns FALSE otherwise
 */
bool_t 
  resolveName (char *realName, const char *alias)
{
  int32_t i = 0;
  bool_t found = FALSE;
  char *actualName = NULL;
  UErrorCode err = U_ZERO_ERROR;

  /*Lazy evaluates the Alias hashtable */
  if (ALIASNAMES_HASHTABLE == NULL)
    setupAliasTableAndAvailableConverters (&err);
  if (U_FAILURE (err))
    return FALSE;


  actualName = (char *) uhash_get (ALIASNAMES_HASHTABLE, uhash_hashIString (alias));

  if (actualName != NULL)
    {
      icu_strcpy (realName, actualName);
      found = TRUE;
    }

  return found;
}

/*Higher level function, takes in an alias name
 *and returns a file pointer of the table file
 *Will return NULL if the file isn't found for
 *any given reason (file not there, name not in
 *"convrtrs.txt"
 */
FileStream *
  openConverterFile (const char *name)
{
  char actualFullFilenameName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
  FileStream *tableFile = NULL;

  icu_strcpy (actualFullFilenameName, uloc_getDataDirectory ());

  if (resolveName (actualFullFilenameName + icu_strlen (actualFullFilenameName), name))
    {
      icu_strcat (actualFullFilenameName, CONVERTER_FILE_EXTENSION);
      tableFile = T_FileStream_open (actualFullFilenameName, "rb");
    }

  return tableFile;
}
