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

struct ULocaleBundle {
  char         *fLocale;
  
  UNumberFormat    *fNumberFormat;
  UNumberFormat    *fPercentFormat;
  UNumberFormat    *fCurrencyFormat;
  UNumberFormat    *fScientificFormat;
  UNumberFormat    *fSpelloutFormat;

  UDateFormat    *fDateFormat;
  UDateFormat    *fTimeFormat;
};
typedef struct ULocaleBundle ULocaleBundle;


/**
 * Create a new ULocaleBundle, initializing all formatters to 0.
 * @param loc The locale of the ULocaleBundle.
 * @return A pointer to a ULocaleBundle, or 0 if <TT>loc</TT> was invalid.
 */
ULocaleBundle*
u_locbund_new(const char *loc);

/**
 * Create a deep copy of this ULocaleBundle;
 * @param bundle The ULocaleBundle to clone.
 * @return A new ULocaleBundle.
 */
ULocaleBundle*
u_locbund_clone(const ULocaleBundle *bundle);

/**
 * Delete the specified ULocaleBundle, freeing all associated memory.
 * @param bundle The ULocaleBundle to delete
 */
void
u_locbund_delete(ULocaleBundle *bundle);

/**
 * Get the NumberFormat used to format and parse numbers in a ULocaleBundle.
 * @param bundle The ULocaleBundle to use
 * @return A pointer to the NumberFormat used for number formatting and parsing.
 */
UNumberFormat*        
u_locbund_getNumberFormat(ULocaleBundle *bundle);

/**
 * Get the NumberFormat used to format and parse percents in a ULocaleBundle.
 * @param bundle The ULocaleBundle to use
 * @return A pointer to the NumberFormat used for percent formatting and parsing.
 */
UNumberFormat*        
u_locbund_getPercentFormat(ULocaleBundle *bundle);

/**
 * Get the NumberFormat used to format and parse currency in a ULocaleBundle.
 * @param bundle The ULocaleBundle to use
 * @return A pointer to the NumberFormat used for currency formatting and parsing.
 */
UNumberFormat*    
u_locbund_getCurrencyFormat(ULocaleBundle *bundle);

/**
 * Get the NumberFormat used to format and parse scientific numbers in a ULocaleBundle.
 * @param bundle The ULocaleBundle to use
 * @return A pointer to the NumberFormat used for scientific formatting and parsing.
 */
UNumberFormat*    
u_locbund_getScientificFormat(ULocaleBundle *bundle);

/**
 * Get the NumberFormat used format to and parse spelled-out numbers in a ULocaleBundle.
 * @param bundle The ULocaleBundle to use
 * @return A pointer to the NumberFormat used for spelled-out number formatting and parsing.
 */
UNumberFormat*
u_locbund_getSpelloutFormat(ULocaleBundle *bundle);

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
