// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2009-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  n2builder.cpp
*   encoding:   UTF-8
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009nov25
*   created by: Markus W. Scherer
*
* Builds Normalizer2 data and writes a binary .nrm file.
* For the file format see source/common/normalizer2impl.h.
*/

#include "unicode/utypes.h"
#include "n2builder.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <vector>
#include "unicode/errorcode.h"
#include "unicode/localpointer.h"
#include "unicode/putil.h"
#include "unicode/udata.h"
#include "unicode/unistr.h"
#include "unicode/ustring.h"
#include "charstr.h"
#include "extradata.h"
#include "hash.h"
#include "normalizer2impl.h"
#include "norms.h"
#include "toolutil.h"
#include "unewdata.h"
#include "utrie2.h"
#include "uvectr32.h"
#include "writesrc.h"

#if !UCONFIG_NO_NORMALIZATION

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0x4e, 0x72, 0x6d, 0x32 }, /* dataFormat="Nrm2" */
    { 2, 0, 0, 0 },             /* formatVersion */
    { 5, 2, 0, 0 }              /* dataVersion (Unicode version) */
};

U_NAMESPACE_BEGIN

class HangulIterator {
public:
    struct Range {
        UChar32 start, limit;
        uint16_t norm16;
    };

    HangulIterator() : rangeIndex(0) {}
    const Range *nextRange() {
        if(rangeIndex<UPRV_LENGTHOF(ranges)) {
            return ranges+rangeIndex++;
        } else {
            return NULL;
        }
    }
    void reset() { rangeIndex=0; }
private:
    static const Range ranges[4];
    int32_t rangeIndex;
};

const HangulIterator::Range HangulIterator::ranges[4]={
    { Hangul::JAMO_L_BASE, Hangul::JAMO_L_BASE+Hangul::JAMO_L_COUNT, 1 },
    { Hangul::JAMO_V_BASE, Hangul::JAMO_V_BASE+Hangul::JAMO_V_COUNT, Normalizer2Impl::JAMO_VT },
    // JAMO_T_BASE+1: not U+11A7
    { Hangul::JAMO_T_BASE+1, Hangul::JAMO_T_BASE+Hangul::JAMO_T_COUNT, Normalizer2Impl::JAMO_VT },
    { Hangul::HANGUL_BASE, Hangul::HANGUL_BASE+Hangul::HANGUL_COUNT, 0 },  // will become minYesNo
};

Normalizer2DataBuilder::Normalizer2DataBuilder(UErrorCode &errorCode) :
        norms(errorCode),
        phase(0), overrideHandling(OVERRIDE_PREVIOUS), optimization(OPTIMIZE_NORMAL),
        norm16Trie(nullptr), norm16TrieLength(0) {
    memset(unicodeVersion, 0, sizeof(unicodeVersion));
    memset(indexes, 0, sizeof(indexes));
    memset(smallFCD, 0, sizeof(smallFCD));
}

Normalizer2DataBuilder::~Normalizer2DataBuilder() {
    utrie2_close(norm16Trie);
}

void
Normalizer2DataBuilder::setUnicodeVersion(const char *v) {
    UVersionInfo nullVersion={ 0, 0, 0, 0 };
    UVersionInfo version;
    u_versionFromString(version, v);
    if( 0!=memcmp(version, unicodeVersion, U_MAX_VERSION_LENGTH) &&
        0!=memcmp(nullVersion, unicodeVersion, U_MAX_VERSION_LENGTH)
    ) {
        char buffer[U_MAX_VERSION_STRING_LENGTH];
        u_versionToString(unicodeVersion, buffer);
        fprintf(stderr, "gennorm2 error: multiple inconsistent Unicode version numbers %s vs. %s\n",
                buffer, v);
        exit(U_ILLEGAL_ARGUMENT_ERROR);
    }
    memcpy(unicodeVersion, version, U_MAX_VERSION_LENGTH);
}

