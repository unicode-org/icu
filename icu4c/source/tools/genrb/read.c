/*
*******************************************************************************
*
*   Copyright (C) 1998-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File read.c
*
* Modification History:
*
*   Date        Name        Description
*   05/26/99    stephen     Creation.
*   5/10/01     Ram         removed ustdio dependency
*******************************************************************************
*/

#include "read.h"
#include "error.h"
#include "unicode/ustring.h"

#define OPENBRACE    0x007B
#define CLOSEBRACE   0x007D
#define COMMA        0x002C
#define QUOTE        0x0022
#define ESCAPE       0x005C
#define SLASH        0x002F
#define ASTERISK     0x002A
#define SPACE        0x0020
#define COLON        0x003A
#define BADBOM       0xFFFE

/*
U_STRING_DECL(k_start_string, "string", 6);
U_STRING_DECL(k_start_binary, "binary", 6);
U_STRING_DECL(k_start_table, "table", 5);
U_STRING_DECL(k_start_int, "int", 3);
U_STRING_DECL(k_start_array, "array", 5);
U_STRING_DECL(k_start_intvector, "intvector", 9);
U_STRING_DECL(k_start_reserved, "reserved", 8);

static UBool didInit=FALSE;
*/

extern int32_t lineCount;

/* Protos */
static enum ETokenType getStringToken(UCHARBUF* buf, UChar initialChar,
                      struct UString *token,
                      UErrorCode *status);
static UChar getNextChar(UCHARBUF* buf, UBool skipwhite, UErrorCode *status);
static void seekUntilNewline(UCHARBUF* buf, UErrorCode *status);
static void seekUntilEndOfComment(UCHARBUF* buf, UErrorCode *status);
static UBool isWhitespace(UChar32 c);
static UBool isNewline(UChar32 c);


/* Read and return the next token from the stream.  If the token is of
   type eString, fill in the token parameter with the token.  If the
   token is eError, then the status parameter will contain the
   specific error.  This will be eItemNotFound at the end of file,
   indicating that all tokens have been returned.  This method will
   never return eString twice in a row; instead, multiple adjacent
   string tokens will be merged into one, with no intervening
   space. */
enum ETokenType getNextToken(UCHARBUF* buf,
                             struct UString *token,
                             UErrorCode *status)
{
    UChar c;

    /*enum ETokenType tokenType;*/

    if(U_FAILURE(*status))
      return tok_error;

    /* Skip whitespace */
    c = getNextChar(buf, TRUE, status);
    if(U_FAILURE(*status))
      return tok_error;

    switch(c) {
    case BADBOM:       return tok_error;
    case OPENBRACE:    return tok_open_brace;
    case CLOSEBRACE:   return tok_close_brace;
    case COMMA:        return tok_comma;
    case U_EOF:        return tok_EOF;
/*
    case COLON:        return tok_colon;
      c = getNextChar(f, TRUE, status);
      tokenType = getStringToken(f, c, token, status);
      break;
*/
    default:           return getStringToken(buf, c, token, status);
    }
/*
    if(!didInit) {
        U_STRING_INIT(k_start_string, "string", 6);
        U_STRING_INIT(k_start_binary, "binary", 6);
        U_STRING_INIT(k_start_table, "table", 5);
        U_STRING_INIT(k_start_int, "int", 3);
        U_STRING_INIT(k_start_array, "array", 5);
        U_STRING_INIT(k_start_intvector, "intvector", 9);
        U_STRING_INIT(k_start_reserved, "reserved", 8);
        didInit=TRUE;
    }
    if(u_strcmp(token->fChars, k_start_string) == 0) {
        return(tok_start_string);
    } else if(u_strcmp(token->fChars, k_start_binary) == 0) {
        return(tok_start_binary);
    } else if(u_strcmp(token->fChars, k_start_table) == 0) {
        return(tok_start_table);
    } else if(u_strcmp(token->fChars, k_start_int) == 0) {
        return(tok_start_int);
    } else if(u_strcmp(token->fChars, k_start_array) == 0) {
        return(tok_start_array);
    } else if(u_strcmp(token->fChars, k_start_intvector) == 0) {
        return(tok_start_intvector);
    } else if(u_strcmp(token->fChars, k_start_reserved) == 0) {
        return(tok_start_reserved);
    } else {
        return tok_error;
    }
*/
}

