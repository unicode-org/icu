/*
********************************************************************************
*   Copyright (C) 1997-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File DECIMFMT.H
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/20/97    clhuang     Updated per C++ implementation.
*   04/03/97    aliu        Rewrote parsing and formatting completely, and
*                           cleaned up and debugged.  Actually works now.
*   04/17/97    aliu        Changed DigitCount to int per code review.
*   07/10/97    helena      Made ParsePosition a class and get rid of the function
*                           hiding problems.
*   09/09/97    aliu        Ported over support for exponential formats.
*    07/20/98    stephen        Changed documentation
********************************************************************************
*/
 
#ifndef DECIMFMT_H
#define DECIMFMT_H
 
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/dcfmtsym.h"
#include "unicode/numfmt.h"
#include "unicode/locid.h"

U_NAMESPACE_BEGIN

class DigitList;
class ChoiceFormat;

/**
 * Concrete class for formatting decimal numbers, allowing a variety
 * of parameters, and localization to Western, Arabic, or Indic numbers.
 * <P>
 * Normally, you get the proper NumberFormat for a specific locale
 * (including the default locale) using the NumberFormat factory methods,
 * rather than constructing a DecimalNumberFormat directly.
 * <P>
 * Either the prefixes or the suffixes must be different for the parse
 * to distinguish positive from negative.  Parsing will be unreliable
 * if the digits, thousands or decimal separators are the same, or if
 * any of them occur in the prefixes or suffixes.
 * <P>
 * [Special cases:] 
 * <P>
 * NaN is formatted as a single character, typically \\uFFFD.
 * <P>
 * +/-Infinity is formatted as a single character, typically \\u221E,
 * plus the positive and negative pre/suffixes.
 * <P>
 * Note: this class is designed for common users; for very large or small
 * numbers, use a format that can express exponential values.
 * <P>
 * [Example:] 
 * <pre>
 * \code
 *     // normally we would have a GUI with a menu for this
 *     int32_t locCount;
 *     const Locale* locales = NumberFormat::getAvailableLocales(locCount);
 *     if (locCount > 12) locCount = 12;  //limit output
 * 
 *     double myNumber = -1234.56;
 *     UErrorCode success = U_ZERO_ERROR;
 *     NumberFormat* form; //= NumberFormat::createInstance(success);
 * 
 *     // just for fun, we print out a number with the locale number, currency
 *     // and percent format for each locale we can.
 *     UnicodeString countryName;
 *     UnicodeString displayName;
 *     UnicodeString str;
 *     UnicodeString pattern;
 *     Formattable fmtable;
 *     for (int32_t j = 0; j < 3; ++j) {
 *         cout << endl << "FORMAT " << j << endl;
 *         for (int32_t i = 0; i < locCount; ++i) {
 *             if (locales[i].getCountry(countryName).size() == 0) {
 *                 // skip language-only
 *                 continue;
 *             }
 *             switch (j) {
 *             default:
 *                 form = NumberFormat::createInstance(locales[i], success ); break;
 *             case 1:
 *                 form = NumberFormat::createCurrencyInstance(locales[i], success ); break;
 *             case 0:
 *                 form = NumberFormat::createPercentInstance(locales[i], success ); break;
 *             }
 *             if (form) {
 *                 str.remove();
 *                 pattern = ((DecimalFormat*)form)->toPattern(pattern);
 *                 cout << locales[i].getDisplayName(displayName) << ": " << pattern;
 *                 cout << "  ->  " << form->format(myNumber,str) << endl;
 *                 form->parse(form->format(myNumber,str), fmtable, success);
 *                 //cout << "   parsed: " << fmtable << endl;
 *                 delete form;  
 *             }
 *         }
 *     }
 * \endcode
 * </pre>
 * [The following shows the structure of the pattern.] 
 * <pre>
 * \code
 *     pattern    := subpattern{;subpattern}
 *     subpattern := {prefix}integer{.fraction}{suffix}
 *     
 *     prefix     := '\\u0000'..'\\uFFFD' - specialCharacters
 *     suffix     := '\\u0000'..'\\uFFFD' - specialCharacters
 *     integer    := '#'* '0'* '0'
 *     fraction   := '0'* '#'*
 *   
 *  Notation:
 *     X*       0 or more instances of X
 *     (X | Y)  either X or Y.
 *     X..Y     any character from X up to Y, inclusive.
 *     S - T    characters in S, except those in T
 * \code
 * /pre>
 * The first subpattern is for positive numbers. The second (optional)
 * subpattern is used for negative numbers. (In both cases, ',' can
 * occur inside the integer portion--it is just too messy to indicate
 * in BNF.)  For the second subpattern, only the PREFIX and SUFFIX are
 * noted; other attributes are taken only from the first subpattern.
 * <P>
 * Here are the special characters used in the parts of the
 * subpattern, with notes on their usage.
 * <pre>
 * \code
 *     Symbol   Meaning
 *       0      a digit, showing up a zero if it is zero
 *       #      a digit, supressed if zero
 *       .      placeholder for decimal separator
 *       ,      placeholder for grouping separator.
 *       E      separates mantissa and exponent for exponential formats.
 *       ;      separates formats.
 *       -      default negative prefix.
 *       %      multiply by 100 and show as percentage
 *       \u2030 multiply by 1000 and show as per mille
 *       \u00A4 currency sign; replaced by currency symbol; if
 *              doubled, replaced by international currency symbol.
 *              If present in a pattern, the monetary decimal separator
 *              is used instead of the decimal separator.
 *       X      any other characters can be used in the prefix or suffix
 *       '      used to quote special characters in a prefix or suffix.
 * \endcode
 * </pre>
 * [Notes] 
 * <P>
 * If there is no explicit negative subpattern, - is prefixed to the
 * positive form. That is, "0.00" alone is equivalent to "0.00;-0.00".
 * <P>
 * Illegal formats, such as "#.#.#" in the same format, will cause a
 * failing UErrorCode to be returned. 
 * <P>
 * The grouping separator is commonly used for thousands, but in some
 * countries for ten-thousands. The interval is a constant number of
 * digits between the grouping characters, such as 100,000,000 or 1,0000,0000.
 * If you supply a pattern with multiple grouping characters, the interval
 * between the last one and the end of the integer determines the primary
 * grouping size, and the interval between the last two determines
 * the secondary grouping size (see below); all others are ignored.
 * So "#,##,###,####" == "###,###,####" == "##,#,###,####".
 * <P>
 * Some locales have two different grouping intervals:  One used for the
 * least significant integer digits (the primary grouping size), and
 * one used for all others (the secondary grouping size).  For example,
 * if the primary grouping interval is 3, and the secondary is 2, then
 * this corresponds to the pattern "#,##,##0", and the number 123456789
 * is formatted as "12,34,56,789".
 * <P>
 * This class only handles localized digits where the 10 digits are
 * contiguous in Unicode, from 0 to 9. Other digits sets (such as
 * superscripts) would need a different subclass.
 */
