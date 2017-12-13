// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

public class PropertiesAffixPatternProvider implements AffixPatternProvider {
    private final String posPrefix;
    private final String posSuffix;
    private final String negPrefix;
    private final String negSuffix;

    public PropertiesAffixPatternProvider(DecimalFormatProperties properties) {
        // There are two ways to set affixes in DecimalFormat: via the pattern string (applyPattern), and via the
        // explicit setters (setPositivePrefix and friends).  The way to resolve the settings is as follows:
        //
        // 1) If the explicit setting is present for the field, use it.
        // 2) Otherwise, follows UTS 35 rules based on the pattern string.
        //
        // Importantly, the explicit setters affect only the one field they override.  If you set the positive
        // prefix, that should not affect the negative prefix.  Since it is impossible for the user of this class
        // to know whether the origin for a string was the override or the pattern, we have to say that we always
        // have a negative subpattern and perform all resolution logic here.

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
    }

    @Override
    public char charAt(int flags, int i) {
        return getStringForFlags(flags).charAt(i);
    }

    @Override
    public int length(int flags) {
        return getStringForFlags(flags).length();
    }

    private String getStringForFlags(int flags) {
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
        // See comments in the constructor for more information on why this is always true.
        return true;
    }

    @Override
    public boolean negativeHasMinusSign() {
        return AffixUtils.containsType(negPrefix, AffixUtils.TYPE_MINUS_SIGN)
                || AffixUtils.containsType(negSuffix, AffixUtils.TYPE_MINUS_SIGN);
    }

    @Override
    public boolean hasCurrencySign() {
        return AffixUtils.hasCurrencySymbols(posPrefix) || AffixUtils.hasCurrencySymbols(posSuffix)
                || AffixUtils.hasCurrencySymbols(negPrefix) || AffixUtils.hasCurrencySymbols(negSuffix);
    }

    @Override
    public boolean containsSymbolType(int type) {
        return AffixUtils.containsType(posPrefix, type) || AffixUtils.containsType(posSuffix, type)
                || AffixUtils.containsType(negPrefix, type) || AffixUtils.containsType(negSuffix, type);
    }
}