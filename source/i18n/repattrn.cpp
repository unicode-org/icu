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
#include "unicode/regex.h"
#include "uassert.h"
#include "uvector.h"
#include "regexcmp.h"
#include "regeximp.h"

#include "stdio.h"    // TODO:  get rid of this...

U_NAMESPACE_BEGIN

//--------------------------------------------------------------------------
//
//    RegexPattern    Constructors and destructor
//
//--------------------------------------------------------------------------
RegexPattern::RegexPattern() {
    UErrorCode status = U_ZERO_ERROR;
    fFlags            = 0;
    fCompiledPat      = NULL;
    fSets             = NULL;
    fBadState         = FALSE;
    fNumCaptureGroups = 0;

    fCompiledPat      = new UVector(status);

    // fSets is a vector of all UnicodeSets built for this pattern.
    //   Reserve element 0, to allow a sanity check against refs to element 0.
    fSets             = new UVector(status);
    fSets->addElement((int32_t)0, status);

    if (U_FAILURE(status)) {
        fBadState = TRUE;
        delete fCompiledPat;
        delete fSets;
        fCompiledPat      = NULL;
        fSets             = NULL;
    }
};


RegexPattern::RegexPattern(const RegexPattern &other) :  UObject(other) {
    // TODO.   Need to add a reasonable assign or copy  constructor
    //         to UVector.
    U_ASSERT(FALSE);
};


RegexPattern::~RegexPattern() {
    delete fCompiledPat;
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
};

RegexPattern  *RegexPattern::clone() const { 
    RegexPattern  *copy = new RegexPattern(*this);
    return copy;
};

//---------------------------------------------------------------------
//
//   compile        
//
//---------------------------------------------------------------------
RegexPattern  *RegexPattern::compile(
                             const UnicodeString &regex,
                             uint32_t              flags,
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

    RegexCompile     compiler(err);
    compiler.compile(*This, regex, pe, err);

    return This;
};
    



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
uint32_t  RegexPattern::split(const UnicodeString &input,
        UnicodeString    dest[],
        uint32_t         destCapacity,
        UErrorCode       &err) const
{
    if (U_FAILURE(err)) {
        return 0;
    };
    // TODO:  
    return 0;
}



//---------------------------------------------------------------------
//
//   hashcode
//
//---------------------------------------------------------------------
int32_t   RegexPattern::hashCode(void) const {
    return 0;           // TODO:   Do something better here
};


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
        "?10",
        "SETREF",
        "DOTANY",
        "JMP",
        "FAIL"
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
            // Types with no operand field of interest.
            break;

        case URX_START_CAPTURE:
        case URX_END_CAPTURE:
        case URX_SETREF:
        case URX_STATE_SAVE:
        case URX_JMP:
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

U_NAMESPACE_END
