/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File locbund.h
*
* Modification History:
*
*   Date        Name        Description
*   10/16/98    stephen     Creation.
*   02/25/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef LOCBUND_H
#define LOCBUND_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/unum.h"
#include "unicode/udat.h"

#define ULOCALEBUNDLE_NUMBERFORMAT_COUNT ((int32_t)UNUM_SPELLOUT)

typedef struct ULocaleBundle {
    char            *fLocale;

    UNumberFormat   *fNumberFormat[ULOCALEBUNDLE_NUMBERFORMAT_COUNT];

    UDateFormat     *fDateFormat;
    UDateFormat     *fTimeFormat;
} ULocaleBundle;


/**
 * Initialize a ULocaleBundle, initializing all formatters to 0.
 * @param result A ULocaleBundle to initialize.
 * @param loc The locale of the ULocaleBundle.
 * @return A pointer to a ULocaleBundle, or 0 if <TT>loc</TT> was invalid.
 */
ULocaleBundle*        
u_locbund_init(ULocaleBundle *result, const char *loc);

/**
 * Create a new ULocaleBundle, initializing all formatters to 0.
 * @param loc The locale of the ULocaleBundle.
 * @return A pointer to a ULocaleBundle, or 0 if <TT>loc</TT> was invalid.
 */
/*ULocaleBundle*
u_locbund_new(const char *loc);*/

/**
 * Create a deep copy of this ULocaleBundle;
 * @param bundle The ULocaleBundle to clone.
 * @return A new ULocaleBundle.
 */
/*ULocaleBundle*
u_locbund_clone(const ULocaleBundle *bundle);*/

/**
 * Delete the specified ULocaleBundle, freeing all associated memory.
 * @param bundle The ULocaleBundle to delete
 */
void
u_locbund_close(ULocaleBundle *bundle);

/**
 * Get the NumberFormat used to format and parse numbers in a ULocaleBundle.
 * @param bundle The ULocaleBundle to use
 * @return A pointer to the NumberFormat used for number formatting and parsing.
 */
UNumberFormat*        
u_locbund_getNumberFormat(ULocaleBundle *bundle, UNumberFormatStyle style);

/**
 * Get the DateFormat used to format and parse dates in a ULocaleBundle.
 * @param bundle The ULocaleBundle to use
 * @return A pointer to the DateFormat used for date formatting and parsing.
 */
UDateFormat*
u_locbund_getDateFormat(ULocaleBundle *bundle);

/**
 * Get the DateFormat used to format and parse times in a ULocaleBundle.
 * @param bundle The ULocaleBundle to use
 * @return A pointer to the DateFormat used for time formatting and parsing.
 */
UDateFormat*
u_locbund_getTimeFormat(ULocaleBundle *bundle);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
