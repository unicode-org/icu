//
//  file:  repattrn.cpp    
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
#include "uassert.h"
#include "uvector.h"
#include "uvectr32.h"
#include "regexcmp.h"
#include "regeximp.h"

U_NAMESPACE_BEGIN

//--------------------------------------------------------------------------
//
//    RegexPattern    Default Constructor
//
//--------------------------------------------------------------------------
RegexPattern::RegexPattern() {
    init();
};


//--------------------------------------------------------------------------
//
//   Copy Constructor        Note:  This is a rather inefficient implementation,
//                                  but it probably doesn't matter.
//
//--------------------------------------------------------------------------
RegexPattern::RegexPattern(const RegexPattern &other) :  UObject(other) {
    init(); 
    *this = other;
}



//--------------------------------------------------------------------------
//
//    Assignmenet Operator
//
//--------------------------------------------------------------------------
RegexPattern &RegexPattern::operator = (const RegexPattern &other) {
    if (this == &other) {
        // Source and destination are the same.  Don't do anything.
        return *this;
    }

    // Clean out any previous contents of object being assigned to.
    zap();

    // Give target object a default initialization
    init();

    // Copy simple fields
    fPattern          = other.fPattern;
    fFlags            = other.fFlags;
    fLiteralText      = other.fLiteralText;
    fBadState         = other.fBadState;
    fMinMatchLen      = other.fMinMatchLen;
    fMaxMatchLen      = other.fMaxMatchLen;
    fMaxCaptureDigits = other.fMaxCaptureDigits;
    fStaticSets       = other.fStaticSets; 
    
    fStartType        = other.fStartType;
    fInitialStringIdx = other.fInitialStringIdx;
    fInitialStringLen = other.fInitialStringLen;
    fInitialChars     = new UnicodeSet(*other.fInitialChars);
    fInitialChar      = other.fInitialChar;
    if (fBadState) {
        return *this;
    }

    //  Copy the pattern.  It's just values, nothing deep to copy.
    //  TODO:  something with status
    UErrorCode status = U_ZERO_ERROR;
    fCompiledPat->assign(*other.fCompiledPat, status);
    fGroupMap->assign(*other.fGroupMap, status);

    // Note:  do not copy fMatcher.  It'll be created on first use if the
    //        destination needs one. 

    //  Copy the Unicode Sets.  
    //    Could be made more efficient if the sets were reference counted and shared,
    //    but I doubt that pattern copying will be particularly common. 
    //    Note:  init() already added an empty element zero to fSets
    int32_t i;
    for (i=1; i<other.fSets->size(); i++) {
        UnicodeSet *sourceSet = (UnicodeSet *)other.fSets->elementAt(i);
        UnicodeSet *newSet    = new UnicodeSet(*sourceSet);
        if (newSet == NULL) {
            fBadState = TRUE;
            break;
        }
        fSets->addElement(newSet, status);
    }
    if (U_FAILURE(status)) {
        fBadState = TRUE;
    }
    return *this;
}


//--------------------------------------------------------------------------
//
//    init        Shared initialization for use by constructors.
//                Bring an uninitialized RegexPattern up to a default state.
//
//--------------------------------------------------------------------------
void RegexPattern::init() {
    fFlags            = 0;
    fBadState         = FALSE;
    fMinMatchLen      = 0;
    fMaxMatchLen      = -1;
    fMaxCaptureDigits = 1;  
    fStaticSets       = NULL;
    fMatcher          = NULL;
    fFrameSize        = 0;
    fDataSize         = 0;
    fStartType        = START_NO_INFO;
    fInitialStringIdx = 0;
    fInitialStringLen = 0;
    fInitialChars     = NULL;
    fInitialChar      = 0;
    
    UErrorCode status=U_ZERO_ERROR;
    // Init of a completely new RegexPattern.
    fCompiledPat  = new UVector32(status);
    fGroupMap     = new UVector32(status);
    fSets         = new UVector(status);
    fInitialChars = new UnicodeSet;
    if (U_FAILURE(status) || fCompiledPat == NULL || fSets == NULL || fInitialChars == NULL) {
        fBadState = TRUE;
        return;
    }

    // Slot zero of the vector of sets is reserved.  Fill it here.
    fSets->addElement((int32_t)0, status);
}


//--------------------------------------------------------------------------
//
//   zap            Delete everything owned by this RegexPattern. 
//
//--------------------------------------------------------------------------
void RegexPattern::zap() {
    delete fMatcher;
    fMatcher = NULL;
    delete fCompiledPat;
    fCompiledPat = NULL;
    int i;
    for (i=1; i<fSets->size(); i++) {
        UnicodeSet *s;
        s = (UnicodeSet *)fSets->elementAt(i);
        if (s != NULL) {
            delete s;
        }
    }
    delete fSets;
    fSets = NULL;
    delete fGroupMap;
    fGroupMap = NULL;
    delete fInitialChars;
    fInitialChars = NULL;
}


