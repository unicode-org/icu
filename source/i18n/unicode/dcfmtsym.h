/*
********************************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
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
 
#include "unicode/utypes.h"
#include "unicode/locid.h"

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
     * Constants for specifying a number format symbol.
     * @draft
     */
    enum ENumberFormatSymbol {
      /** The decimal separator */
      kDecimalSeparator,
      /** The grouping separator */
      kGroupingSeparator,
      /** The pattern separator */
      kPatternSeparator,
      /** The percent sign */
      kPercent,
      /** Zero*/
      kZeroDigit,
      /** Character representing a digit in the pattern */
      kDigit,
      /** The minus sign */
      kMinusSign,
      /** The plus sign */
      kPlusSign,
      /** The currency symbol */
      kCurrency,
      /** The international currency symbol */
      kIntlCurrency,
      /** The monetary separator */
      kMonetarySeparator,
      /** The exponential symbol */
      kExponential,
      /** Per mill symbol */
      kPermill,
      /** Escape padding character */
      kPadEscape,
      /** Infinity symbol */
      kInfinity,
      /** Nan symbol */
      kNaN,
      /** count symbol constants */
      kCount
    };

    /**
     * Create a DecimalFormatSymbols object for the given locale.
     *
     * @param locale    The locale to get symbols for.
     * @param status    Input/output parameter, set to success or
     *                  failure code upon return.
     * @stable
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
     * @stable
     */
    DecimalFormatSymbols( UErrorCode& status);

    /**
     * Copy constructor.
     * @stable
     */
    DecimalFormatSymbols(const DecimalFormatSymbols&);

    /**
     * Assignment operator.
     * @stable
     */
    DecimalFormatSymbols& operator=(const DecimalFormatSymbols&);

    /**
     * Destructor.
     * @stable
     */
    ~DecimalFormatSymbols();

    /**
     * Return true if another object is semantically equal to this one.
     * @stable
     */
    UBool operator==(const DecimalFormatSymbols& other) const;

    /**
     * Return true if another object is semantically unequal to this one.
     * @stable
     */
    UBool operator!=(const DecimalFormatSymbols& other) const { return !operator==(other); }

    /**
     * Get one of the format symbols by its enum constant.
     * Each symbol is stored as a string so that graphemes
     * (characters with modifyer letters) can be used.
     * @draft
     */
    UnicodeString getSymbol(ENumberFormatSymbol symbol) const;

    /**
     * Set one of the format symbols by its enum constant.
     * Each symbol is stored as a string so that graphemes
     * (characters with modifyer letters) can be used.
     * @draft
     */
    void setSymbol(ENumberFormatSymbol symbol, UnicodeString value);

    /**
     * character used for zero. Different for Arabic, etc.
     * @deprecated remove after 2000-dec-31
     */
    UChar getZeroDigit(void) const;
    /**
     * character used for zero. Different for Arabic, etc.
     * @deprecated remove after 2000-dec-31
     */
    void setZeroDigit(UChar zeroDigit);

    /**
     * character used for thousands separator. Different for French, etc.
     * @deprecated remove after 2000-dec-31
     */
    UChar getGroupingSeparator(void) const;
    /**
     * character used for thousands separator. Different for French, etc.
     * @deprecated remove after 2000-dec-31
     */
    void setGroupingSeparator(UChar groupingSeparator);

    /**
     * character used for decimal sign. Different for French, etc.
     * @deprecated remove after 2000-dec-31
     */
    UChar getDecimalSeparator(void) const;
    /**
     * character used for decimal sign. Different for French, etc.
     * @deprecated remove after 2000-dec-31
     */
    void setDecimalSeparator(UChar decimalSeparator);

    /**
     * character used for per mill sign. Different for Arabic, etc.
     * @deprecated remove after 2000-dec-31
     */
    UChar getPerMill(void) const;
    /**
     * character used for per mill sign. Different for Arabic, etc.
     * @deprecated remove after 2000-dec-31
     */
    void setPerMill(UChar permill);

    /**
     * character used for percent sign. Different for Arabic, etc.
     * @deprecated remove after 2000-dec-31
     */
    UChar getPercent(void) const;
    /**
     * character used for percent sign. Different for Arabic, etc.
     * @deprecated remove after 2000-dec-31
     */
    void setPercent(UChar percent);

    /**
     * character used for a digit in a pattern.
     * @deprecated remove after 2000-dec-31
     */
    UChar getDigit(void) const;
    /**
     * character used for a digit in a pattern.
     * @deprecated remove after 2000-dec-31
     */
    void setDigit(UChar digit);

    /**
     * character used to separate positive and negative subpatterns
     * in a pattern.
     * @deprecated remove after 2000-dec-31
     */
    UChar getPatternSeparator(void) const;
    /**
     * character used to separate positive and negative subpatterns
     * in a pattern.
     * @deprecated remove after 2000-dec-31
     */
    void setPatternSeparator(UChar patternSeparator);

    /**
     * character used to represent infinity. Almost always left
     * unchanged.
     * @deprecated remove after 2000-dec-31
     */
    UnicodeString& getInfinity(UnicodeString& result) const;
    /**
     * character used to represent infinity. Almost always left
     * unchanged.
     * @deprecated remove after 2000-dec-31
     */
    void setInfinity(const UnicodeString& infinity);

    /**
     * character used to represent NaN. Almost always left
     * unchanged.
     * @deprecated remove after 2000-dec-31
     */
    UnicodeString& getNaN(UnicodeString& result) const;
    /**
     * character used to represent NaN. Almost always left
     * unchanged.
     * @deprecated remove after 2000-dec-31
     */
    void setNaN(const UnicodeString& NaN);

    /**
     * character used to represent plus sign
     * @deprecated remove after 2000-dec-31
     */
    UChar getPlusSign(void) const;
    /**
     * character used to represent plus sign
     * @deprecated remove after 2000-dec-31
     */
    void setPlusSign(UChar minusSign);

    /**
     * character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     * @deprecated remove after 2000-dec-31
     */
    UChar getMinusSign(void) const;
    /**
     * character used to represent minus sign. If no explicit
     * negative format is specified, one is formed by prefixing
     * minusSign to the positive format.
     * @deprecated remove after 2000-dec-31
     */
    void setMinusSign(UChar minusSign);
 
    /**
     * character used to represent exponential. Almost always left
     * unchanged.
     * @deprecated remove after 2000-dec-31
     */
    UChar getExponentialSymbol(void) const;
    /**
     * character used to represent exponential. Almost always left
     * unchanged.
     * @deprecated remove after 2000-dec-31
     */
    void setExponentialSymbol(UChar exponential);

    /**
     * The string denoting the local currency.
     * @deprecated remove after 2000-dec-31
     */
    UnicodeString& getCurrencySymbol(UnicodeString& result) const;
    /**
     * The string denoting the local currency.
     * @deprecated remove after 2000-dec-31
     */
    void setCurrencySymbol(const UnicodeString& currency);

    /**
     * The international string denoting the local currency.
     * @deprecated remove after 2000-dec-31
     */
    UnicodeString& getInternationalCurrencySymbol(UnicodeString& result) const;
    /**
     * The international string denoting the local currency.
     * @deprecated remove after 2000-dec-31
     */
    void setInternationalCurrencySymbol(const UnicodeString& currency);

    /**
     * The monetary decimal separator.
     * @deprecated remove after 2000-dec-31
     */
    UChar getMonetaryDecimalSeparator(void) const;
    /**
     * The monetary decimal separator.
     * @deprecated remove after 2000-dec-31
     */
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
     * @deprecated remove after 2000-dec-31
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
     * @deprecated remove after 2000-dec-31
     */
    void setPadEscape(UChar c);

