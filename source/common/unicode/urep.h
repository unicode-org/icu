/*
*******************************************************************************
*   Copyright (C) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   Date        Name        Description
*   06/23/00    aliu        Creation.
*******************************************************************************
*/

#ifndef UREP_H
#define UREP_H

#include "unicode/utypes.h"

/********************************************************************
 * General Notes
 ********************************************************************
 * TODO
 * Add usage scenario
 * Add test code
 * Talk about pinning
 * Talk about "can truncate result if out of memory"
 */

/********************************************************************
 * Data Structures
 ********************************************************************/

/**
 * An opaque replaceable text object.  This will be manipulated only
 * through the caller-supplied UReplaceableFunctor struct.  Related
 * to the C++ class Replaceable.
 */
typedef void* UReplaceable;

/**
 * A set of function pointers that transliterators use to manipulate a
 * UReplaceable.  The caller should supply the required functions to
 * manipulate their text appropriately.  Related to the C++ class
 * Replaceable.
 */
typedef struct _UReplaceableCallbacks {

    /**
     * Function pointer that returns the number of UChar code units in
     * this text.
     */
    int32_t (*length)(const UReplaceable* rep);

    /**
     * Function pointer that returns a UChar code units at the given
     * offset into this text; 0 <= offset < n, where n is the value
     * returned by (*length)(rep).  See unistr.h for a description of
     * charAt() vs. char32At().
     */
    UChar   (*charAt)(const UReplaceable* rep,
                      int32_t offset);

    /**
     * Function pointer that returns a UChar32 code point at the given
     * offset into this text.  See unistr.h for a description of
     * charAt() vs. char32At().
     */
    UChar32 (*char32At)(const UReplaceable* rep,
                        int32_t offset);
    
    /**
     * Function pointer that replaces text between start and limit in
     * this text with the given text.  Attributes (out of band info)
     * should be retained.
     * @param start the starting index of the text to be replaced,
     * inclusive.
     * @param limit the ending index of the text to be replaced,
     * exclusive.
     * @param text the new text to replace the UChars from
     * start..limit-1.
     * @param textLength the number of UChars at text, or -1 if text
     * is null-terminated.
     */
    void    (*replace)(UReplaceable* rep,
                       int32_t start,
                       int32_t limit,
                       const UChar* text,
                       int32_t textLength);
    
    /**
     * Function pointer that copies text between start and limit in
     * this text to another index in the text.  Attributes (out of
     * band info) should be retained.  After this call, there will be
     * (at least) two copies of the characters originally located at
     * start..limit-1.
     * @param start the starting index of the text to be copied,
     * inclusive.
     * @param limit the ending index of the text to be copied,
     * exclusive.
     * @param dest the index at which the copy of the UChars should be
     * inserted.
     */
    void    (*copy)(UReplaceable* rep,
                    int32_t start,
                    int32_t limit,
                    int32_t dest);    

} UReplaceableCallbacks;

#endif
