/*
*******************************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

#ifndef UCOLEITR_H
#define UCOLEITR_H

/** This indicates the last element in a UCollationElements has been consumed. 
 *
 */
#define UCOL_NULLORDER        0xFFFFFFFF

#include "unicode/ucol.h"

/** The UCollationElements struct.
 *  For usage in C programs.
 */
typedef void * UCollationElements;

/**
 * The UCollationElements  is used as an iterator to walk through
 * each character of an international string. Use the iterator to return the
 * ordering priority of the positioned character. The ordering priority of
 * a character, which we refer to as a key, defines how a character is
 * collated in the given collation object.
 * For example, consider the following in Spanish:
 * <pre>
 * .       "ca" -> the first key is key('c') and second key is key('a').
 * .       "cha" -> the first key is key('ch') and second key is key('a').
 * </pre>
 * And in German,
 * <pre>
 * .       "æb"-> the first key is key('a'), the second key is key('e'), and
 * .       the third key is key('b').
 * </pre>
 * The key of a character, is an const UCOL_PRIMARYMASK, UCOL_SECONDARY_MASK,
 * UCOL_TERTIARYMASK.    
 * <p>Example of the iterator usage: (without error checking)
 * <pre>
 * .  void CollationElementIterator_Example()
 * .  {
 * .      UChar *s;
 * .      t_int32 order, primaryOrder;
 * .      UCollationElements *c;
 * .      UCollatorOld *coll;
 * .      UErrorCode success = U_ZERO_ERROR;
 * .      s=(UChar*)malloc(sizeof(UChar) * (strlen("This is a test")+1) );
 * .      u_uastrcpy(s, "This is a test");
 * .      coll = ucol_open(NULL, &success);
 * .      c = ucol_openElements(coll, str, u_strlen(str), &status);
 * .      order = ucol_next(c, &success);
 * .      primaryOrder = order & UCOL_PRIMARYMASK;
 * .      free(s);
 * .      ucol_close(coll);
 * .      ucol_closeElements(c);
 * .  }
 * </pre>
 * <p>
 * ucol_next() returns the collation order of the next
 * character based on the comparison level of the collator.  A collation order 
 * consists of primary order, secondary order and tertiary order.  The data 
 * type of the collation order is <strong>t_int32</strong>.  The first 16 bits of 
 * a collation order is its primary order; the next 8 bits is the secondary 
 * order and the last 8 bits is the tertiary order.
 *
 * @see                Collator
 */

/**
 * Open the collation elements for a string.
 *
 * @param coll The collator containing the desired collation rules.
 * @param text The text to iterate over.
 * @param textLength The number of characters in text, or -1 if null-terminated
 * @param status A pointer to an UErrorCode to receive any errors.
 * @stable
 */
U_CAPI UCollationElements*
ucol_openElements(    const    UCollator       *coll,
            const    UChar           *text,
            int32_t                  textLength,
            UErrorCode         *status);

/**
 * get a hash code for a key... Not very useful!
 * @deprecated
 */
U_CAPI int32_t
ucol_keyHashCode(const uint8_t* key, int32_t length);

/**
 * Close a UCollationElements.
 * Once closed, a UCollationElements may no longer be used.
 * @param elems The UCollationElements to close.
 * @stable
 */
U_CAPI void
ucol_closeElements(UCollationElements *elems);

/**
 * Reset the collation elements to their initial state.
 * This will move the 'cursor' to the beginning of the text.
 * @param elems The UCollationElements to reset.
 * @see ucol_next
 * @see ucol_previous
 * @stable
 */
U_CAPI void
ucol_reset(UCollationElements *elems);

/**
 * Get the ordering priority of the next collation element in the text.
 * A single character may contain more than one collation element.
 * @param elems The UCollationElements containing the text.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The next collation elements ordering, or \Ref{UCOL_NULLORDER} if the
 * end of the text is reached.
 * @stable
 */
U_CAPI int32_t
ucol_next(    UCollationElements    *elems,
        UErrorCode        *status);

/**
 * Get the ordering priority of the previous collation element in the text.
 * A single character may contain more than one collation element.
 * @param elems The UCollationElements containing the text.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The previous collation elements ordering, or \Ref{UCOL_NULLORDER}
 * if the end of the text is reached.
 * @stable
 */
U_CAPI int32_t
ucol_previous(    UCollationElements    *elems,
        UErrorCode        *status);

/**
 * Get the maximum length of any expansion sequences that end with the 
 * specified comparison order.
 * This is useful for .... ?
 * @param elems The UCollationElements containing the text.
 * @param order A collation order returned by previous or next.
 * @return The maximum length of any expansion sequences ending with the 
 * specified order.
 * @stable
 */
U_CAPI int32_t
ucol_getMaxExpansion(    const    UCollationElements    *elems,
            int32_t                order);

/**
 * Set the text containing the collation elements.
 * This 
 * @param elems The UCollationElements to set.
 * @param text The source text containing the collation elements.
 * @param textLength The length of text, or -1 if null-terminated.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @see ucol_getText
 * @stable
 */
U_CAPI void
ucol_setText(    UCollationElements    *elems,
        const    UChar        *text,
        int32_t            textLength,
        UErrorCode        *status);

/**
 * Get the offset of the current source character.
 * This is an offset into the text of the character containing the current
 * collation elements.
 * @param elems The UCollationElements to query.
 * @return The offset of the current source character.
 * @see ucol_setOffset
 * @stable
 */
U_CAPI UTextOffset
ucol_getOffset(const UCollationElements *elems);

/**
 * Set the offset of the current source character.
 * This is an offset into the text of the character to be processed.
 * @param elems The UCollationElements to set.
 * @param offset The desired character offset.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @see ucol_getOffset
 * @stable
 */
U_CAPI void
ucol_setOffset(    UCollationElements    *elems,
        UTextOffset        offset,
        UErrorCode        *status);

#endif
