/*
******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File uscanset.c
*
* Modification History:
*
*   Date        Name        Description
*   12/03/98    stephen        Creation.
*   03/13/99    stephen     Modified for new C API.
******************************************************************************
*/

#include "uscanset.h"


static UBool
u_scanf_scanset_add(u_scanf_scanset     *scanset,
                    UChar         c)
{
    if(scanset->single_count == U_SCANF_MAX_SCANSET_SIZE - 1)
        return FALSE;

    scanset->singles[scanset->single_count++] = c;

    return TRUE;
}

static UBool
u_scanf_scanset_addrange(u_scanf_scanset     *scanset,
                         UChar             start,
                         UChar             end)
{
    if(scanset->pair_count == U_SCANF_MAX_SCANSET_SIZE - 1)
        return FALSE;

    if (end < start) {
        /* swap chars */
        UChar temp = end;
        end = start;
        start = temp;
    }

    scanset->pairs[scanset->pair_count].start = start;
    scanset->pairs[scanset->pair_count].end   = end;
    scanset->pair_count++;

    return TRUE;
}

UBool
u_scanf_scanset_init(u_scanf_scanset     *scanset,
                     const UChar    *s,
                     int32_t        *len)
{
    UChar       c;
    const UChar *limit;
    int32_t     count;
    UBool       result = FALSE;


    /* set up parameters */
    limit = s + *len;
    count = 0;

    /* initialize to defaults */
    scanset->single_count = 0;
    scanset->pair_count     = 0;
    scanset->is_inclusive = TRUE;

    /* check to see if this is an inclusive or exclusive scanset */
    if(*s == 0x005E) { /* '^' */
        scanset->is_inclusive = FALSE;

        /* increment s and count */
        ++s;
        ++count;
    }

    /* if ']' is the first character, add it */
    else if(*s == 0x005D) {
        result = u_scanf_scanset_add(scanset, *s++);

        /* increment count */
        ++count;
    }

    /* if the first character is '^' and the second is ']', add ']' */
    if( ! scanset->is_inclusive && *s == 0x005D) {
        result = u_scanf_scanset_add(scanset, *s++);

        /* increment count */
        ++count;
    }

    /* add characters until a ']' is seen, adding ranges as necessary */
    while(s < limit) {

        /* grab the current character */
        c = *s++;

        /* if it's a ']', we're done */
        if(c == 0x005D)
            break;

        /* check if this is a range */
        if(*s == 0x002D && *(s+1) != 0x005D) {
            result = u_scanf_scanset_addrange(scanset, c, *(s+1));

            /* increment count and s */
            s       += 2;
            count   += 2;
        }
        else {
            /* otherwise, just add the character */
            result = u_scanf_scanset_add(scanset, c);
        }

        /* increment count */
        ++count;
    }

    /* update length to reflect # of characters consumed */
    *len = count;
    return result;
}

UBool
u_scanf_scanset_in(u_scanf_scanset     *scanset,
                   UChar         c)
{
    int i;

    /* if this is an inclusive scanset, make sure c is in it */
    if(scanset->is_inclusive) {

        /* check the single chars first*/
        for(i = 0; i < scanset->single_count; ++i) {
            if(c == scanset->singles[i]) {
                return TRUE;
            }
        }

        /* check the pairs */
        for(i = 0; i < scanset->pair_count; ++i) {
            if(c >= scanset->pairs[i].start && c <= scanset->pairs[i].end) {
                return TRUE;
            }
        }

        /* didn't find it, so c isn't in set */
        return FALSE;
    }

    /* otherwise, make sure c isn't in it */
    else {

        /* check the single chars first*/
        for(i = 0; i < scanset->single_count; ++i) {
            if(c == scanset->singles[i]) {
                return FALSE;
            }
        }

        /* check the pairs */
        for(i = 0; i < scanset->pair_count; ++i) {
            if(c >= scanset->pairs[i].start && c <= scanset->pairs[i].end) {
                return FALSE;
            }
        }

        /* didn't find it, so c is in set */
        return TRUE;
    }
}








