/*
**********************************************************************
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/
//
//   file:   regex.h
//
//           ICU Regular Expressions, API for C++
//

#ifndef REGEX_H
#define REGEX_H


/**
  * \file
  * \brief  C++ API:  Regular Expressions
  *
  * <h2>Regular Expression API</h2>
  *
  * <p>The ICU API for processing regular expressions consists of two classes,
  *    <code>RegexPattern</code> and <code>RegexMatcher</code>. 
  *    <code>RegexPattern</code> objects represent a pre-processed, or compiled
  *    regular expression.  They are created from a regular expression pattern string,
  *    and can be used to create <RegexMatcher> objects for the pattern. </p>
  *
  * <p> Class <code>RegexMatcher</code> bundles together a regular expression pattern
  *     and a target string to which the search pattern will be applied. 
  *     <code>RegexMatcher</code> includes API for doing plain find or search
  *     operations, for search and replace operations, and for obtaining detailed
  *     information about bounds of a match. </p>
  */

#include "unicode/utypes.h"

#if !UCONFIG_NO_REGULAR_EXPRESSIONS

#include "unicode/uobject.h"
#include "unicode/unistr.h"
#include "unicode/parseerr.h"

U_NAMESPACE_BEGIN

// Forward Declarations...
class RegexMatcher;
class UVector;
class UStack;
class UnicodeSet;


/**
 * Constants for Regular Expression Match Modes.
 * <p>Note that non-default match modes will not be supported until ICU 2.6</p>
 * @draft ICU 2.4 
 */
enum {
        /** Forces normalization of pattern and strings.  @draft ICU 2.4 */
        UREGEX_CANON_EQ         = 128, 
        /**  Enable case insensitive matching.  @draft ICU 2.4 */
        UREGEX_CASE_INSENSITIVE = 2,  
        /**  Allow white space and comments within patterns  @draft ICU 2.4 */
        UREGEX_COMMENTS         = 4,  
        /**  If set, '.' matches line terminators,  otherwise '.' matching stops at line end.
          *  @draft ICU 2.4 */
        UREGEX_DOTALL           = 32,  
        /**   Control behavior of "$" and "^"
          *    If set, recognize line terminators within string,
          *    otherwise, match only at start and end of input string.
          *   @draft ICU 2.4 */
        UREGEX_MULTILINE        = 8  
};



/**
  * Class <code>RegexPattern</code> represents a compiled regular expression.  It includes
  * factory methods for creating a RegexPattern object from the source (string) form
  * of a regular expression, methods for creating RegexMatchers that allow the pattern
  * to be applied to input text, and a few convenience methods for simple common
  * uses of regular expressions.
  *
  * @draft ICU 2.4
  */
class U_I18N_API RegexPattern: public UObject {
public:
    
    /**
      * default constructor.  Create a RegexPattern object that refers to no actual
      *   pattern.  Not normally needed; RegexPattern objects are usually
      *   created using the factory method <code>compile()</code.  
      *
      * @draft ICU 2.4
      */
    RegexPattern();


    /**
      * Copy Constructor.  Create a new RegexPattern object that is equivalent
      *                    to the source object. 
      * @draft ICU 2.4
      */
    RegexPattern(const RegexPattern &source);

    /**
      * Destructor.  Note that a RegexPattern object must persist so long as any
      *  RegexMatcher objects that were created from the RegexPattern are active.
      * @draft ICU 2.4
      */
    virtual ~RegexPattern();
    
    /**
      * Comparison operator.  Two RegexPattern objects are considered equal if they
      * were constructed from identical source patterns using the same match flag
      * settings.
      * @param that a RegexPattern object to compare with "this".
      * @return TRUE if the objects are equavelent.
      * @draft ICU 2.4
      */
    UBool                  operator==(const RegexPattern& that) const;

    /**
      * Comparison operator.  Two RegexPattern objects are considered equal if they
      * were constructed from identical source patterns using the same match flag
      * settings.
      * @param that a RegexPattern object to compare with "this".
      * @return TRUE if the objects are different.
      * @draft ICU 2.4
      */
    inline UBool           operator!=(const RegexPattern& that) const {return ! operator ==(that);};
    
