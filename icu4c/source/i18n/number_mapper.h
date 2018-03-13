// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMBER_MAPPER_H__
#define __NUMBER_MAPPER_H__

#include "number_types.h"

U_NAMESPACE_BEGIN namespace number {
namespace impl {


/**
 * Utilities for converting between a DecimalFormatProperties and a MacroProps.
 */
class NumberPropertyMapper {
  public:
    /** Convenience method to create a NumberFormatter directly from Properties. */
    static UnlocalizedNumberFormatter create(const DecimalFormatProperties& properties,
                                             const DecimalFormatSymbols& symbols, UErrorCode& status);

    /** Convenience method to create a NumberFormatter directly from Properties. */
    static UnlocalizedNumberFormatter create(const DecimalFormatProperties& properties,
                                             const DecimalFormatSymbols& symbols,
                                             DecimalFormatProperties& exportedProperties,
                                             UErrorCode& status);

    /**
     * Convenience method to create a NumberFormatter directly from a pattern string. Something like this
     * could become public API if there is demand.
     */
    static UnlocalizedNumberFormatter create(const UnicodeString& pattern,
                                             const DecimalFormatSymbols& symbols, UErrorCode& status);

    /**
     * Creates a new {@link MacroProps} object based on the content of a {@link DecimalFormatProperties}
     * object. In other words, maps Properties to MacroProps. This function is used by the
     * JDK-compatibility API to call into the ICU 60 fluent number formatting pipeline.
     *
     * @param properties
     *            The property bag to be mapped.
     * @param symbols
     *            The symbols associated with the property bag.
     * @param exportedProperties
     *            A property bag in which to store validated properties. Used by some DecimalFormat
     *            getters.
     * @return A new MacroProps containing all of the information in the Properties.
     */
    static void oldToNew(const DecimalFormatProperties& properties, const DecimalFormatSymbols& symbols,
                         DecimalFormatProperties& exportedProperties, MacroProps& output,
                         UErrorCode& status);
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMBER_MAPPER_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