class U_I18N_API DecimalFormat: public NumberFormat {
public:
  /** Rounding mode 
   *  @draft ICU 2.4
   */
    enum ERoundingMode {
        kRoundCeiling,
        kRoundFloor,
        kRoundDown,
        kRoundUp,
        kRoundHalfEven,
        kRoundHalfDown,
        kRoundHalfUp
        // We don't support ROUND_UNNECESSARY
    };

  /** Pad position
   *  @draft ICU 2.4
   */
    enum EPadPosition {
        kPadBeforePrefix,
        kPadAfterPrefix,
        kPadBeforeSuffix,
        kPadAfterSuffix
    };

    /**
     * Create a DecimalFormat using the default pattern and symbols
     * for the default locale. This is a convenient way to obtain a
     * DecimalFormat when internationalization is not the main concern.
     * <P>
     * To obtain standard formats for a given locale, use the factory methods
     * on NumberFormat such as createInstance. These factories will
     * return the most appropriate sub-class of NumberFormat for a given
     * locale.
     * @param status    Output param set to success/failure code. If the
     *                  pattern is invalid this will be set to a failure code.
     * @stable ICU 2.0
     */
    DecimalFormat(UErrorCode& status);

    /**
     * Create a DecimalFormat from the given pattern and the symbols
     * for the default locale. This is a convenient way to obtain a
     * DecimalFormat when internationalization is not the main concern.
     * <P>
     * To obtain standard formats for a given locale, use the factory methods
     * on NumberFormat such as createInstance. These factories will
     * return the most appropriate sub-class of NumberFormat for a given
     * locale.
     * @param pattern   A non-localized pattern string.
     * @param status    Output param set to success/failure code. If the
     *                  pattern is invalid this will be set to a failure code.
     * @stable ICU 2.0
     */
    DecimalFormat(const UnicodeString& pattern,
                  UErrorCode& status);

    /**
     * Create a DecimalFormat from the given pattern and symbols.
     * Use this constructor when you need to completely customize the
     * behavior of the format.
     * <P>
     * To obtain standard formats for a given
     * locale, use the factory methods on NumberFormat such as
     * createInstance or createCurrencyInstance. If you need only minor adjustments
     * to a standard format, you can modify the format returned by
     * a NumberFormat factory method.
     *
     * @param pattern           a non-localized pattern string
     * @param symbolsToAdopt    the set of symbols to be used.  The caller should not
     *                          delete this object after making this call.
     * @param status            Output param set to success/failure code. If the
     *                          pattern is invalid this will be set to a failure code.
     * @stable ICU 2.0
     */
    DecimalFormat(  const UnicodeString& pattern,
                    DecimalFormatSymbols* symbolsToAdopt,
                    UErrorCode& status);

    /**
     * Create a DecimalFormat from the given pattern and symbols.
     * Use this constructor when you need to completely customize the
     * behavior of the format.
     * <P>
     * To obtain standard formats for a given
     * locale, use the factory methods on NumberFormat such as
     * createInstance or createCurrencyInstance. If you need only minor adjustments
     * to a standard format, you can modify the format returned by
     * a NumberFormat factory method.
     *
     * @param pattern           a non-localized pattern string
     * @param symbolsToAdopt    the set of symbols to be used.  The caller should not
     *                          delete this object after making this call.
     * @param parseError        Output param to receive errors occured during parsing 
     * @param status            Output param set to success/failure code. If the
     *                          pattern is invalid this will be set to a failure code.
     * @stable ICU 2.0
     */
    DecimalFormat(  const UnicodeString& pattern,
                    DecimalFormatSymbols* symbolsToAdopt,
                    UParseError& parseError,
                    UErrorCode& status);
    /**
     * Create a DecimalFormat from the given pattern and symbols.
     * Use this constructor when you need to completely customize the
     * behavior of the format.
     * <P>
     * To obtain standard formats for a given
     * locale, use the factory methods on NumberFormat such as
     * createInstance or createCurrencyInstance. If you need only minor adjustments
     * to a standard format, you can modify the format returned by
     * a NumberFormat factory method.
     *
     * @param pattern           a non-localized pattern string
     * @param symbols   the set of symbols to be used
     * @param status            Output param set to success/failure code. If the
     *                          pattern is invalid this will be set to a failure code.
     * @stable ICU 2.0
     */
    DecimalFormat(  const UnicodeString& pattern,
                    const DecimalFormatSymbols& symbols,
                    UErrorCode& status);

