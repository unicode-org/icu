/*
   **********************************************************************
   *   Copyright (C) 1999, International Business Machines
   *   Corporation and others.  All Rights Reserved.
   **********************************************************************
   *
   *
   *  ucnv_io.h:
   *  defines  variables and functions pertaining to file access, and name resolution
   *  aspect of the library
 */

#ifndef UCNV_IO_H
#define UCNV_IO_H

#include "unicode/utypes.h"

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
 * the (n)th alias.
 * Returns NULL if the alias is not found.
 */
U_CFUNC const char *
ucnv_io_getAlias(const char *alias, uint16_t n, UErrorCode *pErrorCode);

/**
 * Return the number of all standard names.
 */
U_CFUNC uint16_t
ucnv_io_countStandards(UErrorCode *pErrorCode);

/**
 * Return the (n)th standard name in mixed case, or NULL
 * if there is none (typically, if the data cannot be loaded).
 * 0 <= index < ucnv_io_countStandards().
 */
U_CFUNC const char *
ucnv_io_getStandard(uint16_t n, UErrorCode *pErrorCode);

/**
 * Map a converter alias name to a canonical converter name according to
 * a given standard.
 * The alias and standard are searched for case-insensitively, the converter
 * name is returned in mixed-case.
 * Returns NULL if the alias is not found, the standard does not exist, or
 * the standard does not specify a name for the alias.
 */
U_CFUNC const char *
ucnv_io_getStandardName(const char *alias, const char *standard, UErrorCode *pErrorCode);

/**
 * Return the number of all converter names.
 */
U_CFUNC uint16_t
ucnv_io_countAvailableConverters(UErrorCode *pErrorCode);

/**
 * Return the (n)th converter name in mixed case, or NULL
 * if there is none (typically, if the data cannot be loaded).
 * 0<=index<ucnv_io_countAvailableConverters().
 */
U_CFUNC const char *
ucnv_io_getAvailableConverter(uint16_t n, UErrorCode *pErrorCode);

/**
 * Fill an array const char *aliases[ucnv_io_countAvailableConverters()]
 * with pointers to all converter names in mixed-case.
 */
U_CFUNC void
ucnv_io_fillAvailableConverters(const char **aliases, UErrorCode *pErrorCode);

/**
 * Return the number of all aliases (and converter names).
 */
U_CFUNC uint16_t
ucnv_io_countAvailableAliases(UErrorCode *pErrorCode);

/**
 * Return the (n)th alias or converter name in mixed case, or NULL
 * if there is none (typically, if the data cannot be loaded).
 * 0<=index<ucnv_io_countAvailableAliases().
 */
U_CFUNC const char *
ucnv_io_getAvailableAlias(uint16_t n, UErrorCode *pErrorCode);

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
ucnv_io_getDefaultConverterName(void);

/**
 * Set the name of the default converter.
 */
U_CFUNC void
ucnv_io_setDefaultConverterName(const char *name);

/**
 * Lexically compare two converter names/aliases, ignoring case and
 * the delimiters '-', '_', and ' '.
 */
U_CFUNC int
charsetNameCmp(const char *name1, const char *name2);

#endif /* _UCNV_IO */

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
