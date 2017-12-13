// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.number.MutablePatternModifier;
import com.ibm.icu.impl.number.NumberStringBuilder;

/**
 * @author sffc
 *
 */
public class AffixMatcher implements NumberParseMatcher {
    private final String prefix;
    private final String suffix;
    private final int flags;

    /**
     * Comparator for two AffixMatcher instances which prioritizes longer prefixes followed by longer suffixes, ensuring
     * that the longest prefix/suffix pair is always chosen.
     */
    public static final Comparator<AffixMatcher> COMPARATOR = new Comparator<AffixMatcher>() {
        @Override
        public int compare(AffixMatcher o1, AffixMatcher o2) {
            if (o1.prefix.length() != o2.prefix.length()) {
                return o1.prefix.length() > o2.prefix.length() ? -1 : 1;
            } else if (o1.suffix.length() != o2.suffix.length()) {
                return o1.suffix.length() > o2.suffix.length() ? -1 : 1;
            } else if (!o1.equals(o2)) {
                // If the prefix and suffix are the same length, arbitrarily break ties.
                // We can't return zero unless the elements are equal.
                return o1.hashCode() > o2.hashCode() ? -1 : 1;
            } else {
                return 0;
            }
        }
    };

    /**
     * Creates multiple AffixMatchers, enough to cover the requirements for the given pattern modifier, appending them
     * in order to the NumberParserImpl.
     */
    public static void generateFromPatternModifier(
            MutablePatternModifier patternModifier,
            int flags,
            NumberParserImpl output) {

        // Store the matchers in a TreeSet to ensure both uniqueness and order.
        Set<AffixMatcher> matchers = new TreeSet<AffixMatcher>(COMPARATOR);

        // Construct one matcher per isNegative/plural combination. Most of the time, plurals aren't needed, so only
        // two matchers will be created, one for positive and one for negative.
        NumberStringBuilder nsb = new NumberStringBuilder();
        boolean isNegative = false;
        while (true) {
            if (isNegative) {
                flags |= ParsedNumber.FLAG_NEGATIVE;
            }

            if (patternModifier.needsPlurals()) {
                for (StandardPlural plural : StandardPlural.VALUES) {
                    patternModifier.setNumberProperties(isNegative, plural);
                    matchers.add(getInstance(patternModifier, flags, nsb));
                }
            } else {
                patternModifier.setNumberProperties(isNegative, null);
                matchers.add(getInstance(patternModifier, flags, nsb));
            }

            if (isNegative) {
                break;
            } else {
                isNegative = true;
            }
        }

        for (AffixMatcher matcher : matchers) {
            output.addMatcher(matcher);
        }
    }

    /**
     * Constructs an AffixMatcher from the given MutablePatternModifier and flags. The NumberStringBuilder is used as a
     * temporary object only.
     */
    private static AffixMatcher getInstance(
            MutablePatternModifier patternModifier,
            int flags,
            NumberStringBuilder nsb) {
        // TODO: Make this more efficient (avoid the substrings and things)
        nsb.clear();
        patternModifier.apply(nsb, 0, 0);
        int prefixLength = patternModifier.getPrefixLength();
        String full = nsb.toString();
        String prefix = full.substring(0, prefixLength);
        String suffix = full.substring(prefixLength);
        return new AffixMatcher(prefix, suffix, flags);
    }

    private AffixMatcher(String prefix, String suffix, int flags) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.flags = flags;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        if (result.quantity == null) {
            // Prefix
            if (result.prefix != null || prefix.length() == 0) {
                return false;
            }
            int overlap = segment.getCommonPrefixLength(prefix);
            if (overlap == prefix.length()) {
                result.prefix = prefix;
                segment.adjustOffset(overlap);
                result.setCharsConsumed(segment);
                return false;
            } else if (overlap == segment.length()) {
                return true;
            }

        } else {
            // Suffix
            if (result.suffix != null || suffix.length() == 0 || !prefix.equals(orEmpty(result.prefix))) {
                return false;
            }
            int overlap = segment.getCommonPrefixLength(suffix);
            if (overlap == suffix.length()) {
                result.suffix = suffix;
                segment.adjustOffset(overlap);
                result.setCharsConsumed(segment);
                return false;
            } else if (overlap == segment.length()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // Check to see if our affix is the one that was matched. If so, set the flags in the result.
        if (prefix.equals(orEmpty(result.prefix)) && suffix.equals(orEmpty(result.suffix))) {
            result.flags |= flags;
        }
    }

    /**
     * Returns the input string, or "" if input is null.
     */
    static String orEmpty(String str) {
        return str == null ? "" : str;
    }

    /**
     * Returns the sum of prefix and suffix length in the ParsedNumber.
     */
    public static int affixLength(ParsedNumber o2) {
        return orEmpty(o2.prefix).length() + orEmpty(o2.suffix).length();
    }

    @Override
    public boolean equals(Object _other) {
        if (!(_other instanceof AffixMatcher)) {
            return false;
        }
        AffixMatcher other = (AffixMatcher) _other;
        return prefix.equals(other.prefix) && suffix.equals(other.suffix) && flags == other.flags;
    }

    @Override
    public int hashCode() {
        return prefix.hashCode() ^ suffix.hashCode() ^ flags;
    }

    @Override
    public String toString() {
        return "<AffixMatcher \"" + prefix + "\" \"" + suffix + "\">";
    }
}