    /**
     * Copy constructor.
     * 
     * @param source    the DecimalFormat object to be copied from.
     * @stable ICU 2.0
     */
    DecimalFormat(const DecimalFormat& source);

    /**
     * Assignment operator.
     *
     * @param rhs    the DecimalFormat object to be copied.
     * @stable ICU 2.0
     */
    DecimalFormat& operator=(const DecimalFormat& rhs);

    /**
     * Destructor.
     * @stable ICU 2.0
     */
    virtual ~DecimalFormat();

    /**
     * Clone this Format object polymorphically. The caller owns the
     * result and should delete it when done.
     *
     * @return    a polymorphic copy of this DecimalFormat.
     * @stable ICU 2.0
     */
    virtual Format* clone(void) const;

    /**
     * Return true if the given Format objects are semantically equal.
     * Objects of different subclasses are considered unequal.
     *
     * @param other    the object to be compared with.
     * @return         true if the given Format objects are semantically equal.
     * @stable ICU 2.0
     */
    virtual UBool operator==(const Format& other) const;

    /**
     * Format a double or long number using base-10 representation.
     *
     * @param number    The value to be formatted.
     * @param appendTo  Output parameter to receive result.
     *                  Result is appended to existing contents.
     * @param pos       On input: an alignment field, if desired.
     *                  On output: the offsets of the alignment field.
     * @return          Reference to 'appendTo' parameter.
     * @stable ICU 2.0
    */
    virtual UnicodeString& format(double number,
                                  UnicodeString& appendTo,
                                  FieldPosition& pos) const;
    /**
     * Format a long number using base-10 representation.
     *
     * @param number    The value to be formatted.
     * @param appendTo  Output parameter to receive result.
     *                  Result is appended to existing contents.
     * @param pos       On input: an alignment field, if desired.
     *                  On output: the offsets of the alignment field.
     * @return          Reference to 'appendTo' parameter.
     * @stable ICU 2.0
     */
    virtual UnicodeString& format(int32_t number,
                                  UnicodeString& appendTo,
                                  FieldPosition& pos) const;
    /**
     * Format a Formattable using base-10 representation.
     *
     * @param obj       The value to be formatted.
     * @param appendTo  Output parameter to receive result.
     *                  Result is appended to existing contents.
     * @param pos       On input: an alignment field, if desired.
     *                  On output: the offsets of the alignment field.
     * @param status    Error code indicating success or failure.
     * @return          Reference to 'appendTo' parameter.
     * @stable ICU 2.0
     */
    virtual UnicodeString& format(const Formattable& obj,
                                  UnicodeString& appendTo,
                                  FieldPosition& pos,
                                  UErrorCode& status) const;

    /**
     * Redeclared NumberFormat method.
     * Formats an object to produce a string.
     *
     * @param obj       The object to format.
     * @param appendTo  Output parameter to receive result.
     *                  Result is appended to existing contents.
     * @param status    Output parameter filled in with success or failure status.
     * @return          Reference to 'appendTo' parameter.
     * @stable ICU 2.0
     */
    UnicodeString& format(const Formattable& obj,
                          UnicodeString& appendTo,
                          UErrorCode& status) const;

    /**
     * Redeclared NumberFormat method.
     * Format a double number.
     *
     * @param number    The value to be formatted.
     * @param appendTo  Output parameter to receive result.
     *                  Result is appended to existing contents.
     * @return          Reference to 'appendTo' parameter.
     * @stable ICU 2.0
     */
    UnicodeString& format(double number,
                          UnicodeString& appendTo) const;

    /**
     * Redeclared NumberFormat method.
     * Format a long number. These methods call the NumberFormat
     * pure virtual format() methods with the default FieldPosition.
     *
     * @param number    The value to be formatted.
     * @param appendTo  Output parameter to receive result.
     *                  Result is appended to existing contents.
     * @return          Reference to 'appendTo' parameter.
     * @stable ICU 2.0
     */
    UnicodeString& format(int32_t number,
                          UnicodeString& appendTo) const;

   /**
    * Parse the given string using this object's choices. The method
    * does string comparisons to try to find an optimal match.
    * If no object can be parsed, index is unchanged, and NULL is
    * returned.  The result is returned as the most parsimonious
    * type of <code>Formattable</code> that will accomodate all of the
    * necessary precision.  For example, if the result is exactly 12,
    * it will be returned as a long.  However, if it is 1.5, it will
    * be returned as a double.
    *
    * @param text           The text to be parsed.
    * @param result         Formattable to be set to the parse result.
    *                       If parse fails, return contents are undefined.
    * @param parsePosition  The position to start parsing at on input.
    *                       On output, moved to after the last successfully
    *                       parse character. On parse failure, does not change.
    * @see Formattable
    * @stable ICU 2.0
    */
    virtual void parse(const UnicodeString& text,
                       Formattable& result,
                       ParsePosition& parsePosition) const;

    // Declare here again to get rid of function hiding problems.
    /** 
     * Parse the given string using this object's choices.
     *
     * @param text           The text to be parsed.
     * @param result         Formattable to be set to the parse result.
     * @param status    Output parameter filled in with success or failure status.
     * @stable ICU 2.0
     */
    virtual void parse(const UnicodeString& text, 
                       Formattable& result, 
                       UErrorCode& status) const;

    /**
     * Returns the decimal format symbols, which is generally not changed
     * by the programmer or user.
     * @return desired DecimalFormatSymbols
     * @see DecimalFormatSymbols
     * @stable ICU 2.0
     */
    virtual const DecimalFormatSymbols* getDecimalFormatSymbols(void) const;

    /**
     * Sets the decimal format symbols, which is generally not changed
     * by the programmer or user.
     * @param symbolsToAdopt DecimalFormatSymbols to be adopted.
     * @stable ICU 2.0
     */
    virtual void adoptDecimalFormatSymbols(DecimalFormatSymbols* symbolsToAdopt);

