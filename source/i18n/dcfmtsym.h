/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1999      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
*
* File DCFMTSYM.H
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/18/97    clhuang     Updated per C++ implementation.
*   03/27/97    helena      Updated to pass the simple test after code review.
*   08/26/97    aliu        Added currency/intl currency symbol support.
*    07/22/98    stephen        Changed to match C++ style 
*                            currencySymbol -> fCurrencySymbol
*                            Constants changed from CAPS to kCaps
*   06/24/99    helena      Integrated Alan's NF enhancements and Java2 bug fixes
********************************************************************************
*/
 
#ifndef DCFMTSYM_H
#define DCFMTSYM_H
 
#include "utypes.h"
#include "locid.h"

/**
 * This class represents the set of symbols needed by DecimalFormat
 * to format numbers. DecimalFormat creates for itself an instance of
 * DecimalFormatSymbols from its locale data.  If you need to change any
 * of these symbols, you can get the DecimalFormatSymbols object from
 * your DecimalFormat and modify it.
 * <P>
 * Here are the special characters used in the parts of the
 * subpattern, with notes on their usage.
 * <pre>
 * .       Symbol   Meaning
 * .         0      a digit
 * .         #      a digit, zero shows as absent
 * .         .      placeholder for decimal separator
 * .         ,      placeholder for grouping separator.
 * .         ;      separates formats.
 * .         -      default negative prefix.
 * .         %      divide by 100 and show as percentage
 * .         X      any other characters can be used in the prefix or suffix
 * .         '      used to quote special characters in a prefix or suffix.
 *  </pre>
 * [Notes] 
 * <P>
 * If there is no explicit negative subpattern, - is prefixed to the
 * positive form. That is, "0.00" alone is equivalent to "0.00;-0.00".
 * <P>
 * The grouping separator is commonly used for thousands, but in some
 * countries for ten-thousands. The interval is a constant number of
 * digits between the grouping characters, such as 100,000,000 or 1,0000,0000.
 * If you supply a pattern with multiple grouping characters, the interval
 * between the last one and the end of the integer is the one that is
 * used. So "#,##,###,####" == "######,####" == "##,####,####".
 * <P>
 * This class only handles localized digits where the 10 digits are
 * contiguous in Unicode, from 0 to 9. Other digits sets (such as
 * superscripts) would need a different subclass.
 */
       
class U_I18N_API DecimalFormatSymbols {
public:
    /**
     * Create a DecimalFormatSymbols object for the given locale.
     *
     * @param locale    The locale to get symbols for.
     * @param status    Input/output parameter, set to success or
     *                  failure code upon return.
     */
    DecimalFormatSymbols(const Locale& locale, UErrorCode& status);

    /**
     * Create a DecimalFormatSymbols object for the default locale.
     * This constructor will not fail.  If the resource file data is
     * not available, it will use hard-coded last-resort data and
     * set status to U_USING_FALLBACK_ERROR.
     *
     * @param status    Input/output parameter, set to success or
     *                  failure code upon return.
     */
    DecimalFormatSymbols( UErrorCode& status);

    /**
     * Copy constructor.
     */
    DecimalFormatSymbols(const DecimalFormatSymbols&);

    /**
     * Assignment operator.
     */
    DecimalFormatSymbols& operator=(const DecimalFormatSymbols&);

    /**
     * Destructor.
     */
    ~DecimalFormatSymbols();

    /**
     * Return true if another object is semantically equal to this one.
     */
    bool_t operator==(const DecimalFormatSymbols& other) const;

    /**
     * Return true if another object is semantically unequal to this one.
     */
    bool_t operator!=(const DecimalFormatSymbols& other) const { return !operator==(other); }

    /**
     * character used for zero. Different for Arabic, etc.
     */
    UChar getZeroDigit(void) const;
    void setZeroDigit(UChar zeroDigit);

    /**
     * character used for thousands separator. Different for French, etc.
     */
    UChar getGroupingSeparator(void) const;
    void setGroupingSeparator(UChar groupingSeparator);