private:
    /**
     * Initializes the symbols from the LocaleElements resource bundle.
     * Note: The organization of LocaleElements badly needs to be
     * cleaned up.
     */
    void initialize(const Locale& locale, UErrorCode& success, UBool useLastResortData = FALSE);

    /**
     * Initialize the symbols from the given array of UnicodeStrings.
     * The array must be of the correct size.
     */
    void initialize(const UnicodeString* numberElements, const UnicodeString* currencyElements);
    
    /**
     * The resource tags we use to retrieve decimal format data from
     * locale resource bundles.
     */
    static const char         *fgNumberElements;
    static const char         *fgCurrencyElements;
    static const int32_t         fgNumberElementsLength;
    static const int32_t         fgCurrencyElementsLength;
    static const UnicodeString     fgLastResortNumberElements[];
    static const UnicodeString     fgLastResortCurrencyElements[];
    static const UChar         fgLastResortPermill[];
    static const UChar         fgLastResortInfinity[];
    static const UChar         fgLastResortNaN[];
    static const UChar         fgLastResortCurrency[];
    static const UChar         fgLastResortIntlCurrency[];

    UnicodeString fSymbols[ENumberFormatSymbol::kCount];
};
 

// -------------------------------------
 
inline UnicodeString
DecimalFormatSymbols::getSymbol(ENumberFormatSymbol symbol) const {
    if(symbol<ENumberFormatSymbol::kCount) {
        return fSymbols[symbol];
    } else {
        return UnicodeString();
    }
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setSymbol(ENumberFormatSymbol symbol, UnicodeString value) {
    if(symbol<ENumberFormatSymbol::kCount) {
        fSymbols[symbol]=value;
    }
}

// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getZeroDigit() const
{
    return fSymbols[ENumberFormatSymbol::kZeroDigit].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setZeroDigit(UChar zeroDigit)
{
    fSymbols[ENumberFormatSymbol::kZeroDigit] = zeroDigit;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getGroupingSeparator() const
{
    return fSymbols[ENumberFormatSymbol::kGroupingSeparator].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setGroupingSeparator(UChar groupingSeparator)
{
    fSymbols[ENumberFormatSymbol::kGroupingSeparator] = groupingSeparator;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getDecimalSeparator() const
{
    return fSymbols[ENumberFormatSymbol::kDecimalSeparator].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setDecimalSeparator(UChar decimalSeparator)
{
    fSymbols[ENumberFormatSymbol::kDecimalSeparator] = decimalSeparator;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getPerMill() const
{
    return fSymbols[ENumberFormatSymbol::kPermill].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setPerMill(UChar permill)
{
    fSymbols[ENumberFormatSymbol::kPermill] = permill;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getPercent() const
{
    return fSymbols[ENumberFormatSymbol::kPercent].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setPercent(UChar percent)
{
    fSymbols[ENumberFormatSymbol::kPercent] = percent;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getDigit() const
{
    return fSymbols[ENumberFormatSymbol::kDigit].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setDigit(UChar digit)
{
    fSymbols[ENumberFormatSymbol::kDigit] = digit;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getPatternSeparator() const
{
    return fSymbols[ENumberFormatSymbol::kPatternSeparator].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setPatternSeparator(UChar patternSeparator)
{
    fSymbols[ENumberFormatSymbol::kPatternSeparator] = patternSeparator;
}
 
// -------------------------------------
 
inline UnicodeString&
DecimalFormatSymbols::getInfinity(UnicodeString& result) const
{
    return result = fSymbols[ENumberFormatSymbol::kInfinity];
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setInfinity(const UnicodeString& infinity)
{
    fSymbols[ENumberFormatSymbol::kInfinity] = infinity;
}
 
// -------------------------------------
 
inline UnicodeString&
DecimalFormatSymbols::getNaN(UnicodeString& result) const
{
    return result = fSymbols[ENumberFormatSymbol::kNaN];
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setNaN(const UnicodeString& NaN)
{
    fSymbols[ENumberFormatSymbol::kNaN] = NaN;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getPlusSign() const
{
    return fSymbols[ENumberFormatSymbol::kPlusSign].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setPlusSign(UChar plusSign)
{
    fSymbols[ENumberFormatSymbol::kPlusSign] = plusSign;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getMinusSign() const
{
    return fSymbols[ENumberFormatSymbol::kMinusSign].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setMinusSign(UChar minusSign)
{
    fSymbols[ENumberFormatSymbol::kMinusSign] = minusSign;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getExponentialSymbol(void) const
{
    return fSymbols[ENumberFormatSymbol::kExponential].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setExponentialSymbol(UChar exponential)
{
    fSymbols[ENumberFormatSymbol::kExponential] = exponential;
}
 
// -------------------------------------
 
inline UnicodeString&
DecimalFormatSymbols::getCurrencySymbol(UnicodeString& result) const
{
    return result = fSymbols[ENumberFormatSymbol::kCurrency];
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setCurrencySymbol(const UnicodeString& str)
{
    fSymbols[ENumberFormatSymbol::kCurrency] = str;
}
 
// -------------------------------------
 
inline UnicodeString&
DecimalFormatSymbols::getInternationalCurrencySymbol(UnicodeString& result) const
{
    return result = fSymbols[ENumberFormatSymbol::kIntlCurrency];
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setInternationalCurrencySymbol(const UnicodeString& str)
{
    fSymbols[ENumberFormatSymbol::kIntlCurrency] = str;
}
 
// -------------------------------------
 
inline UChar
DecimalFormatSymbols::getMonetaryDecimalSeparator(void) const
{
    return fSymbols[ENumberFormatSymbol::kMonetarySeparator].charAt(0);
}
 
// -------------------------------------
 
inline void
DecimalFormatSymbols::setMonetaryDecimalSeparator(UChar sep)
{
    fSymbols[ENumberFormatSymbol::kMonetarySeparator] = sep;
}
 
inline UChar DecimalFormatSymbols::getPadEscape(void) const {
    return fSymbols[ENumberFormatSymbol::kPadEscape].charAt(0);
}

inline void DecimalFormatSymbols::setPadEscape(UChar c) {
    fSymbols[ENumberFormatSymbol::kPadEscape] = c;
}

#endif // _DCFMTSYM
//eof
