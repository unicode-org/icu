// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
********************************************************************************
*   Copyright (C) 2012-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
********************************************************************************
*
* File COMPACTDECIMALFORMAT.H
********************************************************************************
*/

#ifndef __COMPACT_DECIMAL_FORMAT_H__
#define __COMPACT_DECIMAL_FORMAT_H__

#include "unicode/utypes.h"
/**
 * \file
 * \brief C++ API: Formats decimal numbers in compact form.
 */

#if !UCONFIG_NO_FORMATTING

#include "unicode/decimfmt.h"

struct UHashtable;

U_NAMESPACE_BEGIN

class PluralRules;

/**
 * The CompactDecimalFormat produces abbreviated numbers, suitable for display in
 * environments will limited real estate. For example, 'Hits: 1.2B' instead of
 * 'Hits: 1,200,000,000'. The format will be appropriate for the given language,
 * such as "1,2 Mrd." for German.
 * <p>
 * For numbers under 1000 trillion (under 10^15, such as 123,456,789,012,345),
 * the result will be short for supported languages. However, the result may
 * sometimes exceed 7 characters, such as when there are combining marks or thin
 * characters. In such cases, the visual width in fonts should still be short.
 * <p>
 * By default, there are 3 significant digits. After creation, if more than
 * three significant digits are set (with setMaximumSignificantDigits), or if a
 * fixed number of digits are set (with setMaximumIntegerDigits or
 * setMaximumFractionDigits), then result may be wider.
 * <p>
 * At this time, parsing is not supported, and will produce a U_UNSUPPORTED_ERROR.
 * Resetting the pattern prefixes or suffixes is not supported; the method calls
 * are ignored.
 * <p>
 * @stable ICU 51
 */
class U_I18N_API CompactDecimalFormat : public DecimalFormat {
public:

     /**
      * Returns a compact decimal instance for specified locale.
      * @param inLocale the given locale.
      * @param style whether to use short or long style.
      * @param status error code returned  here.
      * @stable ICU 51
      */
     static CompactDecimalFormat* U_EXPORT2 createInstance(
          const Locale& inLocale, UNumberCompactStyle style, UErrorCode& status);

    /**
     * Copy constructor.
     *
     * @param source    the DecimalFormat object to be copied from.
     * @stable ICU 51
      */
    CompactDecimalFormat(const CompactDecimalFormat& source);

    /**
     * Destructor.
     * @stable ICU 51
     */
    ~CompactDecimalFormat() U_OVERRIDE;

    /**
     * Assignment operator.
     *
     * @param rhs    the DecimalFormat object to be copied.
     * @stable ICU 51
     */
    CompactDecimalFormat& operator=(const CompactDecimalFormat& rhs);

    using DecimalFormat::format;

    /**
     * Return the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     * <pre>
     * .      Base* polymorphic_pointer = createPolymorphicObject();
     * .      if (polymorphic_pointer->getDynamicClassID() ==
     * .          Derived::getStaticClassID()) ...
     * </pre>
     * @return          The class ID for all objects of this class.
     * @stable ICU 51
     */
    static UClassID U_EXPORT2 getStaticClassID();

    /**
     * Returns a unique class ID POLYMORPHICALLY.  Pure virtual override.
     * This method is to implement a simple version of RTTI, since not all
     * C++ compilers support genuine RTTI.  Polymorphic operator==() and
     * clone() methods call this method.
     *
     * @return          The class ID for this object. All objects of a
     *                  given class have the same class ID.  Objects of
     *                  other classes have different class IDs.
     * @stable ICU 51
     */
    virtual UClassID getDynamicClassID() const;
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif // __COMPACT_DECIMAL_FORMAT_H__
//eof
