/*
********************************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File CHOICFMT.H
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/20/97    helena      Finished first cut of implementation and got rid 
*                           of nextDouble/previousDouble and replaced with
*                           boolean array.
*   4/10/97     aliu        Clean up.  Modified to work on AIX.
*   8/6/97      nos         Removed overloaded constructor, member var 'buffer'.
*    07/22/98    stephen        Removed operator!= (implemented in Format)
********************************************************************************
*/
 
#ifndef CHOICFMT_H
#define CHOICFMT_H
 

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/numfmt.h"
#include "unicode/fieldpos.h"
#include "unicode/format.h"

/**
 * <p><code>ChoiceFormat</code> converts between ranges of numeric values
 * and string names for those ranges. A <code>ChoiceFormat</code> splits
 * the real number line <code>-Inf</code> to <code>+Inf</code> into two
 * or more contiguous ranges. Each range is mapped to a
 * string. <code>ChoiceFormat</code> is generally used in a
 * <code>MessageFormat</code> for displaying grammatically correct
 * plurals such as &quot;There are 2 files.&quot;</p>
 * 
 * <p>There are two methods of defining a <code>ChoiceFormat</code>; both
 * are equivalent.  The first is by using a string pattern. This is the
 * preferred method in most cases.  The second method is through direct
 * specification of the arrays that make up the
 * <code>ChoiceFormat</code>.</p>
 * 
 * <p><strong>Patterns</strong></p>
 * 
 * <p>In most cases, the preferred way to define a
 * <code>ChoiceFormat</code> is with a pattern. Here is an example of a
 * <code>ChoiceFormat</code> pattern:</p>
 * 
 * <pre>    0#are no files|1#is one file|1&lt;are many files</pre>
 * 
 * <p>The pattern consists of a number or <em>range specifiers</em>
 * separated by vertical bars U+007C (<code>|</code>). There is no
 * vertical bar after the last range.  Each range specifier is of the
 * form <em>number separator string</em>.</p>
 * 
 * <p><em>Number</em> is a floating point number that can be parsed by a
 * default <code>NumberFormat</code> for the US locale. It gives the
 * lower limit of this range. The lower limit is either inclusive or
 * exclusive, depending on the <em>separator</em>. (The upper limit is
 * given by the lower limit of the next range.)  The Unicode infinity
 * sign U+221E is recognized for positive infinity. It may be preceded by
 * '<code>-</code>' (U+002D) to indicate negative infinity.</p>
 * 
 * <p><em>String</em> is the format string for this range, with special
 * characters enclosed in single quotes (<code>'The #
 * sign'</code>). Single quotes themselves are indicated by two single
 * quotes in a row (<code>'o''clock'</code>).</p>
 * 
 * <p><em>Separator</em> is one of the following single characters:
 * 
 * <ul>
 *   <li>U+0023 (<code>#</code>) indicates that the lower limit given by
 *     <em>number</em> is inclusive.  That is, the limit value belongs to
 *     this range.  Another way of saying this is that the corresponding
 *     closure is <code>FALSE</code>.  The Unicode less than or equals
 *     sign U+2264 may be used in place of <code>#</code>.</li>
 *   <li>U+003C (<code>&lt;</code>) indicates that the lower limit given
 *     by <em>number</em> is exclusive.  This means that the limit
 *     belongs to the prior range.</li> Another way of saying this is
 *     that the corresponding closure is <code>TRUE</code>.
 * </ul>
 * 
 * <p>See below for more information about closures.</p>
 * 
 * <p><strong>Arrays</strong></p>
 * 
 * <p>A <code>ChoiceFormat</code> defining <code>n</code> intervals
 * (<code>n</code> &gt;= 2) is specified by three arrays of
 * <code>n</code> items:
 * 
 * <ul>
 *   <li><code>double limits[]</code> gives the start of each
 *     interval. This must be a non-decreasing list of values, none of
 *     which may be <code>NaN</code>.</li>
 *   <li><code>UBool closures[]</code> determines whether each limit
 *     value is contained in the interval below it or in the interval
 *     above it. If <code>closures[i]</code> is <code>FALSE</code>, then
 *     <code>limits[i]</code> is a member of interval
 *     <code>i</code>. Otherwise it is a member of interval
 *     <code>i+1</code>. If no closures array is specified, this is
 *     equivalent to having all closures be <code>FALSE</code>. Closures
 *     allow one to specify half-open, open, or closed intervals.</li>
 *   <li><code>UnicodeString formats[]</code> gives the string label
 *     associated with each interval.</li>
 * </ul>
 * 
 * <p><strong>Formatting and Parsing</strong></p>
 * 
 * <p>During formatting, a number is converted to a
 * string. <code>ChoiceFormat</code> accomplishes this by mapping the
 * number to an interval using the following rule. Given a number
 * <code>X</code> and and index value <code>j</code> in the range
 * <code>0..n-1</code>, where <code>n</code> is the number of ranges:</p>
 * 
 * <blockquote><code>X</code> matches <code>j</code> if and only if
 * <code>limit[j] &lt;= X &lt; limit[j+1]</code>
 * </blockquote>
 * 
 * <p>(This assumes that all closures are <code>FALSE</code>.  If some
 * closures are <code>TRUE</code> then the relations must be changed to
 * <code>&lt;=</code> or <code>&lt;</code> as appropriate.) If there is
 * no match, then either the first or last index is used, depending on
 * whether the number is too low or too high. Once a number is mapped to
 * an interval <code>j</code>, the string <code>formats[j]</code> is
 * output.</p>
 * 
 * <p>During parsing, a string is converted to a
 * number. <code>ChoiceFormat</code> finds the element
 * <code>formats[j]</code> equal to the string, and returns
 * <code>limits[j]</code> as the parsed value.</p>
 * 
 * <p><strong>Notes</strong></p>
 * 
 * <p>The first limit value does not define a range boundary. For
 * example, in the pattern &quot;<code>1.0#a|2.0#b</code>&quot;, the
 * intervals are [-Inf, 2.0) and [2.0, +Inf].  It appears that the first
 * interval should be [1.0, 2.0).  However, since all values that are too
 * small are mapped to range zero, the first interval is effectively
 * [-Inf, 2.0).  However, the first limit value <em>is</em> used during
 * formatting. In this example, <code>parse(&quot;a&quot;)</code> returns
 * 1.0.</p>
 * 
 * <p>There are no gaps between intervals and the entire number line is
 * covered.  A <code>ChoiceFormat</code> maps <em>all</em> possible
 * double values to a finite set of intervals.</p>
 * 
 * <p>The non-number <code>NaN</code> is mapped to interval zero during
 * formatting.</p>
 * 
 * <p><strong>Examples</strong></p>
 * 
 * <p>Here is an example of two arrays that map the number
 * <code>1..7</code> to the English day of the week abbreviations
 * <code>Sun..Sat</code>. No closures array is given; this is the same as
 * specifying all closures to be <code>FALSE</code>.</p>
 * 
 * <pre>    {1,2,3,4,5,6,7},
 *     {&quot;Sun&quot;,&quot;Mon&quot;,&quot;Tue&quot;,&quot;Wed&quot;,&quot;Thur&quot;,&quot;Fri&quot;,&quot;Sat&quot;}</pre>
 * 
 * <p>Here is an example that maps the ranges [-Inf, 1), [1, 1], and (1,
 * +Inf] to three strings. That is, the number line is split into three
 * ranges: x &lt; 1.0, x = 1.0, and x &gt; 1.0.</p>
 * 
 * <pre>    {0, 1, 1},
 *     {FALSE, FALSE, TRUE},
 *     {&quot;no files&quot;, &quot;one file&quot;, &quot;many files&quot;}</pre>
 * 
 * <p>Here is a simple example that shows formatting and parsing: </p>
 * 
 * <pre>
 * \code
 *   #include &lt;unicode/choicfmt.h&gt;
 *   #include &lt;unicode/unistr.h&gt;
 *   #include &lt;iostream.h&gt;
 *   
 *   int main(int argc, char *argv[]) {
 *       double limits[] = {1,2,3,4,5,6,7};
 *       UnicodeString monthNames[] = {
 *           &quot;Sun&quot;,&quot;Mon&quot;,&quot;Tue&quot;,&quot;Wed&quot;,&quot;Thu&quot;,&quot;Fri&quot;,&quot;Sat&quot;};
 *       ChoiceFormat fmt(limits, monthNames, 7);
 *       UnicodeString str;
 *       char buf[256];
 *       for (double x = 1.0; x &lt;= 8.0; x += 1.0) {
 *           fmt.format(x, str);
 *           buf[str.extract(0, str.length(), buf, 256, &quot;&quot;)] = 0;
 *           str.truncate(0);
 *           cout &lt;&lt; x &lt;&lt; &quot; -&gt; &quot;
 *                &lt;&lt; buf &lt;&lt; endl;
 *       }
 *       cout &lt;&lt; endl;
 *       return 0;
 *   }
 * \endcode
 * </pre>
 * 
 * <p>Here is a more complex example using a <code>ChoiceFormat</code>
 * constructed from a pattern together with a
 * <code>MessageFormat</code>.</p>
 * 
 * <pre>
 * \code
 *   #include &lt;unicode/choicfmt.h&gt;
 *   #include &lt;unicode/msgfmt.h&gt;
 *   #include &lt;unicode/unistr.h&gt;
 *   #include &lt;iostream.h&gt;
 * 
 *   int main(int argc, char *argv[]) {
 *       UErrorCode status = U_ZERO_ERROR;
 *       double filelimits[] = {0,1,2};
 *       UnicodeString filepart[] =
 *           {&quot;are no files&quot;,&quot;is one file&quot;,&quot;are {0} files&quot;};
 *       ChoiceFormat* fileform = new ChoiceFormat(filelimits, filepart, 3 );
 *       Format* testFormats[] =
 *           {fileform, NULL, NumberFormat::createInstance(status)};
 *       MessageFormat pattform(&quot;There {0} on {1}&quot;, status );
 *       pattform.adoptFormats(testFormats, 3);
 *       Formattable testArgs[] = {0L, &quot;Disk A&quot;};
 *       FieldPosition fp(0);
 *       UnicodeString str;
 *       char buf[256];
 *       for (int32_t i = 0; i &lt; 4; ++i) {
 *           Formattable fInt(i);
 *           testArgs[0] = fInt;
 *           pattform.format(testArgs, 2, str, fp, status );
 *           buf[str.extract(0, str.length(), buf, &quot;&quot;)] = 0;
 *           str.truncate(0);
 *           cout &lt;&lt; &quot;Output for i=&quot; &lt;&lt; i &lt;&lt; &quot; : &quot; &lt;&lt; buf &lt;&lt; endl;
 *       }
 *       cout &lt;&lt; endl;
 *       return 0;
 *   }
 * \endcode
 * </pre>
 */
