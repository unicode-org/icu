/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
*
* File NUMFMT.H
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/18/97    clhuang     Updated per C++ implementation.
*   04/17/97    aliu        Changed DigitCount to int per code review.
*    07/20/98    stephen        JDK 1.2 sync up. Added scientific support.
*                            Changed naming conventions to match C++ guidelines
*                            Derecated Java style constants (eg, INTEGER_FIELD)
********************************************************************************
*/
 
#ifndef NUMFMT_H
#define NUMFMT_H
 

#include "utypes.h"
#include "unistr.h"
#include "format.h"

class Locale;

/**
 * Abstract base class for all number formats.  Provides interface for
 * formatting and parsing a number.  Also provides methods for
 * determining which locales have number formats, and what their names
 * are.
 * <P>
 * NumberFormat helps you to format and parse numbers for any locale.
 * Your code can be completely independent of the locale conventions
 * for decimal points, thousands-separators, or even the particular
 * decimal digits used, or whether the number format is even decimal.
 * <P>
 * To format a number for the current Locale, use one of the static
 * factory methods:
 * <pre>
 * .   double myNumber = 7.0;
 * .   UnicodeString myString;
 * .   UErrorCode success = U_ZERO_ERROR;
 * .   NumberFormat* nf = NumberFormat::createInstance(success)
 * .   nf->format(myNumber, myString);
 * .   cout &lt;&lt; " Example 1: " &lt;&lt; myString &lt;&lt; endl;
 * </pre>
 * If you are formatting multiple numbers, it is more efficient to get
 * the format and use it multiple times so that the system doesn't
 * have to fetch the information about the local language and country
 * conventions multiple times.
 * <pre>
 * .    UnicodeString myString;
 * .    UErrorCode success = U_ZERO_ERROR;
 * .    nf = NumberFormat::createInstance( success );
 * .    int32_t a[] = { 123, 3333, -1234567 };
 * .    const int32_t a_len = sizeof(a) / sizeof(a[0]);
 * .    myString.remove();
 * .    for (int32_t i = 0; i < a_len; i++) {
 * .        nf->format(a[i], myString);
 * .        myString += " ; ";
 * .    }
 * .    cout &lt;&lt; " Example 2: " &lt;&lt; myString &lt;&lt; endl;
 * </pre>
 * To format a number for a different Locale, specify it in the
 * call to createInstance().
 * <pre>
 * .    nf = NumberFormat::createInstance( Locale::FRENCH, success );
 * </pre>
 * You can use a NumberFormat to parse also.
 * <pre>
 * .   UErrorCode success;
 * .   Formattable result(-999);  // initialized with error code
 * .   nf->parse(myString, result, success);
 * </pre>
 * Use createInstance to get the normal number format for that country.
 * There are other static factory methods available.  Use getCurrency
 * to get the currency number format for that country.  Use getPercent
 * to get a format for displaying percentages. With this format, a
 * fraction from 0.53 is displayed as 53%.
 * <P>
 * You can also control the display of numbers with such methods as
 * getMinimumFractionDigits.  If you want even more control over the
 * format or parsing, or want to give your users more control, you can
 * try casting the NumberFormat you get from the factory methods to a
 * DecimalNumberFormat. This will work for the vast majority of
 * countries; just remember to put it in a try block in case you
 * encounter an unusual one.
 * <P>
 * You can also use forms of the parse and format methods with
 * ParsePosition and FieldPosition to allow you to:
 * <ul type=round>
 *   <li>(a) progressively parse through pieces of a string.
 *   <li>(b) align the decimal point and other areas.
 * </ul>
 * For example, you can align numbers in two ways.
 * <P>
 * If you are using a monospaced font with spacing for alignment, you
 * can pass the FieldPosition in your format call, with field =
 * INTEGER_FIELD. On output, getEndIndex will be set to the offset
 * between the last character of the integer and the decimal. Add
 * (desiredSpaceCount - getEndIndex) spaces at the front of the
 * string.
 * <P>
 * If you are using proportional fonts, instead of padding with
 * spaces, measure the width of the string in pixels from the start to
 * getEndIndex.  Then move the pen by (desiredPixelWidth -
 * widthToAlignmentPoint) before drawing the text.  It also works
 * where there is no decimal, but possibly additional characters at
 * the end, e.g. with parentheses in negative numbers: "(12)" for -12.
 */
