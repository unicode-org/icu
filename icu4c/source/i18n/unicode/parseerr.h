/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   03/14/00    aliu        Creation.
**********************************************************************
*/
#ifndef PARSEERR_H
#define PARSEERR_H

/**
 * A ParseError struct is used to returned detailed information about
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
 * <p>Examples of engines which use ParseError (or may use it in the
 * future) are RuleBasedTransliterator and RuleBasedBreakIterator.
 */
struct ParseError {
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
    int32_t        code;

    /**
     * The line on which the error occured.  If the parse engine
     * is not using this field, it should set it to zero.  Otherwise
     * it should be a positive integer.
     */
    int32_t        line;

    /**
     * The character offset to the error.  If the line field is
     * being used, then this offset is from the start of the line.
     * If the line field is not being used, then this offset is from
     * the start of the text.
     */
    UTextOffset    offset;

    /**
     * Textual context showing the error.  For example, this field
     * may contain a copy of the line on which the error occurs.  If
     * line numbers are not being used, this field may contain a copy
     * of the substring offset - 8 to offset + 8 (or some other
     * range).
     */
    UnicodeString  context;
};

#endif