class U_I18N_API ChoiceFormat: public NumberFormat {
public:
    /**
     * Construct a new ChoiceFormat with the limits and the corresponding formats
     * based on the pattern.
     *
     * @param pattern   Pattern used to construct object.
     * @param status    Output param to receive success code.  If the
     *                  pattern cannot be parsed, set to failure code.
     * @stable
     */
    ChoiceFormat(const UnicodeString& newPattern,
                 UErrorCode& status);


    /**
     * Construct a new ChoiceFormat with the given limits and formats.  Copy
     * the limits and formats instead of adopting them.
     *
     * @param limits    Array of limit values.
     * @param formats   Array of formats.
     * @param count     Size of 'limits' and 'formats' arrays.
     * @stable
     */
    
    ChoiceFormat(const double* limits,
                 const UnicodeString* formats,
                 int32_t count );

    /**
     * Construct a new ChoiceFormat with the given limits and formats.
     * Copy the limits and formats (instead of adopting them).  By
     * default, each limit in the array specifies the inclusive lower
     * bound of its range, and the exclusive upper bound of the previous
     * range.  However, if the isLimitOpen element corresponding to a
     * limit is TRUE, then the limit is the exclusive lower bound of its
     * range, and the inclusive upper bound of the previous range.
     * @param limits Array of limit values
     * @param closures Array of booleans specifying whether each
     * element of 'limits' is open or closed.  If FALSE, then the
     * corresponding limit is a member of the range above it.  If TRUE,
     * then the limit belongs to the range below it.
     * @param formats Array of formats
     * @param count Size of 'limits', 'closures', and 'formats' arrays
     */
    ChoiceFormat(const double* limits,
                 const UBool* closures,
                 const UnicodeString* formats,
                 int32_t count);