    /**
     * Sets the decimal format symbols, which is generally not changed
     * by the programmer or user.
     * @param symbols DecimalFormatSymbols.
     * @stable ICU 2.0
     */
    virtual void setDecimalFormatSymbols(const DecimalFormatSymbols& symbols);


    /**
     * Get the positive prefix.
     *
     * @param result    Output param which will receive the positive prefix.
     * @return          A reference to 'result'.
     * Examples: +123, $123, sFr123
     * @stable ICU 2.0
     */
    UnicodeString& getPositivePrefix(UnicodeString& result) const;

    /**
     * Set the positive prefix.
     *
     * @param newValue    the new value of the the positive prefix to be set.
     * Examples: +123, $123, sFr123
     * @stable ICU 2.0
     */
    virtual void setPositivePrefix(const UnicodeString& newValue);

    /**
     * Get the negative prefix.
     *
     * @param result    Output param which will receive the negative prefix.
     * @return          A reference to 'result'.
     * Examples: -123, ($123) (with negative suffix), sFr-123
     * @stable ICU 2.0
     */
    UnicodeString& getNegativePrefix(UnicodeString& result) const;

    /**
     * Set the negative prefix.
     *
     * @param newValue    the new value of the the negative prefix to be set.
     * Examples: -123, ($123) (with negative suffix), sFr-123
     * @stable ICU 2.0
     */
    virtual void setNegativePrefix(const UnicodeString& newValue);

    /**
     * Get the positive suffix.
     *
     * @param result    Output param which will receive the positive suffix.
     * @return          A reference to 'result'.
     * Example: 123%
     * @stable ICU 2.0
     */
    UnicodeString& getPositiveSuffix(UnicodeString& result) const;

    /**
     * Set the positive suffix.
     *
     * @param newValue    the new value of the positive suffix to be set.
     * Example: 123%
     * @stable ICU 2.0
     */
    virtual void setPositiveSuffix(const UnicodeString& newValue);

    /**
     * Get the negative suffix.
     *
     * @param result    Output param which will receive the negative suffix.
     * @return          A reference to 'result'.
     * Examples: -123%, ($123) (with positive suffixes)
     * @stable ICU 2.0
     */
    UnicodeString& getNegativeSuffix(UnicodeString& result) const;

    /**
     * Set the negative suffix.
     *
     * @param newValue    the new value of the negative suffix to be set.
     * Examples: 123%
     * @stable ICU 2.0
     */
    virtual void setNegativeSuffix(const UnicodeString& newValue);

    /**
     * Get the multiplier for use in percent, permill, etc.
     * For a percentage, set the suffixes to have "%" and the multiplier to be 100.
     * (For Arabic, use arabic percent symbol).
     * For a permill, set the suffixes to have "\u2031" and the multiplier to be 1000.
     *
     * @return    the multiplier for use in percent, permill, etc.
     * Examples: with 100, 1.23 -> "123", and "123" -> 1.23
     * @stable ICU 2.0
     */
    int32_t getMultiplier(void) const;

    /**
     * Set the multiplier for use in percent, permill, etc.
     * For a percentage, set the suffixes to have "%" and the multiplier to be 100.
     * (For Arabic, use arabic percent symbol).
     * For a permill, set the suffixes to have "\u2031" and the multiplier to be 1000.
     *
     * @param newValue    the new value of the multiplier for use in percent, permill, etc.
     * Examples: with 100, 1.23 -> "123", and "123" -> 1.23
     * @stable ICU 2.0
     */
    virtual void setMultiplier(int32_t newValue);

    /**
     * Get the rounding increment.
     * @return A positive rounding increment, or 0.0 if rounding
     * is not in effect.
     * @see #setRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     * @stable ICU 2.0
     */
    virtual double getRoundingIncrement(void);

    /**
     * Set the rounding increment.  This method also controls whether
     * rounding is enabled.
     * @param newValue A positive rounding increment, or 0.0 to disable rounding.
     * Negative increments are equivalent to 0.0.
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @see #setRoundingMode
     * @stable ICU 2.0
     */
    virtual void setRoundingIncrement(double newValue);

    /**
     * Get the rounding mode.
     * @return A rounding mode
     * @see #setRoundingIncrement
     * @see #getRoundingIncrement
     * @see #setRoundingMode
     * @stable ICU 2.0
     */
    virtual ERoundingMode getRoundingMode(void);

    /**
     * Set the rounding mode.  This has no effect unless the rounding
     * increment is greater than zero.
     * @param roundingMode A rounding mode
     * @see #setRoundingIncrement
     * @see #getRoundingIncrement
     * @see #getRoundingMode
     * @stable ICU 2.0
     */
    virtual void setRoundingMode(ERoundingMode roundingMode);

    /**
     * Get the width to which the output of <code>format()</code> is padded.
     * @return the format width, or zero if no padding is in effect
     * @see #setFormatWidth
     * @see #getPadCharacter
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     * @stable ICU 2.0
     */
    virtual int32_t getFormatWidth(void);

    /**
     * Set the width to which the output of <code>format()</code> is padded.
     * This method also controls whether padding is enabled.
     * @param width the width to which to pad the result of
     * <code>format()</code>, or zero to disable padding.  A negative
     * width is equivalent to 0.
     * @see #getFormatWidth
     * @see #getPadCharacter
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     * @stable ICU 2.0
     */
    virtual void setFormatWidth(int32_t width);

    /**
     * Get the grapheme string (a character, possibly with modifier letters)
     * used to pad to the format width.  The default is " ".
     * Note: The current implementation only stores the first code unit of the
     * pad string.
     * @return the pad grapheme string
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     * @stable ICU 2.0
     */
    virtual UnicodeString getPadCharacterString();

