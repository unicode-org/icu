/*
**********************************************************************
*   Copyright (c) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/19/2001  aliu        Creation.
**********************************************************************
*/

#include "util.h"
#include "unicode/uchar.h"
#include "unicode/unimatch.h"
#include "uprops.h"

// Define UChar constants using hex for EBCDIC compatibility

static const UChar BACKSLASH  = 0x005C; /*\*/
static const UChar UPPER_U    = 0x0055; /*U*/
static const UChar LOWER_U    = 0x0075; /*u*/
static const UChar APOSTROPHE = 0x0027; // '\''
static const UChar SPACE      = 0x0020; // ' '

// "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
static const UChar DIGITS[] = {
    48,49,50,51,52,53,54,55,56,57,
    65,66,67,68,69,70,71,72,73,74,
    75,76,77,78,79,80,81,82,83,84,
    85,86,87,88,89,90
};

UnicodeString& ICU_Utility::appendNumber(UnicodeString& result, int32_t n,
                                     int32_t radix, int32_t minDigits) {
    if (radix < 2 || radix > 36) {
        // Bogus radix
        return result.append((UChar)63/*?*/);
    }
    // Handle negatives
    if (n < 0) {
        n = -n;
        result.append((UChar)45/*-*/);
    }
    // First determine the number of digits
    int32_t nn = n;
    int32_t r = 1;
    while (nn >= radix) {
        nn /= radix;
        r *= radix;
        --minDigits;
    }
    // Now generate the digits
    while (--minDigits > 0) {
        result.append(DIGITS[0]);
    }
    while (r > 0) {
        int32_t digit = n / r;
        result.append(DIGITS[digit]);
        n -= digit * r;
        r /= radix;
    }
    return result;
}

static const UChar HEX[16] = {48,49,50,51,52,53,54,55,  // 0-7
                              56,57,65,66,67,68,69,70}; // 8-9 A-F

/**
 * Return true if the character is NOT printable ASCII.
 */
UBool ICU_Utility::isUnprintable(UChar32 c) {
    return !(c >= 0x20 && c <= 0x7E);
}

/**
 * Escape unprintable characters using \uxxxx notation for U+0000 to
 * U+FFFF and \Uxxxxxxxx for U+10000 and above.  If the character is
 * printable ASCII, then do nothing and return FALSE.  Otherwise,
 * append the escaped notation and return TRUE.
 */
UBool ICU_Utility::escapeUnprintable(UnicodeString& result, UChar32 c) {
    if (isUnprintable(c)) {
        result.append(BACKSLASH);
        if (c & ~0xFFFF) {
            result.append(UPPER_U);
            result.append(HEX[0xF&(c>>28)]);
            result.append(HEX[0xF&(c>>24)]);
            result.append(HEX[0xF&(c>>20)]);
            result.append(HEX[0xF&(c>>16)]);
        } else {
            result.append(LOWER_U);
        }
        result.append(HEX[0xF&(c>>12)]);
        result.append(HEX[0xF&(c>>8)]);
        result.append(HEX[0xF&(c>>4)]);
        result.append(HEX[0xF&c]);
        return TRUE;
    }
    return FALSE;
}

/**
 * Returns the index of a character, ignoring quoted text.
 * For example, in the string "abc'hide'h", the 'h' in "hide" will not be
 * found by a search for 'h'.
 */
int32_t ICU_Utility::quotedIndexOf(const UnicodeString& text,
                               int32_t start, int32_t limit,
                               UChar charToFind) {
    for (int32_t i=start; i<limit; ++i) {
        UChar c = text.charAt(i);
        if (c == BACKSLASH) {
            ++i;
        } else if (c == APOSTROPHE) {
            while (++i < limit
                   && text.charAt(i) != APOSTROPHE) {}
        } else if (c == charToFind) {
            return i;
        }
    }
    return -1;
}

/**
 * Skip over a sequence of zero or more white space characters at pos.
 * @param advance if true, advance pos to the first non-white-space
 * character at or after pos, or str.length(), if there is none.
 * Otherwise leave pos unchanged.
 * @return the index of the first non-white-space character at or
 * after pos, or str.length(), if there is none.
 */
