/*
   ********************************************************************************
   *                                                                              *
   * COPYRIGHT:                                                                   *
   *   (C) Copyright International Business Machines Corporation, 1999            *
   *   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
   *   US Government Users Restricted Rights - Use, duplication, or disclosure    *
   *   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
   *                                                                              *
   ********************************************************************************
   *
   *
   *  ucnv_io.h:
   *  defines  variables and functions pertaining to file access, and name resolution
   *  aspect of the library
 */

#ifndef UCNV_IO_H
#define UCNV_IO_H

#include "utypes.h"

/**
 * Map a converter alias name to a canonical converter name.
 * The alias is searched for case-insensitively, the converter name
 * is returned in mixed-case.
 * Returns NULL if the alias is not found.
 */
U_CFUNC const char *
ucnv_io_getConverterName(const char *alias, UErrorCode *pErrorCode);

/**
 * Search case-insensitively for a converter alias and set aliases to
 * a pointer to the list of aliases for the actual converter.
 * The first "alias" is the canonical converter name.
 * The aliases are stored consecutively, in mixed case, each NUL-terminated.
 * There are as many strings in this list as the return value specifies.
 * Returns the number of aliases including the canonical converter name,
 * or 0 if the alias is not found.
 */
U_CFUNC uint16_t
ucnv_io_getAliases(const char *alias, const char **aliases, UErrorCode *pErrorCode);

/**
 * Search case-insensitively for a converter alias and return
 * the (index)th alias.
 * Returns NULL if the alias is not found.
 */
U_CFUNC const char *
ucnv_io_getAlias(const char *alias, uint16_t index, UErrorCode *pErrorCode);

/**
 * Return the number of all aliases (and converter names).
 */
U_CFUNC uint16_t
ucnv_io_countAvailableAliases(UErrorCode *pErrorCode);

/**
 * Return the (index)th alias or converter name in mixed case, or NULL
 * if there is none (typically, if the data cannot be loaded).
 * 0<=index<=ucnv_io_countAvailableAliases().
 */
U_CFUNC const char *
ucnv_io_getAvailableAlias(uint16_t index, UErrorCode *pErrorCode);

/**
 * Fill an array const char *aliases[ucnv_io_countAvailableAliases()]
 * with pointers to all aliases and converter names in mixed-case.
 */
U_CFUNC void
ucnv_io_fillAvailableAliases(const char **aliases, UErrorCode *pErrorCode);

/**
 * Get the name of the default converter.
 * This name is already resolved by <code>ucnv_io_getConverterName()</code>.
 */
U_CFUNC const char *
ucnv_io_getDefaultConverterName();

/**
 * Set the name of the default converter.
 */
U_CFUNC void
ucnv_io_setDefaultConverterName(const char *name);

#endif /* _UCNV_IO */