    /*
     * Assignment operator.  After assignment, this RegexPattern will behave identically
     *     to the source object.
     * @draft ICU 2.4
     */
    RegexPattern  &operator =(const RegexPattern &source);

    /*
     * Create an exact copy of this RegexPattern object.  Since RegexPattern is not
     * intended to be subclasses, <code>clone()</code> and the copy construction are
     * equivalent operations.
     */
    virtual RegexPattern  *clone() const;

    
   /**
    *     <p>Compiles the given regular expression in string form into a RegexPattern
    *     object.  The compile methods, rather than the constructors, are the usual
    *     way that RegexPattern objects are created.</p>
    *
    *     <p>Note that RegexPattern objects must not be deleted while RegexMatcher
    *     objects created from the pattern are active.  RegexMatchers keep a pointer
    *     back to their pattern, so premature deletion of the pattern is a
    *     catastrophic error.</p>
    *
    *     <p>All pattern match mode flags are set to their default values.</p>
    *
    *    @param regex The regular expression to be compiles.
    *    @param pe    Receives the position (line and column nubers) of any error
    *                 within the regular expression.)
    *    @param err A reference to a UErrorCode to receive any errors.
    *    @return      A regexPattern object for the compiled pattern.
    *
    *    @draft ICU 2.4
    */
    static RegexPattern *compile( const UnicodeString &regex,
        UParseError          &pe,
        UErrorCode           &err); 
    
   /**
    *     Compiles the given regular expression into a pattern with the given flags 
    */
   /**
    *     <p>Compiles the given regular expression in string form into a RegexPattern
    *     object using the specified match mode flags.  The compile methods,
    *     rather than the constructors, are the usual way that RegexPattern objects
    *     are created.</p>
    *
    *     <p>Note that RegexPattern objects must not be deleted while RegexMatcher
    *     objects created from the pattern are active.  RegexMatchers keep a pointer
    *     back to their pattern, so premature deletion of the pattern is a
    *     catastrophic error.</p>
    *
    *    @param regex The regular expression to be compiles.
    *    @param flags The match mode flags to be used.
    *    @param pe    Receives the position (line and column nubers) of any error
    *                 within the regular expression.)
    *    @param err   A reference to a UErrorCode to receive any errors.
    *    @return      A regexPattern object for the compiled pattern.
    *
    *    @draft ICU 2.4
    */
    static RegexPattern *compile( const UnicodeString &regex,
        int32_t              flags,
        UParseError          &pe,
        UErrorCode           &err); 


   /**
    *     Get the match mode flags that were used when compiling this pattern.
    *     @return  the match mode flags
    *     @draft ICU 2.4
    */
    virtual int32_t flags() const;
    
   /*
    *  Creates a RegexMatcher that will match the given input against this pattern.  The
    *   RegexMatcher can then be used to perform match, find or replace operations
    *   on on the input.  Note that a RegexPattern object must not be deleted while
    *   any RegexMatchers created from it still exist and might possibly be used again.
    *
    *   @param input The input string to which the regular expression will be applied.
    *   @param err   A reference to a UErrorCode to receive any errors.
    *   @return      A RegexMatcher object for this pattern and input.
    *
    *   @draft ICU 2.4
    */
    virtual RegexMatcher *matcher(const UnicodeString &input,
        UErrorCode          &err) const;
    
    
   /**
    *  Test whether a string matches a regular expression.  This convenience function
    *   both compiles the reguluar expression and applies it in a single operation.  
    *   Note that if the same pattern needs to be applied repeatedly, this method will be
    *   less efficient than creating and reusing RegexPattern object.
    *
    *  @param regex The regular expression
    *  @param input The string data to be matched
    *  @param pe Receives the position of any syntax errors within the regular expression
    *  @param err A reference to a UErrorCode to receive any errors.
    *  @return True if the regular expression exactly matches the full input string.
    *
    *  @draft ICU 2.4
    */
    static UBool matches(const UnicodeString   &regex,
        const UnicodeString   &input,
        UParseError     &pe,
        UErrorCode      &err); 
    
    
   /*
    *    Returns the regular expression from which this pattern was compiled. 
    *    @draft ICU 2.4
    */
    virtual UnicodeString pattern() const;
    
    
    /*
    *    Split a string around matches of the pattern.  Somewhat like split() from Perl.
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
    *    @draft ICU 2.4
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
     * @draft ICU 2.4
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }
    
    /**
    * ICU "poor man's RTTI", returns a UClassID for this class.
    *
    * @draft ICU 2.4
    */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }
    
    static const char fgClassID;