    /**
     * Copy constructor.
     * @stable
     */
    ChoiceFormat(const ChoiceFormat&);

    /**
     * Assignment operator.
     * @stable
     */
    const ChoiceFormat& operator=(const ChoiceFormat&);

    /**
     * Destructor.
     * @stable
     */
    virtual ~ChoiceFormat();

    /**
     * Clone this Format object polymorphically. The caller owns the
     * result and should delete it when done.
     * @stable
     */
    virtual Format* clone(void) const;

    /**
     * Return true if the given Format objects are semantically equal.
     * Objects of different subclasses are considered unequal.
     * @stable
     */
    virtual UBool operator==(const Format& other) const;

    /**
     * Sets the pattern.
     * @param pattern   The pattern to be applied.
     * @param status    Output param set to success/failure code on
     *                  exit. If the pattern is invalid, this will be
     *                  set to a failure result.
     * @stable
     */
    virtual void applyPattern(const UnicodeString& pattern,
                              UErrorCode& status);

    /**
     * Gets the pattern.
     * @stable
     */
    virtual UnicodeString& toPattern(UnicodeString &pattern) const;

    /**
     * Set the choices to be used in formatting.  The arrays are adopted and
     * should not be deleted by the caller.
     *
     * @param limitsToAdopt     Contains the top value that you want
     *                          parsed with that format,and should be in
     *                          ascending sorted order. When formatting X,
     *                          the choice will be the i, where limit[i]
     *                          &lt;= X &lt; limit[i+1].
     * @param formatsToAdopt    The format strings you want to use for each limit.
     * @param count             The size of the above arrays.
     * @stable
     */
    virtual void adoptChoices(double* limitsToAdopt,
                              UnicodeString* formatsToAdopt,
                              int32_t count );  