class U_I18N_API NumberFormat : public Format {
public:

    /**
     * Alignment Field constants used to construct a FieldPosition object.
     * Signifies that the position of the integer part or fraction part of
     * a formatted number should be returned.
     *
     * @see FieldPosition
     */
    enum EAlignmentFields {
        kIntegerField,
        kFractionField,


    /**
     * These constants are provided for backwards compatibility only,
     * and are deprecated.  Please use the C++ style constants defined above.
     */       
        INTEGER_FIELD        = kIntegerField,
        FRACTION_FIELD        = kFractionField
    };

    virtual ~NumberFormat();

    /**
     * Return true if the given Format objects are semantically equal.
     * Objects of different subclasses are considered unequal.
     */
    virtual bool_t operator==(const Format& other) const;

    /**
     * Format an object to produce a string.  This method handles
     * Formattable objects with numeric types. If the Formattable
     * object type is not a numeric type, then it returns a failing
     * UErrorCode.
     *
     * @param obj           The object to format.
     * @param toAppendTo    Where the text is to be appended.
     * @param pos           On input: an alignment field, if desired.
     *                      On output: the offsets of the alignment field.
     * @param status        Output param filled with success/failure status.
     * @return              The value passed in as toAppendTo (this allows chaining,
     *                      as with UnicodeString::append())
     */
    virtual UnicodeString& format(const Formattable& obj,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos,
                                  UErrorCode& status) const;

    /**
     * Parse a string to produce an object.  This methods handles
     * parsing of numeric strings into Formattable objects with numeric
     * types.
     * <P>
     * Before calling, set parse_pos.index to the offset you want to
     * start parsing at in the source. After calling, parse_pos.index
     * is the end of the text you parsed.  If error occurs, index is
     * unchanged.
     * <P>
     * When parsing, leading whitespace is discarded (with successful
     * parse), while trailing whitespace is left as is.
     * <P>
     * See Format::parseObject() for more.
     *
     * @param source    The string to be parsed into an object.
     * @param result    Formattable to be set to the parse result.
     *                  If parse fails, return contents are undefined.
     * @param parse_pos The position to start parsing at. Upon return
     *                  this param is set to the position after the
     *                  last character successfully parsed. If the
     *                  source is not parsed successfully, this param
     *                  will remain unchanged.
     * @return          A newly created Formattable* object, or NULL
     *                  on failure.  The caller owns this and should
     *                  delete it when done.
     */
    virtual void parseObject(const UnicodeString& source,
                             Formattable& result,
                             ParsePosition& parse_pos) const;

    /**
     * Format a double or long number. These methods call the NumberFormat
     * pure virtual format() methods with the default FieldPosition.
     *
     * @param number    The value to be formatted.
     * @param output    Output param with the formatted string.
     * @return          A reference to 'output' param.
     */
    UnicodeString& format(  double number,
                            UnicodeString& output) const;

    UnicodeString& format(  int32_t number,
                            UnicodeString& output) const;

   /**
    * Format a double or long number. Concrete subclasses must implement
    * these pure virtual methods.
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
                                  FieldPosition& pos) const = 0;
    virtual UnicodeString& format(int32_t number,
                                  UnicodeString& toAppendTo,
                                  FieldPosition& pos) const = 0;

   /**
    * Return a long if possible (e.g. within range LONG_MAX,
    * LONG_MAX], and with no decimals), otherwise a double.  If
    * IntegerOnly is set, will stop at a decimal point (or equivalent;
    * e.g. for rational numbers "1 2/3", will stop after the 1).
    * <P>
    * If no object can be parsed, index is unchanged, and NULL is
    * returned.
    * <P>
    * This is a pure virtual which concrete subclasses must implement.
    *
    * @param text           The text to be parsed.
    * @param result         Formattable to be set to the parse result.
    *                       If parse fails, return contents are undefined.
    * @param parsePosition  The position to start parsing at on input.
    *                       On output, moved to after the last successfully
    *                       parse character. On parse failure, does not change.
    * @return               A Formattable object of numeric type.  The caller
    *                       owns this an must delete it.  NULL on failure.
    */
    virtual void parse(const UnicodeString& text,
                       Formattable& result,
                       ParsePosition& parsePosition) const = 0;

