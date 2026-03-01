// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.number.AffixUtils;

/**
 * A specialized version of {@link SeriesMatcher} that matches EITHER a prefix OR a suffix.
 * {@link AffixMatcher} combines two of these in order to match both the prefix and suffix.
 *
 * @author sffc
 */
public class AffixPatternMatcher extends SeriesMatcher implements AffixUtils.TokenConsumer {

    private final String affixPattern;

    // Used during construction only:
    private AffixTokenMatcherFactory factory;
    private IgnorablesMatcher ignorables;
    private int lastTypeOrCp;

    private AffixPatternMatcher(String affixPattern) {
        this.affixPattern = affixPattern;
    }

    /**
     * Creates an AffixPatternMatcher (based on SeriesMatcher) from the given affix pattern. Returns null
     * if the affix pattern is empty.
     */
    public static AffixPatternMatcher fromAffixPattern(
            String affixPattern,
            AffixTokenMatcherFactory factory,
            int parseFlags) {
        if (affixPattern.isEmpty()) {
            return null;
        }

        AffixPatternMatcher series = new AffixPatternMatcher(affixPattern);
        series.factory = factory;
        series.ignorables = (0 != (parseFlags & ParsingUtils.PARSE_FLAG_EXACT_AFFIX)) ? null
                : factory.ignorables();
        series.lastTypeOrCp = 0;
        AffixUtils.iterateWithConsumer(affixPattern, series);

        // De-reference the memory
        series.factory = null;
        series.ignorables = null;
        series.lastTypeOrCp = 0;

        series.freeze();
        return series;
    }

    /**
     * This method is NOT intended to be called directly. It is here for the AffixUtils.TokenConsumer
     * interface only.
     */
    @Override
    public void consumeToken(int typeOrCp) {
        // This is called by AffixUtils.iterateWithConsumer() for each token.

        // Add an ignorables matcher between tokens except between two literals, and don't put two
        // ignorables matchers in a row.
        if (ignorables != null
                && length() > 0
                && (lastTypeOrCp < 0 || !ignorables.getSet().contains(lastTypeOrCp))) {
            addMatcher(ignorables);
        }

        if (typeOrCp < 0) {
            // Case 1: the token is a symbol.
            switch (typeOrCp) {
            case AffixUtils.TYPE_MINUS_SIGN:
                addMatcher(factory.minusSign());
                break;
            case AffixUtils.TYPE_PLUS_SIGN:
                addMatcher(factory.plusSign());
                break;
            case AffixUtils.TYPE_PERCENT:
                addMatcher(factory.percent());
                break;
            case AffixUtils.TYPE_PERMILLE:
                addMatcher(factory.permille());
                break;
            case AffixUtils.TYPE_CURRENCY_SINGLE:
            case AffixUtils.TYPE_CURRENCY_DOUBLE:
            case AffixUtils.TYPE_CURRENCY_TRIPLE:
            case AffixUtils.TYPE_CURRENCY_QUAD:
            case AffixUtils.TYPE_CURRENCY_QUINT:
                // All currency symbols use the same matcher
                addMatcher(factory.currency());
                break;
            default:
                throw new AssertionError();
            }

        } else if (ignorables != null && ignorables.getSet().contains(typeOrCp)) {
            // Case 2: the token is an ignorable literal.
            // No action necessary: the ignorables matcher has already been added.

        } else {
            // Case 3: the token is a non-ignorable literal.
            addMatcher(CodePointMatcher.getInstance(typeOrCp));
        }
        lastTypeOrCp = typeOrCp;
    }

    public String getPattern() {
        return affixPattern;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof AffixPatternMatcher))
            return false;
        return affixPattern.equals(((AffixPatternMatcher) other).affixPattern);
    }

    @Override
    public int hashCode() {
        return affixPattern.hashCode();
    }

    @Override
    public String toString() {
        return affixPattern;
    }
}