int32_t ICU_Utility::skipWhitespace(const UnicodeString& str, int32_t& pos,
                                    UBool advance) {
    int32_t p = pos;
    while (p < str.length()) {
        UChar32 c = str.char32At(p);
        if (!uprv_isRuleWhiteSpace(c)) {
            break;
        }
        p += UTF_CHAR_LENGTH(c);
    }
    if (advance) {
        pos = p;
    }
    return p;
}

/**
 * Skip over whitespace in a Replaceable.  Whitespace is defined by
 * uprv_isRuleWhiteSpace().  Skipping may be done in the forward or
 * reverse direction.  In either case, the leftmost index will be
 * inclusive, and the rightmost index will be exclusive.  That is,
 * given a range defined as [start, limit), the call
 * skipWhitespace(text, start, limit) will advance start past leading
 * whitespace, whereas the call skipWhitespace(text, limit, start),
 * will back up limit past trailing whitespace.
 * @param text the text to be analyzed
 * @param pos either the start or limit of a range of 'text', to skip
 * leading or trailing whitespace, respectively
 * @param stop either the limit or start of a range of 'text', to skip
 * leading or trailing whitespace, respectively
 * @return the new start or limit, depending on what was passed in to
 * 'pos'
 */
//?FOR FUTURE USE.  DISABLE FOR NOW for coverage reasons.
//?int32_t ICU_Utility::skipWhitespace(const Replaceable& text,
//?                                    int32_t pos, int32_t stop) {
//?    UChar32 c;
//?    UBool isForward = (stop >= pos);
//?
//?    if (!isForward) {
//?        --pos; // pos is a limit, so back up by one
//?    }
//?    
//?    while (pos != stop &&
//?           uprv_isRuleWhiteSpace(c = text.char32At(pos))) {
//?        if (isForward) {
//?            pos += UTF_CHAR_LENGTH(c);
//?        } else {
//?            pos -= UTF_CHAR_LENGTH(c);
//?        }
//?    }
//?
//?    if (!isForward) {
//?        ++pos; // make pos back into a limit
//?    }
//?
//?    return pos;
//?}

/**
 * Parse a single non-whitespace character 'ch', optionally
 * preceded by whitespace.
 * @param id the string to be parsed
 * @param pos INPUT-OUTPUT parameter.  On input, pos[0] is the
 * offset of the first character to be parsed.  On output, pos[0]
 * is the index after the last parsed character.  If the parse
 * fails, pos[0] will be unchanged.
 * @param ch the non-whitespace character to be parsed.
 * @return true if 'ch' is seen preceded by zero or more
 * whitespace characters.
 */
UBool ICU_Utility::parseChar(const UnicodeString& id, int32_t& pos, UChar ch) {
    int32_t start = pos;
    skipWhitespace(id, pos, TRUE);
    if (pos == id.length() ||
        id.charAt(pos) != ch) {
        pos = start;
        return FALSE;
    }
    ++pos;
    return TRUE;
}

/**
 * Parse a pattern string starting at offset pos.  Keywords are
 * matched case-insensitively.  Spaces may be skipped and may be
 * optional or required.  Integer values may be parsed, and if
 * they are, they will be returned in the given array.  If
 * successful, the offset of the next non-space character is
 * returned.  On failure, -1 is returned.
 * @param pattern must only contain lowercase characters, which
 * will match their uppercase equivalents as well.  A space
 * character matches one or more required spaces.  A '~' character
 * matches zero or more optional spaces.  A '#' character matches
 * an integer and stores it in parsedInts, which the caller must
 * ensure has enough capacity.
 * @param parsedInts array to receive parsed integers.  Caller
 * must ensure that parsedInts.length is >= the number of '#'
 * signs in 'pattern'.
 * @return the position after the last character parsed, or -1 if
 * the parse failed
 */
int32_t ICU_Utility::parsePattern(const UnicodeString& rule, int32_t pos, int32_t limit,
                              const UnicodeString& pattern, int32_t* parsedInts) {
    // TODO Update this to handle surrogates
    int32_t p;
    int32_t intCount = 0; // number of integers parsed
    for (int32_t i=0; i<pattern.length(); ++i) {
        UChar cpat = pattern.charAt(i);
        UChar c;
        switch (cpat) {
        case 32 /*' '*/:
            if (pos >= limit) {
                return -1;
            }
            c = rule.charAt(pos++);
            if (!uprv_isRuleWhiteSpace(c)) {
                return -1;
            }
            // FALL THROUGH to skipWhitespace
        case 126 /*'~'*/:
            pos = skipWhitespace(rule, pos);
            break;
        case 35 /*'#'*/:
            p = pos;
            parsedInts[intCount++] = parseInteger(rule, p, limit);
            if (p == pos) {
                // Syntax error; failed to parse integer
                return -1;
            }
            pos = p;
            break;
        default:
            if (pos >= limit) {
                return -1;
            }
            c = (UChar) u_tolower(rule.charAt(pos++));
            if (c != cpat) {
                return -1;
            }
            break;
        }
    }
    return pos;
}

