// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.StringSegment;
import com.ibm.icu.impl.number.AffixPatternProvider;
import com.ibm.icu.impl.number.AffixUtils;
import com.ibm.icu.impl.number.PatternStringUtils;
import com.ibm.icu.impl.number.PatternStringUtils.PatternSignType;

/**
 * @author sffc
 *
 */
public class AffixMatcher implements NumberParseMatcher {
    private final AffixPatternMatcher prefix;
    private final AffixPatternMatcher suffix;
    private final int flags;

    /**
     * Comparator for two AffixMatcher instances which prioritizes longer prefixes followed by longer
     * suffixes, ensuring that the longest prefix/suffix pair is always chosen.
     */
    public static final Comparator<AffixMatcher> COMPARATOR = new Comparator<AffixMatcher>() {
        @Override
        public int compare(AffixMatcher lhs, AffixMatcher rhs) {
            if (length(lhs.prefix) != length(rhs.prefix)) {
                return length(lhs.prefix) > length(rhs.prefix) ? -1 : 1;
            } else if (length(lhs.suffix) != length(rhs.suffix)) {
                return length(lhs.suffix) > length(rhs.suffix) ? -1 : 1;
            } else if (!lhs.equals(rhs)) {
                // If the prefix and suffix are the same length, arbitrarily break ties.
                // We can't return zero unless the elements are equal.
                return lhs.hashCode() > rhs.hashCode() ? -1 : 1;
            } else {
                return 0;
            }
        }
    };

    private static boolean isInteresting(
            AffixPatternProvider patternInfo,
            IgnorablesMatcher ignorables,
            int parseFlags) {
        String posPrefixString = patternInfo.getString(AffixPatternProvider.FLAG_POS_PREFIX);
        String posSuffixString = patternInfo.getString(AffixPatternProvider.FLAG_POS_SUFFIX);
        String negPrefixString = null;
        String negSuffixString = null;
        if (patternInfo.hasNegativeSubpattern()) {
            negPrefixString = patternInfo.getString(AffixPatternProvider.FLAG_NEG_PREFIX);
            negSuffixString = patternInfo.getString(AffixPatternProvider.FLAG_NEG_SUFFIX);
        }

        if (0 == (parseFlags & ParsingUtils.PARSE_FLAG_USE_FULL_AFFIXES)
                && AffixUtils.containsOnlySymbolsAndIgnorables(posPrefixString, ignorables.getSet())
                && AffixUtils.containsOnlySymbolsAndIgnorables(posSuffixString, ignorables.getSet())
                && AffixUtils.containsOnlySymbolsAndIgnorables(negPrefixString, ignorables.getSet())
                && AffixUtils.containsOnlySymbolsAndIgnorables(negSuffixString, ignorables.getSet())
                // HACK: Plus and minus sign are a special case: we accept them trailing only if they are
                // trailing in the pattern string.
                && !AffixUtils.containsType(posSuffixString, AffixUtils.TYPE_PLUS_SIGN)
                && !AffixUtils.containsType(posSuffixString, AffixUtils.TYPE_MINUS_SIGN)
                && !AffixUtils.containsType(negSuffixString, AffixUtils.TYPE_PLUS_SIGN)
                && !AffixUtils.containsType(negSuffixString, AffixUtils.TYPE_MINUS_SIGN)) {
            // The affixes contain only symbols and ignorables.
            // No need to generate affix matchers.
            return false;
        }
        return true;
    }

    public static void createMatchers(
            AffixPatternProvider patternInfo,
            NumberParserImpl output,
            AffixTokenMatcherFactory factory,
            IgnorablesMatcher ignorables,
            int parseFlags) {
        if (!isInteresting(patternInfo, ignorables, parseFlags)) {
            return;
        }

        // The affixes have interesting characters, or we are in strict mode.
        // Use initial capacity of 6, the highest possible number of AffixMatchers.
        StringBuilder sb = new StringBuilder();
        ArrayList<AffixMatcher> matchers = new ArrayList<>(6);
        boolean includeUnpaired = 0 != (parseFlags & ParsingUtils.PARSE_FLAG_INCLUDE_UNPAIRED_AFFIXES);

        AffixPatternMatcher posPrefix = null;
        AffixPatternMatcher posSuffix = null;

        // Pre-process the affix strings to resolve LDML rules like sign display.
        for (PatternSignType type : PatternSignType.VALUES) {

            // Skip affixes in some cases
            if (type == PatternSignType.POS
                    && 0 != (parseFlags & ParsingUtils.PARSE_FLAG_PLUS_SIGN_ALLOWED)) {
                continue;
            }
            if (type == PatternSignType.POS_SIGN
                    && 0 == (parseFlags & ParsingUtils.PARSE_FLAG_PLUS_SIGN_ALLOWED)) {
                continue;
            }

            // Generate Prefix
            // TODO: Handle approximately sign?
            PatternStringUtils.patternInfoToStringBuilder(patternInfo,
                    true,
                    type,
                    false,
                    StandardPlural.OTHER,
                    false,
                    sb);
            AffixPatternMatcher prefix = AffixPatternMatcher
                    .fromAffixPattern(sb.toString(), factory, parseFlags);

            // Generate Suffix
            // TODO: Handle approximately sign?
            PatternStringUtils.patternInfoToStringBuilder(patternInfo,
                    false,
                    type,
                    false,
                    StandardPlural.OTHER,
                    false,
                    sb);
            AffixPatternMatcher suffix = AffixPatternMatcher
                    .fromAffixPattern(sb.toString(), factory, parseFlags);

            if (type == PatternSignType.POS) {
                posPrefix = prefix;
                posSuffix = suffix;
            } else if (Objects.equals(prefix, posPrefix) && Objects.equals(suffix, posSuffix)) {
                // Skip adding these matchers (we already have equivalents)
                continue;
            }

            // Flags for setting in the ParsedNumber; the token matchers may add more.
            int flags = (type == PatternSignType.NEG) ? ParsedNumber.FLAG_NEGATIVE : 0;

            // Note: it is indeed possible for posPrefix and posSuffix to both be null.
            // We still need to add that matcher for strict mode to work.
            matchers.add(getInstance(prefix, suffix, flags));
            if (includeUnpaired && prefix != null && suffix != null) {
                // The following if statements are designed to prevent adding two identical matchers.
                if (type == PatternSignType.POS || !Objects.equals(prefix, posPrefix)) {
                    matchers.add(getInstance(prefix, null, flags));
                }
                if (type == PatternSignType.POS || !Objects.equals(suffix, posSuffix)) {
                    matchers.add(getInstance(null, suffix, flags));
                }
            }
        }

        // Put the AffixMatchers in order, and then add them to the output.
        Collections.sort(matchers, COMPARATOR);
        output.addMatchers(matchers);
    }

