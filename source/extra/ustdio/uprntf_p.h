/*
******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File uprntf_p.h
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
******************************************************************************
*/

#ifndef UPRNTF_P_H
#define UPRNTF_P_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "uprintf.h"

#define UP_PERCENT 0x0025

/**
 * Parse a single u_printf format specifier.
 * @param fmt A pointer to a '%' character in a u_printf format specification.
 * @param spec A pointer to a <TT>u_printf_spec</TT> to receive the parsed
 * format specifier.
 * @return The number of characters contained in this specifier.
 */
int32_t
u_printf_print_spec(const u_printf_stream_handler *streamHandler,
                    const UChar     *fmt,
                    void            *context,
                    ULocaleBundle   *formatBundle,
                    int32_t         patCount,
                    int32_t         *written,
                    va_list         *ap);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
