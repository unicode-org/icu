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
#include "regexcmp.h"
#include "regeximp.h"

#include "stdio.h"    // TODO:  get rid of this...

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
    fNumCaptureGroups = other.fNumCaptureGroups;
    fMaxCaptureDigits = other.fMaxCaptureDigits;
    fStaticSets       = other.fStaticSets;    
    if (fBadState) {
        return *this;
    }

    //  Copy the pattern.  It's just values, nothing deep to copy.
    int        i;
    UErrorCode status = U_ZERO_ERROR;
    for (i=0; i<other.fCompiledPat->size(); i++) {
        fCompiledPat->addElement(other.fCompiledPat->elementAti(i), status);
    }

    // Note:  do not copy fMatcher.  It'll be created on first use if the
    //        destination needs one. 

    //  Copy the Unicode Sets.  
    //    Could be made more efficient if the sets were reference counted and shared,
    //    but I doubt that pattern copying will be particularly common. 
    fSets->addElement((UnicodeSet *)NULL, status);
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
    fNumCaptureGroups = 0;
    fMaxCaptureDigits = 1;     // TODO:  calculate for real.
    fStaticSets       = NULL;
    fMatcher          = NULL;
    
    UErrorCode status=U_ZERO_ERROR;
    // Init of a completely new RegexPattern.
    fCompiledPat = new UVector(status);
    fSets        = new UVector(status);
    if (U_FAILURE(status) || fCompiledPat == NULL || fSets == NULL) {
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
    for (i=0; i<fSets->size(); i++) {
        UnicodeSet *s;
        s = (UnicodeSet *)fSets->elementAt(i);
        if (s != NULL) {
            delete s;
        }
    }
    delete fSets;
    fSets = NULL;
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
                             int32_t              flags,
                             UParseError          &pe,
                             UErrorCode           &err)  {

    if (U_FAILURE(err)) {
        return NULL;
    }
    RegexPattern *This = new RegexPattern;
    if (This == NULL) {
        err = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    This->fFlags = flags;

    RegexCompile     compiler(err);
    compiler.compile(*This, regex, pe, err);

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
int32_t RegexPattern::flags() const {
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

    if (U_FAILURE(err)) {return NULL;};

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

    UBool   retVal        = FALSE;
    RegexPattern *pat     = NULL;
    RegexMatcher *matcher = NULL;
    if (U_FAILURE(status)) {goto ret;}

    pat = RegexPattern::compile(regex, 0, pe, status);
    if (U_FAILURE(status)) {goto ret;}

    matcher = pat->matcher(input, status);
    if (U_FAILURE(status)) {goto ret;}

    retVal = matcher->matches(status);

ret:
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
//            TODO:  perl returns captured strings intermixed with the
//                   fields.  Should we do this too?
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
            if (nextOutputStringStart == inputLen) {
                // The delimiter was at the end of the string.  We're done.
                break;
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
static char *opNames[] = {
        "ZERO",
        "?1",
        "END",
        "ONECHAR",
        "STRING",
        "STRING_LEN",
        "STATE_SAVE",
        "NOP",
        "START_CAPTURE",
        "END_CAPTURE",
        "URX_STATIC_SETREF",
        "SETREF",
        "DOTANY",
        "JMP",
        "FAIL",
        "URX_BACKSLASH_A",
        "URX_BACKSLASH_B",
        "URX_BACKSLASH_G",
        "URX_BACKSLASH_W",
        "URX_BACKSLASH_X",
        "URX_BACKSLASH_Z",
        "URX_DOTANY_ALL",
        "URX_BACKSLASH_D",
        "URX_CARET",
        "URX_DOLLAR"
};

void   RegexPattern::dump() {
    int      index;
    int      i;
    UChar    c;
    int32_t  op;
    int32_t  pinnedType;
    int32_t  type;
    int32_t  val;
    int32_t  stringStart;


    printf("Original Pattern:  ");
    for (i=0; i<fPattern.length(); i++) {
        printf("%c", fPattern.charAt(i));
    }
    printf("\n");
    printf("Pattern Valid?:     %s\n", fBadState? "no" : "yes");
    printf("\nIndex   Binary     Type             Operand\n"
           "-------------------------------------------\n");
    for (index = 0; ; index++) {
        op         = fCompiledPat->elementAti(index);
        val        = URX_VAL(op);
        type       = URX_TYPE(op);
        pinnedType = type;
        if (pinnedType >= sizeof(opNames)/sizeof(char *)) {
            pinnedType = 0;
        }

        printf("%4d   %08x    %-15s  ", index, op, opNames[pinnedType]);
        switch (type) {
        case URX_NOP:
        case URX_DOTANY:
        case URX_FAIL:
        case URX_BACKSLASH_A:
        case URX_BACKSLASH_G:
        case URX_BACKSLASH_X:
            // Types with no operand field of interest.
            break;

        case URX_START_CAPTURE:
        case URX_END_CAPTURE:
        case URX_SETREF:
        case URX_STATIC_SETREF:
        case URX_STATE_SAVE:
        case URX_JMP:
        case URX_BACKSLASH_B:
        case URX_BACKSLASH_D:
        case URX_BACKSLASH_W:
        case URX_BACKSLASH_Z:
        case URX_CARET:
        case URX_DOLLAR:
            // types with an integer operand field.
            printf("%d", val);
            break;

        case URX_ONECHAR:
            printf("%c", val<256?val:'?');
            break;

        case URX_STRING:
            stringStart = val;
            break;

        case URX_STRING_LEN:
            for (i=stringStart; i<stringStart+val; i++) {
                c = fLiteralText[i];
                if (c >= 256) {c = '?';};
                printf("%c", c);
            }
            break;
            
        case URX_END:
            goto breakFromLoop;
            
        default:
            printf("??????");
            break;
        }
        printf("\n");
    }
breakFromLoop:
    printf("\n\n");
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
