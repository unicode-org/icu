// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.rbbi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A segmentation rule expressed as in UAXes #14 and #29.
 *
 * The application of a remap rule is a normal regex replacement on the remapped
 * string.  This replacement may use capturing groups.  Any positions in the
 * original string that correspond to positions within the replaced text are
 * resolved to NO_BREAK by this rule.
 */
public class RemapRule extends SegmentationRule {
    RemapRule(String name, String pattern, String replacement) {
        super(name);
        replacement_ = replacement;
        pattern_ = Pattern.compile(expandUnicodeSets(pattern), Pattern.COMMENTS | Pattern.DOTALL);
    }

    @Override
    void apply(StringBuilder remapped, BreakContext[] resolved) {
        // This one has to be a StringBuffer rather than a StringBuilder because the
        // overload of
        // AppendReplacement that takes a StringBuilder is new in Java 9.
        StringBuffer result = new StringBuffer();
        int i = 0;
        int offset = 0;
        // We find all matches of the `pattern_` and replace them according to
        // the `replacement_`, producing the new remapped string `result`.
        // For every position i in the original string,
        // `resolved[i].indexInRemapped` is null if i lies within a replaced
        // match, and is set to the new index in `result` otherwise, by adding
        // the accumulated difference `offset` between match lengths and
        // replacement lengths.
        // Consider a 4-codepoint, 6 code unit string s = ⟨ 𒀀, ◌́, ␠, ◌𝅲 ⟩, where
        // ␠ stands for U+0020 and U+12000 𒀀 and U+1D172 ◌𝅲 each require two code
        // units, and apply the following two rules:
        // 1. (?<X>\P{lb=SP}) \p{lb=CM}* → ${X}
        // 2. \p{lb=CM} → A
        // The string remapped and the indexInRemapped values change as follows:
        // indexInRemapped remapped string rule final
        // (aligned on the initial string) applied offset
        // 𒀀 ◌́ ␠ ◌𝅲
        // 0 1 2 3 4 5 6 ⟨ 𒀀, ◌́, ␠, ◌𝅲 ⟩ (none)
        // 0 - - 2 3 4 5 ⟨ 𒀀, ␠, ◌𝅲 ⟩ 1 -1
        // 0 - - 2 3 - 4 ⟨ 𒀀, ␠, A ⟩ 2 -1
        //
        // Note that the last indexInRemapped is always equal to the length of
        // the remapped string.
        final Matcher matcher = pattern_.matcher(remapped);
        while (matcher.find()) {
            for (;; ++i) {
                if (resolved[i].indexInRemapped == null) {
                    continue;
                }
                if (resolved[i].indexInRemapped != null &&
                        resolved[i].indexInRemapped > matcher.start()) {
                    break;
                }
                resolved[i].indexInRemapped += offset;
            }
            for (;; ++i) {
                if (resolved[i].indexInRemapped == null) {
                    continue;
                }
                // Note that
                // `*resolved[i].indexInRemapped > matcher.end()` should
                // never happen with ordinary rules, but could in principle
                // happen with rules that remap to code point sequences, e.g.,
                // 1. BC → TYZ
                // 2. AT → X
                // applied to ⟨ A, B, C ⟩:
                // indexInRemapped remapped rule
                // A B C
                // 0 1 2 3 ⟨ A, B, C ⟩ (none)
                // 0 1 - 4 ⟨ A, T, Y, Z ⟩ 1
                // 0 - - 3 ⟨ X, Y, Z ⟩ 2
                // Where for the application of rule 2, the match ends at
                // position 2 in remapped, which does not correspond to a
                // position in the original string.
                if (resolved[i].indexInRemapped != null &&
                        resolved[i].indexInRemapped >= matcher.end()) {
                    break;
                }
                if (resolved[i].appliedRule != null &&
                        resolved[i].appliedRule.resolution() == Resolution.BREAK) {
                    throw new IllegalArgumentException(
                            "Replacement rule at remapped indices " +
                                    matcher.start() +
                                    " sqq. spans a break");
                }
                resolved[i].appliedRule = this;
                resolved[i].indexInRemapped = null;
            }
            // While replacing, we need to check that we are not creating
            // surrogate pairs.  Since appendReplacement performs two
            // concatenations (the unreplaced segment and the replacement), we
            // need to check in two places: whether the unreplaced segment
            // starts with a trailing surrogate that ends up after a leading
            // surrogate, and whether the replaced segment starts with a leading
            // surrogate that ends up after a trailing surrogate.
            // We break the pair by replacing one of the surrogates with U+FFFF,
            // which has the same properties for all but line breaking, and the
            // same behaviour in line breaking (lb=SG and lb=XX are both treated
            // as lb=AL).
            Integer trailingLead = null;
            if (result.length() > 0 && Character.isHighSurrogate(result.charAt(result.length() - 1))) {
                trailingLead = result.length() - 1;
            }

            matcher.appendReplacement(result, replacement_);

            if (trailingLead != null && trailingLead + 1 < result.length() &&
                    Character.isLowSurrogate(result.charAt(trailingLead + 1))) {
                result.setCharAt(trailingLead, '\uFFFF');
            }

            if (matcher.start() + offset > 0 &&
                    Character.isHighSurrogate(result.charAt(matcher.start() + offset - 1)) &&
                    Character.isLowSurrogate(result.charAt(matcher.start() + offset))) {
                result.setCharAt(matcher.start() + offset, '\uFFFF');
            }
            offset = result.length() - resolved[i].indexInRemapped;
        }
        for (; i < resolved.length; ++i) {
            if (resolved[i].indexInRemapped == null) {
                continue;
            }
            resolved[i].indexInRemapped += offset;
        }

        Integer trailingLead = null;
        if (result.length() > 0 && Character.isHighSurrogate(result.charAt(result.length() - 1))) {
            trailingLead = result.length() - 1;
        }
        matcher.appendTail(result);
        if (trailingLead != null && trailingLead + 1 < result.length() &&
                Character.isLowSurrogate(result.charAt(trailingLead + 1))) {
            result.setCharAt(trailingLead, '\uFFFF');
        }

        if (resolved[resolved.length - 1].indexInRemapped != result.length()) {
            StringBuilder indices = new StringBuilder();
            for (final BreakContext r : resolved) {
                indices.append(r.indexInRemapped == null ? "null" : r.indexInRemapped.toString());
                indices.append(",");
            }
            throw new IllegalArgumentException("Inconsistent indexInRemapped " + indices + " for new remapped string " +
                    result);
        }
        remapped.setLength(0);
        remapped.append(result);
    }

    @Override
    Resolution resolution() {
        return Resolution.NO_BREAK;
    }

    private final Pattern pattern_;
    private final String replacement_;
}