/*
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
 *  ucnv_cb.h:
 *  External APIs for the ICU's codeset conversion library
 *  Helena Shih
 * 
 * Modification History:
 *
 *   Date        Name        Description
 */

/**
 * \file 
 * \brief Description of converter callback C API
 *
 * <h2> Callback API for ICU's codeset conversion libray </h2>
 *
 */

#ifndef UCNV_CB_H
#define UCNV_CB_H

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"

/**
 * Used by FromU callback functions.  Writes out the specified byte output bytes to the target
 * byte buffer or to converter internal buffers.
 *
 * @param args callback fromUnicode arguments
 * @param source source bytes to write
 * @param length length of bytes to write
 * @param offsetIndex the relative offset index from callback.
 * @param err error status. If <TT>U_BUFFER_OVERFLOW</TT> is returned, then U_BUFFER_OVERFLOW <STRONG>must</STRONG> 
 * be returned to the user, because it means that not all data could be written into the target buffer, and some is 
 * in the converter error buffer.
 * @see ucnv_cbFromUWriteSub
 * @draft
 */

U_CAPI void U_EXPORT2
ucnv_cbFromUWriteBytes (UConverterFromUnicodeArgs *args,
                        const char* source,
                        int32_t length,
                        int32_t offsetIndex,
                        UErrorCode * err);

/**
 * Used by FromU callback functions.  This function will write out the
 * correct substitution character sequence to the target.
 *
 * @param args callback fromUnicode arguments
 * @param offsetIndex the relative offset index from the current source pointer to be used
 * @param err error status. If <TT>U_BUFFER_OVERFLOW</TT> is returned, then U_BUFFER_OVERFLOW <STRONG>must</STRONG> 
 * be returned to the user, because it means that not all data could be written into the target buffer, and some is 
 * in the converter error buffer.
 * @see ucnv_cbFromUWriteBytes
 * @draft
 */

U_CAPI void U_EXPORT2 
ucnv_cbFromUWriteSub (UConverterFromUnicodeArgs *args,
                      int32_t offsetIndex,
                      UErrorCode * err);

/**
 * Used by the callback functions.  This function will write out the error
 * character(s) to the target UChar buffer.
 *
 * @param args : callback fromUnicode arguments
 * @param source : pointer to pointer to first UChar to write [on exit: 1 after last UChar processed]
 * @param sourceLimit : pointer after last UChar to write
 * @param offsetIndex : the relative offset index from callback.
 * @param err error status <TT>U_BUFFER_OVERFLOW</TT>
 * @see ucnv_cbToUWriteSub
 * @draft
 */
U_CAPI void U_EXPORT2 ucnv_cbFromUWriteUChars(UConverterFromUnicodeArgs *args,
                             const UChar** source,
                             const UChar*  sourceLimit,
                             int32_t offsetIndex,
                             UErrorCode * err);

/**
 * Used by the callback functions.  This function will write out the error
 * character(s) to the target UChar buffer.
 *
 * @param args : callback toUnicode arguments
 * @param target : output target buffer of the callback result.
 * @param length : the length of target buffer
 * @param offsetIndex : the relative offset index from callback.
 * @param err error status <TT>U_BUFFER_OVERFLOW</TT>
 * @see ucnv_cbToUWriteSub
 * @draft
 */

U_CAPI void U_EXPORT2 ucnv_cbToUWriteUChars (UConverterToUnicodeArgs *args,
                                             const UChar* source,
                                             int32_t length,
                                             int32_t offsetIndex,
                                             UErrorCode * err);

/**
 * Used by the callback functions.  This function will write out the error
 * character(s) to the substitution character.
 *
 * @param args : callback fromUnicode arguments
 * @param offsetIndex : the relative offset index from callback.
 * @param err error status <TT>U_BUFFER_OVERFLOW</TT>
 * @see ucnv_cbToUWriteUChars
 * @draft
 */

U_CAPI void U_EXPORT2 ucnv_cbToUWriteSub (UConverterToUnicodeArgs *args,
                       int32_t offsetIndex,
                       UErrorCode * err);
#endif

