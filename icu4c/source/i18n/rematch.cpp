//
//  file:  rematch.cpp    
//
//         Contains the implementation of class RegexMatcher,
//         which is one of the main API classes for the ICU regular expression package.
//
/*
**********************************************************************
*   Copyright (C) 2002 International Business Machines Corporation   *
*   and others. All rights reserved.                                 *
**********************************************************************
*/

#include "unicode/utypes.h"
#if !UCONFIG_NO_REGULAR_EXPRESSIONS

#include "unicode/regex.h"
#include "unicode/uniset.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "uassert.h"
#include "cmemory.h"
#include "uvector.h"
#include "uvectr32.h"
#include "regeximp.h"


U_NAMESPACE_BEGIN

//-----------------------------------------------------------------------------
//
//   Constructor and Destructor
//
//-----------------------------------------------------------------------------
RegexMatcher::RegexMatcher(const RegexPattern *pat)  { 
    fPattern           = pat;
    fInput             = NULL;
    fInputUC           = NULL;
    fInputLength       = 0;
    UErrorCode  status = U_ZERO_ERROR;
    fStack             = new UVector32(status);   // TODO:  do something with status.
    fData              = fSmallData;
    if (pat->fDataSize > sizeof(fSmallData)/sizeof(int32_t)) {
        fData = (int32_t *)uprv_malloc(pat->fDataSize * sizeof(int32_t));      // TODO:  null check
    }
        
    reset();
}



