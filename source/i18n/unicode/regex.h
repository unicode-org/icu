/*
**********************************************************************
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#ifndef REGEX_H
#define REGEX_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/unistr.h"
#include "unicode/parseerr.h"

U_NAMESPACE_BEGIN

// Forward Declarations...
class RegexMatcher;
class UVector;
class UStack;


//---------------------------------------------------------------------------------
//
//  Flags for Regular Expression Modes.
//   TODO:  Move to C header once one exists.
//   All flags default to off or false
//   All are as defined by Java Regexes.
//
//---------------------------------------------------------------------------------
enum {
        UREGEX_CANON_EQ         = 128,    // Forces normalization of pattern and strings.
        UREGEX_CASE_INSENSITIVE = 2,      // Enable case insensitive matching.
        UREGEX_COMMENTS         = 4,      // Allow white space and comments within patterns
        UREGEX_DOTALL           = 32,     // If set, "." matches line terminators.
                                          //   otherwise matching stops at line end.
        UREGEX_MULTILINE        = 8,      // Control behavior of "$" and "^". 
                                          //   If set, recognize line terminators within string
                                          //   otherwise, match only at start and end of
                                          //   input string
        UREGEX_UNICODE_CASE     = 64,     // If set, use full Unicode case folding for case
                                          //   insensitive matches.  Otherwise, case insensitive
                                          //   matching only affects chars in the ASCII range.
                                          //   TODO:  do we want to support this option at all?
        UREGEX_UNIX_LINES       = 1       // If set, only \n is recognized as a line terminator.
                                          //   otherwise recognize all Unicode line endings.
};



//---------------------------------------------------------------------------------
//
//    class  RegexPattern
//
//---------------------------------------------------------------------------------
class U_I18N_API RegexPattern: public UObject {
public:
    
    
    RegexPattern();
    RegexPattern(const RegexPattern &other);
    virtual ~RegexPattern();
    
    UBool                  operator==(const RegexPattern& that) const;
    inline UBool           operator!=(const RegexPattern& that) const {return ! operator ==(that);};
    
    RegexPattern  &operator =(const RegexPattern &other);
    virtual RegexPattern  *clone() const;

    // TODO:  Do we really want a hashCode function on this class?
    virtual int32_t         hashCode(void) const;
    
    
   /**
    *     Compiles the given regular expression into a pattern 
    */
    static RegexPattern *compile( const UnicodeString &regex,
        UParseError          &pe,
        UErrorCode           &err); 
    
   /**
    *     Compiles the given regular expression into a pattern with the given flags 
    */
    static RegexPattern *compile( const UnicodeString &regex,
        int32_t              flags,
        UParseError          &pe,
        UErrorCode           &err); 


   /**
    *     Return the flags for this pattern
    */
    virtual int32_t flags() const;
    
   /*
    *  Creates a matcher that will match the given input against this pattern.
    */
    virtual RegexMatcher *matcher(const UnicodeString &input,
        UErrorCode          &err) const;
    
    
   /*
    *  Compiles the given regular expression and attempts to match the given input against it.
    */
    static UBool matches(const UnicodeString   &regex,
        const UnicodeString   &input,
        UParseError     &pe,
        UErrorCode      &err); 
    
    
   /*
    *    Returns the regular expression from which this pattern was compiled. 
    */
    virtual UnicodeString pattern() const;
    
    
    /*
    *    Split a string around matches of the pattern.  Somewhat like split() form Perl.
    *    @param input   The string to be split into fields.  The field delimiters
    *                   match the pattern (in the "this" object)
    *    @param dest    An array of UnicodeStrings to receive the results of the split.
    *                   This is an array of actual UnicodeString objects, not an
    *                   array of pointers to strings.  Local (stack based) arrays can
    *                   work well here.
    *    @param destCapacity  The number of elements in the destination array.
    *                   If the number of fields found is less than destCapacity, the
    *                   extra strings in the destination array are not altered.
    *                   If the number of destination strings is less than the number
    *                   of fields, the trailing part of the input string, including any
    *                   field delimiters, is placed in the last destination string.
    *    @return        The number of fields into which the input string was split.
    */
    virtual int32_t  split(const UnicodeString &input,
        UnicodeString    dest[],
        int32_t          destCapacity,
        UErrorCode       &err) const;
    
    
    
    //
    //   dump   Debug function, displays the compiled form of a pattern.
    //
    void dump();

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
    
    static const char fgClassID;

