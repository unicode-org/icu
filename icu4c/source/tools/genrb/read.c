/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
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
*******************************************************************************
*/

#include "read.h"
#include "error.h"
#include "unicode/ustdio.h"
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

U_STRING_DECL(k_start_string, "string", 6);
U_STRING_DECL(k_start_binary, "binary", 6);
U_STRING_DECL(k_start_table, "table", 5);
U_STRING_DECL(k_start_int, "int", 3);
U_STRING_DECL(k_start_array, "array", 5);
U_STRING_DECL(k_start_intvector, "intvector", 9);
U_STRING_DECL(k_start_reserved, "reserved", 8);

static bool_t didInit=FALSE;

/* Protos */
static enum ETokenType getStringToken(UFILE *f, UChar initialChar, 
				      struct UString *token,
				      UErrorCode *status);
static UChar unescape(UFILE *f, UErrorCode *status);
static UChar getNextChar(UFILE *f, bool_t skipwhite, UErrorCode *status);
static void seekUntilNewline(UFILE *f, UErrorCode *status);
static void seekUntilEndOfComment(UFILE *f, UErrorCode *status);
static bool_t isWhitespace(UChar c);
static bool_t isNewline(UChar c);
     

/* Read and return the next token from the stream.  If the token is of
   type eString, fill in the token parameter with the token.  If the
   token is eError, then the status parameter will contain the
   specific error.  This will be eItemNotFound at the end of file,
   indicating that all tokens have been returned.  This method will
   never return eString twice in a row; instead, multiple adjacent
   string tokens will be merged into one, with no intervening
   space. */
