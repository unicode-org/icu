/*
********************************************************************************
*   Copyright (C) 1997-1999, International Business Machines
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
 

#include "utypes.h"
#include "unistr.h"
#include "numfmt.h"
#include "fieldpos.h"
#include "format.h"


/**
 * A ChoiceFormat allows you to attach a format to a range of numbers.
 * It is generally used in a MessageFormat for doing things like plurals.
 * The choice is specified with an ascending list of doubles, where each item
 * specifies a half-open interval up to the next item:
 * <pre>
 * .    X matches j if and only if limit[j] &lt;= X &lt; limit[j+1]
 * </pre>
 * If there is no match, then either the first or last index is used, depending
 * on whether the number is too low or too high.  The length of the array of
 * formats must be the same as the length of the array of limits.
 * For example,
 * <pre>
 * .     {1,2,3,4,5,6,7},
 * .          {"Sun","Mon","Tue","Wed","Thur","Fri","Sat"}
 * .     {0, 1, ChoiceFormat::nextDouble(1)},
 * .          {"no files", "one file", "many files"}
 * </pre>
 * (nextDouble can be used to get the next higher double, to make the half-open
 * interval.)
 * <P>
 * Here is a simple example that shows formatting and parsing:
 * <pre>
 * .  void SimpleChoiceExample( void )
 * .  {
 * .      double limits[] = {1,2,3,4,5,6,7};
 * .      UnicodeString monthNames[] = {"Sun","Mon","Tue","Wed","Thur","Fri","Sat"};
 * .      ChoiceFormat* form = new ChoiceFormat(limits, monthNames, 7 );
 * .      ParsePosition* status = new ParsePosition(0);
 * .      UnicodeString str;
 * .      FieldPosition f1(0), f2(0);
 * .      for (double i = 0.0; i &lt;= 8.0; ++i) {
 * .          status->setIndex(0);
 * .          Formattable parseResult;
 * .          str.remove();
 * .          cout &lt;&lt; i &lt;&lt; " -> " &lt;&lt; form->format(i,str, f1) 
 * .                    &lt;&lt; " -> " &lt;&lt; parseResult &lt;&lt; endl;
 * .      }
 * .      delete form;
 * .      delete status;
 * .      cout &lt;&lt; endl;
 * .  }
 * </pre>
 * Here is a more complex example, with a pattern format.
 * <pre>
 * .  void ComplexChoiceExample( void )
 * .  {
 * .      double filelimits[] = {0,1,2};
 * .      UnicodeString filepart[] = {"are no files","is one file","are {2} files"};
 * .      ChoiceFormat* fileform = new ChoiceFormat(filelimits, filepart, 3 );
 * .      UErrorCode success = U_ZERO_ERROR;
 * .      const Format* testFormats[] = { fileform, NULL, NumberFormat::createInstance(success) };
 * .      MessageFormat* pattform = new MessageFormat("There {0} on {1}", success );
 * .      pattform->setFormats( testFormats, 3 );
 * .      Formattable testArgs[] = {0L, "Disk_A", 0L};
 * .      FieldPosition fp(0);
 * .      UnicodeString str;
 * .      for (int32_t i = 0; i &lt; 4; ++i) {
 * .          Formattable fInt(i);
 * .          testArgs[0] = fInt;
 * .          testArgs[2] = testArgs[0];
 * .          str.remove();
 * .          pattform->format(testArgs, 3, str, fp, success );
 * .          cout &lt;&lt; "Output for i=" &lt;&lt; i &lt;&lt; " : " &lt;&lt; str &lt;&lt; endl;
 * .      }
 * .      delete pattform;
 * .      cout &lt;&lt; endl;
 * .  }
 * </pre>
 * ChoiceFormat objects may be converted to and from patterns.  The
 * syntax of these patterns is [TODO fill in this section with detail].
 * Here is an example of a ChoiceFormat pattern:
 * <P>
 * You can either do this programmatically, as in the above example,
 * or by using a pattern (see ChoiceFormat for more information) as in:
 * <pre>
 * .       "0#are no files|1#is one file|1&lt;are many files"
 * </pre>
 * Here the notation is:
 * <pre>
 * .       &lt;number> "#"  Specifies a limit value.
 * .       &lt;number> "&lt;"  Specifies a limit of nextDouble(&lt;number>).
 * .       &lt;number> ">"  Specifies a limit of previousDouble(&lt;number>).
 * </pre>
 * Each limit value is followed by a string, which is terminated by
 * a vertical bar character ("|"), except for the last string, which
 * is terminated by the end of the string.
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
     */
    
    ChoiceFormat(const double* limits,
                 const UnicodeString* formats,
                 int32_t count );

    /**
     * Copy constructor.
     */
    ChoiceFormat(const ChoiceFormat&);

    /**
     * Assignment operator.
     */
    const ChoiceFormat& operator=(const ChoiceFormat&);

    /**
     * Destructor.
     */
    virtual ~ChoiceFormat();

    /**
     * Clone this Format object polymorphically. The caller owns the
     * result and should delete it when done.
     */
    virtual Format* clone(void) const;

    /**
     * Return true if the given Format objects are semantically equal.
     * Objects of different subclasses are considered unequal.
     */
    virtual bool_t operator==(const Format& other) const;

    /**
     * Sets the pattern.
     * @param pattern   The pattern to be applied.
     * @param status    Output param set to success/failure code on
     *                  exit. If the pattern is invalid, this will be
     *                  set to a failure result.
     */
    virtual void applyPattern(const UnicodeString& pattern,
                              UErrorCode& status);

    /**
     * Gets the pattern.
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
     */
    virtual void adoptChoices(double* limitsToAdopt,
                              UnicodeString* formatsToAdopt,
                              int32_t count );  

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
     */
    virtual void setChoices(const double* limitsToCopy,
                            const UnicodeString* formatsToCopy,
                            int32_t count );    
    /**
     * Get the limits passed in the constructor.
     * @return the limits.
     */
    virtual const double* getLimits(int32_t& count) const;

    /**
     * Get the formats passed in the constructor.
     * @return the formats.
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
    */
    virtual UnicodeString& format(double number,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos) const;
    virtual UnicodeString& format(int32_t number,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos) const;
    virtual UnicodeString& format(const Formattable* objs,
                                  int32_t cnt,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos,
                                  UErrorCode& success) const;
    virtual UnicodeString& format(const Formattable& obj,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos, 
                                  UErrorCode& status) const;

    /**
     * Redeclared NumberFormat method.
     */
    UnicodeString& format(const Formattable& obj,
                          UnicodeString& result,
                          UErrorCode& status) const;

    /**
     * Redeclared NumberFormat method.
     */
    UnicodeString& format(  double number,
                            UnicodeString& output) const;

    /**
     * Redeclared NumberFormat method.
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
     */
    static UClassID getStaticClassID(void) { return (UClassID)&fgClassID; }

    /*
     * Finds the least double greater than d (if positive == true),
     * or the greatest double less than d (if positive == false).
     * If NaN, returns same value.
     * <P>
     * Does not affect floating-point flags,
     */
    static double nextDouble(double d, bool_t positive);

    /**
     * Finds the least double greater than d.
     * If NaN, returns same value.
     * Used to make half-open intervals.
     * @see ChoiceFormat::previousDouble
     */
    static double nextDouble(double d );

    /**
     * Finds the greatest double less than d.
     * If NaN, returns same value.
     * @see ChoiceFormat::nextDouble
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

    static NumberFormat* fgNumberFormat;
    static char fgClassID;

    double*         fChoiceLimits;
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
