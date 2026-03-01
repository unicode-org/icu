// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.text.CurrencyPluralInfo;

public class CurrencyPluralInfoAffixProvider implements AffixPatternProvider {
    private final PropertiesAffixPatternProvider[] affixesByPlural;

    public CurrencyPluralInfoAffixProvider(CurrencyPluralInfo cpi, DecimalFormatProperties properties) {
        // We need to use a PropertiesAffixPatternProvider, not the simpler version ParsedPatternInfo,
        // because user-specified affix overrides still need to work.
        affixesByPlural = new PropertiesAffixPatternProvider[StandardPlural.COUNT];
        DecimalFormatProperties pluralProperties = new DecimalFormatProperties();
        pluralProperties.copyFrom(properties);
        for (StandardPlural plural : StandardPlural.VALUES) {
            String pattern = cpi.getCurrencyPluralPattern(plural.getKeyword());
            PatternStringParser.parseToExistingProperties(pattern, pluralProperties);
            affixesByPlural[plural.ordinal()] = new PropertiesAffixPatternProvider(pluralProperties);
        }
    }

    @Override
    public char charAt(int flags, int i) {
        int pluralOrdinal = (flags & Flags.PLURAL_MASK);
        return affixesByPlural[pluralOrdinal].charAt(flags, i);
    }

    @Override
    public int length(int flags) {
        int pluralOrdinal = (flags & Flags.PLURAL_MASK);
        return affixesByPlural[pluralOrdinal].length(flags);
    }

    @Override
    public String getString(int flags) {
        int pluralOrdinal = (flags & Flags.PLURAL_MASK);
        return affixesByPlural[pluralOrdinal].getString(flags);
    }

    @Override
    public boolean positiveHasPlusSign() {
        return affixesByPlural[StandardPlural.OTHER.ordinal()].positiveHasPlusSign();
    }

    @Override
    public boolean hasNegativeSubpattern() {
        return affixesByPlural[StandardPlural.OTHER.ordinal()].hasNegativeSubpattern();
    }

    @Override
    public boolean negativeHasMinusSign() {
        return affixesByPlural[StandardPlural.OTHER.ordinal()].negativeHasMinusSign();
    }

    @Override
    public boolean hasCurrencySign() {
        return affixesByPlural[StandardPlural.OTHER.ordinal()].hasCurrencySign();
    }

    @Override
    public boolean containsSymbolType(int type) {
        return affixesByPlural[StandardPlural.OTHER.ordinal()].containsSymbolType(type);
    }

    @Override
    public boolean hasBody() {
        return affixesByPlural[StandardPlural.OTHER.ordinal()].hasBody();
    }
}