    /**
     * Set the grapheme string (a character, possibly with modifier letters)
     * used to pad to the format width.  This has no effect
     * unless padding is enabled.
     * Note: The current implementation only stores the first code unit of the
     * pad string.
     * @param padChar the pad grapheme
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #getPadCharacter
     * @see #getPadPosition
     * @see #setPadPosition
     * @stable ICU 2.0
     */
    virtual void setPadCharacter(const UnicodeString &padChar);

    /**
     * Get the position at which padding will take place.  This is the location
     * at which padding will be inserted if the result of <code>format()</code>
     * is shorter than the format width.
     * @return the pad position, one of <code>kPadBeforePrefix</code>,
     * <code>kPadAfterPrefix</code>, <code>kPadBeforeSuffix</code>, or
     * <code>kPadAfterSuffix</code>.
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadCharacter
     * @see #setPadPosition
     * @see #kPadBeforePrefix
     * @see #kPadAfterPrefix
     * @see #kPadBeforeSuffix
     * @see #kPadAfterSuffix
     * @stable ICU 2.0
     */
    virtual EPadPosition getPadPosition(void);

    /**
     * <strong><font face=helvetica color=red>NEW</font></strong>
     * Set the position at which padding will take place.  This is the location
     * at which padding will be inserted if the result of <code>format()</code>
     * is shorter than the format width.  This has no effect unless padding is
     * enabled.
     * @param padPos the pad position, one of <code>kPadBeforePrefix</code>,
     * <code>kPadAfterPrefix</code>, <code>kPadBeforeSuffix</code>, or
     * <code>kPadAfterSuffix</code>.
     * @see #setFormatWidth
     * @see #getFormatWidth
     * @see #setPadCharacter
     * @see #getPadCharacter
     * @see #getPadPosition
     * @see #kPadBeforePrefix
     * @see #kPadAfterPrefix
     * @see #kPadBeforeSuffix
     * @see #kPadAfterSuffix
     * @stable ICU 2.0
     */
    virtual void setPadPosition(EPadPosition padPos);

    /**
     * Return whether or not scientific notation is used.
     * @return TRUE if this object formats and parses scientific notation
     * @see #setScientificNotation
     * @see #getMinimumExponentDigits
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    virtual UBool isScientificNotation(void);

    /**
     * Set whether or not scientific notation is used.
     * @param useScientific TRUE if this object formats and parses scientific
     * notation
     * @see #isScientificNotation
     * @see #getMinimumExponentDigits
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    virtual void setScientificNotation(UBool useScientific);

    /**
     * Return the minimum exponent digits that will be shown.
     * @return the minimum exponent digits that will be shown
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    virtual int8_t getMinimumExponentDigits(void);

    /**
     * Set the minimum exponent digits that will be shown.  This has no
     * effect unless scientific notation is in use.
     * @param minExpDig a value >= 1 indicating the fewest exponent digits
     * that will be shown.  Values less than 1 will be treated as 1.
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #getMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    virtual void setMinimumExponentDigits(int8_t minExpDig);

    /**
     * Return whether the exponent sign is always shown.
     * @return TRUE if the exponent is always prefixed with either the
     * localized minus sign or the localized plus sign, false if only negative
     * exponents are prefixed with the localized minus sign.
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #getMinimumExponentDigits
     * @see #setExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    virtual UBool isExponentSignAlwaysShown(void);

    /**
     * Set whether the exponent sign is always shown.  This has no effect
     * unless scientific notation is in use.
     * @param expSignAlways TRUE if the exponent is always prefixed with either
     * the localized minus sign or the localized plus sign, false if only
     * negative exponents are prefixed with the localized minus sign.
     * @see #setScientificNotation
     * @see #isScientificNotation
     * @see #setMinimumExponentDigits
     * @see #getMinimumExponentDigits
     * @see #isExponentSignAlwaysShown
     * @stable ICU 2.0
     */
    virtual void setExponentSignAlwaysShown(UBool expSignAlways);

    /**
     * Return the grouping size. Grouping size is the number of digits between
     * grouping separators in the integer portion of a number.  For example,
     * in the number "123,456.78", the grouping size is 3.
     *
     * @return    the grouping size.
     * @see setGroupingSize
     * @see NumberFormat::isGroupingUsed
     * @see DecimalFormatSymbols::getGroupingSeparator
     * @stable ICU 2.0
     */
    int32_t getGroupingSize(void) const;

    /**
     * Set the grouping size. Grouping size is the number of digits between
     * grouping separators in the integer portion of a number.  For example,
     * in the number "123,456.78", the grouping size is 3.
     *
     * @param newValue    the new value of the grouping size.
     * @see getGroupingSize
     * @see NumberFormat::setGroupingUsed
     * @see DecimalFormatSymbols::setGroupingSeparator
     * @stable ICU 2.0
     */
    virtual void setGroupingSize(int32_t newValue);

    /**
     * Return the secondary grouping size. In some locales one
     * grouping interval is used for the least significant integer
     * digits (the primary grouping size), and another is used for all
     * others (the secondary grouping size).  A formatter supporting a
     * secondary grouping size will return a positive integer unequal
     * to the primary grouping size returned by
     * <code>getGroupingSize()</code>.  For example, if the primary
     * grouping size is 4, and the secondary grouping size is 2, then
     * the number 123456789 formats as "1,23,45,6789", and the pattern
     * appears as "#,##,###0".
     * @return the secondary grouping size, or a value less than
     * one if there is none
     * @see setSecondaryGroupingSize
     * @see NumberFormat::isGroupingUsed
     * @see DecimalFormatSymbols::getGroupingSeparator
     * @draft ICU 2.4
     */
    int32_t getSecondaryGroupingSize(void) const;