    /**
     * Set the choices to be used in formatting.  The arrays are adopted
     * and should not be deleted by the caller.  See class description
     * for documenatation of the limits, closures, and formats arrays.
     * @param limitsToAdopt Array of limits to adopt
     * @param closuresToAdopt Array of limit booleans to adopt
     * @param formatsToAdopt Array of format string to adopt
     * @param count The size of the above arrays
     */
    virtual void adoptChoices(double* limitsToAdopt,
                              UBool* closuresToAdopt,
                              UnicodeString* formatsToAdopt,
                              int32_t count);
    
    /**
     * Set the choices to be used in formatting.
     *
     * @param limitsToCopy      Contains the top value that you want
     *                          parsed with that format,and should be in
     *                          ascending sorted order. When formatting X,
     *                          the choice will be the i, where limit[i]
     *                          &lt;= X &lt; limit[i+1].
     * @param formatsToCopy     The format strings you want to use for each limit.
     * @param count             The size of the above arrays.
     * @stable
     */
    virtual void setChoices(const double* limitsToCopy,
                            const UnicodeString* formatsToCopy,
                            int32_t count );    

    /**
     * Set the choices to be used in formatting.  See class description
     * for documenatation of the limits, closures, and formats arrays.
     * @param limits Array of limits
     * @param closures Array of limit booleans
     * @param formats Array of format string
     * @param count The size of the above arrays
     */
    virtual void setChoices(const double* limits,
                            const UBool* closures,
                            const UnicodeString* formats,
                            int32_t count);

    /**
     * Get the limits passed in the constructor.
     * @return the limits.
     * @stable
     */
    virtual const double* getLimits(int32_t& count) const;
    
    /**
     * Get the limit booleans passed in the constructor.  The caller
     * must not delete the result.
     * @return the closures
     */
    virtual const UBool* getClosures(int32_t& count) const;

    /**
     * Get the formats passed in the constructor.
     * @return the formats.
     * @stable
     */
    virtual const UnicodeString* getFormats(int32_t& count) const;

