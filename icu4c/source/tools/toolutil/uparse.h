/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uparse.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000apr18
*   created by: Markus W. Scherer
*
*   This file provides a parser for files that are delimited by one single
*   character like ';' or TAB. Example: the Unicode Character Properties files
*   like UnicodeData.txt are semicolon-delimited.
*/

#ifndef __UPARSE_H__
#define __UPARSE_H__

#include "unicode/utypes.h"

/* Function type for u_parseDelimitedFile()'s fields parameter. */
typedef void
UParseFieldFn(void *context,
              char *start, char *limit,
              int32_t fieldNr,
              UErrorCode *pErrorCode);

/*
 * Parser for files that are similar to UnicodeData.txt:
 * This function opens the file and reads it line by line. It skips empty lines
 * and comment lines that start with a '#'.
 * All other lines are separated into fields with one delimiter character
 * (semicolon for Unicode Properties files) between two fields. The last field in
 * a line does not need to be terminated with a delimiter.
 *
 * For each field, and at the beginning and end of the processing of a line,
 * a field function is called. There must be fieldCount+2 function pointers in
 * the fields array. Any of them may be NULL to indicate that there is no function.
 * For each function call, the context parameter of the field function is
 * the same as the one for the parse function. The start pointer indicates the
 * beginning of the field. The limit pointer points to the delimiter or NUL
 * character.
 * For the functions for the beginning and end of a line, start and limit
 * are set to the entire line.
 * Before a line's fields are processed, fields[0] is called with
 * fieldNr=-1.
 * For each field i (i=0..fieldCount-1), fieldFn[i+1] is called with fieldNr=i.
 * After a line's fields are processed, fields[fieldCount+1] is called
 * with fieldNr=fieldCount.
 * If the file cannot be opened, or there is a parsing error or a field function
 * sets *pErrorCode, then the parser returns with *pErrorCode set to an error code.
 */
U_CAPI void U_EXPORT2
u_parseDelimitedFile(const char *filename, char delimiter,
                     UParseFieldFn *fields[], int32_t fieldCount,
                     void *context,
                     UErrorCode *pErrorCode);

#endif
