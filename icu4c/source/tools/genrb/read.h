/*
*******************************************************************************
*
*   Copyright (C) 1998-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File read.h
*
* Modification History:
*
*   Date        Name        Description
*   05/26/99    stephen     Creation.
*   5/10/01     Ram			removed ustdio dependency
*******************************************************************************
*/

#ifndef READ_H
#define READ_H 1

#include "unicode/utypes.h"
#include "ustr.h"
#include "ucbuf.h"

/* The types of tokens which may be returned by getNextToken. */
enum ETokenType
{
  tok_string,               /* A string token, such as "MonthNames" */
  tok_open_brace,           /* An opening brace character */
  tok_close_brace,          /* A closing brace character */
  tok_comma,                /* A comma */
  tok_colon,                /* A colon */
  tok_start_string,         /* :String */
  tok_start_binary,         /* :Binary */
  tok_start_table,          /* :Table */
  tok_start_int,            /* :Integer */
  tok_start_array,          /* :Array */
  tok_start_intvector,      /* :IntVector */
  tok_start_reserved,       /* :Reserved - treat like a string */

  tok_EOF,                  /* End of the file has been reached successfully */
  tok_error,                /* An error, such an unterminated quoted string */
  tok_token_type_count = 12 /* Number of "real" token types */
};

UChar unescape(UCHARBUF* buf, UErrorCode *status);
enum ETokenType getNextToken(UCHARBUF* buf,
			     struct UString *token,
			     UErrorCode *status);

#endif
