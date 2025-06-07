// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.rbbi;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A regex rule expressed as in UAXes #14 and #29.
 *
 * <p>The rule consists of two regexes for context before and after a position in the remapped text,
 * and of a resolution (break or not) that applies to the corresponding position in the original
 * string if both match.
 */
class RegexRule extends SegmentationRule {
    RegexRule(String name, String before, Resolution resolution, String after) {
        super(name);
        resolution_ = resolution;
        before_ = Pattern.compile(expandUnicodeSets(before), Pattern.COMMENTS | Pattern.DOTALL);
        endsWithBefore_ =
                Pattern.compile(
                        ".*(" + expandUnicodeSets(before) + ")", Pattern.COMMENTS | Pattern.DOTALL);
        after_ = Pattern.compile(expandUnicodeSets(after), Pattern.COMMENTS | Pattern.DOTALL);
    }

    @Override
    void apply(StringBuilder remapped, BreakContext[] resolved) {
        // The unicodetools implementation simply tries, for each index, to
        // match the string up to the index against /.*(before)/ (with
        // `matches`) and the beginning of the string after the index against
        // /after/ (with `lookingAt`), but that is very slow, especially for
        // nonempty /before/. While the old monkeys are not a production
        // implementation, we still do not want them to be too slow, since we
        // need to test millions of sample strings. Instead we search for
        // /before/ and /after/, and check resulting candidates. This speeds
        // things up by a factor of ~40.
        // We need to be careful about greedy matching: The first position where
        // the rule matches may be before the end of the first /before/ match.
        // However, it is both:
        // 1. within a /before/ match or at its bounds,
        // 2. at the beginning of an /after/ match.
        // Further, the /before/ context of the rule matches within the
        // aforementioned /before/ match. Note that we need to look for
        // overlapping matches, thus calls to `find` are always preceded by a
        // reset via `region`.
        final Matcher beforeSearch = before_.matcher(remapped);
        final Matcher afterSearch = after_.matcher(remapped);
        beforeSearch.useAnchoringBounds(false);
        afterSearch.useAnchoringBounds(false);
        if (beforeSearch.find() && afterSearch.find()) {
            for (; ; ) {
                if (afterSearch.start() < beforeSearch.start()) {
                    afterSearch.region(beforeSearch.start(), remapped.length());
                    if (!afterSearch.find()) {
                        break;
                    }
                } else if (afterSearch.start() > beforeSearch.end()) {
                    if (beforeSearch.start() == remapped.length()) {
                        break;
                    }
                    beforeSearch.region(
                            remapped.offsetByCodePoints(beforeSearch.start(), 1),
                            remapped.length());
                    if (!beforeSearch.find()) {
                        break;
                    }
                } else {
                    final Optional<BreakContext> position =
                            Arrays.stream(resolved)
                                    .filter(
                                            r ->
                                                    r.indexInRemapped != null
                                                            && r.indexInRemapped
                                                                    == afterSearch.start())
                                    .findFirst();
                    if (!position.isPresent()) {
                        throw new IllegalArgumentException(
                                ("Rule "
                                        + name()
                                        + " matched at position "
                                        + afterSearch.start()
                                        + " in "
                                        + remapped
                                        + " which does not correspond to an index in "
                                        + "the original string"));
                    }
                    if (position.get().appliedRule == null
                            && endsWithBefore_
                                    .matcher(remapped)
                                    .useAnchoringBounds(false)
                                    .region(beforeSearch.start(), afterSearch.start())
                                    .matches()) {
                        position.get().appliedRule = this;
                    }
                    if (afterSearch.start() == remapped.length()) {
                        break;
                    }
                    afterSearch.region(
                            remapped.offsetByCodePoints(afterSearch.start(), 1), remapped.length());
                    if (!afterSearch.find()) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    Resolution resolution() {
        return resolution_;
    }

    private final Pattern before_;
    private final Pattern endsWithBefore_;
    private final Pattern after_;
    private final Resolution resolution_;
}