   /**
    * Format a double or long number using this object's choices.
    *
    * @param number     The value to be formatted.
    * @param toAppendTo The string to append the formatted string to.
    *                   This is an output parameter.
    * @param pos        On input: an alignment field, if desired.
    *                   On output: the offsets of the alignment field.
    * @return           A reference to 'toAppendTo'.
    * @stable
    */
    virtual UnicodeString& format(double number,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos) const;
   /**
    * Format a int_32t number using this object's choices.
    *
    * @stable
    */
    virtual UnicodeString& format(int32_t number,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos) const;
   /**
    * Format an array of objects using this object's choices.
    *
    * @stable
    */
    virtual UnicodeString& format(const Formattable* objs,
                                  int32_t cnt,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos,
                                  UErrorCode& success) const;
   /**
    * Format an object using this object's choices.
    *
    * @stable
    */
    virtual UnicodeString& format(const Formattable& obj,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos, 
                                  UErrorCode& status) const;

    /**
     * Redeclared NumberFormat method.
     * @stable
     */
    UnicodeString& format(const Formattable& obj,
                          UnicodeString& result,
                          UErrorCode& status) const;

    /**
     * Redeclared NumberFormat method.
     * @stable
     */
    UnicodeString& format(  double number,
                            UnicodeString& output) const;

    /**
     * Redeclared NumberFormat method.
     * @stable
     */
    UnicodeString& format(  int32_t number,
                            UnicodeString& output) const;

   /**
    * Return a long if possible (e.g. within range LONG_MAX,
    * LONG_MAX], and with no decimals), otherwise a double.  If
    * IntegerOnly is set, will stop at a decimal point (or equivalent;
    * e.g. for rational numbers "1 2/3", will stop after the 1).
    * <P>
    * If no object can be parsed, parsePosition is unchanged, and NULL is
    * returned.
    *
    * @param text           The text to be parsed.
    * @param result         Formattable to be set to the parse result.
    *                       If parse fails, return contents are undefined.
    * @param parsePosition  The position to start parsing at on input.
    *                       On output, moved to after the last successfully
    *                       parse character. On parse failure, does not change.
    * @return               A Formattable object of numeric type.  The caller
    *                       owns this an must delete it.  NULL on failure.
    * @see                  NumberFormat::isParseIntegerOnly
    * @stable
    */
    virtual void parse(const UnicodeString& text,
                       Formattable& result,
                       ParsePosition& parsePosition) const;
    virtual void parse(const UnicodeString& text,
                       Formattable& result,
                       UErrorCode& status) const;
    
    
public:
    /**
     * Returns a unique class ID POLYMORPHICALLY.  Pure virtual override.
     * This method is to implement a simple version of RTTI, since not all
     * C++ compilers support genuine RTTI.  Polymorphic operator==() and
     * clone() methods call this method.
     *
     * @return          The class ID for this object. All objects of a
     *                  given class have the same class ID.  Objects of
     *                  other classes have different class IDs.
     * @stable
     */
    virtual UClassID getDynamicClassID(void) const;

    /**
     * Return the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     * <pre>
     * .       Base* polymorphic_pointer = createPolymorphicObject();
     * .       if (polymorphic_pointer->getDynamicClassID() ==
     * .           Derived::getStaticClassID()) ...
     * </pre>
     * @return          The class ID for all objects of this class.
     * @stable
     */
    static UClassID getStaticClassID(void) { return (UClassID)&fgClassID; }

    /*
     * Finds the least double greater than d (if positive == true),
     * or the greatest double less than d (if positive == false).
     * If NaN, returns same value.
     * <P>
     * Does not affect floating-point flags,
     * @deprecated This will be removed after 2002-Jun-30. Use closures API instead.
     */
    static double nextDouble(double d, UBool positive);

    /**
     * Finds the least double greater than d.
     * If NaN, returns same value.
     * Used to make half-open intervals.
     * @see ChoiceFormat::previousDouble
     * @deprecated This will be removed after 2002-Jun-30. Use closures API instead.
     */
    static double nextDouble(double d );