    private static final AffixMatcher getInstance(
            AffixPatternMatcher prefix,
            AffixPatternMatcher suffix,
            int flags) {
        // TODO: Special handling for common cases like both strings empty.
        return new AffixMatcher(prefix, suffix, flags);
    }

    private AffixMatcher(AffixPatternMatcher prefix, AffixPatternMatcher suffix, int flags) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.flags = flags;
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        if (!result.seenNumber()) {
            // Prefix
            // Do not match if:
            // 1. We have already seen a prefix (result.prefix != null)
            // 2. The prefix in this AffixMatcher is empty (prefix == null)
            if (result.prefix != null || prefix == null) {
                return false;
            }

            // Attempt to match the prefix.
            int initialOffset = segment.getOffset();
            boolean maybeMore = prefix.match(segment, result);
            if (initialOffset != segment.getOffset()) {
                result.prefix = prefix.getPattern();
            }
            return maybeMore;

        } else {
            // Suffix
            // Do not match if:
            // 1. We have already seen a suffix (result.suffix != null)
            // 2. The suffix in this AffixMatcher is empty (suffix == null)
            // 3. The matched prefix does not equal this AffixMatcher's prefix
            if (result.suffix != null || suffix == null || !matched(prefix, result.prefix)) {
                return false;
            }

            // Attempt to match the suffix.
            int initialOffset = segment.getOffset();
            boolean maybeMore = suffix.match(segment, result);
            if (initialOffset != segment.getOffset()) {
                result.suffix = suffix.getPattern();
            }
            return maybeMore;
        }
    }

    @Override
    public boolean smokeTest(StringSegment segment) {
        return (prefix != null && prefix.smokeTest(segment))
                || (suffix != null && suffix.smokeTest(segment));
    }

    @Override
    public void postProcess(ParsedNumber result) {
        // Check to see if our affix is the one that was matched. If so, set the flags in the result.
        if (matched(prefix, result.prefix) && matched(suffix, result.suffix)) {
            // Fill in the result prefix and suffix with non-null values (empty string).
            // Used by strict mode to determine whether an entire affix pair was matched.
            if (result.prefix == null) {
                result.prefix = "";
            }
            if (result.suffix == null) {
                result.suffix = "";
            }
            result.flags |= flags;
            if (prefix != null) {
                prefix.postProcess(result);
            }
            if (suffix != null) {
                suffix.postProcess(result);
            }
        }
    }

    /**
     * Helper method to return whether the given AffixPatternMatcher equals the given pattern string.
     * Either both arguments must be null or the pattern string inside the AffixPatternMatcher must equal
     * the given pattern string.
     */
    static boolean matched(AffixPatternMatcher affix, String patternString) {
        return (affix == null && patternString == null)
                || (affix != null && affix.getPattern().equals(patternString));
    }

    /**
     * Helper method to return the length of the given AffixPatternMatcher. Returns 0 for null.
     */
    private static int length(AffixPatternMatcher matcher) {
        return matcher == null ? 0 : matcher.getPattern().length();
    }

    @Override
    public boolean equals(Object _other) {
        if (!(_other instanceof AffixMatcher)) {
            return false;
        }
        AffixMatcher other = (AffixMatcher) _other;
        return Objects.equals(prefix, other.prefix)
                && Objects.equals(suffix, other.suffix)
                && flags == other.flags;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(prefix) ^ Objects.hashCode(suffix) ^ flags;
    }

    @Override
    public String toString() {
        boolean isNegative = 0 != (flags & ParsedNumber.FLAG_NEGATIVE);
        return "<AffixMatcher" + (isNegative ? ":negative " : " ") + prefix + "#" + suffix + ">";
    }
}
