/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File uprntf_p.c
*
* Modification History:
*
*   Date        Name        Description
*   11/23/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "uprntf_p.h"
#include "ufmt_cmn.h"

/* flag characters for uprintf */
#define FLAG_MINUS 0x002D
#define FLAG_PLUS 0x002B
#define FLAG_SPACE 0x0020
#define FLAG_POUND 0x0023
#define FLAG_ZERO  0x0030
#define FLAG_PAREN 0x0028

#define ISFLAG(s)    (s) == FLAG_MINUS || \
            (s) == FLAG_PLUS || \
            (s) == FLAG_SPACE || \
            (s) == FLAG_POUND || \
            (s) == FLAG_ZERO || \
            (s) == FLAG_PAREN

/* special characters for uprintf */
#define SPEC_ASTERISK 0x002A
#define SPEC_DOLLARSIGN 0x0024
#define SPEC_PERIOD 0x002E
#define SPEC_PERCENT 0x0025

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

/* u_printf modifiers */
#define MOD_H 0x0068
#define MOD_LOWERL 0x006C
#define MOD_L 0x004C

#define ISMOD(s)    (s) == MOD_H || \
            (s) == MOD_LOWERL || \
            (s) == MOD_L

/* We parse the argument list in Unicode */
int32_t
u_printf_parse_spec (const UChar     *fmt,
             u_printf_spec    *spec)
{
    const UChar *s = fmt;
    const UChar *backup;
    u_printf_spec_info *info = &(spec->fInfo);

    /* initialize spec to default values */
    spec->fWidthPos     = -1;
    spec->fPrecisionPos = -1;
    spec->fArgPos       = -1;

    info->fPrecision    = -1;
    info->fWidth        = -1;
    info->fSpec         = 0x0000;
    info->fPadChar      = 0x0020;
    info->fAlt          = FALSE;
    info->fSpace        = FALSE;
    info->fLeft         = FALSE;
    info->fShowSign     = FALSE;
    info->fZero         = FALSE;
    info->fIsLongDouble = FALSE;
    info->fIsShort      = FALSE;
    info->fIsLong       = FALSE;
    info->fIsLongLong   = FALSE;

    /* skip over the initial '%' */
    s++;

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
            s++;
    }

    /* Get any format flags */
    while(ISFLAG(*s)) {
        switch(*s++) {

            /* left justify */
        case FLAG_MINUS:
            info->fLeft = TRUE;
            break;

            /* always show sign */
        case FLAG_PLUS:
            info->fShowSign = TRUE;
            break;

            /* use space if no sign present */
        case FLAG_SPACE:
            info->fShowSign = TRUE;
            info->fSpace = TRUE;
            break;

            /* use alternate form */
        case FLAG_POUND:
            info->fAlt = TRUE;
            break;

            /* pad with leading zeroes */
        case FLAG_ZERO:
            info->fZero = TRUE;
            info->fPadChar = 0x0030;
            break;

            /* pad character specified */
        case FLAG_PAREN:

            /* first four characters are hex values for pad char */
            info->fPadChar = (UChar)ufmt_digitvalue(*s++);
            info->fPadChar = (UChar)((info->fPadChar * 16) + ufmt_digitvalue(*s++));
            info->fPadChar = (UChar)((info->fPadChar * 16) + ufmt_digitvalue(*s++));
            info->fPadChar = (UChar)((info->fPadChar * 16) + ufmt_digitvalue(*s++));

            /* final character is ignored */
            s++;

            break;
        }
    }

    /* Get the width */

    /* width is specified out of line */
    if(*s == SPEC_ASTERISK) {

        info->fWidth = -2;

        /* Skip the '*' */
        s++;

        /* Save the current position */
        backup = s;

        /* handle positional parameters */
        if(ISDIGIT(*s)) {
            spec->fWidthPos = (int) (*s++ - DIGIT_ZERO);

            while(ISDIGIT(*s)) {
                spec->fWidthPos *= 10;
                spec->fWidthPos += (int) (*s++ - DIGIT_ZERO);
            }
        }

        /* if there is no '$', don't read anything */
        if(*s != SPEC_DOLLARSIGN) {
            spec->fWidthPos = -1;
            s = backup;
        }
        /* munge the '$' */
        else
            s++;
    }
    /* read the width, if present */
    else if(ISDIGIT(*s)){
        info->fWidth = (int) (*s++ - DIGIT_ZERO);

        while(ISDIGIT(*s)) {
            info->fWidth *= 10;
            info->fWidth += (int) (*s++ - DIGIT_ZERO);
        }
    }

    /* Get the precision */

    if(*s == SPEC_PERIOD) {

        /* eat up the '.' */
        s++;

        /* precision is specified out of line */
        if(*s == SPEC_ASTERISK) {

            info->fPrecision = -2;

            /* Skip the '*' */
            s++;

            /* save the current position */
            backup = s;

            /* handle positional parameters */
            if(ISDIGIT(*s)) {
                spec->fPrecisionPos = (int) (*s++ - DIGIT_ZERO);

                while(ISDIGIT(*s)) {
                    spec->fPrecisionPos *= 10;
                    spec->fPrecisionPos += (int) (*s++ - DIGIT_ZERO);
                }

                /* if there is no '$', don't read anything */
                if(*s != SPEC_DOLLARSIGN) {
                    spec->fPrecisionPos = -1;
                    s = backup;
                }
                else {
                    /* munge the '$' */
                    s++;
                }
            }
        }
        /* read the precision */
        else if(ISDIGIT(*s)){
            info->fPrecision = (int) (*s++ - DIGIT_ZERO);

            while(ISDIGIT(*s)) {
                info->fPrecision *= 10;
                info->fPrecision += (int) (*s++ - DIGIT_ZERO);
            }
        }
    }

    /* Get any modifiers */
    if(ISMOD(*s)) {
        switch(*s++) {

            /* short */
        case MOD_H:
            info->fIsShort = TRUE;
            break;

            /* long or long long */
        case MOD_LOWERL:
            if(*s == MOD_LOWERL) {
                info->fIsLongLong = TRUE;
                /* skip over the next 'l' */
                s++;
            }
            else
                info->fIsLong = TRUE;
            break;

            /* long double */
        case MOD_L:
            info->fIsLongDouble = TRUE;
            break;
        }
    }

    /* finally, get the specifier letter */
    info->fSpec = *s++;

    /* return # of characters in this specifier */
    return (int32_t)(s - fmt);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
