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

/* Function type for u_parseDelimitedFile(). */
typedef void
UParseLineFn(void *context,
              char *fields[][2],
              int32_t fieldCount,
              UErrorCode *pErrorCode);

/*
 * Parser for files that are similar to UnicodeData.txt:
 * This function opens the file and reads it line by line. It skips empty lines
 * and comment lines that start with a '#'.
 * All other lines are separated into fields with one delimiter character
 * (semicolon for Unicode Properties files) between two fields. The last field in
 * a line does not need to be terminated with a delimiter.
 *
 * For each line, after segmenting it, a line function is called.
 * It gets passed the array of field start and limit pointers that is
 * passed into this parser and filled by it for each line.
 * For each field i of the line, the start pointer in fields[i][0]
 * points to the beginning of the field, while the limit pointer in fields[i][1]
 * points behind the field, i.e., to the delimiter or the line end.
 *
 * The context parameter of the line function is
 * the same as the one for the parse function.
 *
 * The line function may modify the contents of the fields including the
 * limit characters.
 *
 * If the file cannot be opened, or there is a parsing error or a field function
 * sets *pErrorCode, then the parser returns with *pErrorCode set to an error code.
 */
U_CAPI void U_EXPORT2
u_parseDelimitedFile(const char *filename, char delimiter,
                     char *fields[][2], int32_t fieldCount,
                     UParseLineFn *lineFn, void *context,
                     UErrorCode *pErrorCode);

#endif
