/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File uscnnf_p.c
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/13/99    stephen     Modified for new C API.
*******************************************************************************
*/

#include "uscanf_p.h"
#include "ufmt_cmn.h"

/* flag characters for u_scanf */
#define FLAG_ASTERISK 0x002A
#define FLAG_PAREN 0x0028

#define ISFLAG(s)    (s) == FLAG_ASTERISK || \
            (s) == FLAG_PAREN

/* special characters for u_scanf */
#define SPEC_DOLLARSIGN 0x0024

/* unicode digits */
#define DIGIT_ZERO 0x0030
#define DIGIT_ONE 0x0031
#define DIGIT_TWO 0x0032
#define DIGIT_THREE 0x0033
#define DIGIT_FOUR 0x0034
#define DIGIT_FIVE 0x0035
#define DIGIT_SIX 0x0036
#define DIGIT_SEVEN 0x0037
#define DIGIT_EIGHT 0x0038
#define DIGIT_NINE 0x0039

#define ISDIGIT(s)    (s) == DIGIT_ZERO || \
            (s) == DIGIT_ONE || \
            (s) == DIGIT_TWO || \
            (s) == DIGIT_THREE || \
            (s) == DIGIT_FOUR || \
            (s) == DIGIT_FIVE || \
            (s) == DIGIT_SIX || \
            (s) == DIGIT_SEVEN || \
            (s) == DIGIT_EIGHT || \
            (s) == DIGIT_NINE

/* u_scanf modifiers */
#define MOD_H 0x0068
#define MOD_LOWERL 0x006C
#define MOD_L 0x004C

#define ISMOD(s)    (s) == MOD_H || \
            (s) == MOD_LOWERL || \
            (s) == MOD_L

/* We parse the argument list in Unicode */
int32_t
u_scanf_parse_spec (const UChar     *fmt,
            u_scanf_spec    *spec)
{
  const UChar *s = fmt;
  const UChar *backup;

  /* initialize spec to default values */  
  spec->fArgPos            = -1;
  spec->fSkipArg        = FALSE;

  spec->fInfo.fSpec         = 0x0000;
  spec->fInfo.fWidth         = -1;
  spec->fInfo.fPadChar         = 0x0020;
  spec->fInfo.fIsLongDouble     = FALSE;
  spec->fInfo.fIsShort         = FALSE;
  spec->fInfo.fIsLong         = FALSE;
  spec->fInfo.fIsLongLong    = FALSE;


  /* skip over the initial '%' */
  *s++;

  /* Check for positional argument */
  if(ISDIGIT(*s)) {

    /* Save the current position */
    backup = s;
    
    /* handle positional parameters */
    if(ISDIGIT(*s)) {
      spec->fArgPos = (int) (*s++ - DIGIT_ZERO);
      
      while(ISDIGIT(*s)) {
    spec->fArgPos *= 10;
    spec->fArgPos += (int) (*s++ - DIGIT_ZERO);
      }
    }
    
    /* if there is no '$', don't read anything */
    if(*s != SPEC_DOLLARSIGN) {
      spec->fArgPos = -1;
      s = backup;
    }
    /* munge the '$' */
    else
      *s++;
  }
  
  /* Get any format flags */
  while(ISFLAG(*s)) {
    switch(*s++) {
      
      /* skip argument */
    case FLAG_ASTERISK:
      spec->fSkipArg = TRUE;
      break;

      /* pad character specified */
    case FLAG_PAREN:

      /* first four characters are hex values for pad char */
      spec->fInfo.fPadChar = ufmt_digitvalue(*s++);
      spec->fInfo.fPadChar *= 16;
      spec->fInfo.fPadChar += ufmt_digitvalue(*s++);
      spec->fInfo.fPadChar *= 16;
      spec->fInfo.fPadChar += ufmt_digitvalue(*s++);
      spec->fInfo.fPadChar *= 16;
      spec->fInfo.fPadChar += ufmt_digitvalue(*s++);
      
      /* final character is ignored */
      *s++;
      
      break;
    }
  }

  /* Get the width */
  if(ISDIGIT(*s)){
    spec->fInfo.fWidth = (int) (*s++ - DIGIT_ZERO);
    
    while(ISDIGIT(*s)) {
      spec->fInfo.fWidth *= 10;
      spec->fInfo.fWidth += (int) (*s++ - DIGIT_ZERO);
    }
  }
  
  /* Get any modifiers */
  if(ISMOD(*s)) {
    switch(*s++) {

      /* short */
    case MOD_H:
      spec->fInfo.fIsShort = TRUE;
      break;

      /* long or long long */
    case MOD_LOWERL:
      if(*s == MOD_LOWERL) {
    spec->fInfo.fIsLongLong = TRUE;
    /* skip over the next 'l' */
    *s++;
      }
      else
    spec->fInfo.fIsLong = TRUE;
      break;
      
      /* long double */
    case MOD_L:
      spec->fInfo.fIsLongDouble = TRUE;
      break;
    }
  }

  /* finally, get the specifier letter */
  spec->fInfo.fSpec = *s++;

  /* return # of characters in this specifier */
  return (s - fmt);
}
