// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.UnicodeSet;

/**
 * Composes a number of matchers, running one after another. Matches the input string only if all of the
 * matchers in the series succeed. Performs greedy matches within the context of the series.
 *
 * @author sffc
 * @see AnyMatcher
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
        for (int i = 0; i < matchers.size(); i++) {
            NumberParseMatcher matcher = matchers.get(i);
            int matcherOffset = segment.getOffset();
            if (segment.length() != 0) {
                maybeMore = matcher.match(segment, result);
            } else {
                // Nothing for this matcher to match; ask for more.
                maybeMore = true;
            }
            if (segment.getOffset() == matcherOffset && !matcher.matchesEmpty()) {
                // Match failed.
                segment.setOffset(initialOffset);
                result.copyFrom(backup);
                return maybeMore;
            }
        }

        // All matchers in the series succeeded.
        return maybeMore;
    }

    @Override
    public UnicodeSet getLeadCodePoints() {
        assert frozen;
        if (matchers == null) {
            return UnicodeSet.EMPTY;
        }

        if (!matchers.get(0).matchesEmpty()) {
            return matchers.get(0).getLeadCodePoints();
        }

        UnicodeSet leadCodePoints = new UnicodeSet();
        for (int i = 0; i < matchers.size(); i++) {
            NumberParseMatcher matcher = matchers.get(i);
            leadCodePoints.addAll(matcher.getLeadCodePoints());
            if (!matcher.matchesEmpty()) {
                break;
            }
        }
        return leadCodePoints.freeze();
    }

    @Override
    public boolean matchesEmpty() {
        assert frozen;
        if (matchers == null) {
            return true;
        }

        for (int i = 0; i < matchers.size(); i++) {
            NumberParseMatcher matcher = matchers.get(i);
            if (!matcher.matchesEmpty()) {
                return false;
            }
        }
        return true;
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