    /**
     * character used for decimal sign. Different for French, etc.
     */
    UChar getDecimalSeparator(void) const;
    void setDecimalSeparator(UChar decimalSeparator);

    /**
     * character used for per mill sign. Different for Arabic, etc.
     */
    UChar getPerMill(void) const;
    void setPerMill(UChar perMill);

    /**
     * character used for percent sign. Different for Arabic, etc.
     */
    UChar getPercent(void) const;
    void setPercent(UChar percent);

    /**
     * character used for a digit in a pattern.
     */
    UChar getDigit(void) const;
    void setDigit(UChar digit);

    /**
     * character used to separate positive and negative subpatterns
     * in a pattern.
     */
    UChar getPatternSeparator(void) const;
    void setPatternSeparator(UChar patternSeparator);

    /**
     * character used to represent infinity. Almost always left
     * unchanged.
     */
    UnicodeString& getInfinity(UnicodeString& result) const;
    void setInfinity(const UnicodeString& infinity);

    /**
     * character used to represent NaN. Almost always left
     * unchanged.
     */
    UnicodeString& getNaN(UnicodeString& result) const;
    void setNaN(const UnicodeString& NaN);

    /**
     * character used to represent plus sign
     */
    UChar getPlusSign(void) const;
    void setPlusSign(UChar minusSign);

    /**
     * character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     */
    UChar getMinusSign(void) const;
    void setMinusSign(UChar minusSign);
 
    /**
     * character used to represent exponential. Almost always left
     * unchanged.
     */
    UChar getExponentialSymbol(void) const;
    void setExponentialSymbol(UChar exponential);

    /**
     * Return the string denoting the local currency.
     */
    UnicodeString& getCurrencySymbol(UnicodeString& result) const;
    void setCurrencySymbol(const UnicodeString& currency);

    /**
     * Return the international string denoting the local currency.
     */
    UnicodeString& getInternationalCurrencySymbol(UnicodeString& result) const;
    void setInternationalCurrencySymbol(const UnicodeString& currency);

    /**
     * Return the monetary decimal separator.
     */
    UChar getMonetaryDecimalSeparator(void) const;
    void setMonetaryDecimalSeparator(UChar sep);

    /**
     * Return the character used to pad numbers out to a specified width.  This
     * is not the pad character itself; rather, it is the special pattern
     * character <em>preceding</em> the pad character.  In the pattern
     * "*_#,##0", '*' is the pad escape, and '_' is the pad character.
     * @return the character 
     * @see #setPadEscape
     * @see DecimalFormat#getFormatWidth
     * @see DecimalFormat#getPadPosition
     * @see DecimalFormat#getPadCharacter
     */
    UChar getPadEscape(void) const;

    /**
     * Set the character used to pad numbers out to a specified width.  This is
     * not the pad character itself; rather, it is the special pattern character
     * <em>preceding</em> the pad character.  In the pattern "*_#,##0", '*' is
     * the pad escape, and '_' is the pad character.
     * @see #getPadEscape
     * @see DecimalFormat#setFormatWidth
     * @see DecimalFormat#setPadPosition
     * @see DecimalFormat#setPadCharacter
     */
    void setPadEscape(UChar c);

private:
    /**
     * Initializes the symbols from the LocaleElements resource bundle.
     * Note: The organization of LocaleElements badly needs to be
     * cleaned up.
     */
    void initialize(const Locale& locale, UErrorCode& success, bool_t useLastResortData = FALSE);

    /**
     * Initialize the symbols from the given array of UnicodeStrings.
     * The array must be of the correct size.
     */
    void initialize(const UnicodeString* numberElements, const UnicodeString* currencyElements);
    
