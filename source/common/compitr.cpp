/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   IBM Open Class Library                                                    *
*   (C) Copyright Taligent, Inc.,  1996                                       *
*   (C) Copyright International Business Machines Corporation,  1996-1998     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*/

#include "dcmpdata.h"

#include "compitr.h"

#include "normlzr.h"

/**
 * Constant that indicates the iteration has completed.
 * {@link #next} returns this value when there are no more composed characters
 * over which to iterate.
 */
const UChar ComposedCharIter::DONE = Normalizer::DONE;

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
ComposedCharIter::ComposedCharIter(bool_t compat, 
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
bool_t ComposedCharIter::hasNext() const {
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
    result.truncate(0);

    UChar pos = ucmp16_getu(DecompData::offsets, curChar);
    if (pos > minDecomp) {
        Normalizer::doAppend(DecompData::contents, pos, result);
    } 
    else if (hangul && curChar >= Normalizer::HANGUL_BASE && curChar < Normalizer::HANGUL_LIMIT) {
        Normalizer::hangulToJamo(curChar, result, minDecomp);
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