private:
    //
    //  Implementation Data
    //
    UnicodeString   fPattern;      // The original pattern string.
    int32_t         fFlags;        // The flags used when compiling the pattern.
                                   //   
    UVector         *fCompiledPat; // The compiled pattern.
    UnicodeString   fLiteralText;  // Any literal string data from the pattern, 
                                   //   after un-escaping, for use during the match.
    UVector         *fSets;        // Any UnicodeSets referenced from the pattern.
    UBool           fBadState;     // True if some prior error has left this
                                   //  RegexPattern in an unusable state.

    RegexMatcher    *fMatcher;     // A cached matcher for this pattern, used for
                                   //  split(), to avoid having to
                                   //  make new ones on each call.

    int32_t         fNumCaptureGroups;
    int32_t         fMaxCaptureDigits;

    const UnicodeSet  **fStaticSets;  // Ptr to static (shared) sets for predefined
                                    //   regex character classes, e.g. Word.

    friend class RegexCompile;
    friend class RegexMatcher;

    //
    //  Implementation Methods
    //
    void        init();            // Common initialization, for use by constructors.
    void        zap();             // Common cleanup



};









/**
  *  class RegexMatcher bundles together a reular expression pattern and
  *  input text to which the expression can be applied.  It includes methods
  *  for testing for matches, and for find and replace operations.
  *
  * @draft ICU 2.4
  */
  class U_I18N_API RegexMatcher: public UObject {
public:
   /**
    *   Destructor.  Note that there are no public constructors; creation is
    *   done with RegexPattern::matcher().
    *
    *  @draft ICU 2.4
    */
    virtual ~RegexMatcher();

   /**
    *   Implements a replace operation intended to be used as part of an
    *   incremental find-and-replace.
    *
    *   The input string, starting from the end of the previous match and ending at
    *   the start of the current match, is appended to the destination string.
    *
    *   Then the replacement string is appended to the output string,
    *   including handling any substitutions of captured text.
    *
    *   For simple, prepackaged, non-incremental find-and-replace
    *   operations, see replaceFirst() or replaceAll().
    *
    *   @param   dest        A UnicodeString to which the results of the find-and-replace are appended.
    *   @param   replacement A UnicodeString that provides the text to be substitured for
    *                        the input text that matched the regexp pattern.  The replacement
    *                        text may contain references to captured text from the
    *                        input.
    *   @param   status      A reference to a UErrorCode to receive any errors.  Possible 
    *                        errors are  U_REGEX_INVALID_STATE if no match has been
    *                        attempted or the last match failed, and U_INDEX_OUTOFBOUNDS_ERROR
    *                        if the replacement text specifies a capture group that
    *                        does not exist in the pattern.
    *                        
    *   @return  this  RegexMatcher
    *   @draft ICU 2.4
    *
    */
    virtual RegexMatcher &appendReplacement(UnicodeString &dest,
        const UnicodeString &replacement, UErrorCode &status);
    
    
   /**
    * As the final step in a find-and-replace operation, append the remainder
    * of the input string, starting at the position following the last match,
    * to the destination string. It is intended to be invoked after one
    * or more invocations of the <code>RegexMatcher::appendReplacement()</code>. 
    *
    *  @param dest A UnicodeString to which the results of the find-and-replace are appended.
    *  @return  the destination string.
    *  @draft ICU 2.4
    */
    virtual UnicodeString &appendTail(UnicodeString &dest); 
    
    
   /**
    *    Find the ending position of the most recent match.
    *   @param   status      A reference to a UErrorCode to receive any errors.  Possible 
    *                        errors are  U_REGEX_INVALID_STATE if no match has been
    *                        attempted or the last match failed.
    *    @return the index of the last character matched, plus one.
    *   @draft ICU 2.4
    */
    virtual int32_t end(UErrorCode &status) const;
    
    
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
    UBool        isWordBoundary(int32_t pos);         // perform the \b test


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
#endif  // UCONFIG_NO_REGULAR_EXPRESSIONS
#endif