    /**
     * Finds the greatest double less than d.
     * If NaN, returns same value.
     * @see ChoiceFormat::nextDouble
     * @deprecated This will be removed after 2002-Jun-30. Use closures API instead.
     */
    static double previousDouble(double d );

private:
    // static cache management (thread-safe)
    static NumberFormat* getNumberFormat(UErrorCode &status); // call this function to 'check out' a numberformat from the cache.
    static void          releaseNumberFormat(NumberFormat *adopt); // call this function to 'return' the number format to the cache.
    
    /**
     * Converts a string to a double value using a default NumberFormat object
     * which is static (shared by all ChoiceFormat instances).
     * @param string the string to be converted with.
     * @param status error code.
     * @return the converted double number.
     */
    static double stod(const UnicodeString& string, UErrorCode& status);

    /**
     * Converts a double value to a string using a default NumberFormat object
     * which is static (shared by all ChoiceFormat instances).
     * @@param value the double number to be converted with.
     * @@param string the result string.
     * @@param status error code.
     * @@return the converted string.
     */
    static UnicodeString& dtos(double value, UnicodeString& string, UErrorCode& status);

    static UMTX fgMutex;
    static NumberFormat* fgNumberFormat;
    static char fgClassID;

    static const UnicodeString fgPositiveInfinity;
    static const UnicodeString fgNegativeInfinity;

    /**
     * Each ChoiceFormat divides the range -Inf..+Inf into fCount
     * intervals.  The intervals are:
     *
     *         0: fChoiceLimits[0]..fChoiceLimits[1]
     *         1: fChoiceLimits[1]..fChoiceLimits[2]
     *        ...
     *  fCount-2: fChoiceLimits[fCount-2]..fChoiceLimits[fCount-1]
     *  fCount-1: fChoiceLimits[fCount-1]..+Inf
     *
     * Interval 0 is special; during formatting (mapping numbers to
     * strings), it also contains all numbers less than
     * fChoiceLimits[0], as well as NaN values.
     *
     * Interval i maps to and from string fChoiceFormats[i].  When
     * parsing (mapping strings to numbers), then intervals map to
     * their lower limit, that is, interval i maps to fChoiceLimit[i].
     *
     * The intervals may be closed, half open, or open.  This affects
     * formatting but does not affect parsing.  Interval i is affected
     * by fClosures[i] and fClosures[i+1].  If fClosures[i]
     * is FALSE, then the value fChoiceLimits[i] is in interval i.
     * That is, intervals i and i are:
     *
     *  i-1:                 ... x < fChoiceLimits[i]
     *    i: fChoiceLimits[i] <= x ...
     *
     * If fClosures[i] is TRUE, then the value fChoiceLimits[i] is
     * in interval i-1.  That is, intervals i-1 and i are:
     *
     *  i-1:                ... x <= fChoiceLimits[i]
     *    i: fChoiceLimits[i] < x ...
     *
     * Because of the nature of interval 0, fClosures[0] has no
     * effect.

     */
    double*         fChoiceLimits;
    UBool*          fClosures;
    UnicodeString*  fChoiceFormats;
    int32_t         fCount;
};
 
inline UClassID 
ChoiceFormat::getDynamicClassID() const
{ 
    return ChoiceFormat::getStaticClassID(); 
}

inline double ChoiceFormat::nextDouble( double d )
{
    return ChoiceFormat::nextDouble( d, TRUE );
}
    
inline double ChoiceFormat::previousDouble( double d )
{
    return ChoiceFormat::nextDouble( d, FALSE );
}

inline UnicodeString&
ChoiceFormat::format(const Formattable& obj,
                     UnicodeString& result,
                     UErrorCode& status) const {
    // Don't use Format:: - use immediate base class only,
    // in case immediate base modifies behavior later.
    return NumberFormat::format(obj, result, status);
}

inline UnicodeString&
ChoiceFormat::format(double number,
                     UnicodeString& output) const {
    return NumberFormat::format(number, output);
}

inline UnicodeString&
ChoiceFormat::format(int32_t number,
                     UnicodeString& output) const {
    return NumberFormat::format(number, output);
}

#endif // _CHOICFMT
//eof