Norm *Normalizer2DataBuilder::checkNormForMapping(Norm *p, UChar32 c) {
    if(p!=NULL) {
        if(p->mappingType!=Norm::NONE) {
            if( overrideHandling==OVERRIDE_NONE ||
                (overrideHandling==OVERRIDE_PREVIOUS && p->mappingPhase==phase)
            ) {
                fprintf(stderr,
                        "error in gennorm2 phase %d: "
                        "not permitted to override mapping for U+%04lX from phase %d\n",
                        (int)phase, (long)c, (int)p->mappingPhase);
                exit(U_INVALID_FORMAT_ERROR);
            }
            delete p->mapping;
            p->mapping=NULL;
        }
        p->mappingPhase=phase;
    }
    return p;
}

void Normalizer2DataBuilder::setOverrideHandling(OverrideHandling oh) {
    overrideHandling=oh;
    ++phase;
}

void Normalizer2DataBuilder::setCC(UChar32 c, uint8_t cc) {
    norms.createNorm(c)->cc=cc;
}

static UBool isWellFormed(const UnicodeString &s) {
    UErrorCode errorCode=U_ZERO_ERROR;
    u_strToUTF8(NULL, 0, NULL, toUCharPtr(s.getBuffer()), s.length(), &errorCode);
    return U_SUCCESS(errorCode) || errorCode==U_BUFFER_OVERFLOW_ERROR;
}

void Normalizer2DataBuilder::setOneWayMapping(UChar32 c, const UnicodeString &m) {
    if(!isWellFormed(m)) {
        fprintf(stderr,
                "error in gennorm2 phase %d: "
                "illegal one-way mapping from U+%04lX to malformed string\n",
                (int)phase, (long)c);
        exit(U_INVALID_FORMAT_ERROR);
    }
    Norm *p=checkNormForMapping(norms.createNorm(c), c);
    p->mapping=new UnicodeString(m);
    p->mappingType=Norm::ONE_WAY;
    p->setMappingCP();
}

void Normalizer2DataBuilder::setRoundTripMapping(UChar32 c, const UnicodeString &m) {
    if(U_IS_SURROGATE(c)) {
        fprintf(stderr,
                "error in gennorm2 phase %d: "
                "illegal round-trip mapping from surrogate code point U+%04lX\n",
                (int)phase, (long)c);
        exit(U_INVALID_FORMAT_ERROR);
    }
    if(!isWellFormed(m)) {
        fprintf(stderr,
                "error in gennorm2 phase %d: "
                "illegal round-trip mapping from U+%04lX to malformed string\n",
                (int)phase, (long)c);
        exit(U_INVALID_FORMAT_ERROR);
    }
    int32_t numCP=u_countChar32(toUCharPtr(m.getBuffer()), m.length());
    if(numCP!=2) {
        fprintf(stderr,
                "error in gennorm2 phase %d: "
                "illegal round-trip mapping from U+%04lX to %d!=2 code points\n",
                (int)phase, (long)c, (int)numCP);
        exit(U_INVALID_FORMAT_ERROR);
    }
    Norm *p=checkNormForMapping(norms.createNorm(c), c);
    p->mapping=new UnicodeString(m);
    p->mappingType=Norm::ROUND_TRIP;
    p->mappingCP=U_SENTINEL;
}

void Normalizer2DataBuilder::removeMapping(UChar32 c) {
    Norm *p=checkNormForMapping(norms.getNorm(c), c);
    if(p!=NULL) {
        p->mappingType=Norm::REMOVED;
    }
}

