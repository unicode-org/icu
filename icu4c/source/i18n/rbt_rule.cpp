/*
**********************************************************************
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "rbt_rule.h"
#include "unicode/rep.h"
#include "rbt_data.h"
#include "unicode/unifilt.h"
#include "unicode/uniset.h"
#include "unicode/unicode.h"
#include "cmemory.h"

const UChar TransliterationRule::ETHER = 0xFFFF;

static const UChar APOSTROPHE = 0x0027; // '
static const UChar BACKSLASH  = 0x005C; // \

/**
 * Construct a new rule with the given input, output text, and other
 * attributes.  A cursor position may be specified for the output text.
 * @param input input string, including key and optional ante and
 * post context
 * @param anteContextPos offset into input to end of ante context, or -1 if
 * none.  Must be <= input.length() if not -1.
 * @param postContextPos offset into input to start of post context, or -1
 * if none.  Must be <= input.length() if not -1, and must be >=
 * anteContextPos.
 * @param output output string
 * @param cursorPosition offset into output at which cursor is located, or -1 if
 * none.  If less than zero, then the cursor is placed after the
 * <code>output</code>; that is, -1 is equivalent to
 * <code>output.length()</code>.  If greater than
 * <code>output.length()</code> then an exception is thrown.
 * @param adoptedSegs array of 2n integers.  Each of n pairs consists of offset,
 * limit for a segment of the input string.  Characters in the output string
 * refer to these segments if they are in a special range determined by the
 * associated RuleBasedTransliterator.Data object.  May be null if there are
 * no segments.
 * @param anchorStart TRUE if the the rule is anchored on the left to
 * the context start
 * @param anchorEnd TRUE if the rule is anchored on the right to the
 * context limit
 */
TransliterationRule::TransliterationRule(const UnicodeString& input,
                                         int32_t anteContextPos, int32_t postContextPos,
                                         const UnicodeString& outputStr,
                                         int32_t cursorPosition, int32_t cursorOffset,
                                         int32_t* adoptedSegs,
                                         UBool anchorStart, UBool anchorEnd,
                                         const TransliterationRuleData& theData,
                                         UErrorCode& status) :
    data(theData) {
    init(input, anteContextPos, postContextPos,
         outputStr, cursorPosition, cursorOffset, adoptedSegs,
         anchorStart, anchorEnd, status);
}

/**
 * Construct a new rule with the given input, output text, and other
 * attributes.  A cursor position may be specified for the output text.
 * @param input input string, including key and optional ante and
 * post context
 * @param anteContextPos offset into input to end of ante context, or -1 if
 * none.  Must be <= input.length() if not -1.
 * @param postContextPos offset into input to start of post context, or -1
 * if none.  Must be <= input.length() if not -1, and must be >=
 * anteContextPos.
 * @param output output string
 * @param cursorPosition offset into output at which cursor is located, or -1 if
 * none.  If less than zero, then the cursor is placed after the
 * <code>output</code>; that is, -1 is equivalent to
 * <code>output.length()</code>.  If greater than
 * <code>output.length()</code> then an exception is thrown.
 */
TransliterationRule::TransliterationRule(const UnicodeString& input,
                                         int32_t anteContextPos, int32_t postContextPos,
                                         const UnicodeString& outputStr,
                                         int32_t cursorPosition,
                                         const TransliterationRuleData& theData,
                                         UErrorCode& status) :
    data(theData) {
    init(input, anteContextPos, postContextPos,
         outputStr, cursorPosition, 0, NULL, FALSE, FALSE, status);
}

/**
 * Copy constructor.
 */
TransliterationRule::TransliterationRule(TransliterationRule& other) :
    pattern(other.pattern),
    output(other.output),
    anteContextLength(other.anteContextLength),
    keyLength(other.keyLength),
    cursorPos(other.cursorPos),
    flags(other.flags),
    data(other.data) {

    segments = 0;
    if (other.segments != 0) {
        // Find the end marker, which is a -1.
        int32_t len = 0;
        while (other.segments[len] >= 0) {
            ++len;
        }
        ++len;
        segments = new int32_t[len];
        uprv_memcpy(segments, other.segments, len*sizeof(segments[0]));
    }
}

