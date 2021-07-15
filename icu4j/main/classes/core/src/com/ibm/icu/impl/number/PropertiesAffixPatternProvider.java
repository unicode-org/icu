// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

public class PropertiesAffixPatternProvider implements AffixPatternProvider {
    private final String posPrefix;
    private final String posSuffix;
    private final String negPrefix;
    private final String negSuffix;
    private final boolean isCurrencyPattern;

    public static AffixPatternProvider forProperties(DecimalFormatProperties properties) {
        if (properties.getCurrencyPluralInfo() == null) {
            return new PropertiesAffixPatternProvider(properties);
        } else {
            return new CurrencyPluralInfoAffixProvider(properties.getCurrencyPluralInfo(), properties);
        }
    }

    PropertiesAffixPatternProvider(DecimalFormatProperties properties) {
        // There are two ways to set affixes in DecimalFormat: via the pattern string (applyPattern), and via the
        // explicit setters (setPositivePrefix and friends).  The way to resolve the settings is as follows:
        //
        // 1) If the explicit setting is present for the field, use it.
        // 2) Otherwise, follows UTS 35 rules based on the pattern string.
        //
        // Importantly, the explicit setters affect only the one field they override.  If you set the positive
        // prefix, that should not affect the negative prefix.

        // Convenience: Extract the properties into local variables.
        // Variables are named with three chars: [p/n][p/s][o/p]
        // [p/n] => p for positive, n for negative
        // [p/s] => p for prefix, s for suffix
        // [o/p] => o for escaped custom override string, p for pattern string
        String ppo = AffixUtils.escape(properties.getPositivePrefix());
        String pso = AffixUtils.escape(properties.getPositiveSuffix());
        String npo = AffixUtils.escape(properties.getNegativePrefix());
        String nso = AffixUtils.escape(properties.getNegativeSuffix());
        String ppp = properties.getPositivePrefixPattern();
        String psp = properties.getPositiveSuffixPattern();
        String npp = properties.getNegativePrefixPattern();
        String nsp = properties.getNegativeSuffixPattern();

        if (ppo != null) {
            posPrefix = ppo;
        } else if (ppp != null) {
            posPrefix = ppp;
        } else {
            // UTS 35: Default positive prefix is empty string.
            posPrefix = "";
        }

        if (pso != null) {
            posSuffix = pso;
        } else if (psp != null) {
            posSuffix = psp;
        } else {
            // UTS 35: Default positive suffix is empty string.
            posSuffix = "";
        }

        if (npo != null) {
            negPrefix = npo;
        } else if (npp != null) {
            negPrefix = npp;
        } else {
            // UTS 35: Default negative prefix is "-" with positive prefix.
            // Important: We prepend the "-" to the pattern, not the override!
            negPrefix = ppp == null ? "-" : "-" + ppp;
        }

        if (nso != null) {
            negSuffix = nso;
        } else if (nsp != null) {
            negSuffix = nsp;
        } else {
            // UTS 35: Default negative prefix is the positive prefix.
            negSuffix = psp == null ? "" : psp;
        }

        // For declaring if this is a currency pattern, we need to look at the
        // original pattern, not at any user-specified overrides.
        isCurrencyPattern = (
            AffixUtils.hasCurrencySymbols(ppp) ||
            AffixUtils.hasCurrencySymbols(psp) ||
            AffixUtils.hasCurrencySymbols(npp) ||
            AffixUtils.hasCurrencySymbols(nsp));
    }

    @Override
    public char charAt(int flags, int i) {
        return getString(flags).charAt(i);
    }

    @Override
    public int length(int flags) {
        return getString(flags).length();
    }

    @Override
    public String getString(int flags) {
        boolean prefix = (flags & Flags.PREFIX) != 0;
        boolean negative = (flags & Flags.NEGATIVE_SUBPATTERN) != 0;
        if (prefix && negative) {
            return negPrefix;
        } else if (prefix) {
            return posPrefix;
        } else if (negative) {
            return negSuffix;
        } else {
            return posSuffix;
        }
    }

    @Override
    public boolean positiveHasPlusSign() {
        return AffixUtils.containsType(posPrefix, AffixUtils.TYPE_PLUS_SIGN)
                || AffixUtils.containsType(posSuffix, AffixUtils.TYPE_PLUS_SIGN);
    }

    @Override
    public boolean hasNegativeSubpattern() {
        return (
            negSuffix != posSuffix ||
            negPrefix.length() != posPrefix.length() + 1 ||
            !negPrefix.regionMatches(1, posPrefix, 0, posPrefix.length()) ||
            negPrefix.charAt(0) != '-'
        );
    }

    @Override
    public boolean negativeHasMinusSign() {
        return AffixUtils.containsType(negPrefix, AffixUtils.TYPE_MINUS_SIGN)
                || AffixUtils.containsType(negSuffix, AffixUtils.TYPE_MINUS_SIGN);
    }

    @Override
    public boolean hasCurrencySign() {
        return isCurrencyPattern;
    }

    @Override
    public boolean containsSymbolType(int type) {
        return AffixUtils.containsType(posPrefix, type) || AffixUtils.containsType(posSuffix, type)
                || AffixUtils.containsType(negPrefix, type) || AffixUtils.containsType(negSuffix, type);
    }

    @Override
    public boolean hasBody() {
        return true;
    }

    @Override
    public String toString() {
        return super.toString()
                + " {"
                + posPrefix
                + "#"
                + posSuffix
                + ";"
                + negPrefix
                + "#"
                + negSuffix
                + "}";
    }
}