    /**
     * Set the secondary grouping size. If set to a value less than 1,
     * then secondary grouping is turned off, and the primary grouping
     * size is used for all intervals, not just the least significant.
     *
     * @param newValue    the new value of the secondary grouping size.
     * @see getSecondaryGroupingSize
     * @see NumberFormat#setGroupingUsed
     * @see DecimalFormatSymbols::setGroupingSeparator
     * @draft ICU 2.4
     */
    virtual void setSecondaryGroupingSize(int32_t newValue);

    /**
     * Allows you to get the behavior of the decimal separator with integers.
     * (The decimal separator will always appear with decimals.)
     *
     * @return    TRUE if the decimal separator always appear with decimals.
     * Example: Decimal ON: 12345 -> 12345.; OFF: 12345 -> 12345
     * @stable ICU 2.0
     */
    UBool isDecimalSeparatorAlwaysShown(void) const;

    /**
     * Allows you to set the behavior of the decimal separator with integers.
     * (The decimal separator will always appear with decimals.)
     *
     * @param newValue    set TRUE if the decimal separator will always appear with decimals.
     * Example: Decimal ON: 12345 -> 12345.; OFF: 12345 -> 12345
     * @stable ICU 2.0
     */
    virtual void setDecimalSeparatorAlwaysShown(UBool newValue);

    /**
     * Synthesizes a pattern string that represents the current state
     * of this Format object.
     *
     * @param result    Output param which will receive the pattern.
     *                  Previous contents are deleted.
     * @return          A reference to 'result'.
     * @see applyPattern
     * @stable ICU 2.0
     */
    virtual UnicodeString& toPattern(UnicodeString& result) const;

    /**
     * Synthesizes a localized pattern string that represents the current
     * state of this Format object.
     *
     * @param result    Output param which will receive the localized pattern.
     *                  Previous contents are deleted.
     * @return          A reference to 'result'.
     * @see applyPattern
     * @stable ICU 2.0
     */
    virtual UnicodeString& toLocalizedPattern(UnicodeString& result) const;
 
    /**
     * Apply the given pattern to this Format object.  A pattern is a
     * short-hand specification for the various formatting properties.
     * These properties can also be changed individually through the
     * various setter methods.
     * <P>
     * There is no limit to integer digits are set
     * by this routine, since that is the typical end-user desire;
     * use setMaximumInteger if you want to set a real value.
     * For negative numbers, use a second pattern, separated by a semicolon
     * <pre>
     * .      Example "#,#00.0#" -> 1,234.56
     * </pre>
     * This means a minimum of 2 integer digits, 1 fraction digit, and
     * a maximum of 2 fraction digits.
     * <pre>
     * .      Example: "#,#00.0#;(#,#00.0#)" for negatives in parantheses.
     * </pre>
     * In negative patterns, the minimum and maximum counts are ignored;
     * these are presumed to be set in the positive pattern.
     *
     * @param pattern    The pattern to be applied.
     * @param parseError Struct to recieve information on position 
     *                   of error if an error is encountered
     * @param status     Output param set to success/failure code on
     *                   exit. If the pattern is invalid, this will be
     *                   set to a failure result.
     * @stable ICU 2.0
     */
    virtual void applyPattern(const UnicodeString& pattern,
                             UParseError& parseError,
                             UErrorCode& status);
    /**
     * Sets the pattern.
     * @param pattern   The pattern to be applied.
     * @param status    Output param set to success/failure code on
     *                  exit. If the pattern is invalid, this will be
     *                  set to a failure result.
     * @stable ICU 2.0
     */  
    virtual void applyPattern(const UnicodeString& pattern,
                             UErrorCode& status);

    /**
     * Apply the given pattern to this Format object.  The pattern
     * is assumed to be in a localized notation. A pattern is a
     * short-hand specification for the various formatting properties.
     * These properties can also be changed individually through the
     * various setter methods.
     * <P>
     * There is no limit to integer digits are set
     * by this routine, since that is the typical end-user desire;
     * use setMaximumInteger if you want to set a real value.
     * For negative numbers, use a second pattern, separated by a semicolon
     * <pre>
     * .      Example "#,#00.0#" -> 1,234.56
     * </pre>
     * This means a minimum of 2 integer digits, 1 fraction digit, and
     * a maximum of 2 fraction digits.
     *
     * Example: "#,#00.0#;(#,#00.0#)" for negatives in parantheses.
     *
     * In negative patterns, the minimum and maximum counts are ignored;
     * these are presumed to be set in the positive pattern.
     *
     * @param pattern   The localized pattern to be applied.
     * @param parseError Struct to recieve information on position 
     *                   of error if an error is encountered
     * @param status    Output param set to success/failure code on
     *                  exit. If the pattern is invalid, this will be
     *                  set to a failure result.
     * @stable ICU 2.0
     */
    virtual void applyLocalizedPattern(const UnicodeString& pattern,
                                       UParseError& parseError,
                                       UErrorCode& status);

    /**
     * Apply the given pattern to this Format object.
     *
     * @param pattern   The localized pattern to be applied.
     * @param status    Output param set to success/failure code on
     *                  exit. If the pattern is invalid, this will be
     *                  set to a failure result.
     * @stable ICU 2.0
     */
    virtual void applyLocalizedPattern(const UnicodeString& pattern,
                                       UErrorCode& status);


    /**
     * Sets the maximum number of digits allowed in the integer portion of a
     * number. This override limits the integer digit count to 309.
     *
     * @param newValue    the new value of the maximum number of digits 
     *                      allowed in the integer portion of a number.
     * @see NumberFormat#setMaximumIntegerDigits
     * @stable ICU 2.0
     */
    virtual void setMaximumIntegerDigits(int32_t newValue);

