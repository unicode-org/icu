//
//  file:  rematch.cpp    
//
/*
**********************************************************************
*   Copyright (C) 2002 International Business Machines Corporation   *
*   and others. All rights reserved.                                 *
**********************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/regex.h"
#include "unicode/uniset.h"
#include "unicode/uchar.h"
#include "uassert.h"
#include "uvector.h"
#include "regeximp.h"

#include "stdio.h"

U_NAMESPACE_BEGIN

//-----------------------------------------------------------------------------
//
//   Constructor and Destructor
//
//-----------------------------------------------------------------------------
RegexMatcher::RegexMatcher(const RegexPattern *pat)  { 
    fPattern           = pat;
    fInput             = NULL;
    fInputLength       = 0;
    UErrorCode  status = U_ZERO_ERROR;
    fBackTrackStack    = new UStack(status);   // TODO:  do something with status.
    fCaptureStarts     = new UVector(status);
    fCaptureEnds       = new UVector(status);
    int i;
    for (i=0; i<=fPattern->fNumCaptureGroups; i++) {
        fCaptureStarts->addElement(-1, status);
        fCaptureEnds  ->addElement(-1, status);
    }
    reset();
}


RegexMatcher::RegexMatcher(const RegexMatcher &other) {
    U_ASSERT(FALSE);
}


RegexMatcher::~RegexMatcher() {
    delete fBackTrackStack;
    delete fCaptureStarts;
    delete fCaptureEnds;
}



static const UChar BACKSLASH  = 0x5c;
static const UChar DOLLARSIGN = 0x24;
//--------------------------------------------------------------------------------
//
//    appendReplacement
//
//--------------------------------------------------------------------------------
RegexMatcher &RegexMatcher::appendReplacement(UnicodeString &dest,
                                              const UnicodeString &replacement,
                                              UErrorCode &status) {
    if (U_FAILURE(status)) {
        return *this;
    }
    if (fMatch == FALSE) {
        status = U_REGEX_INVALID_STATE;
        return *this;
    }

    // Copy input string from the end of previous match to start of current match
    int32_t  len = fMatchStart-fLastMatchEnd;
    if (len > 0) {
        dest.append(*fInput, fLastMatchEnd, len);
    }
    

    // scan the replacement text, looking for substitutions ($n) and \escapes.
    int32_t  replLen = replacement.length();
    int32_t  replIdx;
    for (replIdx = 0; replIdx<replLen; replIdx++) {
        UChar  c = replacement.charAt(replIdx);
        if (c == BACKSLASH) {
            // Backslash Escape.  Copy the following char out without further checks.
            replIdx++;
            if (replIdx >= replLen) {
                break;
            }
            c = replacement.charAt(replIdx);
            dest.append(c);
            continue;
        }

        if (c != DOLLARSIGN) {
            // Normal char, not a $.  Copy it out without further checks.
            dest.append(c);
            continue;
        }

        // We've got a $.  Pick up a capture group number if one follows.
        // Consume at most the number of digits necessary for the largest capture
        // number that is valid for this pattern.
        if (++replIdx >= replLen) {
            // $ was at the end of the replacement string.  Dump it out and be done.
            dest.append(c);
            break;
        }

        int32_t numDigits = 0;
        int32_t groupNum  = 0;
        for (;;) {
            c = replacement.charAt(replIdx);
            if (u_isdigit(c) == FALSE) {
                break;
            }
            groupNum=groupNum*10 + u_charDigitValue(c);
            numDigits++;
            if (++replIdx >= replLen) {
                break;
            }
            if (numDigits >= fPattern->fMaxCaptureDigits) {
                break;
            }
        }

        // We've scanned one char ahead in the pattern.  Back up so the
        //  next iteration of the loop picks the char again.
        --replIdx;

        if (numDigits == 0) {
            // The $ didn't introduce a group number at all.
            // Treat it as just part of the substitution text.
            dest.append(DOLLARSIGN);
            continue;
        }

        // Finally, append the capture group data to the destination.
        dest.append(group(groupNum, status));
        if (U_FAILURE(status)) {
            // Can fail if group number is out of range.
            return *this;
        }

    }

    return *this;
}



//--------------------------------------------------------------------------------
//
//    appendTail     Intended to be used in conjunction with appendReplacement()
//                   To the destination string, append everything following
//                   the last match position from the input string.
//
//--------------------------------------------------------------------------------
UnicodeString &RegexMatcher::appendTail(UnicodeString &dest) {
    int32_t  len = fInputLength-fMatchEnd;
    if (len > 0) {
        dest.append(*fInput, fMatchEnd, len);
    }
    return dest;
}



//--------------------------------------------------------------------------------
//
//   end
//
//--------------------------------------------------------------------------------
int32_t RegexMatcher::end(UErrorCode &err) const {
    return end(0, err);
}



int32_t RegexMatcher::end(int group, UErrorCode &err) const {
    if (U_FAILURE(err)) {
        return 0;
    }
    if (fMatch == FALSE) {
        err = U_REGEX_INVALID_STATE;
        return 0;
    }
    if (group < 0 || group > fPattern->fNumCaptureGroups) {
        err = U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    int32_t e = -1;
    if (group == 0) {
        e = fMatchEnd; 
    } else {
        // Note:  When the match engine backs out of a capture group, it sets the
        //        group's start position to -1.  The end position is left with junk.
        //        So, before returning an end position, we must first check that
        //        the start position indicates that the group matched something.
        int32_t s = fCaptureStarts->elementAti(group);
        if (s  != -1) {
            e = fCaptureEnds->elementAti(group);
        }
    }
    return e;
}



//--------------------------------------------------------------------------------
//
//   find()
//
//--------------------------------------------------------------------------------
UBool RegexMatcher::find() {
    // Start at the position of the last match end.  (Will be zero if the
    //   matcher has been reset.
    UErrorCode status = U_ZERO_ERROR;
    return find(fMatchEnd, status);
}



UBool RegexMatcher::find(int32_t start, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return FALSE;
    }
    if (start < 0 || start >= fInputLength) {
        status = U_INDEX_OUTOFBOUNDS_ERROR;
        return FALSE;
    }

    // TODO:  optimize a search for the first char of a possible match.
    // TODO:  optimize the search for a leading literal string.
    // TODO:  optimize based on the minimum length of a possible match
    int32_t  startPos;
    for (startPos=start; startPos < fInputLength; startPos++) {
        MatchAt(startPos, status);
        if (U_FAILURE(status)) {
            return FALSE;
        }
        if (fMatch) {
            return TRUE;
        }
    }
    return FALSE;
}



//--------------------------------------------------------------------------------
//
//  group()
//
//--------------------------------------------------------------------------------
UnicodeString RegexMatcher::group(UErrorCode &status) const {
    return group(0, status);
}



UnicodeString RegexMatcher::group(int32_t group, UErrorCode &status) const {
    int32_t  s = start(group, status);
    int32_t  e = end(group, status);
    if (U_FAILURE(status)) {
        return UnicodeString();
    }

    if (s < 0 || s >= e) {
        // Possible cases when a capture group didn't match 
        // TODO:  firgure out what non-matching capture groups really are supposed to do.
        return UnicodeString();
    }
    return UnicodeString(*fInput, s, e-s);
}




int32_t RegexMatcher::groupCount() const {
    return fPattern->fNumCaptureGroups;
}



const UnicodeString &RegexMatcher::input() const {
    return *fInput;
}




UBool RegexMatcher::lookingAt(UErrorCode &status) {
    if (U_FAILURE(status)) {
        return FALSE;
    }
    reset();
    MatchAt(0, status);
    return fMatch;
}



UBool RegexMatcher::matches(UErrorCode &status) {
    if (U_FAILURE(status)) {
        return FALSE;
    }
    reset();
    MatchAt(0, status);
    UBool   success  = (fMatch && fMatchEnd==fInputLength);
    return success;
}




const RegexPattern &RegexMatcher::pattern() const {
    return *fPattern;
}



//--------------------------------------------------------------------------------
//
//    replaceAll
//
//--------------------------------------------------------------------------------
UnicodeString RegexMatcher::replaceAll(const UnicodeString &replacement, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return *fInput;
    }
    UnicodeString destString;
    for (reset(); find(); ) {
        appendReplacement(destString, replacement, status);
    }
    appendTail(destString);
    return destString;
}




//--------------------------------------------------------------------------------
//
//    replaceFirst
//
//--------------------------------------------------------------------------------
UnicodeString RegexMatcher::replaceFirst(const UnicodeString &replacement, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return *fInput;
    }
    reset();
    if (!find()) {
        return *fInput;
    }

    UnicodeString destString;
    appendReplacement(destString, replacement, status);
    appendTail(destString);
    return destString;
}



//--------------------------------------------------------------------------------
//
//     reset
//
//--------------------------------------------------------------------------------
RegexMatcher &RegexMatcher::reset() {
    fMatchStart   = 0;
    fMatchEnd     = 0;
    fLastMatchEnd = 0;
    fMatch        = FALSE;
    int i;
    for (i=0; i<=fPattern->fNumCaptureGroups; i++) {
        fCaptureStarts->setElementAt(i, -1);
    }
    
    return *this;
}



RegexMatcher &RegexMatcher::reset(const UnicodeString &input) {
    fInput          = &input;
    fInputLength    = input.length();
    reset();
    return *this;
}



int32_t RegexMatcher::start(UErrorCode &err) const {
    return start(0, err);
}




int32_t RegexMatcher::start(int group, UErrorCode &err) const {
    if (U_FAILURE(err)) {
        return 0;
    }
    if (fMatch == FALSE) {
        err = U_REGEX_INVALID_STATE;
        return 0;
    }
    if (group < 0 || group > fPattern->fNumCaptureGroups) {
        err = U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    int32_t s;
    if (group == 0) {
        s = fMatchStart; 
    } else {
        s = fCaptureStarts->elementAti(group);
        // TODO:  what to do if no match on this specific group?
    }
    return s;
}



//--------------------------------------------------------------------------------
//
//    getCaptureText    We have encountered a '\' that might preceed a
//                      capture group specification. 
//                      If a valid capture group number follows the '\', 
//                      return the indicies to the start & end of the captured
//                      text, and update the patIdx to the position following the
//                      \n sequence.
//
//                      This function is used during find and replace operations when
//                      processing caputure references in the replacement text.
//
//--------------------------------------------------------------------------------
UBool  RegexMatcher::getCaptureText(const UnicodeString &rep,
                                int32_t &repIdx,
                                int32_t &textStart,
                                int32_t &textEnd)
{
    return FALSE;
}

//--------------------------------------------------------------------------------
//
//     backTrack    Within the match engine, this function is called when
//                  a local match failure occurs, and the match needs to back
//                  track and proceed down another path.
//
//                  Note:  Inline function.  Keep its body above MatchAt().
//
//--------------------------------------------------------------------------------
void RegexMatcher::backTrack(int32_t &inputIdx, int32_t &patIdx)  {
    inputIdx = fBackTrackStack->popi();
    patIdx   = fBackTrackStack->popi();
    int i;
    for (i=1; i<=fPattern->fNumCaptureGroups; i++) {
        int32_t cge = fBackTrackStack->popi();
        fCaptureEnds->setElementAt(cge, i);
        int32_t cgs = fBackTrackStack->popi();
        fCaptureStarts->setElementAt(cgs, i);
    }
}


            
//--------------------------------------------------------------------------------
//
//   MatchAt      This is the actual matching engine.
//
//--------------------------------------------------------------------------------
void RegexMatcher::MatchAt(int32_t startIdx, UErrorCode &status) {
    int32_t     inputIdx = startIdx;   // Current position in the input string.
    int32_t     patIdx   = 0;          // Current position in the compiled pattern.
    UBool       isMatch  = FALSE;      // True if the we have a match.

    int32_t     op;                    // Operation from the compiled pattern, split into
    int32_t     opType;                //    the opcode
    int32_t     opValue;               //    and the operand value.


    if (U_FAILURE(status)) {
        return;
    }

    //  Cache frequently referenced items from the compiled pattern
    //  in local variables.
    //
    UVector             *pat     = fPattern->fCompiledPat;
    const UnicodeString *litText = &fPattern->fLiteralText;
    UVector             *sets    = fPattern->fSets;
    

    //
    //  Main loop for interpreting the compiled pattern.
    //  One iteration of the loop per pattern operation performed.
    //
    for (;;) {
        op      = pat->elementAti(patIdx);
        opType  = URX_TYPE(op);
        opValue = URX_VAL(op);
        // printf("%d   %d  \"%c\"\n", patIdx, inputIdx, fInput->char32At(inputIdx));
        patIdx++;

        switch (opType) {


        case URX_NOP:
            break;


        case URX_ONECHAR:
            {
                UChar32 inputChar = fInput->char32At(inputIdx);
                if (inputChar == opValue) {
                    // TODO: handle the bogus 0xffff return from char32At for index out of range.
                    inputIdx = fInput->moveIndex32(inputIdx, 1);
                } else {
                    // No match.  Back up matching to a saved state
                    backTrack(inputIdx, patIdx);
                }
                break;
            }


        case URX_STRING:
            {
                int32_t stringStartIdx, stringLen;
                stringStartIdx = opValue;

                op      = pat->elementAti(patIdx);
                patIdx++;
                opType  = URX_TYPE(op);
                opValue = URX_VAL(op);
                U_ASSERT(opType == URX_STRING_LEN);
                stringLen = opValue;

                if (fInput->compareBetween(inputIdx,
                                            inputIdx+stringLen,
                                            *litText,
                                            stringStartIdx,
                                            stringStartIdx+stringLen) == 0)
                {
                    inputIdx += stringLen;
                } else {
                    // No match.  Back up matching to a saved state
                    backTrack(inputIdx, patIdx);
                }
            }
            break;



        case URX_STATE_SAVE:
            //   Save the state of all capture groups, the pattern continuation
            //   postion and the input position.  
            {
                int i;
                for (i=fPattern->fNumCaptureGroups; i>0; i--) {
                    fBackTrackStack->push(fCaptureStarts->elementAt(i), status);
                    fBackTrackStack->push(fCaptureEnds->elementAt(i), status);
                }
                fBackTrackStack->push(opValue,  status);   // pattern continuation position
                fBackTrackStack->push(inputIdx, status);   // current input position
            }
            break;


        case URX_END:
            // The match loop will exit via this path on a successful match,
            //   when we reach the end of the pattern.
            isMatch = TRUE;
            goto  breakFromLoop;

        case URX_START_CAPTURE:
            U_ASSERT(opValue > 0 && opValue <= fPattern->fNumCaptureGroups);
            fCaptureStarts->setElementAt(inputIdx,   opValue);
            fCaptureEnds  ->setElementAt((int32_t)0, opValue);
            break;


        case URX_END_CAPTURE:
            U_ASSERT(opValue > 0 && opValue <= fPattern->fNumCaptureGroups);
            fCaptureEnds->setElementAt(inputIdx, opValue);
            break;

        case URX_BACKSLASH_A:          // Test for start of input
            if (inputIdx != 0) {
                backTrack(inputIdx, patIdx);
            }
            break;

        case URX_BACKSLASH_B:          // Test for word boundaries
            if (FALSE) {
                backTrack(inputIdx, patIdx);
            }
            break;


        case URX_BACKSLASH_G:          // Test for position at end of previous match
            if (FALSE) {
                backTrack(inputIdx, patIdx);
            }
            break;

        case URX_BACKSLASH_W:          // Match word chars   (TODO:  doesn't belong here?
            if (FALSE) {
                backTrack(inputIdx, patIdx);
            }
            break;

        case URX_BACKSLASH_X:          // Match combining character sequence
            if (FALSE) {
                backTrack(inputIdx, patIdx);
            }
            break;

        case URX_BACKSLASH_Z:          // Test for end of line
            if (FALSE) {
                backTrack(inputIdx, patIdx);
            }
            break;



        case URX_SETREF:
            if (inputIdx < fInputLength) {
                // There is input left.  Pick up one char and test it for set membership.
                UChar32  c = fInput->char32At(inputIdx);
                U_ASSERT(opValue > 0 && opValue < sets->size());
                UnicodeSet *s = (UnicodeSet *)sets->elementAt(opValue);
                if (s->contains(c)) {
                    // The character is in the set.  A Match.
                    inputIdx = fInput->moveIndex32(inputIdx, 1);
                    break;
                }
            }
            // Either at end of input, or the character wasn't in the set.
            // Either way, we need to back track out.
            backTrack(inputIdx, patIdx);
            break;
            

        case URX_DOTANY:
            // . matches anything, but does not match if we've run out of input.
            if (inputIdx < fInputLength) {
                // There is input left.  Advance one character in it.
                inputIdx = fInput->moveIndex32(inputIdx, 1);
            } else {
            backTrack(inputIdx, patIdx);
            }
            break;

        case URX_JMP:
            patIdx = opValue;
            break;

        case URX_FAIL:
            isMatch = FALSE;
            goto breakFromLoop;


        default:
            // Trouble.  The compiled pattern contains an entry with an
            //           unrecognized type tag.
            U_ASSERT(FALSE);
        }

        if (U_FAILURE(status)) {
            break;
        }
    }
    
breakFromLoop:
    fMatch = isMatch;
    if (isMatch) {
        fLastMatchEnd = fMatchEnd;
        fMatchStart   = startIdx;
        fMatchEnd     = inputIdx;
        }
    return;
}





const char RegexMatcher::fgClassID = 0;

U_NAMESPACE_END


