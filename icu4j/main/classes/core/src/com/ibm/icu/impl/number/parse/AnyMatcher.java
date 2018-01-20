// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.UnicodeSet;

/**
 * Composes a number of matchers, and succeeds if any of the matchers succeed.
 *
 * @author sffc
 * @see SeriesMatcher
 */
public class AnyMatcher implements NumberParseMatcher {

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
        boolean maybeMore = false;
        for (int i = 0; i < matchers.size(); i++) {
            NumberParseMatcher matcher = matchers.get(i);
            maybeMore = maybeMore || matcher.match(segment, result);
            if (segment.getOffset() != initialOffset) {
                // Match succeeded. Return true here to be safe.
                // TODO: Better would be to run each matcher and return true only if at least one of the
                // matchers returned true.
                return true;
            }
        }

        // None of the matchers succeeded.
        return maybeMore;
    }

    @Override
    public UnicodeSet getLeadCodePoints() {
        assert frozen;
        if (matchers == null) {
            return UnicodeSet.EMPTY;
        }

        UnicodeSet leadCodePoints = new UnicodeSet();
        for (int i = 0; i < matchers.size(); i++) {
            NumberParseMatcher matcher = matchers.get(i);
            leadCodePoints.addAll(matcher.getLeadCodePoints());
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
            if (matcher.matchesEmpty()) {
                return true;
            }
        }
        return false;
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