private:
    //
    //  Implementation Data
    //
    UnicodeString   fPattern;      // The original pattern string.
    int32_t         fFlags;        // The flags used when compiling the pattern.
                                   //   TODO:  make an enum type for the flags.
    UVector         *fCompiledPat; // The compiled, tokenized pattern.
    UnicodeString   fLiteralText;  // Any literal string data from the pattern, 
                                   //   after un-escaping, for use during the match.
    UVector         *fSets;        // Any UnicodeSets referenced from the pattern.
    UBool           fBadState;     // True if any prior error has left this
                                   //  RegexPattern unusable.

    RegexMatcher    *fMatcher;     // A cached matcher for this pattern, used for
                                   //  split(), to avoid having to
                                   //  make new ones on each call.

    int32_t         fNumCaptureGroups;
    int32_t         fMaxCaptureDigits;

    friend class RegexCompile;
    friend class RegexMatcher;

    //
    //  Implementation Methods
    //
    void        init();            // Common initialization, for use by constructors.
    void        zap();             // Common cleanup



};









//--------------------------------------------------------------------------------
//
//    class RegexMatcher 
//
//--------------------------------------------------------------------------------
class U_I18N_API RegexMatcher: public UObject {
public:
    
   /*   Destructor.  Note that there are no public constructors; creation is
    *   done with RegexPattern::matcher().
    */
    virtual ~RegexMatcher();