RegexMatcher::~RegexMatcher() {
    delete fStack;
    if (fData != fSmallData) {
        delete fData;
    }
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
    //  TODO:  optimize this loop by efficiently scanning for '$' or '\'
    int32_t  replLen = replacement.length();
    int32_t  replIdx = 0;
    while (replIdx<replLen) {
        UChar  c = replacement.charAt(replIdx);
        replIdx++;
        if (c == BACKSLASH) {
            // Backslash Escape.  Copy the following char out without further checks.
            //                    Note:  Surrogate pairs don't need any special handling
            //                           The second half wont be a '$' or a '\', and
            //                           will move to the dest normally on the next
            //                           loop iteration.
            if (replIdx >= replLen) {
                break;
            }
            c = replacement.charAt(replIdx);

            if (c==0x55/*U*/ || c==0x75/*u*/) {
                // We have a \udddd or \Udddddddd escape sequence.
                UChar32 escapedChar = replacement.unescapeAt(replIdx);
                if (escapedChar != 0xFFFFFFFF) {
                    dest.append(escapedChar);
                    replIdx += (c==0x55? 9: 5); 
                    // TODO:  Report errors for mal-formed \u escapes?
                    continue;
                }
            }

            // Plain backslash escape.  Just put out the escaped character.
            dest.append(c);
            replIdx++;
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

        int32_t numDigits = 0;
        int32_t groupNum  = 0;
        UChar32 digitC;
        for (;;) {
            if (replIdx >= replLen) {
                break;
            }
            digitC = replacement.char32At(replIdx);
            if (u_isdigit(digitC) == FALSE) {
                break;
            }
            replIdx = replacement.moveIndex32(replIdx, 1);
            groupNum=groupNum*10 + u_charDigitValue(digitC);
            numDigits++;
            if (numDigits >= fPattern->fMaxCaptureDigits) {
                break;
            }
        }


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
            break;
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
        return -1;
    }
    if (fMatch == FALSE) {
        err = U_REGEX_INVALID_STATE;
        return -1;
    }
    if (group < 0 || group > fPattern->fGroupMap->size()) {
        err = U_INDEX_OUTOFBOUNDS_ERROR;
        return -1;
    }
    int32_t e = -1;
    if (group == 0) {
        e = fMatchEnd; 
    } else {
        // Get the position within the stack frame of the variables for
        //    this capture group.
        int32_t groupOffset = fPattern->fGroupMap->elementAti(group-1);
        U_ASSERT(groupOffset < fPattern->fFrameSize);
        U_ASSERT(groupOffset >= 0);
        e = fFrame->fExtra[groupOffset + 1];
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
    //
    // TODO:  Needs optimization
    UErrorCode status = U_ZERO_ERROR;

    int32_t  startPos;
    // TODO:  needs to go up to the very end, so a pattern that can match a zero lenght
    //        string can match at the end of a string.  Can't do until loop-breaking
    //        is added to the engine, though, otherwise it triggers too many bugs.
    startPos = fMatchEnd;
    U_ASSERT(startPos >= 0 && startPos <= fInputLength);
    for (;;) {
        MatchAt(startPos, status);
        if (U_FAILURE(status)) {
            return FALSE;
        }
        if (fMatch) {
            return TRUE;
        }
        if (startPos >= fInputLength) {
            break;
        }
        startPos = fInput->moveIndex32(startPos, 1);
    }
    return FALSE;
}



UBool RegexMatcher::find(int32_t start, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return FALSE;
    }
    if (start < 0 || start >= fInputLength) {
        status = U_INDEX_OUTOFBOUNDS_ERROR;
        return FALSE;
    }
    this->reset();

    // TODO:  optimize a search for the first char of a possible match.
    // TODO:  optimize the search for a leading literal string.
    // TODO:  optimize based on the minimum length of a possible match
    int32_t  startPos;
    for (startPos=start; startPos < fInputLength; startPos=fInput->moveIndex32(startPos, 1)) {
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



UnicodeString RegexMatcher::group(int32_t groupNum, UErrorCode &status) const {
    int32_t  s = start(groupNum, status);
    int32_t  e = end(groupNum, status);

    // Note:  calling start() and end() above will do all necessary checking that
    //        the group number is OK and that a match exists.  status will be set.
    if (U_FAILURE(status)) {
        return UnicodeString();
    }

    if (s < 0) {
        // A capture group wasn't part of the match 
        return UnicodeString();
    }
    U_ASSERT(s <= e);
    return UnicodeString(*fInput, s, e-s);
}




int32_t RegexMatcher::groupCount() const {
    return fPattern->fGroupMap->size();
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
        if (U_FAILURE(status)) {
            break;
        }
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
    resetStack();
    return *this;
}



RegexMatcher &RegexMatcher::reset(const UnicodeString &input) {
    fInput          = &input;
    fInputLength    = input.length();
    fInputUC        = fInput->getBuffer();
    reset();
    return *this;
}



REStackFrame *RegexMatcher::resetStack() {
    // Discard any previous contents of the state save stack, and initialize a
    //  new stack frame to all -1.  The -1s are needed for capture group limits, where
    //  they indicate that a group has not yet matched anything.
    fStack->removeAllElements();
    UErrorCode  status = U_ZERO_ERROR;    // TODO:  do something with status

    int32_t *iFrame = fStack->reserveBlock(fPattern->fFrameSize, status);
    int i;
    for (i=0; i<fPattern->fFrameSize; i++) {
        iFrame[i] = -1;
    }
    return (REStackFrame *)iFrame;
}

//--------------------------------------------------------------------------------
//
//     start
//
//--------------------------------------------------------------------------------
int32_t RegexMatcher::start(UErrorCode &err) const {
    return start(0, err);
}




int32_t RegexMatcher::start(int group, UErrorCode &err) const {
    if (U_FAILURE(err)) {
        return -1;
    }
    if (fMatch == FALSE) {
        err = U_REGEX_INVALID_STATE;
        return -1;
    }
    if (group < 0 || group > fPattern->fGroupMap->size()) {
        err = U_INDEX_OUTOFBOUNDS_ERROR;
        return -1;
    }
    int32_t s;
    if (group == 0) {
        s = fMatchStart; 
    } else {
        int32_t groupOffset = fPattern->fGroupMap->elementAti(group-1);
        U_ASSERT(groupOffset < fPattern->fFrameSize);
        U_ASSERT(groupOffset >= 0);
        s = fFrame->fExtra[groupOffset];
    }
    return s;
}



//--------------------------------------------------------------------------------
//
//   isWordBoundary 
//                     in perl, "xab..cd..", \b is true at positions 0,3,5,7
//                     For us,
//                       If the current char is a combining mark,
//                          \b is FALSE.
//                       Else Scan backwards to the first non-combining char.
//                            We are at a boundary if the this char and the original chars are
//                               opposite in membership in \w set
//
//          parameters:   pos   - the current position in the input buffer
//                        start - the position where the match operation started.
//                                don't backup before this position when looking back
//                                for a preceding base char.
//
//--------------------------------------------------------------------------------
UBool RegexMatcher::isWordBoundary(int32_t pos) {
    UBool isBoundary = FALSE;
    UBool cIsWord    = FALSE;
    
    // Determine whether char c at current position is a member of the word set of chars.
    // If we're off the end of the string, behave as though we're not at a word char.
    if (pos < fInputLength) {
        UChar32  c = fInput->char32At(pos);
        int8_t ctype = u_charType(c);
        if (ctype==U_NON_SPACING_MARK || ctype==U_ENCLOSING_MARK) {
            // Current char is a combining one.  Not a boundary.
            return FALSE;
        }
        cIsWord = fPattern->fStaticSets[URX_ISWORD_SET]->contains(c);
    }
    
    // Back up until we come to a non-combining char, determine whether
    //  that char is a word char.
    UBool prevCIsWord = FALSE;
    int32_t prevPos = pos;
    for (;;) {
        if (prevPos == 0) {
            break;
        }
        prevPos = fInput->moveIndex32(prevPos, -1);
        UChar32 prevChar = fInput->char32At(prevPos);
        int8_t prevCType = u_charType(prevChar);
        if (!(prevCType==U_NON_SPACING_MARK || prevCType==U_ENCLOSING_MARK)) {
            prevCIsWord = fPattern->fStaticSets[URX_ISWORD_SET]->contains(prevChar);
            break;
        }
    }
    isBoundary = cIsWord ^ prevCIsWord;
    return isBoundary;
}

//--------------------------------------------------------------------------------
//
//   StateSave      
//       Make a new stack frame, initialized as a copy of the current stack frame.
//       Set the pattern index in the original stack frame from the operand value
//       in the opcode.  Execution of the engine continues with the state in
//       the newly created stack frame
//
//       Note that reserveBlock() may grow the stack, resulting in the
//       whole thing being relocated in memory.  
//
//--------------------------------------------------------------------------------
inline REStackFrame *RegexMatcher::StateSave(REStackFrame *fp, int32_t savePatIdx, int32_t frameSize, UErrorCode &status) {
    // push storage for a new frame. 
    int32_t *newFP = fStack->reserveBlock(frameSize, status);
    fp = (REStackFrame *)(newFP - frameSize);  // in case of realloc of stack.
    
    // New stack frame = copy of old top frame.
    int32_t *source = (int32_t *)fp;
    int32_t *dest   = newFP;
    for (;;) {
        *dest++ = *source++;
        if (source == newFP) {
            break;
        }
    }
    
    fp->fPatIdx = savePatIdx;
    return (REStackFrame *)newFP;
}
    
            
//--------------------------------------------------------------------------------
//
//   MatchAt      This is the actual matching engine.
//
//--------------------------------------------------------------------------------
void RegexMatcher::MatchAt(int32_t startIdx, UErrorCode &status) {
    UBool       isMatch  = FALSE;      // True if the we have a match.

    int32_t     op;                    // Operation from the compiled pattern, split into
    int32_t     opType;                //    the opcode
    int32_t     opValue;               //    and the operand value.

    #ifdef REGEX_RUN_DEBUG
    {
        printf("MatchAt(startIdx=%d)\n", startIdx);
        printf("Original Pattern: ");
        int i;
        for (i=0; i<fPattern->fPattern.length(); i++) {
            printf("%c", fPattern->fPattern.charAt(i));
        }
        printf("\n");
        printf("Input String: ");
        for (i=0; i<fInput->length(); i++) {
            UChar c = fInput->charAt(i);
            if (c<32 || c>256) {
                c = '.';
            }
            printf("%c", c);
        }
        printf("\n");
        printf("\n");
    }
    #endif

    if (U_FAILURE(status)) {
        return;
    }

    //  Cache frequently referenced items from the compiled pattern
    //  in local variables.
    //
    int32_t             *pat           = fPattern->fCompiledPat->getBuffer();

    const UChar         *litText       = fPattern->fLiteralText.getBuffer();
    UVector             *sets          = fPattern->fSets;
    int32_t              inputLen      = fInput->length();

    REStackFrame        *fp            = resetStack();
    int32_t              frameSize     = fPattern->fFrameSize;

    fp->fPatIdx   = 0;
    fp->fInputIdx = startIdx;


    //
    //  Main loop for interpreting the compiled pattern.
    //  One iteration of the loop per pattern operation performed.
    //
    for (;;) {
#if 0
        if (_heapchk() != _HEAPOK) {
            fprintf(stderr, "Heap Trouble\n");
        }
#endif
        op      = pat[fp->fPatIdx];
        opType  = URX_TYPE(op);
        opValue = URX_VAL(op);
        #ifdef REGEX_RUN_DEBUG
            printf("inputIdx=%d   inputChar=%c   sp=%3d  ", fp->fInputIdx,
                fInput->char32At(fp->fInputIdx), (int32_t *)fp-fStack->getBuffer());
            fPattern->dumpOp(fp->fPatIdx);
        #endif
        fp->fPatIdx++;

        switch (opType) {


        case URX_NOP:
            break;


        case URX_BACKTRACK:
            // Force a backtrack.  In some circumstances, the pattern compiler
            //   will notice that the pattern can't possibly match anything, and will
            //   emit one of these at that point.
            fp = (REStackFrame *)fStack->popFrame(frameSize);
            break;


        case URX_ONECHAR:
            if (fp->fInputIdx < fInputLength) {
                UChar32   c;
                U16_NEXT(fInputUC, fp->fInputIdx, fInputLength, c);
                if (c == opValue) {           
                    break;
                }
            }
            fp = (REStackFrame *)fStack->popFrame(frameSize);
            break;


        case URX_STRING:
            {
                // Test input against a literal string.
                // Strings require two slots in the compiled pattern, one for the
                //   offset to the string text, and one for the length.
                int32_t stringStartIdx, stringLen;
                stringStartIdx = opValue;

                op      = pat[fp->fPatIdx];
                fp->fPatIdx++;
                opType  = URX_TYPE(op);
                opValue = URX_VAL(op);
                U_ASSERT(opType == URX_STRING_LEN);
                stringLen = opValue;

                int32_t stringEndIndex = fp->fInputIdx + stringLen;
                if (stringEndIndex <= inputLen &&
                    u_strncmp(fInputUC+fp->fInputIdx, litText+stringStartIdx, stringLen) == 0) {
                    // Success.  Advance the current input position.
                    fp->fInputIdx = stringEndIndex;
                } else {
                    // No match.  Back up matching to a saved state
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                }
            }
            break;



        case URX_STATE_SAVE:
            fp = StateSave(fp, opValue, frameSize, status);
            break;


        case URX_END:
            // The match loop will exit via this path on a successful match,
            //   when we reach the end of the pattern.
            isMatch = TRUE;
            goto  breakFromLoop;

        // Start and End Capture stack frame variables are layout out like this:
            //  fp->fExtra[opValue]  - The start of a completed capture group
            //             opValue+1 - The end   of a completed capture group
            //             opValue+2 - the start of a capture group that end
            //                          has not yet been reached (and might not ever be).
        case URX_START_CAPTURE:
            U_ASSERT(opValue >= 0 && opValue < frameSize-3);
            fp->fExtra[opValue+2] = fp->fInputIdx;
            break;


        case URX_END_CAPTURE:
            U_ASSERT(opValue >= 0 && opValue < frameSize-3);
            U_ASSERT(fp->fExtra[opValue+2] >= 0);    // Start pos for this group must be set.
            fp->fExtra[opValue]   = fp->fExtra[opValue+2];   // Tentative start becomes real.
            fp->fExtra[opValue+1] = fp->fInputIdx;           // End position
            U_ASSERT(fp->fExtra[opValue] <= fp->fExtra[opValue+1]);
            break;


        case URX_DOLLAR:                   //  $, test for End of line
                                           //     or for position before new line at end of input
            if (fp->fInputIdx < inputLen-2) {
                // We are no where near the end of input.  Fail.
                fp = (REStackFrame *)fStack->popFrame(frameSize);
                break;
            }
            if (fp->fInputIdx >= inputLen) {
                // We really are at the end of input.  Success.
                break;
            }
            // If we are positioned just before a new-line that is located at the
            //   end of input, succeed.
            if (fp->fInputIdx == inputLen-1) {
                UChar32 c = fInput->char32At(fp->fInputIdx);
                if (c == 0x0a || c==0x0d || c==0x0c || c==0x85 ||c==0x2028 || c==0x2029) {
                    break;                         // At new-line at end of input. Success
                }
            }

            if (fp->fInputIdx == inputLen-2) {
                if (fInput->char32At(fp->fInputIdx) == 0x0d && fInput->char32At(fp->fInputIdx+1) == 0x0a) {
                    break;                         // At CR/LF at end of input.  Success
                }
            }

            fp = (REStackFrame *)fStack->popFrame(frameSize);

            // TODO:  support for multi-line mode.
            break;


        case URX_CARET:                    //  ^, test for start of line
            if (fp->fInputIdx != 0) {
                fp = (REStackFrame *)fStack->popFrame(frameSize);
            }                              // TODO:  support for multi-line mode.
            break;


        case URX_BACKSLASH_A:          // Test for start of input
            if (fp->fInputIdx != 0) {
                fp = (REStackFrame *)fStack->popFrame(frameSize);
            }
            break;

        case URX_BACKSLASH_B:          // Test for word boundaries
            {
                UBool success = isWordBoundary(fp->fInputIdx);
                success ^= (opValue != 0);     // flip sense for \B
                if (!success) {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                }
            }
            break;


        case URX_BACKSLASH_D:            // Test for decimal digit
            {
                if (fp->fInputIdx >= fInputLength) {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                    break;
                }

                UChar32 c = fInput->char32At(fp->fInputIdx);   
                int8_t ctype = u_charType(c);
                UBool success = (ctype == U_DECIMAL_DIGIT_NUMBER);
                success ^= (opValue != 0);        // flip sense for \D
                if (success) {
                    fp->fInputIdx = fInput->moveIndex32(fp->fInputIdx, 1);
                } else {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                }
            }
            break;




        case URX_BACKSLASH_G:          // Test for position at end of previous match
            if (!((fMatch && fp->fInputIdx==fMatchEnd) || fMatch==FALSE && fp->fInputIdx==0)) {
                fp = (REStackFrame *)fStack->popFrame(frameSize);
            }
            break;


        case URX_BACKSLASH_X:          // Match combining character sequence
            {                          //  Closer to Grapheme cluster than to Perl \X
                // Fail if at end of input
                if (fp->fInputIdx >= fInputLength) {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                    break;
                }

                // Always consume one char
                UChar32 c = fInput->char32At(fp->fInputIdx);   
                fp->fInputIdx = fInput->moveIndex32(fp->fInputIdx, 1);

                // Consume CR/LF as a pair
                if (c == 0x0d)  { 
                    UChar32 c = fInput->char32At(fp->fInputIdx);   
                    if (c == 0x0a) {
                         fp->fInputIdx = fInput->moveIndex32(fp->fInputIdx, 1);
                         break;
                    }
                }

                // Consume any combining marks following a non-control char
                int8_t ctype = u_charType(c);
                if (ctype != U_CONTROL_CHAR) {
                    for(;;) {   
                        c = fInput->char32At(fp->fInputIdx);   
                        ctype = u_charType(c);
                        // TODO:  make a set and add the "other grapheme extend" chars
                        //        to the list of stuff to be skipped over.
                        if (!(ctype == U_NON_SPACING_MARK || ctype == U_ENCLOSING_MARK)) {
                            break;
                        }
                        fp->fInputIdx = fInput->moveIndex32(fp->fInputIdx, 1);
                        if (fp->fInputIdx >= fInputLength) {
                            break; 
                        }
                    }
                }
            }
            break;



        case URX_BACKSLASH_Z:          // Test for end of line
            if (fp->fInputIdx < inputLen) {
                fp = (REStackFrame *)fStack->popFrame(frameSize);
            }
            break;



        case URX_STATIC_SETREF:
            {
                // Test input character against one of the predefined sets
                //    (Word Characters, for example)
                // The high bit of the op value is a flag for the match polarity.
                //    0:   success if input char is in set.
                //    1:   success if input char is not in set.
                if (fp->fInputIdx >= fInputLength) {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                    break;
                }

                UBool success = ((opValue & URX_NEG_SET) == URX_NEG_SET);  
                opValue &= ~URX_NEG_SET;
                U_ASSERT(opValue > 0 && opValue < URX_LAST_SET);
                UChar32  c;
                U16_NEXT(fInputUC, fp->fInputIdx, fInputLength, c);
                const UnicodeSet *s = fPattern->fStaticSets[opValue];
                if (s->contains(c)) {
                    success = !success;
                }
                if (!success) {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                }
            }
            break;
            

        case URX_SETREF:
            if (fp->fInputIdx < fInputLength) {
                // There is input left.  Pick up one char and test it for set membership.
                UChar32   c;
                U16_NEXT(fInputUC, fp->fInputIdx, fInputLength, c);
                U_ASSERT(opValue > 0 && opValue < sets->size());
                UnicodeSet *s = (UnicodeSet *)sets->elementAt(opValue);
                if (s->contains(c)) {
                    // The character is in the set.  A Match.
                    break;
                }
            }
            // Either at end of input, or the character wasn't in the set.
            // Either way, we need to back track out.
            fp = (REStackFrame *)fStack->popFrame(frameSize);
            break;
            

        case URX_DOTANY:
            {
                // . matches anything
                if (fp->fInputIdx >= fInputLength) {
                    // At end of input.  Match failed.  Backtrack out.
                        fp = (REStackFrame *)fStack->popFrame(frameSize);
                    break;
                }
                // There is input left.  Advance over one char, unless we've hit end-of-line
                UChar32 c;
                U16_NEXT(fInputUC, fp->fInputIdx, fInputLength, c);
                if (((c & 0x7f) <= 0x29) &&     // First quickly bypass as many chars as possible
                    (c == 0x0a || c==0x0d || c==0x0c || c==0x85 ||c==0x2028 || c==0x2029)) {
                    // End of line in normal mode.   . does not match.
                        fp = (REStackFrame *)fStack->popFrame(frameSize);
                    break;
                }
            }
            break;
            
            
        case URX_DOTANY_ALL:
            {
                // ., in dot-matches-all (including new lines) mode
                // . matches anything
                if (fp->fInputIdx >= fInputLength) {
                    // At end of input.  Match failed.  Backtrack out.
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                    break;
                }
                // There is input left.  Advance over one char, unless we've hit end-of-line
                UChar32 c = fInput->char32At(fp->fInputIdx);
                fp->fInputIdx = fInput->moveIndex32(fp->fInputIdx, 1);
                if (c == 0x0a || c==0x0d || c==0x0c || c==0x85 ||c==0x2028 || c==0x2029) {
                    // In the case of a CR/LF, we need to advance over both.
                    UChar32 nextc = fInput->char32At(fp->fInputIdx);
                    if (c == 0x0d && nextc == 0x0a) {
                        fp->fInputIdx = fInput->moveIndex32(fp->fInputIdx, 1);
                    }
                }
            }
            break;

        case URX_JMP:
            fp->fPatIdx = opValue;
            break;

        case URX_FAIL:
            isMatch = FALSE;
            goto breakFromLoop;

        case URX_CTR_INIT:
            {
                U_ASSERT(opValue >= 0 && opValue < frameSize-2);
                fp->fExtra[opValue] = 0;       //  Set the loop counter variable to zero

                // Pick up the three extra operands that CTR_INIT has, and
                //    skip the pattern location counter past 
                int32_t instrOperandLoc = fp->fPatIdx;
                fp->fPatIdx += 3;
                int32_t loopLoc  = URX_VAL(pat[instrOperandLoc]);
                int32_t minCount = pat[instrOperandLoc+1];
                int32_t maxCount = pat[instrOperandLoc+2];
                U_ASSERT(minCount>=0);
                U_ASSERT(maxCount>=minCount || maxCount==-1);
                U_ASSERT(loopLoc>fp->fPatIdx);

                if (minCount == 0) {
                    fp = StateSave(fp, loopLoc+1, frameSize, status);
                }
                if (maxCount == 0) {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);
                }
            }
            break;

        case URX_CTR_LOOP:
            {
                U_ASSERT(opValue>0 && opValue < fp->fPatIdx-2);
                int32_t initOp = pat[opValue];
                U_ASSERT(URX_TYPE(initOp) == URX_CTR_INIT);
                int32_t *pCounter = &fp->fExtra[URX_VAL(initOp)];
                int32_t minCount  = pat[opValue+2];
                int32_t maxCount = pat[opValue+3];
                // Increment the counter.  Note: we're not worrying about counter
                //   overflow, since the data comes from UnicodeStrings, which
                //   stores its length in an int32_t.
                (*pCounter)++;
                U_ASSERT(*pCounter > 0);
                if ((uint32_t)*pCounter >= (uint32_t)maxCount) {
                    U_ASSERT(*pCounter == maxCount || maxCount == -1);
                    break;
                }
                if (*pCounter >= minCount) {
                    fp = StateSave(fp, fp->fPatIdx, frameSize, status);
                }
                fp->fPatIdx = opValue + 4;    // Loop back.
            }
            break;

        case URX_CTR_INIT_NG:
            {
                U_ASSERT(opValue >= 0 && opValue < frameSize-2);
                fp->fExtra[opValue] = 0;       //  Set the loop counter variable to zero

                // Pick up the three extra operands that CTR_INIT has, and
                //    skip the pattern location counter past 
                int32_t instrOperandLoc = fp->fPatIdx;
                fp->fPatIdx += 3;
                int32_t loopLoc  = URX_VAL(pat[instrOperandLoc]);
                int32_t minCount = pat[instrOperandLoc+1];
                int32_t maxCount = pat[instrOperandLoc+2];
                U_ASSERT(minCount>=0);
                U_ASSERT(maxCount>=minCount || maxCount==-1);
                U_ASSERT(loopLoc>fp->fPatIdx);

                if (minCount == 0) {
                    if (maxCount != 0) {
                        fp = StateSave(fp, fp->fPatIdx, frameSize, status);
                    }
                    fp->fPatIdx = loopLoc+1;   // Continue with stuff after repeated block
                } 
            }
            break;

        case URX_CTR_LOOP_NG:
            {
                U_ASSERT(opValue>0 && opValue < fp->fPatIdx-2);
                int32_t initOp = pat[opValue];
                U_ASSERT(URX_TYPE(initOp) == URX_CTR_INIT_NG);
                int32_t *pCounter = &fp->fExtra[URX_VAL(initOp)];
                int32_t minCount  = pat[opValue+2];
                int32_t maxCount = pat[opValue+3];
                // Increment the counter.  Note: we're not worrying about counter
                //   overflow, since the data comes from UnicodeStrings, which
                //   stores its length in an int32_t.
                (*pCounter)++;
                U_ASSERT(*pCounter > 0);

                if ((uint32_t)*pCounter >= (uint32_t)maxCount) {
                    // The loop has matched the maximum permitted number of times.
                    //   Break out of here with no action.  Matching will
                    //   continue with the following pattern.
                    U_ASSERT(*pCounter == maxCount || maxCount == -1);
                    break;
                }

                if (*pCounter < minCount) {
                    // We haven't met the minimum number of matches yet.
                    //   Loop back for another one.
                    fp->fPatIdx = opValue + 4;    // Loop back.
                } else {
                    // We do have the minimum number of matches.
                    //   Fall into the following pattern, but first do
                    //   a state save to the top of the loop, so that a failure
                    //   in the following pattern will try another iteration of the loop.
                    fp = StateSave(fp, opValue + 4, frameSize, status);
                }
            }
            break;

        case URX_STO_SP:
            U_ASSERT(opValue >= 0 && opValue < fPattern->fDataSize);
            fData[opValue] = fStack->size();
            break;

        case URX_LD_SP:
            {
                U_ASSERT(opValue >= 0 && opValue < fPattern->fDataSize);
                int32_t newStackSize = fData[opValue];
                U_ASSERT(newStackSize <= fStack->size());
                int32_t *newFP = fStack->getBuffer() + newStackSize - frameSize;
                if (newFP == (int32_t *)fp) {
                    break;
                }
                int32_t i;
                for (i=0; i<frameSize; i++) {
                    newFP[i] = ((int32_t *)fp)[i];
                }
                fp = (REStackFrame *)newFP;
                fStack->setSize(newStackSize);
            }
            break;

        case URX_BACKREF:
            {
                U_ASSERT(opValue < frameSize);
                int32_t groupStartIdx = fp->fExtra[opValue];
                int32_t groupEndIdx   = fp->fExtra[opValue+1];
                U_ASSERT(groupStartIdx <= groupEndIdx);
                int32_t len = groupEndIdx-groupStartIdx;
                if (groupStartIdx < 0) {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);   // FAIL, no match.
                }

                if (groupStartIdx < 0 || len == 0) {
                        // This capture group has not participated in the match thus far,
                        //   or the match was of an empty string.
                        //   Verified by testing:  Perl matches succeed in these cases, so
                        //   we do too.
                        break;
                    }
                if ((fp->fInputIdx + len > inputLen) || 
                    u_strncmp(fInputUC+groupStartIdx, fInputUC+fp->fInputIdx, len) != 0) {
                    fp = (REStackFrame *)fStack->popFrame(frameSize);   // FAIL, no match.
                } else {
                    fp->fInputIdx += len;     // Match.  Advance current input position.
                }
            }
            break;

        case URX_STO_INP_LOC:
            {
                U_ASSERT(opValue >= 0 && opValue < frameSize);
                fp->fExtra[opValue] = fp->fInputIdx;
            }
            break;

        case URX_JMPX:
            {
                int32_t instrOperandLoc = fp->fPatIdx;
                fp->fPatIdx += 1;
                int32_t dataLoc  = URX_VAL(pat[instrOperandLoc]);
                U_ASSERT(dataLoc >= 0 && dataLoc < frameSize);
                int32_t savedInputIdx = fp->fExtra[dataLoc];
                U_ASSERT(savedInputIdx <= fp->fInputIdx);
                if (savedInputIdx < fp->fInputIdx) {
                    fp->fPatIdx = opValue;                               // JMP
                } else {
                     fp = (REStackFrame *)fStack->popFrame(frameSize);   // FAIL, no progress in loop.
                }
            }
            break;
                   

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
        fMatchEnd     = fp->fInputIdx;
        REGEX_RUN_DEBUG_PRINTF("Match.  start=%d   end=%d\n\n", fMatchStart, fMatchEnd);
        }
    else
    {
        REGEX_RUN_DEBUG_PRINTF("No match\n\n");
    }

    fFrame = fp;                // The active stack frame when the engine stopped.
                                //   Contains the capture group results that we need to
                                //    access later.

    return;
}



const char RegexMatcher::fgClassID = 0;

U_NAMESPACE_END

#endif  // !UCONFIG_NO_REGULAR_EXPRESSIONS

