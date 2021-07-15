// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number.parse;

import com.ibm.icu.impl.StringSegment;

/**
 * The core interface implemented by all matchers used for number parsing.
 *
 * Given a string, there should NOT be more than one way to consume the string with the same matcher
 * applied multiple times. If there is, the non-greedy parsing algorithm will be unhappy and may enter an
 * exponential-time loop. For example, consider the "A Matcher" that accepts "any number of As". Given
 * the string "AAAA", there are 2^N = 8 ways to apply the A Matcher to this string: you could have the A
 * Matcher apply 4 times to each character; you could have it apply just once to all the characters; you
 * could have it apply to the first 2 characters and the second 2 characters; and so on. A better version
 * of the "A Matcher" would be for it to accept exactly one A, and allow the algorithm to run it
 * repeatedly to consume a string of multiple As. The A Matcher can implement the Flexible interface
 * below to signal that it can be applied multiple times in a row.
 *
 * @author sffc
 */
public interface NumberParseMatcher {

    /**
     * Matchers can implement the Flexible interface to indicate that they are optional and can be run
     * repeatedly. Used by SeriesMatcher, primarily in the context of IgnorablesMatcher.
     */
    public interface Flexible {
    }

    /**
     * Runs this matcher starting at the beginning of the given StringSegment. If this matcher finds
     * something interesting in the StringSegment, it should update the offset of the StringSegment
     * corresponding to how many chars were matched.
     *
     * @param segment
     *            The StringSegment to match against. Matches always start at the beginning of the
     *            segment. The segment is guaranteed to contain at least one char.
     * @param result
     *            The data structure to store results if the match succeeds.
     * @return Whether this matcher thinks there may be more interesting chars beyond the end of the
     *         string segment.
     */
    public boolean match(StringSegment segment, ParsedNumber result);

    /**
     * Performs a fast "smoke check" for whether or not this matcher could possibly match against the
     * given string segment. The test should be as fast as possible but also as restrictive as possible.
     * For example, matchers can maintain a UnicodeSet of all code points that count possibly start a
     * match. Matchers should use the {@link StringSegment#startsWith} method in order to correctly
     * handle case folding.
     *
     * @param segment
     *            The segment to check against.
     * @return true if the matcher might be able to match against this segment; false if it definitely
     *         will not be able to match.
     */
    public boolean smokeTest(StringSegment segment);

    /**
     * Method called at the end of a parse, after all matchers have failed to consume any more chars.
     * Allows a matcher to make final modifications to the result given the knowledge that no more
     * matches are possible.
     *
     * @param result
     *            The data structure to store results.
     */
    public void postProcess(ParsedNumber result);
}
