// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.ibm.icu.impl.number.AffixPatternProvider;
import com.ibm.icu.impl.number.AffixUtils;
import com.ibm.icu.text.UnicodeSet;

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

    public static void generateFromAffixPatternProvider(
            AffixPatternProvider patternInfo,
            NumberParserImpl output,
            IgnorablesMatcher ignorables,
            int parseFlags) {
        // Lazy-initialize the StringBuilder.
        StringBuilder sb = null;

        // Use initial capacity of 6, the highest possible number of AffixMatchers.
        // TODO: Lazy-initialize?
        ArrayList<AffixMatcher> matchers = new ArrayList<AffixMatcher>(6);

        sb = getCleanAffix(patternInfo, AffixPatternProvider.FLAG_POS_PREFIX, ignorables.getSet(), sb);
        String posPrefix = ParsingUtils.maybeFold(toStringOrEmpty(sb), parseFlags);
        sb = getCleanAffix(patternInfo, AffixPatternProvider.FLAG_POS_SUFFIX, ignorables.getSet(), sb);
        String posSuffix = ParsingUtils.maybeFold(toStringOrEmpty(sb), parseFlags);

        boolean includeUnpaired = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_INCLUDE_UNPAIRED_AFFIXES);

        if (!posPrefix.isEmpty() || !posSuffix.isEmpty()) {
            matchers.add(getInstance(posPrefix, posSuffix, 0));
            if (includeUnpaired && !posPrefix.isEmpty() && !posSuffix.isEmpty()) {
                matchers.add(getInstance(posPrefix, "", 0));
                matchers.add(getInstance("", posSuffix, 0));
            }
        }

        if (patternInfo.hasNegativeSubpattern()) {
            sb = getCleanAffix(patternInfo, AffixPatternProvider.FLAG_NEG_PREFIX, ignorables.getSet(), sb);
            String negPrefix = ParsingUtils.maybeFold(toStringOrEmpty(sb), parseFlags);
            sb = getCleanAffix(patternInfo, AffixPatternProvider.FLAG_NEG_SUFFIX, ignorables.getSet(), sb);
            String negSuffix = ParsingUtils.maybeFold(toStringOrEmpty(sb), parseFlags);

            if (negPrefix.equals(posPrefix) && negSuffix.equals(posSuffix)) {
                // No-op: favor the positive AffixMatcher
            } else if (!negPrefix.isEmpty() || !negSuffix.isEmpty()) {
                matchers.add(getInstance(negPrefix, negSuffix, ParsedNumber.FLAG_NEGATIVE));
                if (includeUnpaired && !negPrefix.isEmpty() && !negSuffix.isEmpty()) {
                    if (!negPrefix.equals(posPrefix)) {
                        matchers.add(getInstance(negPrefix, "", ParsedNumber.FLAG_NEGATIVE));
                    }
                    if (!negSuffix.equals(posSuffix)) {
                        matchers.add(getInstance("", negSuffix, ParsedNumber.FLAG_NEGATIVE));
                    }
                }
            }
        }

        // Put the AffixMatchers in order, and then add them to the output.
        Collections.sort(matchers, COMPARATOR);
        output.addMatchers(matchers);
    }

    private static StringBuilder getCleanAffix(
            AffixPatternProvider patternInfo,
            int flag,
            UnicodeSet ignorables,
            StringBuilder sb) {
        if (sb != null) {
            sb.setLength(0);
        }
        if (patternInfo.length(flag) > 0) {
            sb = AffixUtils.trimSymbolsAndIgnorables(patternInfo.getString(flag), ignorables, sb);
        }
        return sb;
    }

    private static String toStringOrEmpty(StringBuilder sb) {
        return (sb == null || sb.length() == 0) ? "" : sb.toString();
    }

    private static final AffixMatcher getInstance(String prefix, String suffix, int flags) {
        // TODO: Special handling for common cases like both strings empty.
        return new AffixMatcher(prefix, suffix, flags);
    }

    private AffixMatcher(String prefix, String suffix, int flags) {
        assert prefix != null;
        assert suffix != null;
        this.prefix = prefix;
        this.suffix = suffix;
        this.flags = flags;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        if (!result.seenNumber()) {
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
    public UnicodeSet getLeadCodePoints() {
        UnicodeSet leadCodePoints = new UnicodeSet();
        ParsingUtils.putLeadCodePoint(prefix, leadCodePoints);
        ParsingUtils.putLeadCodePoint(suffix, leadCodePoints);
        return leadCodePoints.freeze();
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // Check to see if our affix is the one that was matched. If so, set the flags in the result.
        if (prefix.equals(orEmpty(result.prefix)) && suffix.equals(orEmpty(result.suffix))) {
            // Fill in the result prefix and suffix with non-null values (empty string).
            // Used by strict mode to determine whether an entire affix pair was matched.
            result.prefix = prefix;
            result.suffix = suffix;
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
        boolean isNegative = 0 != (flags & ParsedNumber.FLAG_NEGATIVE);
        return "<AffixMatcher" + (isNegative ? ":negative " : " ") + prefix + "#" + suffix + ">";
    }
}