    /**
     * The resource tags we use to retrieve decimal format data from
     * locale resource bundles.
     */
    static const UnicodeString     fgNumberElements;
    static const UnicodeString     fgCurrencyElements;
    static const int32_t         fgNumberElementsLength;
    static const int32_t         fgCurrencyElementsLength;
    static const UnicodeString     fgLastResortNumberElements[];
    static const UnicodeString     fgLastResortCurrencyElements[];
    static const UChar         fgLastResortPerMill[];
    static const UChar         fgLastResortInfinity[];
    static const UChar         fgLastResortNaN[];
    static const UChar         fgLastResortCurrency[];
    static const UChar         fgLastResortIntlCurrency[];

    UChar         fDecimalSeparator;
    UChar         fGroupingSeparator;
    UChar         fPatternSeparator;
    UChar         fPercent;
    UChar         fZeroDigit;
    UChar         fDigit;
    UChar         fPlusSign;
    UChar         fMinusSign;
    UnicodeString   fCurrencySymbol;
    UnicodeString   fIntlCurrencySymbol;
    UChar         fMonetarySeparator;
    UChar         fExponential;
    UChar         fPadEscape;

    UChar         fPerMill;
    UnicodeString   fInfinity;
    UnicodeString   fNaN;
};
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getZeroDigit() const
{
    return fZeroDigit;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setZeroDigit(UChar zeroDigit)
{
    fZeroDigit = zeroDigit;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getGroupingSeparator() const
{
    return fGroupingSeparator;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setGroupingSeparator(UChar groupingSeparator)
{
    fGroupingSeparator = groupingSeparator;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getDecimalSeparator() const
{
    return fDecimalSeparator;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setDecimalSeparator(UChar decimalSeparator)
{
    fDecimalSeparator = decimalSeparator;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getPerMill() const
{
    return fPerMill;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setPerMill(UChar perMill)
{
    fPerMill = perMill;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getPercent() const
{
    return fPercent;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setPercent(UChar percent)
{
    fPercent = percent;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getDigit() const
{
    return fDigit;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setDigit(UChar digit)
{
    fDigit = digit;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getPatternSeparator() const
{
    return fPatternSeparator;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setPatternSeparator(UChar patternSeparator)
{
    fPatternSeparator = patternSeparator;
}
 
// -------------------------------------
 
inline UnicodeString&
DecimalFormatSymbols::getInfinity(UnicodeString& result) const
{
    result = fInfinity;
    return result;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setInfinity(const UnicodeString& infinity)
{
    fInfinity = infinity;
}
 
// -------------------------------------
 
inline UnicodeString&
DecimalFormatSymbols::getNaN(UnicodeString& result) const
{
    result = fNaN;
    return result;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setNaN(const UnicodeString& NaN)
{
    fNaN = NaN;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getPlusSign() const
{
    return fPlusSign;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setPlusSign(UChar plusSign)
{
    fPlusSign = plusSign;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getMinusSign() const
{
    return fMinusSign;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setMinusSign(UChar minusSign)
{
    fMinusSign = minusSign;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getExponentialSymbol(void) const
{
    return fExponential;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setExponentialSymbol(UChar exponential)
{
    fExponential = exponential;
}
 
// -------------------------------------
 
inline UnicodeString&
DecimalFormatSymbols::getCurrencySymbol(UnicodeString& result) const
{
    result = fCurrencySymbol;
    return result;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setCurrencySymbol(const UnicodeString& str)
{
    fCurrencySymbol = str;
}
 
// -------------------------------------
 
inline UnicodeString&
DecimalFormatSymbols::getInternationalCurrencySymbol(UnicodeString& result) const
{
    result = fIntlCurrencySymbol;
    return result;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setInternationalCurrencySymbol(const UnicodeString& str)
{
    fIntlCurrencySymbol = str;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getMonetaryDecimalSeparator(void) const
{
    return fMonetarySeparator;
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setMonetaryDecimalSeparator(UChar sep)
{
    fMonetarySeparator = sep;
}
 
inline UChar DecimalFormatSymbols::getPadEscape(void) const {
    return fPadEscape;
}

inline void DecimalFormatSymbols::setPadEscape(UChar c) {
    fPadEscape = c;
}

#endif // _DCFMTSYM
//eof