    /**
     * Sets the minimum number of digits allowed in the integer portion of a
     * number. This override limits the integer digit count to 309.
     * 
     * @param newValue    the new value of the minimum number of digits 
     *                      allowed in the integer portion of a number.
     * @see NumberFormat#setMinimumIntegerDigits
     * @stable ICU 2.0
     */
    virtual void setMinimumIntegerDigits(int32_t newValue);

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a
     * number. This override limits the fraction digit count to 340.
     *
     * @param newValue    the new value of the maximum number of digits 
     *                    allowed in the fraction portion of a number.
     * @see NumberFormat#setMaximumFractionDigits
     * @stable ICU 2.0
     */
    virtual void setMaximumFractionDigits(int32_t newValue);

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number. This override limits the fraction digit count to 340.
     *
     * @param newValue    the new value of the minimum number of digits 
     *                    allowed in the fraction portion of a number.
     * @see NumberFormat#setMinimumFractionDigits
     * @stable ICU 2.0
     */
    virtual void setMinimumFractionDigits(int32_t newValue);

    /**
     * Sets the currency used to display currency
     * amounts.  This takes effect immediately, if this format is a
     * currency format.  If this format is not a currency format, then
     * the currency is used if and when this object becomes a
     * currency format through the application of a new pattern.
     * @param theCurrency a 3-letter ISO code indicating new currency
     * to use.  It need not be null-terminated.
     * @draft ICU 2.2
     */
    virtual void setCurrency(const UChar* theCurrency);

    /**
     * The resource tags we use to retrieve decimal format data from
     * locale resource bundles.
     * @stable ICU 2.0
     */
    static const char fgNumberPatterns[];

public:

    /**
     * Return the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     * <pre>
     * .      Base* polymorphic_pointer = createPolymorphicObject();
     * .      if (polymorphic_pointer->getDynamicClassID() ==
     * .          Derived::getStaticClassID()) ...
     * </pre>
     * @return          The class ID for all objects of this class.
     * @stable ICU 2.0
     */
    static inline UClassID getStaticClassID(void);

    /**
     * Returns a unique class ID POLYMORPHICALLY.  Pure virtual override.
     * This method is to implement a simple version of RTTI, since not all
     * C++ compilers support genuine RTTI.  Polymorphic operator==() and
     * clone() methods call this method.
     *
     * @return          The class ID for this object. All objects of a
     *                  given class have the same class ID.  Objects of
     *                  other classes have different class IDs.
     * @stable ICU 2.0
     */
    virtual UClassID getDynamicClassID(void) const;

private:
    static const char fgClassID;

    DecimalFormat(); // default constructor not implemented

    /**
     * Do real work of constructing a new DecimalFormat.
     */
    void construct(UErrorCode&               status,
                   UParseError&             parseErr,
                   const UnicodeString*     pattern = 0,
                   DecimalFormatSymbols*    symbolsToAdopt = 0
                   );

    /**
     * Does the real work of generating a pattern.
     *
     * @param result     Output param which will receive the pattern.
     *                   Previous contents are deleted.
     * @param localized  TRUE return localized pattern.
     * @return           A reference to 'result'.
     */
    UnicodeString& toPattern(UnicodeString& result, UBool localized) const;

    /**
     * Does the real work of applying a pattern.
     * @param pattern    The pattern to be applied.
     * @param localized  If true, the pattern is localized; else false.
     * @param parseError Struct to recieve information on position 
     *                   of error if an error is encountered
     * @param status     Output param set to success/failure code on
     *                   exit. If the pattern is invalid, this will be
     *                   set to a failure result.
     */
    void applyPattern(const UnicodeString& pattern,
                            UBool localized,
                            UParseError& parseError,
                            UErrorCode& status);
    /**
     * Do the work of formatting a number, either a double or a long.
     *
     * @param appendTo       Output parameter to receive result.
     *                       Result is appended to existing contents.
     * @param fieldPosition  On input: an alignment field, if desired.
     *                       On output: the offsets of the alignment field.
     * @param digits         the digits to be formatted.
     * @param isInteger      if TRUE format the digits as Integer.
     * @return               Reference to 'appendTo' parameter.
     */
    UnicodeString& subformat(UnicodeString& appendTo,
                             FieldPosition& fieldPosition,
                             DigitList& digits,
                             UBool         isInteger) const;

    enum {
        fgStatusInfinite,
        fgStatusLength      // Leave last in list.
    } StatusFlags;

    /**
     * Parse the given text into a number.  The text is parsed beginning at
     * parsePosition, until an unparseable character is seen.
     * @param text The string to parse.
     * @param parsePosition The position at which to being parsing.  Upon
     *                      return, the first unparseable character.
     * @param digits        The DigitList to set to the parsed value.
     * @param isExponent    If true, parse an exponent.  This means no
     *                      infinite values and integer only.
     * @param status        Upon return contains boolean status flags indicating
     *                      whether the value was infinite and whether it was positive.
     */
    UBool subparse(const UnicodeString& text, ParsePosition& parsePosition,
                    DigitList& digits, UBool* status) const;

    int32_t skipPadding(const UnicodeString& text, int32_t position) const;

    int32_t compareAffix(const UnicodeString& input,
                         int32_t pos,
                         UBool isNegative,
                         UBool isPrefix) const;
    
    static int32_t compareSimpleAffix(const UnicodeString& affix,
                                      const UnicodeString& input,
                                      int32_t pos);
    
    static int32_t skipRuleWhiteSpace(const UnicodeString& text, int32_t pos);
    
    static int32_t skipUWhiteSpace(const UnicodeString& text, int32_t pos);
    
