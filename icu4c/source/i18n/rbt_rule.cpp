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

static const UChar APOSTROPHE = 0x0027; // '\''
static const UChar BACKSLASH  = 0x005C; // '\' 

// To process segments we need to allocate arrays of integers.  We use
// stack storage as long as the segment count is <= MAX_STATIC_SEGS.
// Otherwise, we allocate heap space.
#define MAX_STATIC_SEGS 20

// Macros for accessing the array of integers encoding the position of
// SEGMENTS_COUNT number of segments, n (half the number of parens)
// SEGMENTS_LEN   length of the segments array (number of elements)
// SEGMENTS_POS   position in 'pattern' of parenthesis i, where i=0..2n-1
// SEGMENTS_NUM   index into segments to access POS of $1.open,
//                $1.close, $2.open, $2.close,.., $n.open, $n.close
//                Relative to FIRST_SEG_POS_INDEX.  Ranges from 0..2n-1.
#define FIRST_SEG_POS_INDEX 2
#define SEGMENTS_COUNT(x) x[0]
#define SEGMENTS_LEN(x) (SEGMENTS_COUNT(x)*4+4)
#define SEGMENTS_POS(x,i) x[FIRST_SEG_POS_INDEX+i]
#define SEGMENTS_NUM(x,i) (x[x[1]+i]-FIRST_SEG_POS_INDEX)

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
                                         const TransliterationRuleData* theData,
                                         UErrorCode& status) :
    data(theData) {

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
        firstKeySeg = FIRST_SEG_POS_INDEX;
        while (segments[firstKeySeg] >= 0 &&
               segments[firstKeySeg] < anteContextLength) {
            ++firstKeySeg;
        }
        firstKeySeg -= FIRST_SEG_POS_INDEX; // make relative to FSPI
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

/**
 * Copy constructor.
 */

/* Ram: Reordered member initializers to match declaration order and make GCC happy */
TransliterationRule::TransliterationRule(TransliterationRule& other) :
    pattern(other.pattern),
    output(other.output),
    firstKeySeg(other.firstKeySeg),
    anteContextLength(other.anteContextLength),
    keyLength(other.keyLength),
    cursorPos(other.cursorPos),
    flags(other.flags),
    data(other.data) {

    segments = 0;
    if (other.segments != 0) {
        int32_t len = SEGMENTS_LEN(other.segments);
        segments = new int32_t[len];
        uprv_memcpy(segments, other.segments, len*sizeof(segments[0]));
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
    return (int16_t)(data->lookup(c) == NULL ? (c & 0xFF) : -1);
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
    const UnicodeMatcher* matcher = data->lookup(c);
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

inline int32_t posBefore(const Replaceable& str, int32_t pos) {
    return (pos > 0) ?
        pos - UTF_CHAR_LENGTH(str.char32At(pos-1)) :
        pos - 1;
}

inline int32_t posAfter(const Replaceable& str, int32_t pos) {
    return (pos >= 0 && pos < str.length()) ?
        pos + UTF_CHAR_LENGTH(str.char32At(pos)) :
        pos + 1;
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

    // Record the actual positions, in the text, of the segments.
	// These are recorded in the order that they occur in the pattern.

    // segPos[] is an array of 2*SEGMENTS_COUNT elements.  It
    // records the position in 'text' of each segment boundary, in
    // the order that they occur in 'pattern'.
    int32_t _segPos[2*MAX_STATIC_SEGS];
    int32_t *segPos = _segPos;
    if (segments != 0 && SEGMENTS_COUNT(segments) > MAX_STATIC_SEGS) {
        segPos = new int32_t[2*SEGMENTS_COUNT(segments)];
    }
    // iSeg is an index into segments[] that accesses the first
    // array.  As such it ranges from 0 to SEGMENTS_COUNT*2 - 1.
    // When indexing into segments[] FIRST_SEG_POS_INDEX must be
    // added to it: segments[FIRST_SEG_POS_INDEX + iSeg].
    int32_t iSeg = firstKeySeg - 1;
    // nextSegPos is an offset in 'pattern'.  When the cursor is
    // equal to nextSegPos, we are at a segment boundary, and we
    // record the position in the real text in segPos[].
    int32_t nextSegPos = (iSeg >= 0) ? segments[FIRST_SEG_POS_INDEX+iSeg] : -1;

    UMatchDegree m;
    int32_t lenDelta, keyLimit;

    // ------------------------ Ante Context ------------------------

    // A mismatch in the ante context, or with the start anchor,
    // is an outright U_MISMATCH regardless of whether we are
    // incremental or not.
    int32_t oText; // offset into 'text'
    int32_t newStart = 0;
    int32_t minOText;
    int32_t oPattern; // offset into 'pattern'

    // Backup oText by one
    oText = posBefore(text, pos.start);

    for (oPattern=anteContextLength-1; oPattern>=0; --oPattern) {
        UChar keyChar = pattern.charAt(oPattern);
        const UnicodeMatcher* matcher = data->lookup(keyChar);
        if (matcher == 0) {
            if (oText >= pos.contextStart &&
                keyChar == text.charAt(oText)) {
                --oText;
            } else {
                m = U_MISMATCH;
                goto exit;
            }
        } else {
            // Subtract 1 from contextStart to make it a reverse limit
            if (matcher->matches(text, oText, pos.contextStart-1, FALSE)
                != U_MATCH) {
                m = U_MISMATCH;
                goto exit;
            }
        }
        while (nextSegPos == oPattern) {
            segPos[iSeg] = oText;
            if (oText >= 0) {
                segPos[iSeg] += UTF_CHAR_LENGTH(text.char32At(oText));
            } else {
                ++segPos[iSeg];
            }
            nextSegPos = (--iSeg >= FIRST_SEG_POS_INDEX) ? segments[FIRST_SEG_POS_INDEX+iSeg] : -1;
        }
    }

    minOText = posAfter(text, oText);

    // ------------------------ Start Anchor ------------------------

    if ((flags & ANCHOR_START) && oText != posBefore(text, pos.contextStart)) {
        m = U_MISMATCH;
        goto exit;
    }

    // -------------------- Key and Post Context --------------------

    iSeg = firstKeySeg;
    nextSegPos = (iSeg >= 0) ? (segments[FIRST_SEG_POS_INDEX+iSeg] - anteContextLength) : -1;

    oPattern = 0;
    oText = pos.start;
    keyLimit = 0;
    while (oPattern < (pattern.length() - anteContextLength)) {
        if (incremental && oText == pos.contextLimit) {
            // We've reached the context limit without a mismatch and
            // without completing our match.
            m = U_PARTIAL_MATCH;
            goto exit;
        }
        if (oText == pos.limit && oPattern < keyLength) {
            // We're still in the pattern key but we're entering the
            // post context.
            m = U_MISMATCH;
            goto exit;
        }
        while (oPattern == nextSegPos) {
            segPos[iSeg] = oText;
            nextSegPos = segments[FIRST_SEG_POS_INDEX+(++iSeg)] - anteContextLength;
        }
        if (oPattern == keyLength) {
            keyLimit = oText;
        }
        UChar keyChar = pattern.charAt(anteContextLength + oPattern++);
        const UnicodeMatcher* matcher = data->lookup(keyChar);
        if (matcher == 0) {
            // Don't need the oText < pos.contextLimit check if
            // incremental is TRUE (because it's done above); do need
            // it otherwise.
            if (oText < pos.contextLimit &&
                keyChar == text.charAt(oText)) {
                ++oText;
            } else {
                m = U_MISMATCH;
                goto exit;
            }
        } else {
            m = matcher->matches(text, oText, pos.contextLimit, incremental);
            if (m != U_MATCH) {
                goto exit;
            }
        }
    }
    while (oPattern == nextSegPos) {
        segPos[iSeg] = oText;
        nextSegPos = segments[FIRST_SEG_POS_INDEX+(++iSeg)] - anteContextLength;
    }
	if (oPattern == keyLength) {
		keyLimit = oText;
	}

    // ------------------------- Stop Anchor ------------------------

    if ((flags & ANCHOR_END) != 0) {
        if (oText != pos.contextLimit) {
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

    if (segments == NULL) {
        text.handleReplaceBetween(pos.start, keyLimit, output);
        lenDelta = output.length() - (keyLimit - pos.start);
        if (cursorPos >= 0 && cursorPos < keyLength) {
            // Within the key, the cursor refers to 16-bit code units
            newStart = pos.start + cursorPos;
        } else {
            newStart = pos.start;
            int32_t n = cursorPos;
            // Outside the key, cursorPos counts code points
            while (n > 0) {
                newStart += UTF_CHAR_LENGTH(text.char32At(newStart));
                --n;
            }
            while (n < 0) {
                newStart -= UTF_CHAR_LENGTH(text.char32At(newStart-1));
                ++n;
            }
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
        int oOutput; // offset into 'output'
        for (oOutput=0; oOutput<output.length(); ) {
            if (oOutput == cursorPos) {
                // Record the position of the cursor
                newStart = dest - (keyLimit - pos.start);
            }
            UChar32 c = output.char32At(oOutput);
            int32_t b = data->lookupSegmentReference(c);
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
                int32_t start = segPos[SEGMENTS_NUM(segments,b)];
                int32_t limit = segPos[SEGMENTS_NUM(segments,b+1)];
                text.copy(start, limit, dest);
                dest += limit - start;
            }
            oOutput += UTF_CHAR_LENGTH(c);
        }
        // Insert any accumulated straight text.
        if (buf.length() > 0) {
            text.handleReplaceBetween(dest, dest, buf);
            dest += buf.length();
        }
        if (oOutput == cursorPos) {
            // Record the position of the cursor
            newStart = dest - (keyLimit - pos.start);
        }
        // Delete the key
        buf.remove();
        text.handleReplaceBetween(pos.start, keyLimit, buf);
        lenDelta = dest - keyLimit - (keyLimit - pos.start);
        // Handle cursor in postContext
        if (cursorPos > output.length()) {
            newStart = pos.start + (dest - keyLimit);
            int32_t n = cursorPos - output.length();
            // cursorPos counts code points
            while (n > 0) {
                newStart += UTF_CHAR_LENGTH(text.char32At(newStart));
                n--;
            }
        }
    }
    
    oText += lenDelta;
    pos.limit += lenDelta;
    pos.contextLimit += lenDelta;
    // Restrict new value of start to [minOText, min(oText, pos.limit)].
    pos.start = uprv_max(minOText, uprv_min(uprv_min(oText, pos.limit), newStart));
    m = U_MATCH;
    
  exit:
    if (segPos != _segPos) {
        delete[] segPos;
    }
    return m;
}

/**
 * Append a character to a rule that is being built up.  To flush
 * the quoteBuf to rule, make one final call with isLiteral == TRUE.
 * If there is no final character, pass in (UChar32)-1 as c.
 * @param rule the string to append the character to
 * @param c the character to append, or (UChar32)-1 if none.
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
void TransliterationRule::appendToRule(UnicodeString& rule,
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
        if (c != (UChar32)-1) {
            if (!escapeUnprintable || !UnicodeSet::_escapeUnprintable(rule, c)) {
                rule.append(c);
            }
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
    else if (quoteBuf.length() > 0 ||
             (c >= 0x0021 && c <= 0x007E &&
              !((c >= 0x0030/*'0'*/ && c <= 0x0039/*'9'*/) ||
                (c >= 0x0041/*'A'*/ && c <= 0x005A/*'Z'*/) ||
                (c >= 0x0061/*'a'*/ && c <= 0x007A/*'z'*/))) ||
             Unicode::isWhitespace(c)) {
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

void TransliterationRule::appendToRule(UnicodeString& rule,
                                        const UnicodeString& text,
                                        UBool isLiteral,
                                        UBool escapeUnprintable,
                                        UnicodeString& quoteBuf) {
    for (int32_t i=0; i<text.length(); ++i) {
        appendToRule(rule, text[i], isLiteral, escapeUnprintable, quoteBuf);
    }
}

static const int32_t POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000,
                                10000000, 100000000, 1000000000};

/**
 * Create a source string that represents this rule.  Append it to the
 * given string.
 */
UnicodeString& TransliterationRule::toRule(UnicodeString& rule,
                                           UBool escapeUnprintable) const {
    int32_t i;

    // iseg indexes into segments[] directly (not offset from FSPI)
    int32_t iseg = FIRST_SEG_POS_INDEX-1;
    int32_t nextSeg = -1;
    // Build an array of booleans specifying open vs. close paren
    UBool _isOpen[2*MAX_STATIC_SEGS];
    UBool *isOpen = _isOpen;
    if (segments != 0) {
        if (SEGMENTS_COUNT(segments) > MAX_STATIC_SEGS) {
            isOpen = new UBool[2*SEGMENTS_COUNT(segments)];
        }
        for (i=0; i<2*SEGMENTS_COUNT(segments); i+=2) {
            isOpen[SEGMENTS_NUM(segments,i)  ] = TRUE;
            isOpen[SEGMENTS_NUM(segments,i+1)] = FALSE;
        }
        nextSeg = segments[++iseg];
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
            appendToRule(rule, (UChar) 0x007B /*{*/, TRUE, escapeUnprintable, quoteBuf);
        }

        // Append either '(' or ')' if we are at a segment index
        if (i == nextSeg) {
            appendToRule(rule, isOpen[iseg-FIRST_SEG_POS_INDEX] ?
                             (UChar)0x0028 : (UChar)0x0029,
                             TRUE, escapeUnprintable, quoteBuf);
            nextSeg = segments[++iseg];
        }

        if (emitBraces && i == (anteContextLength + keyLength)) {
            appendToRule(rule, (UChar) 0x007D /*}*/, TRUE, escapeUnprintable, quoteBuf);
        }

        UChar c = pattern.charAt(i);
        const UnicodeMatcher *matcher = data->lookup(c);
        if (matcher == 0) {
            appendToRule(rule, c, FALSE, escapeUnprintable, quoteBuf);
        } else {
            appendToRule(rule, matcher->toPattern(str, escapeUnprintable),
                          TRUE, escapeUnprintable, quoteBuf);
        }
    }

    if (i == nextSeg) {
        // assert(!isOpen[iSeg-FIRST_SEG_POS_INDEX]);
        appendToRule(rule, (UChar)0x0029 /*)*/, TRUE, escapeUnprintable, quoteBuf);
    }

    if (emitBraces && i == (anteContextLength + keyLength)) {
        appendToRule(rule, (UChar)0x007D /*}*/, TRUE, escapeUnprintable, quoteBuf);
    }

    appendToRule(rule, UnicodeString(" > ", ""), TRUE, escapeUnprintable, quoteBuf);

    // Emit the output pattern

    // Handle a cursor preceding the output
    int32_t cursor = cursorPos;
    if (cursor < 0) {
        while (cursor++ < 0) {
            appendToRule(rule, (UChar) 0x0040 /*@*/, TRUE, escapeUnprintable, quoteBuf);
        }
        // Fall through and append '|' below
    }

    for (i=0; i<output.length(); ++i) {
        if (i == cursor) {
            appendToRule(rule, (UChar) 0x007C /*|*/, TRUE, escapeUnprintable, quoteBuf);
        }
        UChar c = output.charAt(i);
        int32_t seg = data->lookupSegmentReference(c);
        if (seg < 0) {
            appendToRule(rule, c, FALSE, escapeUnprintable, quoteBuf);
        } else {
            ++seg; // make 1-based
            appendToRule(rule, (UChar)0x20, TRUE, escapeUnprintable, quoteBuf);
            rule.append((UChar)0x24 /*$*/);
            UBool show = FALSE; // TRUE if we should display digits
            for (int32_t p=9; p>=0; --p) {
                int32_t d = seg / POW10[p];
                seg -= d * POW10[p];
                if (d != 0 || p == 0) {
                    show = TRUE;
                }
                if (show) {
                    rule.append((UChar)(48+d));
                }
            }            
            rule.append((UChar)0x20);
        }
    }

    // Handle a cursor after the output.  Use > rather than >= because
    // if cursor == output.length() it is at the end of the output,
    // which is the default position, so we need not emit it.
    if (cursor > output.length()) {
        cursor -= output.length();
        while (cursor-- > 0) {
            appendToRule(rule, (UChar) 0x0040 /*@*/, TRUE, escapeUnprintable, quoteBuf);
        }
        appendToRule(rule, (UChar) 0x007C /*|*/, TRUE, escapeUnprintable, quoteBuf);
    }

    appendToRule(rule, (UChar) 0x003B /*;*/, TRUE, escapeUnprintable, quoteBuf);

    if (isOpen != _isOpen) {
        delete[] isOpen;
    }
    return rule;
}

//eof
