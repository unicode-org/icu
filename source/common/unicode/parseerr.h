/*
**********************************************************************
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   03/14/00    aliu        Creation.
*   06/27/00    aliu        Change from C++ class to C struct
**********************************************************************
*/
#ifndef PARSEERR_H
#define PARSEERR_H

#include "unicode/utypes.h"


/**
 * The capacity of the context strings in UParseError.
 * @stable ICU 2.0
 */ 
enum { U_PARSE_CONTEXT_LEN = 16 };

/**
 * A UParseError struct is used to returned detailed information about
 * parsing errors.  It is used by ICU parsing engines that parse long
 * rules, patterns, or programs, where the text being parsed is long
 * enough that more information than a UErrorCode is needed to
 * localize the error.
 *
 * <p>The code field is an integer error code specific to each parsing
 * engine, but globally unique.  See the engine header file for
 * possible values.  The line, offset, and context fields are
 * optional; parsing engines may choose not to use to use them.
 *
 * <p>Examples of engines which use UParseError (or may use it in the
 * future) are RuleBasedTransliterator and RuleBasedBreakIterator.
 * 
 * @stable ICU 2.0
 */
typedef struct UParseError {

    /**
     * An integer indicating the type of error.  If no error was
     * encountered, the parse engine sets this to zero, and the
     * other fields' values should be ignored.
     *
     * <p>Each parse engine should use a range of codes from
     * 0xNNNN0001 to 0xNNNNFFFF, where NNNN is a 16-bit integer
     * between 0x0001 and 0xFFFF unique to each parse engine.
     * Parse engines should define the enum PARSE_ERROR_BASE
     * to be 0xNNNN0000.
     */
    /*int32_t        code; */

    /**
     * The line on which the error occured.  If the parse engine
     * is not using this field, it should set it to zero.  Otherwise
     * it should be a positive integer. The default value of this field
     * is -1. It will be set to 0 if the code populating this struct is not
     * using line numbers.
     * @stable ICU 2.0    
     */
    int32_t        line;

    /**
     * The character offset to the error.  If the line field is
     * being used, then this offset is from the start of the line.
     * If the line field is not being used, then this offset is from
     * the start of the text.The default value of this field
     * is -1. It will be set to appropriate value by the code that 
     * populating the struct.
     * @stable ICU 2.0   
     */
    int32_t    offset;

    /**
     * Textual context before the error.  Null-terminated.
     * May be the empty string if not implemented by parser.
     * @stable ICU 2.0   
     */
    UChar          preContext[U_PARSE_CONTEXT_LEN];

    /**
     * Textual context after the error.  Null-terminated.
     * May be the empty string if not implemented by parser.
     * @stable ICU 2.0   
     */
    UChar          postContext[U_PARSE_CONTEXT_LEN];

} UParseError;

#endif
