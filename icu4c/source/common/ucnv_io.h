/*
 **********************************************************************
 *   Copyright (C) 1999-2003, International Business Machines
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
#include "udataswp.h"

#define UCNV_AMBIGUOUS_ALIAS_MAP_BIT 0x8000
#define UCNV_CONVERTER_INDEX_MASK 0xFFF
#define UCNV_NUM_RESERVED_TAGS 2
#define UCNV_NUM_HIDDEN_TAGS 1

/**
 * \var ucnv_io_stripForCompare
 * Remove the underscores, dashes and spaces from the name, and convert
 * the name to lower case.
 * @param dst The destination buffer, which is <= the buffer of name.
 * @param dst The destination buffer, which is <= the buffer of name.
 * @return the destination buffer.
 */
#if U_CHARSET_FAMILY==U_ASCII_FAMILY
#   define ucnv_io_stripForCompare ucnv_io_stripASCIIForCompare
#elif U_CHARSET_FAMILY==U_EBCDIC_FAMILY
#   define ucnv_io_stripForCompare ucnv_io_stripEBCDICForCompare
#else
#   error U_CHARSET_FAMILY is not valid
#endif

U_CFUNC char * U_EXPORT2
ucnv_io_stripASCIIForCompare(char *dst, const char *name);

U_CFUNC char * U_EXPORT2
ucnv_io_stripEBCDICForCompare(char *dst, const char *name);

/**
 * Map a converter alias name to a canonical converter name.
 * The alias is searched for case-insensitively, the converter name
 * is returned in mixed-case.
 * Returns NULL if the alias is not found.
 * @param alias The alias name to be searched.
 * @param pErrorCode The error code
 * @return the converter name in mixed-case, return NULL if the alias is not found.
 */
U_CFUNC const char *
ucnv_io_getConverterName(const char *alias, UErrorCode *pErrorCode);

/**
 * The count for ucnv_io_getAliases and ucnv_io_getAlias
 * @param alias The alias name to be counted
 * @param pErrorCode The error code
 * @return the alias count
 */
U_CFUNC uint16_t
ucnv_io_countAliases(const char *alias, UErrorCode *pErrorCode);

/**
 * Search case-insensitively for a converter alias and set aliases to
 * a pointer to the list of aliases for the actual converter.
 * The first "alias" is the canonical converter name.
 * The aliases are stored consecutively, in mixed case, each NUL-terminated.
 * There are as many strings in this list as the return value specifies.
 * Returns the number of aliases including the canonical converter name,
 * or 0 if the alias is not found.
 * @param alias The canonical converter name
 * @param start 
 * @param aliases A pointer to the list of aliases for the actual converter
 * @return the number of aliases including the canonical converter name, or 0 if the alias is not found.
 */
U_CFUNC uint16_t
ucnv_io_getAliases(const char *alias, uint16_t start, const char **aliases, UErrorCode *pErrorCode);

/**
 * Search case-insensitively for a converter alias and return
 * the (n)th alias.
 * Returns NULL if the alias is not found.
 * @param alias The converter alias
 * @param n The number specifies which alias to get
 * @param pErrorCode The error code
 * @return the (n)th alias and return NULL if the alias is not found.
 */
U_CFUNC const char *
ucnv_io_getAlias(const char *alias, uint16_t n, UErrorCode *pErrorCode);

/**
 * Return the number of all standard names.
 * @param pErrorCode The error code
 * @return the number of all standard names
 */
U_CFUNC uint16_t
ucnv_io_countStandards(UErrorCode *pErrorCode);

/**
 * Return the number of all converter names.
 * @param pErrorCode The error code
 * @return the number of all converter names
 */
U_CFUNC uint16_t
ucnv_io_countAvailableConverters(UErrorCode *pErrorCode);

/**
 * Return the (n)th converter name in mixed case, or NULL
 * if there is none (typically, if the data cannot be loaded).
 * 0<=index<ucnv_io_countAvailableConverters().
 * @param n The number specifies which converter name to get
 * @param pErrorCode The error code
 * @return the (n)th converter name in mixed case, or NULL if there is none.
 */
U_CFUNC const char *
ucnv_io_getAvailableConverter(uint16_t n, UErrorCode *pErrorCode);

/**
 * Return the (n)th converter name in mixed case, or NULL
 * if there is none (typically, if the data cannot be loaded).
 * 0<=index<ucnv_io_countAvailableConverters().
 */
U_CFUNC void
ucnv_io_flushAvailableConverterCache(void);

/**
 * Return the number of all aliases (and converter names).
 * @param pErrorCode The error code
 * @return the number of all aliases
 */
U_CFUNC uint16_t
ucnv_io_countAvailableAliases(UErrorCode *pErrorCode);

/**
 * Get the name of the default converter.
 * This name is already resolved by <code>ucnv_io_getConverterName()</code>.
 * @return the name of the default converter
 */
U_CFUNC const char *
ucnv_io_getDefaultConverterName(void);

/**
 * Set the name of the default converter.
 * @param name The name set to the default converter
 */
U_CFUNC void
ucnv_io_setDefaultConverterName(const char *name);

/**
 * Swap an ICU converter alias table. See ucnv_io.c.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
ucnv_swapAliases(const UDataSwapper *ds,
                 const void *inData, int32_t length, void *outData,
                 UErrorCode *pErrorCode);

#endif /* _UCNV_IO */

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