/* Copy a string token into the given UnicodeString.  Upon entry, we
   have already read the first character of the string token, which is
   not a whitespace character (but may be a QUOTE or ESCAPE). This
   function reads all subsequent characters that belong with this
   string, and copy them into the token parameter. The other
   important, and slightly convoluted purpose of this function is to
   merge adjacent strings.  It looks forward a bit, and if the next
   non comment, non whitespace item is a string, it reads it in as
   well.  If two adjacent strings are quoted, they are merged without
   intervening space.  Otherwise a single SPACE character is
   inserted. */
static enum ETokenType getStringToken(UCHARBUF* buf,
                                      UChar initialChar,
                                      struct UString *token,
                                      UErrorCode *status)
{
  UBool lastStringWasQuoted;
  UChar32 c;
  UChar target[3] ={'\0'};
  UChar* pTarget=target;
  /* We are guaranteed on entry that initialChar is not a whitespace
     character. If we are at the EOF, or have some other problem, it
     doesn't matter; we still want to validly return the initialChar
     (if nothing else) as a string token. */

  if(U_FAILURE(*status))
    return tok_error;

  /* setup */
  lastStringWasQuoted = FALSE;
  c = initialChar;
  ustr_setlen(token, 0, status);

  if(U_FAILURE(*status))
    return tok_error;

  for(;;) {
    if(c == QUOTE) {
      if( ! lastStringWasQuoted && token->fLength > 0) {
        ustr_ucat(token, SPACE, status);
        if(U_FAILURE(*status))
          return tok_error;
      }
      lastStringWasQuoted = TRUE;

      for(;;) {
        c = ucbuf_getc(buf,status);
        /*  c = u_fgetc(f, status);*/

        /* EOF reached */
        if(c == (UChar)U_EOF) {
            return tok_EOF;
        }
        /* Unterminated quoted strings */
        if(U_FAILURE(*status))
          return tok_error;
        if(c == QUOTE)
          break;
        if(c == ESCAPE){
          pTarget=target;
          c = unescape(buf, status);
          if(c==U_ERR){
              return tok_error;
          }
        }
        U_APPEND_CHAR32(c,pTarget);
        pTarget=target;
        ustr_uscat(token,pTarget, status);
        if(U_FAILURE(*status))
          return tok_error;
      }
    }
    else {
      if(token->fLength > 0) {
        ustr_ucat(token, SPACE, status);
        if(U_FAILURE(*status))
          return tok_error;
      }
      lastStringWasQuoted = FALSE;
      if(c == ESCAPE){
        pTarget=target;
        c = unescape(buf, status);
        /* EOF reached */
        if(c == (UChar)U_EOF) {
            return tok_error;
        }
      }
      U_APPEND_CHAR32(c,pTarget);
      pTarget=target;
      ustr_uscat(token,pTarget, status);

      if(U_FAILURE(*status))
        return tok_error;

      for(;;) {
        /* DON'T skip whitespace */
        c = getNextChar(buf, FALSE, status);
        /* EOF reached */
        if(c == (UChar)U_EOF) {
          ucbuf_ungetc(c, buf);
          return tok_string;
        }

        if(U_FAILURE(*status))
          return tok_string;

        if(c == QUOTE
           || c == OPENBRACE
           || c == CLOSEBRACE
           || c == COMMA
           /*|| c == COLON*/)
          {
            ucbuf_ungetc(c, buf);
            /*u_fungetc(c, f, status);*/
            break;
          }

        if(isWhitespace(c))
          break;

        if(c == ESCAPE){
          pTarget=target;
          c = unescape(buf, status);
          if(c==U_ERR){
              return tok_error;
          }
        }
        U_APPEND_CHAR32(c,pTarget);
        pTarget=target;
        ustr_uscat(token,pTarget, status);
        if(U_FAILURE(*status))
          return tok_error;
      }
    }

    /* DO skip whitespace */
    c = getNextChar(buf, TRUE, status);
    if(U_FAILURE(*status))
      return tok_string;

    if(c == OPENBRACE || c == CLOSEBRACE || c == COMMA/* || c == COLON*/) {
       ucbuf_ungetc(c, buf);
       /*u_fungetc(c, f, status);*/
      return tok_string;
    }
  }
}