    int32_t compareComplexAffix(const UnicodeString& affixPat,
                                const UnicodeString& input,
                                int32_t pos) const;

    static int32_t match(const UnicodeString& text, int32_t pos, UChar32 ch);

    static int32_t match(const UnicodeString& text, int32_t pos, const UnicodeString& str);

    /**
     * Get a decimal format symbol.
     * Returns a const reference to the symbol string.
     * @internal
     */
    inline const UnicodeString &getConstSymbol(DecimalFormatSymbols::ENumberFormatSymbol symbol) const;

    int32_t appendAffix(UnicodeString& buf, double number,
                        UBool isNegative, UBool isPrefix) const;

    /**
     * Append an affix to the given UnicodeString, using quotes if
     * there are special characters.  Single quotes themselves must be
     * escaped in either case.
     */
    void appendAffixPattern(UnicodeString& appendTo, const UnicodeString& affix, 
                            UBool localized) const;

    void appendAffixPattern(UnicodeString& appendTo,
                            const UnicodeString* affixPattern,
                            const UnicodeString& expAffix, UBool localized) const;

    void expandAffix(const UnicodeString& pattern,
                     UnicodeString& affix,
                     double number,
                     UBool doFormat) const;

    void expandAffixes();
    
    static double round(double a, ERoundingMode mode, UBool isNegative);

    void addPadding(UnicodeString& appendTo,
                    FieldPosition& fieldPosition,
                    int32_t prefixLen, int32_t suffixLen) const;

    UBool isGroupingPosition(int32_t pos) const;

    void setCurrencyForSymbols();

    void setCurrencyForLocale(const char* locale, UErrorCode& ec);

    /**
     * Constants.
     */
    //static const int8_t fgMaxDigit; // The largest digit, in this case 9

    /*transient*/ //DigitList* fDigitList;

    UnicodeString           fPositivePrefix;
    UnicodeString           fPositiveSuffix;
    UnicodeString           fNegativePrefix;
    UnicodeString           fNegativeSuffix;
    UnicodeString*          fPosPrefixPattern;
    UnicodeString*          fPosSuffixPattern;
    UnicodeString*          fNegPrefixPattern;
    UnicodeString*          fNegSuffixPattern;

    /**
     * Formatter for ChoiceFormat-based currency names.  If this field
     * is not null, then delegate to it to format currency symbols.
     * @since ICU 2.6
     */
    ChoiceFormat*           fCurrencyChoice;

    int32_t                 fMultiplier;
    int32_t                 fGroupingSize;
    int32_t                 fGroupingSize2;
    UBool                  fDecimalSeparatorAlwaysShown;
    /*transient*/ UBool    fIsCurrencyFormat;
    DecimalFormatSymbols*   fSymbols;

    UBool                  fUseExponentialNotation;
    int8_t                  fMinExponentDigits;
    UBool                  fExponentSignAlwaysShown;

    /* If fRoundingIncrement is NULL, there is no rounding.  Otherwise, round to
     * fRoundingIncrement.getDouble().  Since this operation may be expensive,
     * we cache the result in fRoundingDouble.  All methods that update
     * fRoundingIncrement also update fRoundingDouble. */
    DigitList*              fRoundingIncrement;
    /*transient*/ double    fRoundingDouble;
    ERoundingMode           fRoundingMode;

    UChar32                 fPad;
    int32_t                 fFormatWidth;
    EPadPosition            fPadPosition;

    // Constants for characters used in programmatic (unlocalized) patterns.
    static const UChar    kPatternZeroDigit;
    static const UChar    kPatternGroupingSeparator;
    static const UChar    kPatternDecimalSeparator;
    static const UChar    kPatternPerMill;
    static const UChar    kPatternPercent;
    static const UChar    kPatternDigit;
    static const UChar    kPatternSeparator;
    static const UChar    kPatternExponent;
    static const UChar    kPatternPlus;
    static const UChar    kPatternMinus;
    static const UChar    kPatternPadEscape;

    /**
     * The CURRENCY_SIGN is the standard Unicode symbol for currency.  It
     * is used in patterns and substitued with either the currency symbol,
     * or if it is doubled, with the international currency symbol.  If the
     * CURRENCY_SIGN is seen in a pattern, then the decimal separator is
     * replaced with the monetary decimal separator.
     */
    static const UChar    kCurrencySign;
    static const UChar    kQuote;

protected:
  /** number of integer digits 
   * @draft ICU 2.4
   */  
    static const int32_t  kDoubleIntegerDigits;
  /** number of fraction digits 
   * @draft ICU 2.4
   */  
    static const int32_t  kDoubleFractionDigits;
};

inline UClassID
DecimalFormat::getStaticClassID(void)
{ return (UClassID)&fgClassID; }

inline UClassID
DecimalFormat::getDynamicClassID(void) const
{ return DecimalFormat::getStaticClassID(); }

inline UnicodeString&
DecimalFormat::format(const Formattable& obj,
                      UnicodeString& appendTo,
                      UErrorCode& status) const {
    // Don't use Format:: - use immediate base class only,
    // in case immediate base modifies behavior later.
    return NumberFormat::format(obj, appendTo, status);
}

inline UnicodeString&
DecimalFormat::format(double number,
                      UnicodeString& appendTo) const {
    FieldPosition pos(0);
    return format(number, appendTo, pos);
}

inline UnicodeString&
DecimalFormat::format(int32_t number,
                      UnicodeString& appendTo) const {
    FieldPosition pos(0);
    return format(number, appendTo, pos);
}

inline const UnicodeString &
DecimalFormat::getConstSymbol(DecimalFormatSymbols::ENumberFormatSymbol symbol) const {
    return fSymbols->getConstSymbol(symbol);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // _DECIMFMT
//eof