enum ETokenType getNextToken(UFILE *f,
			     struct UString *token,
			     UErrorCode *status)
{
    UChar c;

    enum ETokenType tokenType;

    if(U_FAILURE(*status)) return tok_error;

    /* Skip whitespace */
    c = getNextChar(f, TRUE, status);
    if(U_FAILURE(*status)) return tok_error;

    switch(c) {
    case OPENBRACE:    return tok_open_brace;
    case CLOSEBRACE:   return tok_close_brace;
    case COMMA:        return tok_comma;
    case U_EOF:        return tok_EOF;
    case COLON:        
      c = getNextChar(f, TRUE, status);
      tokenType = getStringToken(f, c, token, status);
      break;              
    default:           return getStringToken(f, c, token, status);
    }
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
static enum ETokenType getStringToken(UFILE *f,
				      UChar initialChar,
				      struct UString *token,
				      UErrorCode *status)
{
  bool_t lastStringWasQuoted;
  UChar c;

  /* We are guaranteed on entry that initialChar is not a whitespace
     character. If we are at the EOF, or have some other problem, it
     doesn't matter; we still want to validly return the initialChar
     (if nothing else) as a string token. */
 
  if(U_FAILURE(*status)) return tok_error;
  
  /* setup */
  lastStringWasQuoted = FALSE;
  c = initialChar;
  ustr_setlen(token, 0, status);

  if(U_FAILURE(*status)) return tok_error;
  
  for(;;) {
    if(c == QUOTE) {
      if( ! lastStringWasQuoted && token->fLength > 0) {
	ustr_ucat(token, SPACE, status);
	if(U_FAILURE(*status)) return tok_error;
      }
      lastStringWasQuoted = TRUE;
      
      for(;;) {
    c = u_fgetc(f);
    /*	c = u_fgetc(f, status);*/

	/* EOF reached */
	if(c == (UChar)U_EOF)        return tok_EOF;
	/* Unterminated quoted strings */
	if(U_FAILURE(*status))  return tok_error;
	if(c == QUOTE) 
	  break;
	if(c == ESCAPE) 
	  c = unescape(f, status);
	ustr_ucat(token, c, status);
	if(U_FAILURE(*status)) return tok_error;
      }
    }
    else {
      if(token->fLength > 0) {
	ustr_ucat(token, SPACE, status);
	if(U_FAILURE(*status)) return tok_error;
      }
      lastStringWasQuoted = FALSE;
      
      if(c == ESCAPE) 
	c = unescape(f, status);
      ustr_ucat(token, c, status);
      if(U_FAILURE(*status)) return tok_error;

      for(;;) {
	/* DON'T skip whitespace */
	c = getNextChar(f, FALSE, status);
	if(U_FAILURE(*status)) 
	  return tok_string;

	if(c == QUOTE
	   || c == OPENBRACE
	   || c == CLOSEBRACE
	   || c == COMMA
       || c == COLON)
	  {
	    u_fungetc(c, f);
	    /*u_fungetc(c, f, status);*/
	    break;
	  }

	if(isWhitespace(c)) 
	  break;
	
	if(c == ESCAPE) 
	  c = unescape(f, status);
	ustr_ucat(token, c, status);
	if(U_FAILURE(*status)) return tok_error;
      }
    }
    
    /* DO skip whitespace */
    c = getNextChar(f, TRUE, status);
    if(U_FAILURE(*status)) 
      return tok_string;
    
    if(c == OPENBRACE || c == CLOSEBRACE || c == COMMA || c == COLON) {
       u_fungetc(c, f);
	   /*u_fungetc(c, f, status);*/
      return tok_string;
    }
  }
}

/* Retrieve the next character, ignoring comments.  If skipwhite is
   true, whitespace is skipped as well. */
static UChar getNextChar(UFILE *f,
			 bool_t skipwhite, 
			 UErrorCode *status)
{
  UChar c;

  if(U_FAILURE(*status)) return U_EOF;
  
  for(;;) {
    c = u_fgetc(f);
    /*c = u_fgetc(f, status);*/
    if(c == (UChar)U_EOF) return U_EOF;
    
    if(skipwhite && isWhitespace(c)) 
      continue;
    
    /* This also handles the get() failing case */
    if(c != SLASH)
      return c;
    
    c = u_fgetc(f);
    /*	c = u_fgetc(f, status);*/
    if(c == (UChar)U_EOF) return U_EOF;
    
    switch(c) {
    case SLASH:
      seekUntilNewline(f, status);
      break;
      
    case ASTERISK:
      /* Note that we silently ignore an unterminated comment */
      seekUntilEndOfComment(f, status);
      break;
      
    default:
        u_fungetc(c, f);
	    /*u_fungetc(c, f, status);*/
      /* If get() failed this is a NOP */
      return SLASH;
    }
  }
}

void seekUntilNewline(UFILE *f,
		      UErrorCode *status)
{
  UChar c;

  if(U_FAILURE(*status)) return;
  
  do {
    c = u_fgetc(f);
    /*	c = u_fgetc(f, status);*/
  } while(! isNewline(c) && c != (UChar)U_EOF && *status == U_ZERO_ERROR);
  
  /*if(U_FAILURE(*status))
    err = kItemNotFound;*/
}

void seekUntilEndOfComment(UFILE *f,
			   UErrorCode *status)
{
  UChar c, d;

  if(U_FAILURE(*status)) return;

  do {
    c = u_fgetc(f);
    /*	c = u_fgetc(f, status);*/
    if(c == ASTERISK) {
        d = u_fgetc(f);
        /*	d = u_fgetc(f, status);*/
      if(d != SLASH)
	    u_fungetc(d, f);
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

static UChar unescape(UFILE *f,
		      UErrorCode *status)
{
  UChar c;
  UChar out;
  int16_t maxChars;
  
  if(U_FAILURE(*status)) return U_EOF;
  
    c = u_fgetc(f);
    /*	c = u_fgetc(f, status);*/
  if(c == (UChar)U_EOF || U_FAILURE(*status)) return U_EOF;

  switch (c) {
    
    /* '\t' or '\T' causes a tab character to be written to the output */
  case 0x0074: case 0x0054:
    return 0x0009;
    
    /* '\n' or '\N' causes a line feed to be written to the output */
  case 0x006E: case 0x004E:
    return 0x000A;
    
    /* \x## and \u#### allow characters to be specified by character
     code.  The characters following \x or \u (up to two after \x or
     four after \u) are treated as hexadecimal digits, and the
     hexadecimal number they represent is the numeric character code
     (Latin1 for \x and Unicode for \u) of the character that is
     written to the output.  A character that isn't a valid
     hexadecimal digit terminates the escape sequence (but still gets
     treated independently).  If the sequence evaluates to zero (i.e.,
     either '\x' or '\u' by itself, or '\x00' or '\u0000'), nothing is
     written to the output, which effectively means you can't have
     null characters in the file. */
  case 0x0078: case 0x0058: case 0x0075: case 0x0055:
    if(c == 0x0078 || c == 0x0058)
      maxChars = 2;
    else
      maxChars = 4;
    out = 0;
    while(maxChars != 0 && *status == U_ZERO_ERROR) {
    c = u_fgetc(f);
    /*	c = u_fgetc(f, status);*/
      if(c == (UChar)U_EOF || U_FAILURE(*status)) return U_EOF;
      
      switch(c) {
	/* '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' */
      case 0x0030: case 0x0031: case 0x0032: case 0x0033: case 0x0034:
      case 0x0035: case 0x0036: case 0x0037: case 0x0038: case 0x0039:
	out = (out << 4) + (c - 0x0030);
	break;
	
	/* 'A', 'B', 'C', 'D', 'E', 'F' */
      case 0x0041: case 0x0042: case 0x0043: case 0x0044: case 0x0045: 
      case 0x0046:
	out = (out << 4) + (c - 0x0041 + 10);
	break;

	/* 'a', 'b', 'c', 'd', 'e', 'f' */
      case 0x0061: case 0x0062: case 0x0063: case 0x0064: case 0x0065: 
      case 0x0066:
	out = (out << 4) + (c - 0x0061 + 10);
	break;
	
      default:
	    u_fungetc(c, f);
	    /*u_fungetc(c, f, status);*/
	maxChars = 1;   /* so we fall out of the loop */
	break;
      }
      --maxChars;
    }
    return out;
    
    /* if a backslash preceds any character other than x, u, t, or n,
     that character is just copied to the output as-is (meaning it's
     deprived of any special meaning it otherwise would have had:
     ESCAPE puts a literal backslash in the output stream, for
     example, and QUOTE puts a literal double quote in the output
     stream. */
  default:
    return c;
  }
}

static bool_t isWhitespace(UChar c)
{
  switch (c) {
    /* ' ', '\t', '\n', '\r', 0x2029 */
  case 0x0020: case 0x0009: case 0x000A: case 0x000D:  case 0x2029:
    return TRUE;
    
  default:
    return FALSE;
  }
}

static bool_t isNewline(UChar c)
{
  switch (c) {
    /* '\n', '\r', 0x2029 */
  case 0x000A: case 0x000D: case 0x2029:
    return TRUE;
    
  default:
    return FALSE;
  }
}