UBool Normalizer2DataBuilder::hasNoCompBoundaryAfter(const BuilderReorderingBuffer &buffer) {
    if(buffer.isEmpty()) {
        return TRUE;  // maps-to-empty-string is no boundary of any kind
    }
    int32_t lastStarterIndex=buffer.lastStarterIndex();
    if(lastStarterIndex<0) {
        return TRUE;  // no starter
    }
    UChar32 starter=buffer.charAt(lastStarterIndex);
    if(Hangul::isJamoL(starter) ||
            (Hangul::isJamoV(starter) &&
            0<lastStarterIndex && Hangul::isJamoL(buffer.charAt(lastStarterIndex-1)))) {
        // A Jamo leading consonant or an LV pair combines-forward if it is at the end,
        // otherwise it is blocked.
        return lastStarterIndex==buffer.length()-1;
    }
    // Note: There can be no Hangul syllable in the fully decomposed mapping.
    const Norm *starterNorm=&norms.getNormRef(starter);
    if(starterNorm->compositions==NULL) {
        return FALSE;  // the last starter does not combine forward
    }
    // Compose as far as possible, and see if further compositions are possible.
    uint8_t prevCC=0;
    for(int32_t combMarkIndex=lastStarterIndex+1; combMarkIndex<buffer.length(); ++combMarkIndex) {
        uint8_t cc=buffer.ccAt(combMarkIndex);  // !=0 because after last starter
        if(norms.combinesWithCCBetween(*starterNorm, prevCC, cc)) {
            return TRUE;
        }
        if(prevCC<cc && (starter=starterNorm->combine(buffer.charAt(combMarkIndex)))>=0) {
            starterNorm=&norms.getNormRef(starter);
            if(starterNorm->compositions==NULL) {
                return FALSE;  // the composite does not combine further
            }
            // Keep prevCC because we "removed" the combining mark.
        } else {
            prevCC=cc;
        }
    }
    // TRUE if the final, forward-combining starter is at the end.
    return prevCC==0;
    // TODO?! prevCC==0 || norms.combinesWithCCBetween(*starterNorm, prevCC, int32_t! 0x100)
    // TODO?! actually, should check if it combines with any cc not seen here
}

void Normalizer2DataBuilder::postProcess(Norm &norm) {
    // Prerequisites: Compositions are built, mappings are recursively decomposed.
    // Mappings are not yet in canonical order.
    //
    // This function works on a Norm struct. We do not know which code point(s) map(s) to it.
    // Therefore, we cannot compute algorithmic mapping deltas here.
    // Error conditions are checked, but printed later when we do know the offending code point.
    if(norm.hasMapping()) {
        // Ensure canonical order.
        BuilderReorderingBuffer buffer;
        if(norm.rawMapping!=nullptr) {
            norms.reorder(*norm.rawMapping, buffer);
            buffer.reset();
        }
        norms.reorder(*norm.mapping, buffer);
        if(buffer.isEmpty()) {
            norm.leadCC=norm.trailCC=0;
        } else {
            norm.leadCC=buffer.ccAt(0);
            norm.trailCC=buffer.ccAt(buffer.length()-1);
        }

        // Set the hasNoCompBoundaryAfter flag for use by the last code branch
        // in Normalizer2Impl::hasCompBoundaryAfter().
        // For details see the comments on hasNoCompBoundaryAfter(buffer).
        if(norm.compositions!=nullptr) {
            norm.hasNoCompBoundaryAfter=TRUE;
        } else {
            norm.hasNoCompBoundaryAfter=hasNoCompBoundaryAfter(buffer);
        }

        if(norm.combinesBack) {
            norm.error="combines-back and decomposes, not possible in Unicode normalization";
        } else if(norm.mappingType==Norm::ROUND_TRIP) {
            if(norm.compositions!=NULL) {
                norm.type=Norm::YES_NO_COMBINES_FWD;
            } else {
                norm.type=Norm::YES_NO_MAPPING_ONLY;
            }
        } else {  // one-way mapping
            if(norm.compositions!=NULL) {
                norm.error="combines-forward and has a one-way mapping, "
                           "not possible in Unicode normalization";
            } else {
                norm.type=Norm::NO_NO;
            }
        }
    } else {  // no mapping
        norm.leadCC=norm.trailCC=norm.cc;

        if(norm.combinesBack) {
            if(norm.compositions!=nullptr) {
                // Earlier code checked ccc=0.
                norm.type=Norm::MAYBE_YES_COMBINES_FWD;
            } else {
                norm.type=Norm::MAYBE_YES_SIMPLE;  // any ccc
            }
        } else if(norm.compositions!=nullptr) {
            // Earlier code checked ccc=0.
            norm.type=Norm::YES_YES_COMBINES_FWD;
        } else if(norm.cc!=0) {
            norm.type=Norm::YES_YES_WITH_CC;
        } else {
            norm.type=Norm::INERT;
        }
    }
}

