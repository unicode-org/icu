/*
**********************************************************************
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#include "dcmpdata.h"

#include "compitr.h"

#include "unicode/normlzr.h"

/**
 * Construct a new <tt>ComposedCharIter</tt>.  The iterator will return
 * all Unicode characters with canonical decompositions, including Korean
 * Hangul characters.
 */
ComposedCharIter::ComposedCharIter()
  : minDecomp(DecompData::MAX_COMPAT), 
    hangul(FALSE),
    curChar(0),
    nextChar(ComposedCharIter::DONE)
{
}


  /**
   * Constructs a non-default <tt>ComposedCharIter</tt> with optional behavior.
   * <p>
   * @param compat    <tt>false</tt> for canonical decompositions only;
   *                  <tt>true</tt> for both canonical and compatibility
   *                  decompositions.
   *
   * @param options   Optional decomposition features.  Currently, the only
   *                  supported option is {@link Normalizer#IGNORE_HANGUL}, which
   *                  causes this <tt>ComposedCharIter</tt> not to iterate
   *                  over the Hangul characters and their corresponding
   *                  Jamo decompositions.
   */
ComposedCharIter::ComposedCharIter(UBool compat, 
                   int32_t options)
  : minDecomp(compat ? 0 : DecompData::MAX_COMPAT),
    hangul((options & Normalizer::IGNORE_HANGUL) == 0),
    curChar(0),
    nextChar(ComposedCharIter::DONE)
{
}

/**
 * Determines whether there any precomposed Unicode characters not yet returned
 * by {@link #next}.
 */
UBool ComposedCharIter::hasNext() const {
    if (nextChar == DONE)  {
        ((ComposedCharIter*)this)->findNextChar();
    }
    return nextChar != DONE;
}

/**
 * Returns the next precomposed Unicode character.
 * Repeated calls to <tt>next</tt> return all of the precomposed characters defined
 * by Unicode, in ascending order.  After all precomposed characters have
 * been returned, {@link #hasNext} will return <tt>false</tt> and further calls
 * to <tt>next</tt> will return {@link #DONE}.
 */
UChar ComposedCharIter::next()
{
    if (nextChar == DONE)  {
        findNextChar();
    }
    curChar = nextChar;
    nextChar = DONE;
    return curChar;
}

/**
 * Returns the Unicode decomposition of the current character.
 * This method returns the decomposition of the precomposed character most
 * recently returned by {@link #next}.  The resulting decomposition is
 * affected by the settings of the
 * {@link Normalizer#COMPATIBILITY COMPATIBILITY}
 * and {@link Normalizer#NO_HANGUL NO_HANGUL} options passed to the constructor.
 */
void ComposedCharIter::getDecomposition(UnicodeString& result) const
{
    // We duplicate most of the implementation of Normalizer::decompose() here
    // for efficiency.  One thing we don't duplicate is the recursive
    // decomposition code.  If we detect a need to do recursive decomposition
    // (which happens for only 16 characters in Unicode 3.0) then we delegate to
    // Normalizer::decompose().  This gives us optimal performance without
    // having a complete copy of Normalizer::decompose() here, with its extra
    // baggage of recursion buffers, etc. - Liu

    result.truncate(0);

    uint16_t offset = ucmp16_getu(DecompData::offsets, curChar);
    uint16_t index  = offset & DecompData::DECOMP_MASK;
    if (index > minDecomp) {
        if ((offset & DecompData::DECOMP_RECURSE) != 0) {
            // Let Normalizer::decompose() handle recursive decomp
            UnicodeString temp(curChar);
            UErrorCode status = U_ZERO_ERROR;
            Normalizer::decompose(temp, minDecomp > 0,
                                  hangul ? Normalizer::IGNORE_HANGUL : 0,
                                  result, status);
        } else {
            Normalizer::doAppend((const UChar*)DecompData::contents, index, result);
        }
    } 
    else if (hangul && curChar >= Normalizer::HANGUL_BASE && curChar < Normalizer::HANGUL_LIMIT) {
        Normalizer::hangulToJamo(curChar, result, (uint16_t)minDecomp);
    } 
    else {
        result += curChar;
    }
}

void ComposedCharIter::findNextChar()
{
    if (curChar != DONE) {
        UChar ch = curChar;
        while (++ch < 0xFFFF) {
            UChar offset = ucmp16_getu(DecompData::offsets, ch);
            if (offset > minDecomp
                || (hangul && ch >= Normalizer::HANGUL_BASE && ch < Normalizer::HANGUL_LIMIT) ) {
                nextChar = ch;
                break;
            }
        }
    }
}