   /*
    *   Implements a replace operation intended to be used as part of an
    *   incremental find-and-replace.
    *
    *   The input sequence, starting from the append position and ending at
    *   the start of the current match is appended to the destination string.
    *
    *   Then the replacement string is appended to the output string,
    *   including handling any substitutions of captured text.
    *
    *   The append position is set to the position of the first
    *   character following the match in the input string.
    *
    *   For complete, prepackaged, non-incremental find-and-replace
    *   operations, see replaceFirst() or replaceAll().
    *
    *   Returns:  This Matcher
    *
    *    error:  Illegal state - no match yet attemtped, or last match failed.
    *            IndexOutOfBounds - caputure string number from replacement string.
    */
    virtual RegexMatcher &appendReplacement(UnicodeString &dest,
        const UnicodeString &replacement, UErrorCode &status);
    
    
   /*
    * This method reads characters from the input sequence,
    * starting at the append position, and appends them to the
    * destination string. It is intended to be invoked after one
    * or more invocations of the appendReplacement method in order
    * to copy the remainder of the input sequence. 
    *
    *  @return  the destination string.
    */
    virtual UnicodeString &appendTail(UnicodeString &dest); 
    
    
    /*
    *    Returns the index of the last character matched, plus one.
    *    error:  Illegal state - no match yet attemtped, or last match failed.
    */
    virtual int32_t end(UErrorCode &err) const;
    
    
    /*
    *    Returns the index of the last character, plus one, of the subsequence 
    *    captured by the given group during the previous match operation. 
    *    Errors:  Illegal state, index out of bounds
    */
    virtual int32_t end(int group, UErrorCode &err) const; 
    
    
    /*
    *  Attempts to find the next subsequence of the input sequence that matches the pattern.
    */
    virtual UBool find();
    
    
    /*
    *   Resets this matcher and then attempts to find the next subsequence of the 
    *   input sequence that matches the pattern, starting at the specified index. 
    *  Errors:  Index out of bounds.
    */
    virtual UBool find(int32_t start, UErrorCode &err); 
    
    
    /*
    *   Returns the input subsequence matched by the previous match. 
    *   If the pattern can match an empty string, an empty string may be returned.
    *    Errors:   illegal state (no match has yet been attempted.)
    */
    virtual UnicodeString group(UErrorCode &err) const;
    
    
    /*
    *    Returns the input subsequence captured by the given group during the previous match operation. 
    *    Group(0) is the entire match.
    *    Errors:   Index out of bounds, illegal state (no match has yet been attempted.)
    */
    virtual UnicodeString group(int32_t group, UErrorCode &err) const; 
    
    
    /*
    *   Returns the number of capturing groups in this matcher's pattern.
    */
    virtual int32_t groupCount() const;
    
    
    /*
    *   Returns the input string being matched.
    */
    virtual const UnicodeString &input() const; 
    
    
    /*
    *   Attempts to match the input string, starting at the beginning, against the pattern.
    *   Like the matches method, this method always starts at the beginning of the input string;
    *   unlike that method, it does not require that the entire input sequence be matched. 
    *
    *   If the match succeeds then more information can be obtained via the start, end,
    *    and group methods.
    */
    virtual UBool lookingAt(UErrorCode &err);
    
    
    /*
    *   Attempts to match the entire input sequence against the pattern.
    */
    virtual UBool matches(UErrorCode &err);
    
    
    /*
    *    Returns the pattern that is interpreted by this matcher.
    */
    virtual const RegexPattern &pattern() const;
    
    
    /*
    *    Replaces every subsequence of the input sequence that matches the pattern
    *    with the given replacement string.  This is a convenience function that
    *    provides a complete find-and-replace-all operation.
    *
    *    This method first resets this matcher. It then scans the input sequence
    *    looking for matches of the pattern. Characters that are not part of any 
    *    match are left unchanged; each match is replaced in the result by the
    *    replacement string. The replacement string may contain references to
    *    captured subsequences as in the appendReplacement method. 
    *
    *    @return   A string containing the results of the find and replace.
    *
    */
    virtual UnicodeString replaceAll(const UnicodeString &replacement, UErrorCode &err); 
    
    
    /*
    * Replaces the first subsequence of the input sequence that matches
    * the pattern with the given replacement string.   This is a convenience
    * function that provides a complete find-and-replace operation.
    *
    * This method first resets this matcher. It then scans the input sequence
    * looking for a match of the pattern. Characters that are not part
    * of the match are appended directly to the result string; the match is replaced
    * in the result by the replacement string. The replacement string may contain
    * references to captured subsequences as in the appendReplacement method. 
    *
    */
    virtual UnicodeString replaceFirst(const UnicodeString &replacement, UErrorCode &err); 
    
    
    /*
    *   Resets this matcher.
    */
    virtual RegexMatcher &reset();
    
    
    /*
    *   Resets this matcher with a new input sequence. 
    */
    virtual RegexMatcher &reset(const UnicodeString &input);  
    
    
    /*
    *   Returns the start index of the previous match. 
    *   Error:  Illegal State (no previous match)
    */
    virtual int32_t start(UErrorCode &err) const;
    
    
    /*
    *   Returns the start index of the subsequence captured by the given group
    *    during the previous match operation.
    *
    *   Error:  Illegal State  (no previous match)
    *           Index out of bounds (no group with specified index)
    */
    virtual int32_t start(int group, UErrorCode &err) const;
    

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
    
    static const char fgClassID;

private:
    // Constructors and other object boilerplate are private.
    // Instances of RegexMatcher can not be assigned, copied, cloned, etc.
    // Creation by users is only through the factory method in class RegexPattern
    RegexMatcher(const RegexPattern *pat); 
    RegexMatcher(const RegexMatcher &other);
    RegexMatcher &operator =(const RegexMatcher &rhs);
    friend class RegexPattern;


    //
    //  MatchAt   This is the internal interface to the match engine itself.
    //            Match status comes back in matcher member variables.
    //
    void         MatchAt(int32_t startIdx, UErrorCode &status);   
    inline  void backTrack(int32_t &inputIdx, int32_t &patIdx);
    UBool        getCaptureText(const UnicodeString &rep,
                                int32_t &repIdx,
                                int32_t &textStart,
                                int32_t &textEnd);


    const RegexPattern  *fPattern;
    const UnicodeString *fInput;
    int32_t              fInputLength;
    UBool                fMatch;           // True if the last match was successful.
    int32_t              fMatchStart;      // Position of the start of the most recent match
    int32_t              fMatchEnd;        // First position after the end of the most recent match
    int32_t              fLastMatchEnd;    // First position after the end of the previous match.
    UStack              *fBackTrackStack;
    UVector             *fCaptureStarts;
    UVector             *fCaptureEnds;

};  



U_NAMESPACE_END
#endif