class Norm16Writer : public Norms::Enumerator {
public:
    Norm16Writer(Norms &n, Normalizer2DataBuilder &b) : Norms::Enumerator(n), builder(b) {}
    void rangeHandler(UChar32 start, UChar32 end, Norm &norm) override {
        builder.writeNorm16(start, end, norm);
    }
    Normalizer2DataBuilder &builder;
};

void Normalizer2DataBuilder::setSmallFCD(UChar32 c) {
    UChar32 lead= c<=0xffff ? c : U16_LEAD(c);
    smallFCD[lead>>8]|=(uint8_t)1<<((lead>>5)&7);
}

void Normalizer2DataBuilder::writeNorm16(UChar32 start, UChar32 end, Norm &norm) {
    if(start<Normalizer2Impl::MIN_CCC_LCCC_CP && (norm.cc!=0 || norm.leadCC!=0)) {
        fprintf(stderr,
                "gennorm2 error: "
                "U+%04lX below U+0300 has ccc!=0 or lccc!=0, not supported by ICU\n",
                (long)start);
        exit(U_INVALID_FORMAT_ERROR);
    }
    if((norm.leadCC|norm.trailCC)!=0) {
        for(UChar32 c=start; c<=end; ++c) {
            setSmallFCD(c);
        }
    }

    int32_t norm16;
    switch(norm.type) {
    case Norm::INERT:
        norm16=0;
        break;
    case Norm::YES_YES_COMBINES_FWD:
        norm16=norm.offset;
        break;
    case Norm::YES_NO_COMBINES_FWD:
        norm16=indexes[Normalizer2Impl::IX_MIN_YES_NO]+norm.offset;
        break;
    case Norm::YES_NO_MAPPING_ONLY:
        norm16=indexes[Normalizer2Impl::IX_MIN_YES_NO_MAPPINGS_ONLY]+norm.offset;
        break;
        // TODO: minMappingNotCompYes, minMappingNoCompBoundaryBefore
    case Norm::NO_NO:
        norm16=indexes[Normalizer2Impl::IX_MIN_NO_NO]+norm.offset;
        break;
    case Norm::NO_NO_DELTA:
        norm16=getCenterNoNoDelta()+norm.offset;
        break;
    case Norm::MAYBE_YES_COMBINES_FWD:
        norm16=indexes[Normalizer2Impl::IX_MIN_MAYBE_YES]+norm.offset;
        break;
    case Norm::MAYBE_YES_SIMPLE:
        norm16=Normalizer2Impl::MIN_NORMAL_MAYBE_YES+norm.cc;  // ccc=0..255
        break;
    case Norm::YES_YES_WITH_CC:
        U_ASSERT(norm.cc!=0);
        norm16=Normalizer2Impl::MIN_YES_YES_WITH_CC-1+norm.cc;  // ccc=1..255
        break;
    default:  // Should not occur.
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
    IcuToolErrorCode errorCode("gennorm2/writeNorm16()");
    utrie2_setRange32(norm16Trie, start, end, (uint32_t)norm16, TRUE, errorCode);

    // Set the minimum code points for real data lookups in the quick check loops.
    UBool isDecompNo=
            (Norm::YES_NO_COMBINES_FWD<=norm.type && norm.type<=Norm::NO_NO_DELTA) ||
            norm.cc!=0;
    if(isDecompNo && start<indexes[Normalizer2Impl::IX_MIN_DECOMP_NO_CP]) {
        indexes[Normalizer2Impl::IX_MIN_DECOMP_NO_CP]=start;
    }
    UBool isCompNoMaybe= norm.type>=Norm::NO_NO;
    if(isCompNoMaybe && start<indexes[Normalizer2Impl::IX_MIN_COMP_NO_MAYBE_CP]) {
        indexes[Normalizer2Impl::IX_MIN_COMP_NO_MAYBE_CP]=start;
    }
}

void Normalizer2DataBuilder::setHangulData() {
    HangulIterator hi;
    const HangulIterator::Range *range;
    // Check that none of the Hangul/Jamo code points have data.
    while((range=hi.nextRange())!=NULL) {
        for(UChar32 c=range->start; c<range->limit; ++c) {
            if(utrie2_get32(norm16Trie, c)!=0) {
                fprintf(stderr,
                        "gennorm2 error: "
                        "illegal mapping/composition/ccc data for Hangul or Jamo U+%04lX\n",
                        (long)c);
                exit(U_INVALID_FORMAT_ERROR);
            }
        }
    }
    // Set data for algorithmic runtime handling.
    IcuToolErrorCode errorCode("gennorm2/setHangulData()");
    hi.reset();
    while((range=hi.nextRange())!=NULL) {
        uint16_t norm16=range->norm16;
        if(norm16==0) {
            norm16=(uint16_t)indexes[Normalizer2Impl::IX_MIN_YES_NO];  // Hangul LV/LVT encoded as minYesNo
            if(range->start<indexes[Normalizer2Impl::IX_MIN_DECOMP_NO_CP]) {
                indexes[Normalizer2Impl::IX_MIN_DECOMP_NO_CP]=range->start;
            }
        } else {
            if(range->start<indexes[Normalizer2Impl::IX_MIN_COMP_NO_MAYBE_CP]) {  // Jamo V/T are maybeYes
                indexes[Normalizer2Impl::IX_MIN_COMP_NO_MAYBE_CP]=range->start;
            }
        }
        utrie2_setRange32(norm16Trie, range->start, range->limit-1, norm16, TRUE, errorCode);
        errorCode.assertSuccess();
    }
}

U_CDECL_BEGIN

static UBool U_CALLCONV
enumRangeMaxValue(const void *context, UChar32 /*start*/, UChar32 /*end*/, uint32_t value) {
    uint32_t *pMaxValue=(uint32_t *)context;
    if(value>*pMaxValue) {
        *pMaxValue=value;
    }
    return TRUE;
}

U_CDECL_END

void Normalizer2DataBuilder::processData() {
    IcuToolErrorCode errorCode("gennorm2/processData()");
    norm16Trie=utrie2_open(0, 0, errorCode);
    errorCode.assertSuccess();

    // Build composition lists before recursive decomposition,
    // so that we still have the raw, pair-wise mappings.
    CompositionBuilder compBuilder(norms);
    norms.enumRanges(compBuilder);

    // Recursively decompose all mappings.
    Decomposer decomposer(norms);
    do {
        decomposer.didDecompose=FALSE;
        norms.enumRanges(decomposer);
    } while(decomposer.didDecompose);

    // Set the Norm::Type and other properties.
    int32_t normsLength=norms.length();
    for(int32_t i=1; i<normsLength; ++i) {
        postProcess(norms.getNormRefByIndex(i));
    }

    // Write the properties, mappings and composition lists to
    // appropriate parts of the "extra data" array.
    ExtraData extra(norms, optimization==OPTIMIZE_FAST);
    norms.enumRanges(extra);

    extraData=extra.yesYesCompositions;
    indexes[Normalizer2Impl::IX_MIN_YES_NO]=extraData.length();
    extraData.append(extra.yesNoMappingsAndCompositions);
    indexes[Normalizer2Impl::IX_MIN_YES_NO_MAPPINGS_ONLY]=extraData.length();
    extraData.append(extra.yesNoMappingsOnly);
    // TODO: minMappingNotCompYes, minMappingNoCompBoundaryBefore
    indexes[Normalizer2Impl::IX_MIN_NO_NO]=extraData.length();
    extraData.append(extra.noNoMappings);
    indexes[Normalizer2Impl::IX_LIMIT_NO_NO]=extraData.length();

    extraData.insert(0, extra.maybeYesCompositions);
    indexes[Normalizer2Impl::IX_MIN_MAYBE_YES]=
        Normalizer2Impl::MIN_NORMAL_MAYBE_YES-
        extra.maybeYesCompositions.length();

    // Pad to even length for 4-byte alignment of following data.
    if(extraData.length()&1) {
        extraData.append((UChar)0);
    }

    int32_t minNoNoDelta=getCenterNoNoDelta()-Normalizer2Impl::MAX_DELTA;
    if(indexes[Normalizer2Impl::IX_LIMIT_NO_NO]>minNoNoDelta) {
        fprintf(stderr,
                "gennorm2 error: "
                "data structure overflow, too much mapping composition data\n");
        exit(U_BUFFER_OVERFLOW_ERROR);
    }

    // writeNorm16() and setHangulData() reduce these as needed.
    indexes[Normalizer2Impl::IX_MIN_DECOMP_NO_CP]=0x110000;
    indexes[Normalizer2Impl::IX_MIN_COMP_NO_MAYBE_CP]=0x110000;

    // Map each code point to its norm16 value,
    // including the properties that fit directly,
    // and the offset to the "extra data" if necessary.
    Norm16Writer norm16Writer(norms, *this);
    norms.enumRanges(norm16Writer);

    setHangulData();

    // Look for the "worst" norm16 value of any supplementary code point
    // corresponding to a lead surrogate, and set it as that surrogate's value.
    // Enables UTF-16 quick check inner loops to look at only code units.
    //
    // We could be more sophisticated:
    // We could collect a bit set for whether there are values in the different
    // norm16 ranges (yesNo, maybeYes, yesYesWithCC etc.)
    // and select the best value that only breaks the composition and/or decomposition
    // inner loops if necessary.
    // However, that seems like overkill for an optimization for supplementary characters.
    for(UChar lead=0xd800; lead<0xdc00; ++lead) {
        uint32_t maxValue=utrie2_get32(norm16Trie, lead);
        utrie2_enumForLeadSurrogate(norm16Trie, lead, NULL, enumRangeMaxValue, &maxValue);
        if( maxValue>=(uint32_t)indexes[Normalizer2Impl::IX_LIMIT_NO_NO] &&
            maxValue>(uint32_t)indexes[Normalizer2Impl::IX_MIN_NO_NO]
        ) {
            // Set noNo ("worst" value) if it got into "less-bad" maybeYes or ccc!=0.
            // Otherwise it might end up at something like JAMO_VT which stays in
            // the inner decomposition quick check loop.
            maxValue=(uint32_t)indexes[Normalizer2Impl::IX_LIMIT_NO_NO]-1;
        }
        utrie2_set32ForLeadSurrogateCodeUnit(norm16Trie, lead, maxValue, errorCode);
    }

    // Adjust supplementary minimum code points to break quick check loops at their lead surrogates.
    // For an empty data file, minCP=0x110000 turns into 0xdc00 (first trail surrogate)
    // which is harmless.
    // As a result, the minimum code points are always BMP code points.
    int32_t minCP=indexes[Normalizer2Impl::IX_MIN_DECOMP_NO_CP];
    if(minCP>=0x10000) {
        indexes[Normalizer2Impl::IX_MIN_DECOMP_NO_CP]=U16_LEAD(minCP);
    }
    minCP=indexes[Normalizer2Impl::IX_MIN_COMP_NO_MAYBE_CP];
    if(minCP>=0x10000) {
        indexes[Normalizer2Impl::IX_MIN_COMP_NO_MAYBE_CP]=U16_LEAD(minCP);
    }

    utrie2_freeze(norm16Trie, UTRIE2_16_VALUE_BITS, errorCode);
    norm16TrieLength=utrie2_serialize(norm16Trie, NULL, 0, errorCode);
    if(errorCode.get()!=U_BUFFER_OVERFLOW_ERROR) {
        fprintf(stderr, "gennorm2 error: unable to freeze/serialize the normalization trie - %s\n",
                errorCode.errorName());
        exit(errorCode.reset());
    }
    errorCode.reset();

    int32_t offset=(int32_t)sizeof(indexes);
    indexes[Normalizer2Impl::IX_NORM_TRIE_OFFSET]=offset;
    offset+=norm16TrieLength;
    indexes[Normalizer2Impl::IX_EXTRA_DATA_OFFSET]=offset;
    offset+=extraData.length()*2;
    indexes[Normalizer2Impl::IX_SMALL_FCD_OFFSET]=offset;
    offset+=sizeof(smallFCD);
    int32_t totalSize=offset;
    for(int32_t i=Normalizer2Impl::IX_RESERVED3_OFFSET; i<=Normalizer2Impl::IX_TOTAL_SIZE; ++i) {
        indexes[i]=totalSize;
    }

    if(beVerbose) {
        printf("size of normalization trie:         %5ld bytes\n", (long)norm16TrieLength);
        printf("size of 16-bit extra data:          %5ld uint16_t\n", (long)extraData.length());
        printf("size of small-FCD data:             %5ld bytes\n", (long)sizeof(smallFCD));
        printf("size of binary data file contents:  %5ld bytes\n", (long)totalSize);
        printf("minDecompNoCodePoint:              U+%04lX\n", (long)indexes[Normalizer2Impl::IX_MIN_DECOMP_NO_CP]);
        printf("minCompNoMaybeCodePoint:           U+%04lX\n", (long)indexes[Normalizer2Impl::IX_MIN_COMP_NO_MAYBE_CP]);
        printf("minYesNo:                          0x%04x\n", (int)indexes[Normalizer2Impl::IX_MIN_YES_NO]);
        printf("minYesNoMappingsOnly:              0x%04x\n", (int)indexes[Normalizer2Impl::IX_MIN_YES_NO_MAPPINGS_ONLY]);
        // TODO: minMappingNotCompYes, minMappingNoCompBoundaryBefore
        printf("minNoNo:                           0x%04x\n", (int)indexes[Normalizer2Impl::IX_MIN_NO_NO]);
        printf("limitNoNo:                         0x%04x\n", (int)indexes[Normalizer2Impl::IX_LIMIT_NO_NO]);
        printf("minMaybeYes:                       0x%04x\n", (int)indexes[Normalizer2Impl::IX_MIN_MAYBE_YES]);
    }

    UVersionInfo nullVersion={ 0, 0, 0, 0 };
    if(0==memcmp(nullVersion, unicodeVersion, 4)) {
        u_versionFromString(unicodeVersion, U_UNICODE_VERSION);
    }
    memcpy(dataInfo.dataVersion, unicodeVersion, 4);
}

void Normalizer2DataBuilder::writeBinaryFile(const char *filename) {
    processData();

    IcuToolErrorCode errorCode("gennorm2/writeBinaryFile()");
    LocalArray<uint8_t> norm16TrieBytes(new uint8_t[norm16TrieLength]);
    utrie2_serialize(norm16Trie, norm16TrieBytes.getAlias(), norm16TrieLength, errorCode);
    errorCode.assertSuccess();

    UNewDataMemory *pData=
        udata_create(NULL, NULL, filename, &dataInfo,
                     haveCopyright ? U_COPYRIGHT_STRING : NULL, errorCode);
    if(errorCode.isFailure()) {
        fprintf(stderr, "gennorm2 error: unable to create the output file %s - %s\n",
                filename, errorCode.errorName());
        exit(errorCode.reset());
    }
    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, norm16TrieBytes.getAlias(), norm16TrieLength);
    udata_writeUString(pData, toUCharPtr(extraData.getBuffer()), extraData.length());
    udata_writeBlock(pData, smallFCD, sizeof(smallFCD));
    int32_t writtenSize=udata_finish(pData, errorCode);
    if(errorCode.isFailure()) {
        fprintf(stderr, "gennorm2: error %s writing the output file\n", errorCode.errorName());
        exit(errorCode.reset());
    }
    int32_t totalSize=indexes[Normalizer2Impl::IX_TOTAL_SIZE];
    if(writtenSize!=totalSize) {
        fprintf(stderr, "gennorm2 error: written size %ld != calculated size %ld\n",
            (long)writtenSize, (long)totalSize);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}

void
Normalizer2DataBuilder::writeCSourceFile(const char *filename) {
    processData();

    IcuToolErrorCode errorCode("gennorm2/writeCSourceFile()");
    const char *basename=findBasename(filename);
    CharString path(filename, (int32_t)(basename-filename), errorCode);
    CharString dataName(basename, errorCode);
    const char *extension=strrchr(basename, '.');
    if(extension!=NULL) {
        dataName.truncate((int32_t)(extension-basename));
    }
    errorCode.assertSuccess();

    LocalArray<uint8_t> norm16TrieBytes(new uint8_t[norm16TrieLength]);
    utrie2_serialize(norm16Trie, norm16TrieBytes.getAlias(), norm16TrieLength, errorCode);
    errorCode.assertSuccess();

    FILE *f=usrc_create(path.data(), basename, "icu/source/tools/gennorm2/n2builder.cpp");
    if(f==NULL) {
        fprintf(stderr, "gennorm2/writeCSourceFile() error: unable to create the output file %s\n",
                filename);
        exit(U_FILE_ACCESS_ERROR);
        return;
    }
    fputs("#ifdef INCLUDED_FROM_NORMALIZER2_CPP\n\n", f);
    char line[100];
    sprintf(line, "static const UVersionInfo %s_formatVersion={", dataName.data());
    usrc_writeArray(f, line, dataInfo.formatVersion, 8, 4, "};\n");
    sprintf(line, "static const UVersionInfo %s_dataVersion={", dataName.data());
    usrc_writeArray(f, line, dataInfo.dataVersion, 8, 4, "};\n\n");
    sprintf(line, "static const int32_t %s_indexes[Normalizer2Impl::IX_COUNT]={\n",
            dataName.data());
    usrc_writeArray(f,
        line,
        indexes, 32, Normalizer2Impl::IX_COUNT,
        "\n};\n\n");
    sprintf(line, "static const uint16_t %s_trieIndex[%%ld]={\n", dataName.data());
    usrc_writeUTrie2Arrays(f,
        line, NULL,
        norm16Trie,
        "\n};\n\n");
    sprintf(line, "static const uint16_t %s_extraData[%%ld]={\n", dataName.data());
    usrc_writeArray(f,
        line,
        extraData.getBuffer(), 16, extraData.length(),
        "\n};\n\n");
    sprintf(line, "static const uint8_t %s_smallFCD[%%ld]={\n", dataName.data());
    usrc_writeArray(f,
        line,
        smallFCD, 8, sizeof(smallFCD),
        "\n};\n\n");
    sprintf(line, "static const UTrie2 %s_trie={\n", dataName.data());
    char line2[100];
    sprintf(line2, "%s_trieIndex", dataName.data());
    usrc_writeUTrie2Struct(f,
        line,
        norm16Trie, line2, NULL,
        "};\n");
    fputs("\n#endif  // INCLUDED_FROM_NORMALIZER2_CPP\n", f);
    fclose(f);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_NORMALIZATION */

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 */
