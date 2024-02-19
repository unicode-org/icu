// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  genuts46.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2010mar02
*   created by: Markus W. Scherer
*
* quick & dirty tool to recreate the UTS #46 data table according to the spec
*/

#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/errorcode.h"
#include "unicode/normalizer2.h"
#include "unicode/uniset.h"
#include "unicode/unistr.h"
#include "unicode/usetiter.h"
#include "unicode/usprep.h"
#include "sprpimpl.h"  // HACK

/**
 * icu::ErrorCode subclass for easy UErrorCode handling.
 * The destructor calls handleFailure() which calls exit(errorCode) when isFailure().
 */
class ExitingErrorCode : public icu::ErrorCode {
public:
    /**
     * @param loc A short string describing where the ExitingErrorCode is used.
     */
    ExitingErrorCode(const char *loc) : location(loc) {}
    virtual ~ExitingErrorCode();
protected:
    virtual void handleFailure() const;
private:
    const char *location;
};

ExitingErrorCode::~ExitingErrorCode() {
    // Safe because our handleFailure() does not throw exceptions.
    if(isFailure()) { handleFailure(); }
}

void ExitingErrorCode::handleFailure() const {
    fprintf(stderr, "error at %s: %s\n", location, errorName());
    exit(errorCode);
}

static int
toIDNA2003(const UStringPrepProfile *prep, UChar32 c, icu::UnicodeString &destString) {
    char16_t src[2];
    int32_t srcLength=0;
    U16_APPEND_UNSAFE(src, srcLength, c);
    char16_t *dest;
    int32_t destLength;
    dest=destString.getBuffer(32);
    if(dest==nullptr) {
        return false;
    }
    UErrorCode errorCode=U_ZERO_ERROR;
    destLength=usprep_prepare(prep, src, srcLength,
                              dest, destString.getCapacity(),
                              USPREP_DEFAULT, nullptr, &errorCode);
    destString.releaseBuffer(destLength);
    if(errorCode==U_STRINGPREP_PROHIBITED_ERROR) {
        return -1;
    } else {
        // Returns false=0 for U_STRINGPREP_UNASSIGNED_ERROR and processing errors,
        // true=1 if c is valid or mapped.
        return U_SUCCESS(errorCode);
    }
}

enum Status {
    DISALLOWED, IGNORED, MAPPED, DEVIATION, VALID,
    DISALLOWED_STD3_VALID, DISALLOWED_STD3_MAPPED
};
static const char *const statusNames[]={
    "disallowed", "ignored", "mapped", "deviation", "valid",
    "disallowed_STD3_valid", "disallowed_STD3_mapped"
};

static void
printLine(UChar32 start, UChar32 end, Status status, const icu::UnicodeString &mapping) {
    if(start==end) {
        printf("%04lX          ", (long)start);
    } else {
        printf("%04lX..%04lX    ", (long)start, (long)end);
    }
    printf("; %s", statusNames[status]);
    if(status==MAPPED || status==DEVIATION || !mapping.isEmpty()) {
        printf(" ;");
        const char16_t *buffer=mapping.getBuffer();
        int32_t length=mapping.length();
        int32_t i=0;
        UChar32 c;
        while(i<length) {
            U16_NEXT(buffer, i, length, c);
            printf(" %04lX", (long)c);
        }
    }
    puts("");
}

static void
getAgeIfAssigned(UChar32 c, UVersionInfo age) {
    if(u_isdefined(c)) {
        u_charAge(c, age);
    } else if(U_IS_UNICODE_NONCHAR(c)) {
        age[0]=0;
        age[1]=0;
        age[2]=0;
        age[3]=1;
    } else {
        memset(age, 0, 4);
    }
}

