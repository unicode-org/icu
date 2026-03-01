// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.impl.StringSegment;

/**
 * Composes a number of matchers, running one after another. Matches the input string only if all of the
 * matchers in the series succeed. Performs greedy matches within the context of the series.
 *
 * @author sffc
 */
public class SeriesMatcher implements NumberParseMatcher {

    protected List<NumberParseMatcher> matchers = null;
    protected boolean frozen = false;

    public void addMatcher(NumberParseMatcher matcher) {
        assert !frozen;
        if (matchers == null) {
            matchers = new ArrayList<NumberParseMatcher>();
        }
        matchers.add(matcher);
    }

    public void freeze() {
        frozen = true;
    }

    public int length() {
        return matchers == null ? 0 : matchers.size();
    }

    @Override
    public boolean match(StringSegment segment, ParsedNumber result) {
        assert frozen;
        if (matchers == null) {
            return false;
        }

        // TODO: Give a nice way to reset ParsedNumber to avoid the copy here.
        ParsedNumber backup = new ParsedNumber();
        backup.copyFrom(result);

        int initialOffset = segment.getOffset();
        boolean maybeMore = true;
        for (int i = 0; i < matchers.size();) {
            NumberParseMatcher matcher = matchers.get(i);
            int matcherOffset = segment.getOffset();
            if (segment.length() != 0) {
                maybeMore = matcher.match(segment, result);
            } else {
                // Nothing for this matcher to match; ask for more.
                maybeMore = true;
            }

            boolean success = (segment.getOffset() != matcherOffset);
            boolean isFlexible = matcher instanceof NumberParseMatcher.Flexible;
            if (success && isFlexible) {
                // Match succeeded, and this is a flexible matcher. Re-run it.
            } else if (success) {
                // Match succeeded, and this is NOT a flexible matcher. Proceed to the next matcher.
                i++;
                // Small hack: if there is another matcher coming, do not accept trailing weak chars.
                // Needed for proper handling of currency spacing.
                if (i < matchers.size() && segment.getOffset() != result.charEnd && result.charEnd > matcherOffset) {
                    segment.setOffset(result.charEnd);
                }
            } else if (isFlexible) {
                // Match failed, and this is a flexible matcher. Try again with the next matcher.
                i++;
            } else {
                // Match failed, and this is NOT a flexible matcher. Exit.
                segment.setOffset(initialOffset);
                result.copyFrom(backup);
                return maybeMore;
            }
        }

        // All matchers in the series succeeded.
        return maybeMore;
    }

    @Override
    public boolean smokeTest(StringSegment segment) {
        assert frozen;
        if (matchers == null) {
            return false;
        }

        // SeriesMatchers are never allowed to start with a Flexible matcher.
        assert !(matchers.get(0) instanceof NumberParseMatcher.Flexible);
        return matchers.get(0).smokeTest(segment);
    }

    @Override
    public void postProcess(ParsedNumber result) {
        assert frozen;
        if (matchers == null) {
            return;
        }

        for (int i = 0; i < matchers.size(); i++) {
            NumberParseMatcher matcher = matchers.get(i);
            matcher.postProcess(result);
        }
    }

    @Override
    public String toString() {
        return "<SeriesMatcher " + matchers + ">";
    }

}