    /**
     * Parse a string as a numeric value, and return a Formattable
     * numeric object. This method parses integers only if IntegerOnly
     * is set.
     *
     * @param text          The text to be parsed.
     * @param result        Formattable to be set to the parse result.
     *                      If parse fails, return contents are undefined.
     * @param status        Success or failure output parameter.
     * @return              A Formattable object of numeric type.  The caller
     *                      owns this an must delete it.  NULL on failure.
     * @see                 NumberFormat::isParseIntegerOnly
     */
    virtual void parse( const UnicodeString& text,
                        Formattable& result,
                        UErrorCode& status) const;

    /**
     * Return true if this format will parse numbers as integers
     * only.  For example in the English locale, with ParseIntegerOnly
     * true, the string "1234." would be parsed as the integer value
     * 1234 and parsing would stop at the "." character.  Of course,
     * the exact format accepted by the parse operation is locale
     * dependant and determined by sub-classes of NumberFormat.
     */
    bool_t isParseIntegerOnly(void) const;

    /**
     * Sets whether or not numbers should be parsed as integers only.
     * @see isParseIntegerOnly
     */
    virtual void setParseIntegerOnly(bool_t value);

    /**
     * Returns the default number format for the current default
     * locale.  The default format is one of the styles provided by
     * the other factory methods: getNumberInstance,
     * getCurrencyInstance or getPercentInstance.  Exactly which one
     * is locale dependant.
     */
    static NumberFormat* createInstance(UErrorCode&);

    /**
     * Returns the default number format for the specified locale.
     * The default format is one of the styles provided by the other
     * factory methods: getNumberInstance, getCurrencyInstance or
     * getPercentInstance.  Exactly which one is locale dependant.
     */
    static NumberFormat* createInstance(const Locale& inLocale,
                                        UErrorCode&);

    /**
     * Returns a currency format for the current default locale.
     */
    static NumberFormat* createCurrencyInstance(UErrorCode&);

    /**
     * Returns a currency format for the specified locale.
     */
    static NumberFormat* createCurrencyInstance(const Locale& inLocale,
                                                UErrorCode&);

    /**
     * Returns a percentage format for the current default locale.
     */
    static NumberFormat* createPercentInstance(UErrorCode&);

    /**
     * Returns a percentage format for the specified locale.
     */
    static NumberFormat* createPercentInstance(const Locale& inLocale,
                                               UErrorCode&);

    /**
     * Returns a scientific format for the current default locale.
     */
    static NumberFormat* createScientificInstance(UErrorCode&);

    /**
     * Returns a scientific format for the specified locale.
     */
    static NumberFormat* createScientificInstance(const Locale& inLocale,
                                                UErrorCode&);

    /**
     * Get the set of Locales for which NumberFormats are installed.
     */
    static const Locale* getAvailableLocales(int32_t& count);

    /**
     * Returns true if grouping is used in this format. For example,
     * in the English locale, with grouping on, the number 1234567
     * might be formatted as "1,234,567". The grouping separator as
     * well as the size of each group is locale dependant and is
     * determined by sub-classes of NumberFormat.
     * @see setGroupingUsed
     */
    bool_t isGroupingUsed(void) const;

    /**
     * Set whether or not grouping will be used in this format.
     * @see getGroupingUsed
     */
    virtual void setGroupingUsed(bool_t newValue);

    /**
     * Returns the maximum number of digits allowed in the integer portion of a
     * number.
     * @see setMaximumIntegerDigits
     */
    int32_t getMaximumIntegerDigits(void) const;

    /**
     * Sets the maximum number of digits allowed in the integer portion of a
     * number. maximumIntegerDigits must be >= minimumIntegerDigits.  If the
     * new value for maximumIntegerDigits is less than the current value
     * of minimumIntegerDigits, then minimumIntegerDigits will also be set to
     * the new value.
     *
     * @see getMaximumIntegerDigits
     */
    virtual void setMaximumIntegerDigits(int32_t newValue);