/**
 * Parse a pattern string within the given Replaceable and a parsing
 * pattern.  Characters are matched literally and case-sensitively
 * except for the following special characters:
 *
 * ~  zero or more uprv_isRuleWhiteSpace chars
 *
 * If end of pattern is reached with all matches along the way,
 * pos is advanced to the first unparsed index and returned.
 * Otherwise -1 is returned.
 * @param pat pattern that controls parsing
 * @param text text to be parsed, starting at index
 * @param index offset to first character to parse
 * @param limit offset after last character to parse
 * @return index after last parsed character, or -1 on parse failure.
 */
int32_t ICU_Utility::parsePattern(const UnicodeString& pat,
                                  const Replaceable& text,
                                  int32_t index,
                                  int32_t limit) {
    int32_t ipat = 0;

    // empty pattern matches immediately
    if (ipat == pat.length()) {
        return index;
    }

    UChar32 cpat = pat.char32At(ipat);

    while (index < limit) {
        UChar32 c = text.char32At(index);

        // parse \s*
        if (cpat == 126 /*~*/) {
            if (uprv_isRuleWhiteSpace(c)) {
                index += UTF_CHAR_LENGTH(c);
                continue;
            } else {
                if (++ipat == pat.length()) {
                    return index; // success; c unparsed
                }
                // fall thru; process c again with next cpat
            }
        }

        // parse literal
        else if (c == cpat) {
            index += UTF_CHAR_LENGTH(c);
            ipat += UTF_CHAR_LENGTH(cpat);
            if (ipat == pat.length()) {
                return index; // success; c parsed
            }
            // fall thru; get next cpat
        }

        // match failure of literal
        else {
            return -1;
        }

        cpat = pat.char32At(ipat);
    }

    return -1; // text ended before end of pat
}

static const UChar ZERO_X[] = {48, 120, 0}; // "0x"

/**
 * Parse an integer at pos, either of the form \d+ or of the form
 * 0x[0-9A-Fa-f]+ or 0[0-7]+, that is, in standard decimal, hex,
 * or octal format.
 * @param pos INPUT-OUTPUT parameter.  On input, the first
 * character to parse.  On output, the character after the last
 * parsed character.
 */
int32_t ICU_Utility::parseInteger(const UnicodeString& rule, int32_t& pos, int32_t limit) {
    int32_t count = 0;
    int32_t value = 0;
    int32_t p = pos;
    int8_t radix = 10;

    if (0 == rule.caseCompare(p, 2, ZERO_X, U_FOLD_CASE_DEFAULT)) {
        p += 2;
        radix = 16;
    } else if (p < limit && rule.charAt(p) == 48 /*0*/) {
        p++;
        count = 1;
        radix = 8;
    }

    while (p < limit) {
        int32_t d = u_digit(rule.charAt(p++), radix);
        if (d < 0) {
            --p;
            break;
        }
        ++count;
        int32_t v = (value * radix) + d;
        if (v <= value) {
            // If there are too many input digits, at some point
            // the value will go negative, e.g., if we have seen
            // "0x8000000" already and there is another '0', when
            // we parse the next 0 the value will go negative.
            return 0;
        }
        value = v;
    }
    if (count > 0) {
        pos = p;
    }
    return value;
}

/**
 * Parse a Unicode identifier from the given string at the given
 * position.  Return the identifier, or an empty string if there
 * is no identifier.
 * @param str the string to parse
 * @param pos INPUT-OUPUT parameter.  On INPUT, pos is the
 * first character to examine.  It must be less than str.length(),
 * and it must not point to a whitespace character.  That is, must
 * have pos < str.length() and
 * !uprv_isRuleWhiteSpace(str.char32At(pos)).  On
 * OUTPUT, the position after the last parsed character.
 * @return the Unicode identifier, or an empty string if there is
 * no valid identifier at pos.
 */
