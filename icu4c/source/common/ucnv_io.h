/*
   ********************************************************************************
   *                                                                              *
   * COPYRIGHT:                                                                   *
   *   (C) Copyright International Business Machines Corporation, 1999            *
   *   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
   *   US Government Users Restricted Rights - Use, duplication, or disclosure    *
   *   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
   *                                                                              *
   ********************************************************************************
   *
   *
   *  uconv_io.h:
   *  defines  variables and functions pertaining to file access, and name resolution
   *  aspect of the library
 */

#ifndef UCNV_IO_H
#define UCNV_IO_H


#include "utypes.h"
#include "filestrm.h"


/*filename containing aliasing information on the converter names */
static const char CONVERTER_FILE_NAME[13] = "convrtrs.txt";
static const char CONVERTER_FILE_EXTENSION[5] = ".cnv";
static const char SPACE_SEPARATORS[4] = {'\n', '\t', ' ', 0x00};


extern char **AVAILABLE_CONVERTERS_NAMES;
extern int32_t AVAILABLE_CONVERTERS;

/*Checks if c is in the NULL terminated setOfChars */
bool_t isInSet (char c, const char *setOfChars);

/*Remove all characters followed by '#' */
CAPI char * U_EXPORT2 removeComments (char *line);


/*Returns pointer to the next non-whitespace (or non-separators) */
CAPI int32_t U_EXPORT2 nextTokenOffset (const char *line, const char *separators);

/*Copies the next string in token and returns an updated pointer to the next token */
CAPI char * U_EXPORT2 getToken (char *token, char *line, const char *separators);

/*Takes an alias name and returns a FileStream pointer of the requested converter table or NULL, if not found */
FileStream * U_EXPORT2 openConverterFile (const char *name);

/*Fills in the Actual name of a converter based on the convrtrs.txt file
   returns TRUE if the name was resolved FALSE otherwise */
bool_t resolveName (char *realName, const char *alias);

/*called through lazy evaluation. Sets up a hashtable containg all the aliases and an array with pointers
   to the values inside the hashtable for quick indexing */
void setupAliasTableAndAvailableConverters (UErrorCode * err);

/*Uppercases a null-terminate string */
CAPI char * U_EXPORT2 strtoupper (char *);

/*case insensitive hash key*/
CAPI int32_t U_EXPORT2 uhash_hashIString(const void* name);
#endif /* _UCNV_IO */