void TransliterationRule::init(const UnicodeString& input,
                               int32_t anteContextPos, int32_t postContextPos,
                               const UnicodeString& outputStr,
                               int32_t cursorPosition, int32_t cursorOffset,
                               int32_t* adoptedSegs,
                               UBool anchorStart, UBool anchorEnd,
                               UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    // Do range checks only when warranted to save time
    if (anteContextPos < 0) {
        anteContextLength = 0;
    } else {
        if (anteContextPos > input.length()) {
            // throw new IllegalArgumentException("Invalid ante context");
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        anteContextLength = anteContextPos;
    }
    if (postContextPos < 0) {
        keyLength = input.length() - anteContextLength;
    } else {
        if (postContextPos < anteContextLength ||
            postContextPos > input.length()) {
            // throw new IllegalArgumentException("Invalid post context");
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        keyLength = postContextPos - anteContextLength;
    }
    if (cursorPosition < 0) {
        cursorPosition = outputStr.length();
    } else {
        if (cursorPosition > outputStr.length()) {
            // throw new IllegalArgumentException("Invalid cursor position");
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
    }
    this->cursorPos = cursorPosition + cursorOffset;
    this->output = outputStr;
    // We don't validate the segments array.  The caller must
    // guarantee that the segments are well-formed.
    this->segments = adoptedSegs;
    // Find the position of the first segment index that is after the
    // anteContext (in the key).  Note that this may be a start or a
    // limit index.  If all segments are in the ante context,
    // firstKeySeg should point past the last segment -- that is, it
    // should point at the end marker, which is -1.  This allows the
    // code to back up by one to obtain the last ante context segment.
    firstKeySeg = -1;
    if (segments != 0) {
        do {
            ++firstKeySeg;
        } while (segments[firstKeySeg] >= 0 &&
                 segments[firstKeySeg] < anteContextLength);
    }

    pattern = input;
    flags = 0;
    if (anchorStart) {
        flags |= ANCHOR_START;
    }
    if (anchorEnd) {
        flags |= ANCHOR_END;
    }
}

TransliterationRule::~TransliterationRule() {
    delete[] segments;
}

/**
 * Return the position of the cursor within the output string.
 * @return a value from 0 to <code>getOutput().length()</code>, inclusive.
 */
int32_t TransliterationRule::getCursorPos(void) const {
    return cursorPos;
}

/**
 * Return the preceding context length.  This method is needed to
 * support the <code>Transliterator</code> method
 * <code>getMaximumContextLength()</code>.  Internally, this is
 * implemented as the anteContextLength, optionally plus one if
 * there is a start anchor.  The one character anchor gap is
 * needed to make repeated incremental transliteration with
 * anchors work.
 */
int32_t TransliterationRule::getContextLength(void) const {
    return anteContextLength + ((flags & ANCHOR_START) ? 1 : 0);
}

/**
 * Internal method.  Returns 8-bit index value for this rule.
 * This is the low byte of the first character of the key,
 * unless the first character of the key is a set.  If it's a
 * set, or otherwise can match multiple keys, the index value is -1.
 */
int16_t TransliterationRule::getIndexValue() const {
    if (anteContextLength == pattern.length()) {
        // A pattern with just ante context {such as foo)>bar} can
        // match any key.
        return -1;
    }
    UChar32 c = pattern.char32At(anteContextLength);
    return (int16_t)(data.lookup(c) == NULL ? (c & 0xFF) : -1);
}

/**
 * Internal method.  Returns true if this rule matches the given
 * index value.  The index value is an 8-bit integer, 0..255,
 * representing the low byte of the first character of the key.
 * It matches this rule if it matches the first character of the
 * key, or if the first character of the key is a set, and the set
 * contains any character with a low byte equal to the index
 * value.  If the rule contains only ante context, as in foo)>bar,
 * then it will match any key.
 */
UBool TransliterationRule::matchesIndexValue(uint8_t v) const {
    if (anteContextLength == pattern.length()) {
        // A pattern with just ante context {such as foo)>bar} can
        // match any key.
        return TRUE;
    }
    UChar32 c = pattern.char32At(anteContextLength);
    const UnicodeMatcher* matcher = data.lookup(c);
    return matcher == NULL ? (uint8_t(c) == v) :
        matcher->matchesIndexValue(v);
}

/**
 * Return true if this rule masks another rule.  If r1 masks r2 then
 * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
 * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
 * "[c]a>x" masks "[dc]a>y".
 */
UBool TransliterationRule::masks(const TransliterationRule& r2) const {
    /* Rule r1 masks rule r2 if the string formed of the
     * antecontext, key, and postcontext overlaps in the following
     * way:
     *
     * r1:      aakkkpppp
     * r2:     aaakkkkkpppp
     *            ^
     * 
     * The strings must be aligned at the first character of the
     * key.  The length of r1 to the left of the alignment point
     * must be <= the length of r2 to the left; ditto for the
     * right.  The characters of r1 must equal (or be a superset
     * of) the corresponding characters of r2.  The superset
     * operation should be performed to check for UnicodeSet
     * masking.
     *
     * Anchors:  Two patterns that differ only in anchors only
     * mask one another if they are exactly equal, and r2 has
     * all the anchors r1 has (optionally, plus some).  Here Y
     * means the row masks the column, N means it doesn't.
     *
     *         ab   ^ab    ab$  ^ab$
     *   ab    Y     Y     Y     Y
     *  ^ab    N     Y     N     Y
     *   ab$   N     N     Y     Y
     *  ^ab$   N     N     N     Y
     *
     * Post context: {a}b masks ab, but not vice versa, since {a}b
     * matches everything ab matches, and {a}b matches {|a|}b but ab
     * does not.  Pre context is different (a{b} does not align with
     * ab).
     */

    /* LIMITATION of the current mask algorithm: Some rule
     * maskings are currently not detected.  For example,
     * "{Lu}]a>x" masks "A]a>y".  This can be added later. TODO
     */

    int32_t len = pattern.length();
    int32_t left = anteContextLength;
    int32_t left2 = r2.anteContextLength;
    int32_t right = len - left;
    int32_t right2 = r2.pattern.length() - left2;

    // TODO Clean this up -- some logic might be combinable with the
    // next statement.

    // Test for anchor masking
    if (left == left2 && right == right2 &&
        keyLength <= r2.keyLength &&
        0 == r2.pattern.compare(0, len, pattern)) {
        // The following boolean logic implements the table above
        return (flags == r2.flags) ||
            (!(flags & ANCHOR_START) && !(flags & ANCHOR_END)) ||
            ((r2.flags & ANCHOR_START) && (r2.flags & ANCHOR_END));
    }

    return left <= left2 &&
        (right < right2 ||
         (right == right2 && keyLength <= r2.keyLength)) &&
        0 == r2.pattern.compare(left2 - left, len, pattern);
}

/**
 * Attempt a match and replacement at the given position.  Return
 * the degree of match between this rule and the given text.  The
 * degree of match may be mismatch, a partial match, or a full
 * match.  A mismatch means at least one character of the text
 * does not match the context or key.  A partial match means some
 * context and key characters match, but the text is not long
 * enough to match all of them.  A full match means all context
 * and key characters match.
 * 
 * If a full match is obtained, perform a replacement, update pos,
 * and return U_MATCH.  Otherwise both text and pos are unchanged.
 * 
 * @param text the text
 * @param pos the position indices
 * @param incremental if TRUE, test for partial matches that may
 * be completed by additional text inserted at pos.limit.
 * @return one of <code>U_MISMATCH</code>,
 * <code>U_PARTIAL_MATCH</code>, or <code>U_MATCH</code>.  If
 * incremental is FALSE then U_PARTIAL_MATCH will not be returned.
 */
UMatchDegree TransliterationRule::matchAndReplace(Replaceable& text,
                                                  UTransPosition& pos,
                                                  UBool incremental) const {
    // Matching and replacing are done in one method because the
    // replacement operation needs information obtained during the
    // match.  Another way to do this is to have the match method
    // create a match result struct with relevant offsets, and to pass
    // this into the replace method.

    // ============================ MATCH ===========================

    // Record the positions of segments.  We assume the following:
    // - The maximum number of segments is 9.
    // - The segment indices occur in ascending order.  That is,
    //   segment 1 start <= segment 1 limit <= sement 2 start...
    // - The segments have been validated such that there are no
    //   references to nonexistent segments.
    // - The end of the segment array is marked by a start of -1.
    // Currently, the parser enforces all of these constraints.
    // In the future, the first two constraints may be lifted,
    // in which case this method will have to be modified.

    int32_t segPos[18];
    int32_t iSeg = firstKeySeg - 1;
    int32_t nextSegPos = (iSeg >= 0) ? segments[iSeg] : -1;

    // ------------------------ Ante Context ------------------------

    // A mismatch in the ante context, or with the start anchor,
    // is an outright U_MISMATCH regardless of whether we are
    // incremental or not.
    int32_t cursor = pos.start;
    int32_t newStart = 0;
    int32_t i;

    // Backup cursor by one
    if (cursor > 0) {
        cursor -= UTF_CHAR_LENGTH(text.char32At(cursor-1));
    } else {
        --cursor;
    }

    for (i=anteContextLength-1; i>=0; --i) {
        UChar keyChar = pattern.charAt(i);
        const UnicodeMatcher* matcher = data.lookup(keyChar);
        if (matcher == 0) {
            if (cursor >= pos.contextStart &&
                keyChar == text.charAt(cursor)) {
                --cursor;
            } else {
                return U_MISMATCH;
            }
        } else {
            // Subtract 1 from contextStart to make it a reverse limit
            if (matcher->matches(text, cursor, pos.contextStart-1, FALSE)
                != U_MATCH) {
                return U_MISMATCH;
            }
        }
        if (cursorPos == (i - anteContextLength)) {
            // Record the position of the cursor
            newStart = cursor;
        }
        while (nextSegPos == i) {
            segPos[iSeg] = cursor;
            if (cursor >= 0) {
                segPos[iSeg] += UTF_CHAR_LENGTH(text.char32At(cursor));
            } else {
                ++segPos[iSeg];
            }
            nextSegPos = (--iSeg >= 0) ? segments[iSeg] : -1;
        }
    }

    // ------------------------ Start Anchor ------------------------

    if ((flags & ANCHOR_START) && cursor != (pos.contextStart-1)) {
        return U_MISMATCH;
    }

    // -------------------- Key and Post Context --------------------

    // YUCKY OPTIMIZATION.  To make things a miniscule amount faster,
    // subtract anteContextLength from all segments[i] with i >=
    // firstKeySeg.  Then we don't have to do so here.  I only mention
    // this here in order to say DO NOT DO THIS.  The gain is
    // miniscule (how long does an integer subtraction take?) and the
    // increase in confusion isn't worth it.

    iSeg = firstKeySeg;
    nextSegPos = (iSeg >= 0) ? (segments[iSeg] - anteContextLength) : -1;

    i = 0;
    cursor = pos.start;
    int32_t keyLimit = 0;
    while (i < (pattern.length() - anteContextLength)) {
        if (incremental && cursor == pos.contextLimit) {
            // We've reached the context limit without a mismatch and
            // without completing our match.
            return U_PARTIAL_MATCH;
        }
        if (cursor == pos.limit && i < keyLength) {
            // We're still in the pattern key but we're entering the
            // post context.
            return U_MISMATCH;
        }
        while (i == nextSegPos) {
            segPos[iSeg] = cursor;
            nextSegPos = segments[++iSeg] - anteContextLength;
        }
        if (i == keyLength) {
            keyLimit = cursor;
        }
        UChar keyChar = pattern.charAt(anteContextLength + i++);
        const UnicodeMatcher* matcher = data.lookup(keyChar);
        if (matcher == 0) {
            // Don't need the cursor < pos.contextLimit check if
            // incremental is TRUE (because it's done above); do need
            // it otherwise.
            if (cursor < pos.contextLimit &&
                keyChar == text.charAt(cursor)) {
                ++cursor;
            } else {
                return U_MISMATCH;
            }
        } else {
            UMatchDegree m =
                matcher->matches(text, cursor, pos.contextLimit, incremental);
            if (m != U_MATCH) {
                return m;
            }
        }
    }
    while (i == nextSegPos) {
        segPos[iSeg] = cursor;
        nextSegPos = segments[++iSeg] - anteContextLength;
    }
	if (i == keyLength) {
		keyLimit = cursor;
	}

    // ------------------------- Stop Anchor ------------------------

    if ((flags & ANCHOR_END) != 0) {
        if (cursor != pos.contextLimit) {
            return U_MISMATCH;
        }
        if (incremental) {
            return U_PARTIAL_MATCH;
        }
    }

    // =========================== REPLACE ==========================

    // We have a full match.  The key is between pos.start and
    // keyLimit.  Segment indices have been recorded in segPos[].
    // Perform a replacement.

    int32_t lenDelta = 0;

    if (segments == NULL) {
        text.handleReplaceBetween(pos.start, keyLimit, output);
        lenDelta = output.length() - (keyLimit - pos.start);
        if (cursorPos >= 0) {
            newStart = pos.start + cursorPos;
        }
    } else {
        /* When there are segments to be copied, use the Replaceable.copy()
         * API in order to retain out-of-band data.  Copy everything to the
         * point after the key, then delete the key.  That is, copy things
         * into offset + keyLength, then replace offset .. offset +
         * keyLength with the empty string.
         *
         * Minimize the number of calls to Replaceable.replace() and
         * Replaceable.copy().
         */
        int32_t dest = keyLimit; // copy new text to here
        UnicodeString buf;
        for (i=0; i<output.length(); ) {
            if (i == cursorPos) {
                // Record the position of the cursor
                newStart = dest - (keyLimit - pos.start);
            }
            UChar32 c = output.char32At(i);
            int32_t b = data.lookupSegmentReference(c);
            if (b < 0) {
                // Accumulate straight (non-segment) text.
                buf.append(c);
            } else {
                // Insert any accumulated straight text.
                if (buf.length() > 0) {
                    text.handleReplaceBetween(dest, dest, buf);
                    dest += buf.length();
                    buf.remove();
                }
                // Copy segment with out-of-band data
                b *= 2;
                text.copy(segPos[b], segPos[b+1], dest);
                dest += segPos[b+1] - segPos[b];
            }
            i += UTF_CHAR_LENGTH(c);
        }
        // Insert any accumulated straight text.
        if (buf.length() > 0) {
            text.handleReplaceBetween(dest, dest, buf);
            dest += buf.length();
        }
        if (i == cursorPos) {
            // Record the position of the cursor
            newStart = dest - (keyLimit - pos.start);
        }
        // Delete the key
        buf.remove();
        text.handleReplaceBetween(pos.start, keyLimit, buf);
        lenDelta = dest - keyLimit - (keyLimit - pos.start);
    }
    
    pos.limit += lenDelta;
    pos.contextLimit += lenDelta;
    pos.start = newStart;
    
    return U_MATCH;
}

/**
 * Append a character to a rule that is being built up.
 * @param rule the string to append the character to
 * @param c the character to append
 * @param isLiteral if true, then the given character should not be
 * quoted or escaped.  Usually this means it is a syntactic element
 * such as > or $
 * @param escapeUnprintable if true, then unprintable characters
 * should be escaped using \uxxxx or \Uxxxxxxxx.  These escapes will
 * appear outside of quotes.
 * @param quoteBuf a buffer which is used to build up quoted
 * substrings.  The caller should initially supply an empty buffer,
 * and thereafter should not modify the buffer.  The buffer should be
 * cleared out by, at the end, calling this method with a literal
 * character.
 */
void TransliterationRule::_appendToRule(UnicodeString& rule,
                                        UChar32 c,
                                        UBool isLiteral,
                                        UBool escapeUnprintable,
                                        UnicodeString& quoteBuf) {
    // If we are escaping unprintables, then escape them outside
    // quotes.  \u and \U are not recognized within quotes.  The same
    // logic applies to literals, but literals are never escaped.
    if (isLiteral ||
        (escapeUnprintable && UnicodeSet::_isUnprintable(c))) {
        if (quoteBuf.length() > 0) {
            // We prefer backslash APOSTROPHE to double APOSTROPHE
            // (more readable, less similar to ") so if there are
            // double APOSTROPHEs at the ends, we pull them outside
            // of the quote.

            // If the first thing in the quoteBuf is APOSTROPHE
            // (doubled) then pull it out.
            while (quoteBuf.length() >= 2 &&
                   quoteBuf.charAt(0) == APOSTROPHE &&
                   quoteBuf.charAt(1) == APOSTROPHE) {
                rule.append(BACKSLASH).append(APOSTROPHE);
                quoteBuf.remove(0, 2);
            }
            // If the last thing in the quoteBuf is APOSTROPHE
            // (doubled) then remove and count it and add it after.
            int32_t trailingCount = 0;
            while (quoteBuf.length() >= 2 &&
                   quoteBuf.charAt(quoteBuf.length()-2) == APOSTROPHE &&
                   quoteBuf.charAt(quoteBuf.length()-1) == APOSTROPHE) {
                quoteBuf.truncate(quoteBuf.length()-2);
                ++trailingCount;
            }
            if (quoteBuf.length() > 0) {
                rule.append(APOSTROPHE);
                rule.append(quoteBuf);
                rule.append(APOSTROPHE);
                quoteBuf.truncate(0);
            }
            while (trailingCount-- > 0) {
                rule.append(BACKSLASH).append(APOSTROPHE);
            }
        }
        if (!escapeUnprintable || !UnicodeSet::_escapeUnprintable(rule, c)) {
            rule.append(c);
        }
    }

    // Escape ' and '\' and don't begin a quote just for them
    else if (quoteBuf.length() == 0 &&
             (c == APOSTROPHE || c == BACKSLASH)) {
        rule.append(BACKSLASH);
        rule.append(c);
    }

    // Specials (printable ascii that isn't [0-9a-zA-Z]) and
    // whitespace need quoting.  Also append stuff to quotes if we are
    // building up a quoted substring already.
    else if ((c >= 0x0021 && c <= 0x007E &&
              !((c >= 0x0030/*'0'*/ && c <= 0x0039/*'9'*/) ||
                (c >= 0x0041/*'A'*/ && c <= 0x005A/*'Z'*/) ||
                (c >= 0x0061/*'a'*/ && c <= 0x007A/*'z'*/))) ||
             Unicode::isWhitespace(c) ||
             quoteBuf.length() > 0) {
        quoteBuf.append(c);
        // Double ' within a quote
        if (c == APOSTROPHE) {
            quoteBuf.append(c);
        }
    }
    
    // Otherwise just append
    else {
        rule.append(c);
    }
}

void TransliterationRule::_appendToRule(UnicodeString& rule,
                                        const UnicodeString& text,
                                        UBool isLiteral,
                                        UBool escapeUnprintable,
                                        UnicodeString& quoteBuf) {
    for (int32_t i=0; i<text.length(); ++i) {
        _appendToRule(rule, text[i], isLiteral, escapeUnprintable, quoteBuf);
    }
}

/**
 * Create a source string that represents this rule.  Append it to the
 * given string.
 */
UnicodeString& TransliterationRule::toRule(UnicodeString& rule,
                                           UBool escapeUnprintable) const {
    int32_t i;

    int32_t iseg = 0;
    int32_t nextSeg = -1;
    if (segments != 0) {
        nextSeg = segments[iseg++];
    }

    // Accumulate special characters (and non-specials following them)
    // into quoteBuf.  Append quoteBuf, within single quotes, when
    // a non-quoted element must be inserted.
    UnicodeString str, quoteBuf;

    // Do not emit the braces '{' '}' around the pattern if there
    // is neither anteContext nor postContext.
    UBool emitBraces =
        (anteContextLength != 0) || (keyLength != pattern.length());

    // Emit the input pattern
    for (i=0; i<pattern.length(); ++i) {
        if (emitBraces && i == anteContextLength) {
            _appendToRule(rule, (UChar) 0x007B /*{*/, TRUE, escapeUnprintable, quoteBuf);
        }

        // Append either '(' or ')' if we are at a segment index
        if (i == nextSeg) {
            _appendToRule(rule, ((iseg % 2) == 0) ?
                             (UChar)0x0029 : (UChar)0x0028,
                             TRUE, escapeUnprintable, quoteBuf);
            nextSeg = segments[iseg++];
        }

        if (emitBraces && i == (anteContextLength + keyLength)) {
            _appendToRule(rule, (UChar) 0x007D /*}*/, TRUE, escapeUnprintable, quoteBuf);
        }

        UChar c = pattern.charAt(i);
        const UnicodeMatcher *matcher = data.lookup(c);
        if (matcher == 0) {
            _appendToRule(rule, c, FALSE, escapeUnprintable, quoteBuf);
        } else {
            _appendToRule(rule, matcher->toPattern(str, escapeUnprintable),
                          TRUE, escapeUnprintable, quoteBuf);
        }
    }

    if (i == nextSeg) {
        // assert((iseg % 2) == 0);
        _appendToRule(rule, (UChar)0x0029 /*)*/, TRUE, escapeUnprintable, quoteBuf);
    }

    if (emitBraces && i == (anteContextLength + keyLength)) {
        _appendToRule(rule, (UChar)0x007D /*}*/, TRUE, escapeUnprintable, quoteBuf);
    }

    _appendToRule(rule, UnicodeString(" > ", ""), TRUE, escapeUnprintable, quoteBuf);

    // Emit the output pattern

    // Handle a cursor preceding the output
    int32_t cursor = cursorPos;
    if (cursor < 0) {
        while (cursor++ < 0) {
            _appendToRule(rule, (UChar) 0x0040 /*@*/, TRUE, escapeUnprintable, quoteBuf);
        }
        // Fall through and append '|' below
    }

    for (i=0; i<output.length(); ++i) {
        if (i == cursor) {
            _appendToRule(rule, (UChar) 0x007C /*|*/, TRUE, escapeUnprintable, quoteBuf);
        }
        UChar c = output.charAt(i);
        int32_t seg = data.lookupSegmentReference(c);
        if (seg < 0) {
            _appendToRule(rule, c, FALSE, escapeUnprintable, quoteBuf);
        } else {
            UChar segRef[4] = {
                0x0020 /* */,
                0x0024 /*$*/,
                (UChar) (0x0031 + seg) /*0..9*/,
                0x0020 /* */
            };
            _appendToRule(rule, UnicodeString(FALSE, segRef, 4), TRUE, escapeUnprintable, quoteBuf);
        }
    }

    // Handle a cursor after the output.  Use > rather than >= because
    // if cursor == output.length() it is at the end of the output,
    // which is the default position, so we need not emit it.
    if (cursor > output.length()) {
        cursor -= output.length();
        while (cursor-- > 0) {
            _appendToRule(rule, (UChar) 0x0040 /*@*/, TRUE, escapeUnprintable, quoteBuf);
        }
        _appendToRule(rule, (UChar) 0x007C /*|*/, TRUE, escapeUnprintable, quoteBuf);
    }

    _appendToRule(rule, (UChar) 0x003B /*;*/, TRUE, escapeUnprintable, quoteBuf);

    return rule;
}

//eof
