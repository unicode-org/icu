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

U_CFUNC const char *
ucnv_io_getConverterName(const char *alias, UErrorCode *pErrorCode);

U_CFUNC uint16_t
ucnv_io_getAliases(const char *alias, const char **aliases, UErrorCode *pErrorCode);

U_CFUNC const char *
ucnv_io_getAlias(const char *alias, uint16_t index, UErrorCode *pErrorCode);

U_CFUNC uint16_t
ucnv_io_countAvailableAliases(UErrorCode *pErrorCode);

U_CFUNC const char *
ucnv_io_getAvailableAlias(uint16_t index, UErrorCode *pErrorCode);

U_CFUNC void
ucnv_io_fillAvailableAliases(const char **aliases, UErrorCode *pErrorCode);

#endif /* _UCNV_IO */
