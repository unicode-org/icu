/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998, 1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File read.h
*
* Modification History:
*
*   Date        Name        Description
*   05/26/99    stephen     Creation.
*******************************************************************************
*/

#ifndef READ_H
#define READ_H 1

#include "utypes.h"
#include "ufile.h"
#include "ustr.h"

/* The types of tokens which may be returned by getNextToken. */
enum ETokenType
{
  tok_string,               /* A string token, such as "MonthNames" */
  tok_open_brace,           /* An opening brace character */
  tok_close_brace,          /* A closing brace character */
  tok_comma,                /* A comma */
  
  tok_EOF,                  /* End of the file has been reached successfully */
  tok_error,                /* An error, such an unterminated quoted string */
  tok_token_type_count = 4     /* Number of "real" token types */
};

enum ETokenType getNextToken(UFILE *f,
			     struct UString *token,
			     UErrorCode *status);

#endif
