//
//  regexcmp.h
//
//  Copyright (C) 2002, International Business Machines Corporation and others.
//  All Rights Reserved.
//
//  This file contains declarations for the class RegexCompile and for compiled
//  regular expression data format
//


#ifndef RBBISCAN_H
#define RBBISCAN_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/uniset.h"
#include "unicode/parseerr.h"
#include "uhash.h"
#include "uvector.h"



U_NAMESPACE_BEGIN


static const UBool REGEX_DEBUG = TRUE;

//--------------------------------------------------------------------------------
//
//  class RegexCompile    does the lowest level, character-at-a-time
//                        scanning of a regular expression.  
//
//                        The output of the scanner is a tokenized form
//                        of the RE, plus prebuilt UnicodeSet objects for each
//                        set of charcters that is referenced.
//
//--------------------------------------------------------------------------------
static const int    kStackSize = 100;               // The size of the state stack for
                                                    //   pattern parsing.  Corresponds roughly
                                                    //   to the depth of parentheses nesting
                                                    //   that is allowed in the rules.

enum EParseAction {dummy01, dummy02};               // Placeholder enum for the specifier for
                                                    //   actions that are specified in the
                                                    //   rule parsing state table.
struct  RegexTableEl;
class   RegexPattern;


class RegexCompile : public UObject {
public:

    struct RegexPatternChar {
        UChar32             fChar;
        UBool               fQuoted;
    };

    RegexCompile(UErrorCode &e);
    
    void       compile(RegexPattern &rxp, const UnicodeString &pat, UParseError &pp, UErrorCode &e);


    virtual    ~RegexCompile();

    void        nextChar(RegexPatternChar &c);      // Get the next char from the input stream.


    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

private:

    UBool       doParseActions(EParseAction a);
    void        error(UErrorCode e);                   // error reporting convenience function.

    UChar32     nextCharLL();
    UChar32     peekCharLL();
    UnicodeSet  *scanSet();
    UnicodeSet  *scanProp();
    void        handleCloseParen();
    int32_t     blockTopLoc(UBool reserve);          // Locate a position in the compiled pattern
                                                     //  at the top of the just completed block
                                                     //  or operation, and optionally ensure that
                                                     //  there is space to add an opcode there.
    void        compileSet(UnicodeSet *theSet);      // Generate the compiled pattern for
                                                     //   a reference to a UnicodeSet.


    UErrorCode                    *fStatus;
    RegexPattern                  *fRXPat;
    UParseError                   *fParseErr;

    //
    //  Data associated with low level character scanning
    //
    int32_t                       fScanIndex;        // Index of current character being processed
                                                     //   in the rule input string.
    int32_t                       fNextIndex;        // Index of the next character, which
                                                     //   is the first character not yet scanned.
    UBool                         fQuoteMode;        // Scan is in a quoted region
    UBool                         fFreeForm;         // Scan mode is free-form, ignore spaces.
    int                           fLineNum;          // Line number in input file.
    int                           fCharNum;          // Char position within the line.
    UChar32                       fLastChar;         // Previous char, needed to count CR-LF
                                                     //   as a single line, not two.
    UChar32                       fPeekChar;         // Saved char, if we've scanned ahead.


    RegexPatternChar              fC;                // Current char for parse state machine
                                                     //   processing.

    int32_t                       fStringOpStart;    // While a literal string is being scanned
                                                     //   holds the start index within RegexPattern.
                                                     //   fLiteralText where the string is being stored.

    RegexTableEl                  **fStateTable;     // State Transition Table for regex Rule
                                                     //   parsing.  index by p[state][char-class]

    uint16_t                      fStack[kStackSize];  // State stack, holds state pushes
    int                           fStackPtr;           //  and pops as specified in the state
                                                       //  transition rules.

    int32_t                       fPatternLength;    // Length of the input pattern string.

    UStack                        fParenStack;       // parentheses stack.  Each frame consists of
                                                     //   the positions of compiled pattern operations
                                                     //   needing fixup, followed by negative vallue.  The  
                                                     //   first entry in each frame is the position of the
                                                     //   spot reserved for use when a quantifier
                                                     //   needs to add a SAVE at the start of a (block)
                                                     //   The negative value (-1, -2,...) indicates
                                                     //   the kind of paren that opened the frame.  Some
                                                     //   need special handling on close.


    int32_t                       fMatchOpenParen;   // The position in the compiled pattern
                                                     //   of the slot reserved for a state save
                                                     //   at the start of the most recently processed
                                                     //   parenthesized block.
    int32_t                       fMatchCloseParen;  // The position in the pattern of the first
                                                     //   location after the most recently processed
                                                     //   parenthesized block.

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END

#endif