extern int
main(int argc, const char *argv[]) {
    ExitingErrorCode errorCode("genuts46");

    // predefined base sets
    icu::UnicodeSet unassignedSet(UNICODE_STRING_SIMPLE("[:Cn:]"), errorCode);

    icu::UnicodeSet labelSeparators(
        UNICODE_STRING_SIMPLE("[\\u002E\\u3002\\uFF0E\\uFF61]"), errorCode);

    icu::UnicodeSet mappedSet(
        UNICODE_STRING_SIMPLE("[:Changes_When_NFKC_Casefolded:]"), errorCode);
    mappedSet.removeAll(labelSeparators);  // simplifies checking of mapped characters

    icu::UnicodeSet baseValidSet(icu::UnicodeString(
        "[[[[:^Changes_When_NFKC_Casefolded:]"
        "-[:C:]-[:Z:]"
        "-[:Block=Ideographic_Description_Characters:]]"
        "[:ascii:]]-[.]]", -1, US_INV), errorCode);

    // Characters that are disallowed when STD3 rules are applied,
    // but valid when STD3 rules are not applied.
    icu::UnicodeSet disallowedSTD3Set(icu::UnicodeString(
        "[[:ascii:]-[\\u002D.a-zA-Z0-9]]", -1, US_INV), errorCode);

    icu::UnicodeSet deviationSet(
        UNICODE_STRING_SIMPLE("[\\u00DF\\u03C2\\u200C\\u200D]"), errorCode);
    errorCode.assertSuccess();

    // derived sets
    icu::LocalUStringPrepProfilePointer namePrep(usprep_openByType(USPREP_RFC3491_NAMEPREP, errorCode));
    const icu::Normalizer2 *nfkc_cf=
        icu::Normalizer2::getInstance(nullptr, "nfkc_cf", UNORM2_COMPOSE, errorCode);
    errorCode.assertSuccess();

    // HACK: The StringPrep API performs a BiDi check according to the data.
    // We need to override that for this data generation, by resetting an internal flag.
    namePrep->checkBiDi=false;

    icu::UnicodeSet baseExclusionSet;
    icu::UnicodeString cString, mapping, namePrepResult;
    for(UChar32 c=0; c<=0x10ffff; ++c) {
        if(c==0xd800) {
            c=0xe000;
        }
        int namePrepStatus=toIDNA2003(namePrep.getAlias(), c, namePrepResult);
        if(namePrepStatus!=0) {
            // get the UTS #46 base mapping value
            switch(c) {
            case 0xff0e:
            case 0x3002:
            case 0xff61:
                mapping.setTo(0x2e);
                break;
            default:
                cString.setTo(c);
                nfkc_cf->normalize(cString, mapping, errorCode);
                break;
            }
            if(
                namePrepStatus>0 ?
                    // c is valid or mapped in IDNA2003
                    !labelSeparators.contains(c) && namePrepResult!=mapping :
                    // namePrepStatus<0: c is prohibited in IDNA2003
                    baseValidSet.contains(c) || (cString!=mapping && baseValidSet.containsAll(mapping))
            ) {
                baseExclusionSet.add(c);
            }
        }
    }

    icu::UnicodeSet disallowedSet(0, 0x10ffff);
    disallowedSet.
        removeAll(labelSeparators).
        removeAll(deviationSet).
        removeAll(mappedSet).
        removeAll(baseValidSet).
        addAll(baseExclusionSet).
        addAll(unassignedSet);

    const icu::Normalizer2 *nfd=
        icu::Normalizer2::getInstance(nullptr, "nfc", UNORM2_DECOMPOSE, errorCode);
    errorCode.assertSuccess();

    icu::UnicodeSet ignoredSet;  // will be a subset of mappedSet
    icu::UnicodeSet removeSet;
    icu::UnicodeString nfdString;
    {
        icu::UnicodeSetIterator iter(mappedSet);
        while(iter.next()) {
            UChar32 c=iter.getCodepoint();
            cString.setTo(c);
            nfkc_cf->normalize(cString, mapping, errorCode);
            if(!baseValidSet.containsAll(mapping)) {
                fprintf(stderr, "U+%04lX mapped -> disallowed: mapping not wholly in base valid set\n", (long)c);
                disallowedSet.add(c);
                removeSet.add(c);
            } else if(mapping.isEmpty()) {
                ignoredSet.add(c);
            }
        }
        mappedSet.removeAll(removeSet);
    }
    errorCode.assertSuccess();

    icu::UnicodeSet validSet(baseValidSet);
    validSet.
        removeAll(labelSeparators).  // non-ASCII label separators will be mapped in the end
        removeAll(deviationSet).
        removeAll(disallowedSet).
        removeAll(mappedSet).
        add(0x2e);  // not mapped, simply valid
    UBool madeChange;
    do {
        madeChange=false;
        {
            removeSet.clear();
            icu::UnicodeSetIterator iter(validSet);
            while(iter.next()) {
                UChar32 c=iter.getCodepoint();
                if(nfd->getDecomposition(c, nfdString) && !validSet.containsAll(nfdString)) {
                    fprintf(stderr, "U+%04lX valid -> disallowed: NFD not wholly valid\n", (long)c);
                    disallowedSet.add(c);
                    removeSet.add(c);
                    madeChange=true;
                }
            }
            validSet.removeAll(removeSet);
        }
        {
            removeSet.clear();
            icu::UnicodeSetIterator iter(mappedSet);
            while(iter.next()) {
                UChar32 c=iter.getCodepoint();
                cString.setTo(c);
                nfkc_cf->normalize(cString, mapping, errorCode);
                nfd->normalize(mapping, nfdString, errorCode);
                if(!validSet.containsAll(nfdString)) {
                    fprintf(stderr, "U+%04lX mapped -> disallowed: NFD of mapping not wholly valid\n", (long)c);
                    disallowedSet.add(c);
                    removeSet.add(c);
                    madeChange=true;
                }
            }
            mappedSet.removeAll(removeSet);
        }
    } while(madeChange);
    errorCode.assertSuccess();

    // finish up
    labelSeparators.remove(0x2e).freeze();  // U+002E is simply valid
    deviationSet.freeze();
    ignoredSet.freeze();
    validSet.freeze();
    mappedSet.freeze();
    disallowedSTD3Set.freeze();

    // output
    UChar32 prevStart=0, c=0;
    Status prevStatus=DISALLOWED_STD3_VALID, status;
    icu::UnicodeString prevMapping;
    UVersionInfo prevAge={ 1, 1, 0, 0 }, age;

    icu::UnicodeSetIterator iter(disallowedSet);
    while(iter.nextRange()) {
        UChar32 start=iter.getCodepoint();
        while(c<start) {
            mapping.remove();
            if(labelSeparators.contains(c)) {
                status=MAPPED;
                mapping.setTo(0x2e);
            } else if(deviationSet.contains(c)) {
                status=DEVIATION;
                cString.setTo(c);
                nfkc_cf->normalize(cString, mapping, errorCode);
            } else if(ignoredSet.contains(c)) {
                status=IGNORED;
            } else if(validSet.contains(c)) {
                if(disallowedSTD3Set.contains(c)) {
                    fprintf(stderr, "U+%04lX valid -> disallowed_STD3_valid: itself not STD3\n", (long)c);
                    status=DISALLOWED_STD3_VALID;
                } else if( nfd->getDecomposition(c, nfdString) &&
                    disallowedSTD3Set.containsSome(nfdString)
                ) {
                    fprintf(stderr, "U+%04lX valid -> disallowed_STD3_valid: NFD not wholly STD3\n", (long)c);
                    status=DISALLOWED_STD3_VALID;
                } else {
                    status=VALID;
                }
            } else if(mappedSet.contains(c)) {
                cString.setTo(c);
                nfkc_cf->normalize(cString, mapping, errorCode);
                if(disallowedSTD3Set.containsSome(mapping)) {
                    fprintf(stderr, "U+%04lX mapped -> disallowed_STD3_mapped\n", (long)c);
                    status=DISALLOWED_STD3_MAPPED;
                } else {
                    status=MAPPED;
                }
            } else {
                fprintf(stderr, "*** undetermined status of U+%04lX\n", (long)c);
            }
            // Print a new line where the status, the mapping or
            // the character age change.
            getAgeIfAssigned(c, age);
            if( prevStart<c &&
                (status!=prevStatus || mapping!=prevMapping || 0!=memcmp(prevAge, age, 4))
            ) {
                printLine(prevStart, c-1, prevStatus, prevMapping);
                prevStart=c;
                prevStatus=status;
                prevMapping=mapping;
                memcpy(prevAge, age, 4);
            }
            ++c;
        }
        // c==start is disallowed
        if(prevStart<c) {
            printLine(prevStart, c-1, prevStatus, prevMapping);
        }
        prevStart=c;
        prevStatus=DISALLOWED;
        prevMapping.remove();
        getAgeIfAssigned(c, prevAge);
        UChar32 end=iter.getCodepointEnd();
        while(++c<=end) {
            getAgeIfAssigned(c, age);
            if(prevStart<c && 0!=memcmp(prevAge, age, 4)) {
                printLine(prevStart, c-1, prevStatus, prevMapping);
                prevStart=c;
                memcpy(prevAge, age, 4);
            }
        }
    }
    if(prevStart<c) {
        printLine(prevStart, c-1, prevStatus, prevMapping);
    }
    return 0;
}
