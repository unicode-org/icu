// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.parse;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.impl.StringSegment;

/**
 * Composes a number of matchers, and succeeds if any of the matchers succeed. Always greedily chooses
 * the first matcher in the list to succeed.
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

        int initialOffset = segment.getOffset();
        boolean maybeMore = false;
        for (int i = 0; i < matchers.size(); i++) {
            NumberParseMatcher matcher = matchers.get(i);
            maybeMore = maybeMore || matcher.match(segment, result);
            if (segment.getOffset() != initialOffset) {
                // Match succeeded.
                // NOTE: Except for a couple edge cases, if a matcher accepted string A, then it will
                // accept any string starting with A. Therefore, there is no possibility that matchers
                // later in the list may be evaluated on longer strings, and we can exit the loop here.
                break;
            }
        }

        // None of the matchers succeeded.
        return maybeMore;
    }

    @Override
    public boolean smokeTest(StringSegment segment) {
        assert frozen;
        if (matchers == null) {
            return false;
        }

        for (int i = 0; i < matchers.size(); i++) {
            if (matchers.get(i).smokeTest(segment)) {
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
        return "<AnyMatcher " + matchers + ">";
    }

}