    /**
     * Returns the minimum number of digits allowed in the integer portion of a
     * number.
     * @see setMinimumIntegerDigits
     */
    int32_t getMinimumIntegerDigits(void) const;

    /**
     * Sets the minimum number of digits allowed in the integer portion of a
     * number. minimumIntegerDigits must be &lt;= maximumIntegerDigits.  If the
     * new value for minimumIntegerDigits exceeds the current value
     * of maximumIntegerDigits, then maximumIntegerDigits will also be set to
     * the new value.
     * @see getMinimumIntegerDigits
     */
    virtual void setMinimumIntegerDigits(int32_t newValue);

    /**
     * Returns the maximum number of digits allowed in the fraction portion of a
     * number.
     * @see setMaximumFractionDigits
     */
    int32_t getMaximumFractionDigits(void) const;

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a
     * number. maximumFractionDigits must be >= minimumFractionDigits.  If the
     * new value for maximumFractionDigits is less than the current value
     * of minimumFractionDigits, then minimumFractionDigits will also be set to
     * the new value.
     * @see getMaximumFractionDigits
     */
    virtual void setMaximumFractionDigits(int32_t newValue);

    /**
     * Returns the minimum number of digits allowed in the fraction portion of a
     * number.
     * @see setMinimumFractionDigits
     */
    int32_t getMinimumFractionDigits(void) const;

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number. minimumFractionDigits must be &lt;= maximumFractionDigits.   If the
     * new value for minimumFractionDigits exceeds the current value
     * of maximumFractionDigits, then maximumIntegerDigits will also be set to
     * the new value
     * @see getMinimumFractionDigits
     */
    virtual void setMinimumFractionDigits(int32_t newValue);

public:

    /**
     * Return the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     * <pre>
     * .   Base* polymorphic_pointer = createPolymorphicObject();
     * .   if (polymorphic_pointer->getDynamicClassID() ==
     * .       Derived::getStaticClassID()) ...
     * </pre>
     * @return The class ID for all objects of this class.
     */
    static ClassID getStaticClassID(void) { return (ClassID)&fgClassID; }

    /**
     * Override Calendar
     * Returns a unique class ID POLYMORPHICALLY.  Pure virtual override.
     * This method is to implement a simple version of RTTI, since not all
     * C++ compilers support genuine RTTI.  Polymorphic operator==() and
     * clone() methods call this method.
     * <P>
     * @return The class ID for this object. All objects of a
     * given class have the same class ID.  Objects of
     * other classes have different class IDs.
     */
    virtual ClassID getDynamicClassID(void) const { return getStaticClassID(); }

protected:

    /**
     * Default constructor for subclass use only.
     */
    NumberFormat();

    /**
     * Copy constructor.
     */
    NumberFormat(const NumberFormat&);

    /**
     * Assignment operator.
     */
    NumberFormat& operator=(const NumberFormat&);

protected:
    static const int32_t fgMaxIntegerDigits;
    static const int32_t fgMinIntegerDigits;

private:
    static char fgClassID;

    enum EStyles {
        kNumberStyle,
        kCurrencyStyle,
        kPercentStyle,
        kScientificStyle,
        kStyleCount // ALWAYS LAST ENUM: number of styles
    };

    static NumberFormat* createInstance(const Locale& desiredLocale, EStyles choice, UErrorCode& success);

    static const int32_t         fgNumberPatternsCount;
    static const UnicodeString     fgLastResortNumberPatterns[];

    bool_t      fGroupingUsed;
    int32_t     fMaxIntegerDigits;
    int32_t     fMinIntegerDigits;
    int32_t     fMaxFractionDigits;
    int32_t     fMinFractionDigits;
    bool_t      fParseIntegerOnly;
};
 
// -------------------------------------
 
inline bool_t
NumberFormat::isParseIntegerOnly() const
{
    return fParseIntegerOnly;
}
 
#endif // _NUMFMT
//eof