UnicodeString ICU_Utility::parseUnicodeIdentifier(const UnicodeString& str, int32_t& pos) {
    // assert(pos < str.length());
    // assert(!uprv_isRuleWhiteSpace(str.char32At(pos)));
    UnicodeString buf;
    int p = pos;
    while (p < str.length()) {
        UChar32 ch = str.char32At(p);
        if (buf.length() == 0) {
            if (u_isIDStart(ch)) {
                buf.append(ch);
            } else {
                buf.truncate(0);
                return buf;
            }
        } else {
            if (u_isIDPart(ch)) {
                buf.append(ch);
            } else {
                break;
            }
        }
        p += UTF_CHAR_LENGTH(ch);
    }
    pos = p;
    return buf;
}

/**
 * Parse an unsigned 31-bit integer at the given offset.  Use
 * UCharacter.digit() to parse individual characters into digits.
 * @param text the text to be parsed
 * @param pos INPUT-OUTPUT parameter.  On entry, pos[0] is the
 * offset within text at which to start parsing; it should point
 * to a valid digit.  On exit, pos[0] is the offset after the last
 * parsed character.  If the parse failed, it will be unchanged on
 * exit.  Must be >= 0 on entry.
 * @param radix the radix in which to parse; must be >= 2 and <=
 * 36.
 * @return a non-negative parsed number, or -1 upon parse failure.
 * Parse fails if there are no digits, that is, if pos[0] does not
 * point to a valid digit on entry, or if the number to be parsed
 * does not fit into a 31-bit unsigned integer.
 */
int32_t ICU_Utility::parseNumber(const UnicodeString& text,
                                 int32_t& pos, int8_t radix) {
    // assert(pos[0] >= 0);
    // assert(radix >= 2);
    // assert(radix <= 36);
    int32_t n = 0;
    int32_t p = pos;
    while (p < text.length()) {
        UChar32 ch = text.char32At(p);
        int32_t d = u_digit(ch, radix);
        if (d < 0) {
            break;
        }
        n = radix*n + d;
        // ASSUME that when a 32-bit integer overflows it becomes
        // negative.  E.g., 214748364 * 10 + 8 => negative value.
        if (n < 0) {
            return -1;
        }
        ++p;
    }
    if (p == pos) {
        return -1;
    }
    pos = p;
    return n;
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
void ICU_Utility::appendToRule(UnicodeString& rule,
                               UChar32 c,
                               UBool isLiteral,
                               UBool escapeUnprintable,
                               UnicodeString& quoteBuf) {
    // If we are escaping unprintables, then escape them outside
    // quotes.  \u and \U are not recognized within quotes.  The same
    // logic applies to literals, but literals are never escaped.
    if (isLiteral ||
        (escapeUnprintable && ICU_Utility::isUnprintable(c))) {
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
            /* Since spaces are ignored during parsing, they are
             * emitted only for readability.  We emit one here
             * only if there isn't already one at the end of the
             * rule.
             */
            if (c == SPACE) {
                int32_t len = rule.length();
                if (len > 0 && rule.charAt(len-1) != c) {
                    rule.append(c);
                }
            } else if (!escapeUnprintable || !ICU_Utility::escapeUnprintable(rule, c)) {
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
             uprv_isRuleWhiteSpace(c)) {
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

void ICU_Utility::appendToRule(UnicodeString& rule,
                               const UnicodeString& text,
                               UBool isLiteral,
                               UBool escapeUnprintable,
                               UnicodeString& quoteBuf) {
    for (int32_t i=0; i<text.length(); ++i) {
        appendToRule(rule, text[i], isLiteral, escapeUnprintable, quoteBuf);
    }
}

/**
 * Given a matcher reference, which may be null, append its
 * pattern as a literal to the given rule.
 */
void ICU_Utility::appendToRule(UnicodeString& rule,
                               const UnicodeMatcher* matcher,
                               UBool escapeUnprintable,
                               UnicodeString& quoteBuf) {
    if (matcher != NULL) {
        UnicodeString pat;
        appendToRule(rule, matcher->toPattern(pat, escapeUnprintable),
                     TRUE, escapeUnprintable, quoteBuf);
    }
}

//eof