/* Retrieve the next character, ignoring comments.  If skipwhite is
   true, whitespace is skipped as well. */
static UChar getNextChar(UCHARBUF* buf,
                         UBool skipwhite,
                         UErrorCode *status)
{
  UChar c;

  if(U_FAILURE(*status))
    return U_EOF;

  for(;;) {
    c = (UChar)ucbuf_getc(buf,status);
    /*c = u_fgetc(f, status);*/
    if(c == (UChar)U_EOF)
      return U_EOF;

    if(skipwhite && isWhitespace(c))
      continue;

    /* This also handles the get() failing case */
    if(c != SLASH)
      return c;

    c = (UChar)ucbuf_getc(buf,status);
    /*  c = u_fgetc(f, status);*/
    if(c == (UChar)U_EOF)
      return U_EOF;

    switch(c) {
    case SLASH:
      seekUntilNewline(buf, status);
      break;

    case ASTERISK:
      /* Note that we silently ignore an unterminated comment */
      seekUntilEndOfComment(buf, status);
      break;

    default:
        ucbuf_ungetc(c, buf);
        /*u_fungetc(c, f, status);*/
      /* If get() failed this is a NOP */
      return SLASH;
    }
  }
}

static void seekUntilNewline(UCHARBUF* buf,
                             UErrorCode *status)
{
  UChar c;

  if(U_FAILURE(*status))
    return;

  do {
    c = (UChar)ucbuf_getc(buf,status);
    /*  c = u_fgetc(f, status);*/
  } while(! isNewline(c) && c != (UChar)U_EOF && *status == U_ZERO_ERROR);

  /*if(U_FAILURE(*status))
    err = kItemNotFound;*/
}

static void seekUntilEndOfComment(UCHARBUF* buf,
                                  UErrorCode *status)
{
  UChar c, d;

  if(U_FAILURE(*status))
    return;

  do {
    c =(UChar) ucbuf_getc(buf,status);
    /*  c = u_fgetc(f, status);*/
    if(c == ASTERISK) {
        d =(UChar) ucbuf_getc(buf,status);
        /*  d = u_fgetc(f, status);*/
      if(d != SLASH)
        ucbuf_ungetc(d, buf);
        /*u_fungetc(d, f, status);*/
      else
        break;
    }
  } while(c != (UChar)U_EOF && *status == U_ZERO_ERROR);

  if(c == (UChar)U_EOF) {
    *status = U_INVALID_FORMAT_ERROR;
    setErrorText("Unterminated comment detected");
  }
}

UChar32 unescape(UCHARBUF* buf,
                      UErrorCode *status)
{
  if(U_FAILURE(*status))
    return U_EOF;
  /* We expect to be called after the ESCAPE has been seen, but
   * u_fgetcx needs an ESCAPE to do its magic. */
  ucbuf_ungetc(ESCAPE, buf);
  return ucbuf_getcx(buf,status);
}

static UBool isWhitespace(UChar32 c)
{
  switch (c) {
    /* ' ', '\t', '\n', '\r', 0x2029, 0xFEFF */
  case 0x000A:
  case 0x2029:
    lineCount++;
  case 0x000D:
  case 0x0020:
  case 0x0009:
  case 0xFEFF:
    return TRUE;

  default:
    return FALSE;
  }
}

static UBool isNewline(UChar32 c)
{
  switch (c) {
    /* '\n', '\r', 0x2029 */
  case 0x000A:
  case 0x2029:
    lineCount++;
  case 0x000D:
    return TRUE;

  default:
    return FALSE;
  }
}