//--------------------------------------------------------------------------
//
//   Destructor
//
//--------------------------------------------------------------------------
RegexPattern::~RegexPattern() {
    zap();
};


//--------------------------------------------------------------------------
//
//   Clone
//
//--------------------------------------------------------------------------
RegexPattern  *RegexPattern::clone() const { 
    RegexPattern  *copy = new RegexPattern(*this);
    return copy;
};


//--------------------------------------------------------------------------
//
//   operator ==   (comparison)    Consider to patterns to be == if the
//                                 pattern strings and the flags are the same.
//
//--------------------------------------------------------------------------
UBool   RegexPattern::operator ==(const RegexPattern &other) const {
    UBool r = this->fFlags    == other.fFlags &&
              this->fPattern  == other.fPattern &&
              this->fBadState == FALSE &&
              other.fBadState == FALSE;
    return r;
}

//---------------------------------------------------------------------
//
//   compile        
//
//---------------------------------------------------------------------
RegexPattern  *RegexPattern::compile(
                             const UnicodeString &regex,
                             uint32_t             flags,
                             UParseError          &pe,
                             UErrorCode           &status)  {

    if (U_FAILURE(status)) {
        return NULL;
    }

    const uint32_t allFlags = UREGEX_CANON_EQ | UREGEX_CASE_INSENSITIVE | UREGEX_COMMENTS |
                              UREGEX_DOTALL   | UREGEX_MULTILINE;

    if ((flags & ~allFlags) != 0) {
        status = U_REGEX_INVALID_FLAG;
        return NULL;
    }

    if ((flags & UREGEX_CANON_EQ) != 0) {
        status = U_REGEX_UNIMPLEMENTED;
        return NULL;
    }

    RegexPattern *This = new RegexPattern;
    if (This == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    if (This->fBadState) {
        status = U_REGEX_INVALID_STATE;
        return NULL;
    }
    This->fFlags = flags;

    RegexCompile     compiler(This, status);
    compiler.compile(regex, pe, status);

    return This;
};
    
//
//   compile with default flags.
//
RegexPattern *RegexPattern::compile( const UnicodeString &regex,
        UParseError          &pe,
        UErrorCode           &err) 
{
    return compile(regex, 0, pe, err); 
}



//---------------------------------------------------------------------
//
//   flags
//
//---------------------------------------------------------------------
uint32_t RegexPattern::flags() const {
    return fFlags;
}


//---------------------------------------------------------------------
//
//   matcher(UnicodeString, err)
//
//---------------------------------------------------------------------
RegexMatcher *RegexPattern::matcher(const UnicodeString &input,
                                    UErrorCode          &err)  const {
    RegexMatcher    *retMatcher = NULL;

    if (U_FAILURE(err)) {
        return NULL;
    }
    if (fBadState) {
        U_FAILURE(U_REGEX_INVALID_STATE);
        return NULL;
    }

    retMatcher = new RegexMatcher(this); 
    if (retMatcher == NULL) {
        err = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    retMatcher->reset(input);
    return retMatcher;
};



//---------------------------------------------------------------------
//
//   matches        Convenience function to test for a match, starting
//                  with a pattern string and a data string.
//
//---------------------------------------------------------------------
UBool RegexPattern::matches(const UnicodeString   &regex,
              const UnicodeString   &input,
                    UParseError     &pe,
                    UErrorCode      &status) {

    if (U_FAILURE(status)) {return FALSE;}

    UBool         retVal;
    RegexPattern *pat     = NULL;
    RegexMatcher *matcher = NULL;

    pat     = RegexPattern::compile(regex, 0, pe, status);
    matcher = pat->matcher(input, status);
    retVal  = matcher->matches(status);

    delete matcher;
    delete pat;
    return retVal;
}




//---------------------------------------------------------------------
//
//   pattern
//
//---------------------------------------------------------------------
UnicodeString RegexPattern::pattern() const {
    return fPattern;
}




//---------------------------------------------------------------------
//
//   split
//
//---------------------------------------------------------------------
int32_t  RegexPattern::split(const UnicodeString &input,
        UnicodeString    dest[],
        int32_t          destCapacity,
        UErrorCode       &status) const
{
    //
    // Check arguements for validity
    //
    if (U_FAILURE(status)) {
        return 0;
    };

    if (destCapacity < 1) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    //
    // If we don't already have a cached matcher object from a previous call
    //   to split(), create one now.
    //  TODO:  NOT THREAD SAFE.   FIX.
    //
    if (fMatcher == NULL) {
        RegexMatcher *m = matcher(input, status);
        if (U_FAILURE(status)) {
            return 0;
        }
        // Need to cast off const to cache the matcher
        RegexPattern *nonConstThis = (RegexPattern *)this;
        nonConstThis->fMatcher = m;
    }

    //
    // Set our input text into the matcher
    //
    fMatcher->reset(input);
    int32_t   inputLen = input.length();
    int32_t   nextOutputStringStart = 0;
    if (inputLen == 0) {
        return 0;
    }


    //
    // Loop through the input text, searching for the delimiter pattern
    //
    int i;
    int32_t numCaptureGroups = fGroupMap->size();
    for (i=0; ; i++) {
        if (i==destCapacity-1) {
            // There is only one output string left.
            // Fill it with whatever is left from the input, then exit the loop.
            dest[i].setTo(input, nextOutputStringStart, inputLen-nextOutputStringStart);
            break;
        }
        if (fMatcher->find()) {
            // We found another delimiter.  Move everything from where we started looking
            //  up until the start of the delimiter into the next output string.
            int32_t fieldLen = fMatcher->fMatchStart - nextOutputStringStart;
            dest[i].setTo(input, nextOutputStringStart, fieldLen);
            nextOutputStringStart = fMatcher->fMatchEnd;

            // If the delimiter pattern has capturing parentheses, the captured
            //  text goes out into the next n destination strings.
            int32_t groupNum;
            for (groupNum=1; groupNum<=numCaptureGroups; groupNum++) {
                if (i==destCapacity-1) {
                    break;
                }
                i++;
                dest[i] = fMatcher->group(groupNum, status);
            }

            if (nextOutputStringStart == inputLen) {
                // The delimiter was at the end of the string.  We're done.
                break;
            }

            if (i==destCapacity-1) {
                // We've filled up the last output string with capture group data.
                //  Give back the last string, to be used for the remainder of the input.
                i--;
            }
        }
        else
        {
            // We ran off the end of the input while looking for the next delimiter.
            // All the remaining text goes into the current output string.
            dest[i].setTo(input, nextOutputStringStart, inputLen-nextOutputStringStart);
            break;
        }
    }
    return i+1;
}




//---------------------------------------------------------------------
//
//   dump    Output the compiled form of the pattern.
//           Debugging function only.
//
//---------------------------------------------------------------------
static const char * const opNames[] = {URX_OPCODE_NAMES};

void   RegexPattern::dumpOp(int32_t index) const {
    int32_t op          = fCompiledPat->elementAti(index);
    int32_t val         = URX_VAL(op);
    int32_t type        = URX_TYPE(op);
    int32_t pinnedType  = type;
    if (pinnedType >= sizeof(opNames)/sizeof(char *)) {
        pinnedType = 0;
    }
    
    REGEX_DUMP_DEBUG_PRINTF("%4d   %08x    %-15s  ", index, op, opNames[pinnedType]);
    switch (type) {
    case URX_NOP:
    case URX_DOTANY:
    case URX_FAIL:
    case URX_CARET:
    case URX_DOLLAR:
    case URX_BACKSLASH_G:
    case URX_BACKSLASH_X:
    case URX_END:
    case URX_DOLLAR_M:
    case URX_CARET_M:
        // Types with no operand field of interest.
        break;
        
    case URX_RESERVED_OP:
    case URX_START_CAPTURE:
    case URX_END_CAPTURE:
    case URX_STATE_SAVE:
    case URX_JMP:
    case URX_BACKSLASH_B:
    case URX_BACKSLASH_D:
    case URX_BACKSLASH_Z:
    case URX_STRING_LEN:
    case URX_CTR_INIT:
    case URX_CTR_INIT_NG:
    case URX_CTR_INIT_P:
    case URX_CTR_LOOP:
    case URX_CTR_LOOP_NG:
    case URX_CTR_LOOP_P:
    case URX_RELOC_OPRND:
    case URX_STO_SP:
    case URX_LD_SP:
    case URX_BACKREF:
    case URX_STO_INP_LOC:
    case URX_JMPX:
    case URX_LA_START:
    case URX_LA_END:
    case URX_BACKREF_I:
    case URX_LB_START:
    case URX_LB_CONT:
    case URX_LB_END:
    case URX_LBN_CONT:
    case URX_LBN_END:
        // types with an integer operand field.
        REGEX_DUMP_DEBUG_PRINTF("%d", val);
        break;
        
    case URX_ONECHAR:
    case URX_ONECHAR_I:
        REGEX_DUMP_DEBUG_PRINTF("%c", val<256?val:'?');
        break;
        
    case URX_STRING:
    case URX_STRING_I:
        {
            int32_t lengthOp       = fCompiledPat->elementAti(index+1);
            U_ASSERT(URX_TYPE(lengthOp) == URX_STRING_LEN);
            int32_t length = URX_VAL(lengthOp);
            int32_t i;
            for (i=val; i<val+length; i++) {
                UChar c = fLiteralText[i];
                if (c < 32 || c >= 256) {c = '.';}
                REGEX_DUMP_DEBUG_PRINTF("%c", c);
            }
        }
        break;

    case URX_SETREF:
        {
            UnicodeString s;
            UnicodeSet *set = (UnicodeSet *)fSets->elementAt(val);
            set->toPattern(s, TRUE);
            for (int32_t i=0; i<s.length(); i++) {
                REGEX_DUMP_DEBUG_PRINTF("%c", s.charAt(i));
            }
        }
        break;

    case URX_STATIC_SETREF:
        {
            UnicodeString s;
            if (val & URX_NEG_SET) {
                REGEX_DUMP_DEBUG_PRINTF("NOT ");
                val &= ~URX_NEG_SET;
            }
            UnicodeSet *set = fStaticSets[val];
            set->toPattern(s, TRUE);
            for (int32_t i=0; i<s.length(); i++) {
                REGEX_DUMP_DEBUG_PRINTF("%c", s.charAt(i));
            }
        }
        break;

        
    default:
        REGEX_DUMP_DEBUG_PRINTF("??????");
        break;
    }
    REGEX_DUMP_DEBUG_PRINTF("\n");
}






void   RegexPattern::dump() const {
    int      index;
    int      i;

    REGEX_DUMP_DEBUG_PRINTF("Original Pattern:  ");
    for (i=0; i<fPattern.length(); i++) {
        REGEX_DUMP_DEBUG_PRINTF("%c", fPattern.charAt(i));
    }
    REGEX_DUMP_DEBUG_PRINTF("\n");
    REGEX_DUMP_DEBUG_PRINTF("Pattern Valid?:     %s\n"  , fBadState? "no" : "yes");
    REGEX_DUMP_DEBUG_PRINTF("   Min Match Length:  %d\n", fMinMatchLen);
    REGEX_DUMP_DEBUG_PRINTF("   Max Match Length:  %d\n", fMaxMatchLen);
    REGEX_DUMP_DEBUG_PRINTF("   Match Start Type:  %s\n", START_OF_MATCH_STR(fStartType));   
    if (fStartType == START_STRING) {
        REGEX_DUMP_DEBUG_PRINTF("    Initial match sting: \"");
        for (i=fInitialStringIdx; i<fInitialStringIdx+fInitialStringLen; i++) {
            REGEX_DUMP_DEBUG_PRINTF("%c", fLiteralText[i]);   // TODO:  non-printables, surrogates.
        }

    } else if (fStartType == START_SET) {
        int32_t numSetChars = fInitialChars->size();
        if (numSetChars > 20) {
            numSetChars = 20;
        }
        REGEX_DUMP_DEBUG_PRINTF("     Match First Chars : ");
        for (i=0; i<numSetChars; i++) {
            UChar32 c = fInitialChars->charAt(i);
            if (0x20<c && c <0x7e) { 
                REGEX_DUMP_DEBUG_PRINTF("%c ", c);
            } else {
                REGEX_DUMP_DEBUG_PRINTF("%#x ", c);
            }
        }
        if (numSetChars < fInitialChars->size()) {
            REGEX_DUMP_DEBUG_PRINTF(" ...");
        }
        REGEX_DUMP_DEBUG_PRINTF("\n");

    } else if (fStartType == START_CHAR) {
        REGEX_DUMP_DEBUG_PRINTF("    First char of Match : ");
        if (0x20 < fInitialChar && fInitialChar<0x7e) {
                REGEX_DUMP_DEBUG_PRINTF("%c\n", fInitialChar);
            } else {
                REGEX_DUMP_DEBUG_PRINTF("%#x\n", fInitialChar);
            }
    }

    REGEX_DUMP_DEBUG_PRINTF("\nIndex   Binary     Type             Operand\n"
           "-------------------------------------------\n");
    for (index = 0; index<fCompiledPat->size(); index++) {
        dumpOp(index);
    }
    REGEX_DUMP_DEBUG_PRINTF("\n\n");
};



const char RegexPattern::fgClassID = 0;

//----------------------------------------------------------------------------------
//
//   regex_cleanup      Memory cleanup function, free/delete all
//                      cached memory.  Called by ICU's u_cleanup() function.
//
//----------------------------------------------------------------------------------
U_CFUNC UBool 
regex_cleanup(void) {
    RegexCompile::cleanup();
    return TRUE;
};

U_NAMESPACE_END
#endif  // !UCONFIG_NO_REGULAR_EXPRESSIONS
